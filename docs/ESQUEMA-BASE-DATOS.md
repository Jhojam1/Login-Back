# Esquema de la base de datos

Hibernate crea estas tablas automáticamente al arrancar (`DDL_AUTO=update`), así que
**no hace falta ejecutar nada a mano**. Este documento existe para dos cosas: entender
el modelo y poder crearlo manualmente si alguna vez hace falta.

---

## 1. Diagrama

```
      ┌──────────────────────┐
      │       users          │
      ├──────────────────────┤
      │ id              PK   │
      │ username        UQ   │
      │ email           UQ   │
      │ password             │◀── hash BCrypt, nunca texto plano
      │ first_name           │
      │ last_name            │
      │ enabled              │
      │ account_locked       │
      │ failed_attempts      │
      │ lock_time            │
      │ last_login_at        │
      │ created_at           │
      │ updated_at           │
      └──────┬───────────────┘
             │ 1
             │
             │ N                      ┌──────────────────────┐
      ┌──────▼───────────────┐        │   refresh_tokens     │
      │     user_roles       │        ├──────────────────────┤
      ├──────────────────────┤        │ id              PK   │
      │ user_id         FK   │        │ token           UQ   │
      │ role_id         FK   │        │ user_id         FK   │──┐
      └──────┬───────────────┘        │ expires_at           │  │
             │ N                      │ revoked              │  │
             │                        │ ip_address           │  │
             │ 1                      │ user_agent           │  │
      ┌──────▼───────────────┐        │ created_at           │  │
      │       roles          │        │ updated_at           │  │
      ├──────────────────────┤        └──────────────────────┘  │
      │ id              PK   │                                  │
      │ name            UQ   │   users.id ──────────────────────┘
      │ description          │
      │ created_at           │
      │ updated_at           │
      └──────┬───────────────┘
             │ 1
             │
             │ N
      ┌──────▼───────────────┐
      │   role_permissions   │
      ├──────────────────────┤
      │ role_id         FK   │
      │ permission_id   FK   │
      └──────┬───────────────┘
             │ N
             │
             │ 1
      ┌──────▼───────────────┐
      │    permissions       │
      ├──────────────────────┤
      │ id              PK   │
      │ name            UQ   │
      │ description          │
      │ ui_only              │◀── true = solo decide qué muestra la interfaz
      │ created_at           │
      │ updated_at           │
      └──────────────────────┘


      ┌──────────────────────┐
      │     menu_items       │   (sin relación formal: el permiso
      ├──────────────────────┤    se guarda por nombre, no por FK)
      │ id              PK   │
      │ label                │
      │ path                 │
      │ required_permission  │···▶ permissions.name
      │ display_order        │
      │ visible              │
      │ system_item          │◀── las del sistema no se pueden borrar
      │ created_at           │
      │ updated_at           │
      └──────────────────────┘
```

**PK** = clave primaria · **FK** = clave externa · **UQ** = valor único

---

## 2. Por qué el modelo es así

### Usuario → Rol → Permiso (en lugar de Usuario → Permiso)

Este modelo se llama **RBAC** (*Role Based Access Control*). La alternativa sería dar
permisos directamente a cada persona, pero entonces, para cambiar lo que puede hacer un
grupo de 50 personas, habría que editar 50 filas. Con roles se edita **una**.

Las dos tablas intermedias (`user_roles` y `role_permissions`) existen porque las
relaciones son **de muchos a muchos**: una persona puede tener varios roles y un rol
puede estar en muchas personas.

### `refresh_tokens` es una tabla, pero el token de acceso no

Esta es la decisión de diseño más importante del sistema:

| | Token de acceso (JWT) | Token de refresco |
|---|---|---|
| ¿Se guarda en base de datos? | **No** | **Sí** |
| Duración | 15 minutos | 7 días |
| Cómo se valida | verificando la firma | buscándolo en la tabla |
| ¿Se puede revocar? | no, hay que esperar que caduque | **sí, al instante** |
| Contenido | usuario, roles y permisos | nada, es un valor aleatorio |

El token de acceso no se guarda a propósito: así validarlo no cuesta ni una consulta,
y el sistema escala a varias instancias EC2 sin compartir estado.

El problema de ese diseño es que un JWT no se puede invalidar antes de que caduque. Por
eso existe el token de refresco **en base de datos**: cuando alguien cierra sesión,
cambia su contraseña o un administrador lo desactiva, se marca `revoked = true` y esa
persona no podrá renovar su acceso. En el peor caso mantiene acceso 15 minutos, no 7 días.

### `menu_items` está en la base de datos, no en el código del frontend

Qué secciones aparecen en el menú, en qué orden y quién las ve es **configuración, no
lógica**: cambia a menudo y no debería exigir un despliegue. Guardarlo aquí permite que
un administrador añada una sección desde el panel y la vea aparecer en la siguiente
recarga.

Funciona porque el filtrado del frontend es genérico: compara `required_permission` con
la lista de permisos del token, sea cual sea el nombre. Por eso una entrada nueva con un
permiso nuevo no necesita ni una línea de React.

`required_permission` se guarda **como texto y no como clave externa** a propósito. El
menú no debería caerse por un problema de integridad referencial, y las dos protecciones
que importan ya están en el servicio: al guardar una entrada se comprueba que el permiso
exista, y al borrar un permiso se comprueba que ninguna entrada lo use.

### `permissions.ui_only` separa dos cosas que se confunden

| `ui_only` | Lo declara | Lo comprueba | Protege |
|---|---|---|---|
| `false` | el enum `PermissionName` | el servidor (`@PreAuthorize`) | una operación real |
| `true` | un administrador, desde el panel | el frontend, por nombre | solo lo que se muestra |

La columna existe para que la diferencia quede explícita en los datos y no dependa de que
alguien la recuerde. El servicio no deja crear un permiso de interfaz cuyo nombre
coincida con uno del enum, ni borrar desde el panel uno que el backend verifique.

### Las columnas de bloqueo están en `users`, no en memoria

`failed_attempts`, `account_locked` y `lock_time` viven en la tabla porque el bloqueo
debe sobrevivir a un reinicio de la aplicación y funcionar aunque haya varias instancias
detrás de un balanceador. Si el contador estuviera en memoria, bastaría reiniciar (o
alternar entre instancias) para esquivar la protección contra fuerza bruta.

---

## 3. Datos que se insertan solos al arrancar

La clase `DataSeeder` inserta esto de forma **idempotente** (puede ejecutarse muchas
veces sin duplicar nada):

### Permisos (13)

| Nombre | Descripción |
|---|---|
| `USER_READ` | Consultar usuarios |
| `USER_CREATE` | Crear usuarios |
| `USER_UPDATE` | Modificar usuarios |
| `USER_DELETE` | Eliminar usuarios |
| `ROLE_READ` | Consultar roles |
| `ROLE_WRITE` | Crear y modificar roles |
| `ROLE_ASSIGN` | Asignar roles a usuarios |
| `PERMISSION_READ` | Consultar el catálogo de permisos |
| `PERMISSION_WRITE` | Crear permisos de interfaz |
| `MENU_WRITE` | Gestionar el menú de navegación |
| `FILE_UPLOAD` | Subir archivos al almacenamiento S3 |
| `FILE_READ` | Listar y descargar archivos de S3 |
| `FILE_DELETE` | Eliminar archivos de S3 |

### Roles (3) y qué permisos concede cada uno

| Rol | Permisos |
|---|---|
| `ROLE_ADMIN` | **todos** (los 13) |
| `ROLE_MANAGER` | `USER_READ`, `USER_CREATE`, `USER_UPDATE`, `ROLE_READ`, `FILE_UPLOAD`, `FILE_READ` |
| `ROLE_USER` | `USER_READ`, `FILE_UPLOAD`, `FILE_READ` |

### Entradas del menú

Se crean las cinco básicas (Inicio, Usuarios, Roles, Menú, Cuenta) marcadas como
`system_item`, para que no se puedan borrar y la aplicación nunca quede sin navegación.
Sí se pueden renombrar, reordenar y ocultar desde el panel. Solo se insertan si falta la
ruta, así que un reinicio no duplica nada ni revierte los cambios del administrador.

### Usuario administrador

Se crea uno solo la primera vez, con los valores de `APP_ADMIN_*`. Si ya existe un
usuario con ese nombre, **no se toca su contraseña** (para no revertir un cambio hecho
desde la aplicación).

> La relación rol → permisos **se sincroniza en cada arranque**. Si en una versión futura
> añadís un permiso a `ROLE_ADMIN` en el enum `RoleName.java`, el despliegue lo aplica
> solo: no hay que ejecutar SQL en el servidor.

---

## 4. SQL de referencia (PostgreSQL)

Solo lo necesitáis si queréis crear el esquema a mano, o si vuestro profesor pide el DDL.

```sql
-- ============================================================
--  Tablas principales
-- ============================================================

CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(180),
    -- true: solo decide qué muestra la interfaz; false: lo verifica el backend
    ui_only     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ
);

CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(180),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ
);

CREATE TABLE users (
    id              BIGSERIAL     PRIMARY KEY,
    username        VARCHAR(50)   NOT NULL UNIQUE,
    email           VARCHAR(120)  NOT NULL UNIQUE,
    password        VARCHAR(100)  NOT NULL,
    first_name      VARCHAR(80),
    last_name       VARCHAR(80),
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    account_locked  BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER       NOT NULL DEFAULT 0,
    lock_time       TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ
);

-- ============================================================
--  Tablas de relación (muchos a muchos)
-- ============================================================

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id)       ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
--  Tokens de refresco
-- ============================================================

CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(512) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ
);

-- ============================================================
--  Menú de navegación
-- ============================================================

CREATE TABLE menu_items (
    id                  BIGSERIAL    PRIMARY KEY,
    label               VARCHAR(60)  NOT NULL,
    path                VARCHAR(180) NOT NULL,
    -- Nombre del permiso necesario, o NULL si la ve cualquier usuario autenticado.
    -- Es texto, no clave externa: ver la explicación en el apartado 2.
    required_permission VARCHAR(60),
    display_order       INTEGER      NOT NULL DEFAULT 0,
    visible             BOOLEAN      NOT NULL DEFAULT TRUE,
    system_item         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ
);

-- ============================================================
--  Índices
--  Los de username/email/token los crea Hibernate por las
--  anotaciones @Index; este acelera buscar las sesiones de
--  un usuario al revocarlas todas de golpe.
-- ============================================================

CREATE INDEX idx_refresh_token_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_exp  ON refresh_tokens(expires_at);
CREATE INDEX idx_menu_order         ON menu_items(display_order);
```

---

## 5. Consultas útiles para depurar

Conéctate desde el servidor EC2:

```bash
sudo apt install -y postgresql-client
psql -h <DB_HOST> -U authadmin -d authdb
```

```sql
-- Ver todos los usuarios con sus roles
SELECT u.username, u.email, u.enabled, u.account_locked,
       STRING_AGG(r.name, ', ') AS roles
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r       ON r.id = ur.role_id
GROUP BY u.id, u.username, u.email, u.enabled, u.account_locked
ORDER BY u.id;

-- Ver los permisos efectivos de una persona
SELECT DISTINCT p.name
FROM users u
JOIN user_roles ur       ON ur.user_id = u.id
JOIN role_permissions rp ON rp.role_id = ur.role_id
JOIN permissions p       ON p.id = rp.permission_id
WHERE u.username = 'jhojam'
ORDER BY p.name;

-- Sesiones activas ahora mismo
SELECT u.username, rt.ip_address, rt.created_at, rt.expires_at
FROM refresh_tokens rt
JOIN users u ON u.id = rt.user_id
WHERE rt.revoked = FALSE AND rt.expires_at > NOW()
ORDER BY rt.created_at DESC;

-- Desbloquear una cuenta sin esperar los 15 minutos
UPDATE users
SET account_locked = FALSE, failed_attempts = 0, lock_time = NULL
WHERE username = 'admin';

-- Convertir a alguien en administrador a mano
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'jhojam' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Cerrar todas las sesiones de todo el mundo (por si hay un incidente)
UPDATE refresh_tokens SET revoked = TRUE WHERE revoked = FALSE;
```

> ⚠️ Tras cambiar roles o permisos con SQL directo, esa persona seguirá teniendo el token
> viejo hasta 15 minutos. Para expulsarla ya, revoca también sus tokens de refresco.
> Si lo hacéis desde la aplicación (`PUT /api/v1/users/{id}/roles`), eso ya ocurre solo.
