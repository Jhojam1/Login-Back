# Servicio de autenticación con roles y permisos

Sistema de login completo: **Spring Boot 3.5 + JWT + PostgreSQL** en el backend y
**React 19 + TypeScript + Tailwind** en el frontend. Pensado para ser la única fuente de
verdad de identidad de un conjunto de microservicios: los demás servicios no guardan
contraseñas, le preguntan a este si un token es válido.

---

## Documentación

| Documento | Para qué sirve |
|---|---|
| [**Guía de despliegue en AWS**](docs/GUIA-AWS-PASO-A-PASO.md) | Paso a paso desde cero: EC2, RDS, Nginx, systemd. Empieza aquí para subirlo a la nube. |
| [**Contrato de integración**](docs/CONTRATO-INTEGRACION.md) | Pásaselo al equipo del servicio de S3. Explica cómo validar tokens con ejemplos en Java, Node y Python. |
| [**Esquema de base de datos**](docs/ESQUEMA-BASE-DATOS.md) | Diagrama, DDL de referencia y consultas SQL útiles. |

---

## Qué incluye

**Autenticación**
- Registro y login (se puede entrar con el usuario **o** con el correo)
- Token de acceso JWT de vida corta (15 min) + token de refresco revocable (7 días)
- **Rotación de tokens de refresco**: al renovar, el anterior se invalida
- Renovación automática y transparente desde el frontend
- Cierre de sesión en un dispositivo o en todos
- Cambio de contraseña (que cierra todas las sesiones abiertas)

**Autorización**
- Modelo RBAC de tres niveles: usuario → roles → permisos
- 3 roles y 11 permisos predefinidos, sincronizados en cada arranque
- Panel para crear usuarios eligiendo sus roles
- Panel para crear roles a medida combinando los permisos existentes
- **Menú de navegación gestionable desde el panel**, guardado en base de datos
- **Permisos de solo interfaz** creables al momento, sin recompilar ni reiniciar
- Los roles del sistema y los que estén en uso no se pueden borrar
- Los roles y permisos viajan dentro del token
- Autorización declarativa en el backend con `@PreAuthorize`
- La interfaz oculta lo que no puedes usar (`<PermissionGate>`), pero quien decide es siempre el backend

**Seguridad**
- Contraseñas con BCrypt (coste 12)
- Bloqueo temporal de la cuenta tras 5 intentos fallidos
- Política de contraseñas validada en cliente y servidor
- Mensajes de error genéricos en el login, para no revelar qué cuentas existen
- Secretos siempre por variable de entorno, nunca en el código
- Endpoint de introspección protegido con clave compartida entre servicios
- Revocación inmediata: desactivar una cuenta o quitar un rol expulsa al momento

**Integración entre servicios**
- `POST /api/v1/auth/introspect` para que otros microservicios validen tokens
- Permisos `FILE_UPLOAD`, `FILE_READ` y `FILE_DELETE` ya preparados para el servicio de S3

---

## Arranque rápido en local

### Requisitos
Java 21 · Maven 3.9+ · Node 20+

### Backend

```bash
cd backend
mvn spring-boot:run
```

Arranca en `http://localhost:8080` con el perfil `dev`, que usa una base de datos
**H2 en memoria**: no hay que instalar nada. Los datos se borran al apagar.

- API: `http://localhost:8080/api/v1`
- Documentación interactiva: `http://localhost:8080/swagger-ui.html`
- Explorador de la base de datos: `http://localhost:8080/h2-console`
  (JDBC URL: `jdbc:h2:mem:authdb`, usuario `sa`, sin contraseña)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173`. Vite redirige `/api` al backend, así que no hay problemas de CORS.

### Con PostgreSQL (igual que en AWS)

Las credenciales de la base de datos van en un archivo `backend/.env`, que **no se sube
a Git**. El código no contiene ninguna contraseña: solo lee esas variables.

```powershell
# 1. Un PostgreSQL local (requiere Docker Desktop abierto)
docker compose up -d

# 2. Crea tu archivo de configuración a partir de la plantilla
cd backend
copy .env.example .env

# 3. Arranca: el .env ya activa el perfil prod y apunta al PostgreSQL local
mvn spring-boot:run
```

Para usar la base de datos de AWS RDS en lugar de la local, cambia en `.env` los valores
`DB_HOST`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` por los de RDS. Para volver a H2,
pon `SPRING_PROFILES_ACTIVE=dev` o borra el `.env`.

En el servidor EC2 se usa el mismo mecanismo; está explicado en el paso 8.2 de la
[guía de AWS](docs/GUIA-AWS-PASO-A-PASO.md).

### Credenciales de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin.2026` | `ROLE_ADMIN` |

Cualquier cuenta que te crees desde el registro recibe `ROLE_USER`.

---

## Estructura del proyecto

```
Login/
├── backend/                         Spring Boot 3.5 · Java 21
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/proyecto/auth/
│       │   ├── AuthServiceApplication.java
│       │   ├── config/              Seguridad, CORS, Swagger, siembra de datos
│       │   ├── controller/          Endpoints REST (solo orquestan)
│       │   ├── domain/
│       │   │   ├── entity/          Entidades JPA
│       │   │   └── enums/           Catálogo de roles y permisos
│       │   ├── dto/                 Records de entrada y salida
│       │   ├── exception/           Excepciones + manejador global
│       │   ├── mapper/              Entidad → DTO
│       │   ├── repository/          Spring Data JPA
│       │   ├── security/            JWT, filtro, UserDetails
│       │   ├── service/             Interfaces de los casos de uso
│       │   │   └── impl/            Sus implementaciones
│       │   └── util/
│       └── resources/
│           ├── application.yml      Común
│           ├── application-dev.yml  H2 en memoria
│           └── application-prod.yml PostgreSQL (AWS RDS)
│
├── frontend/                        React 19 · TypeScript · Tailwind 4
│   └── src/
│       ├── api/                     Cliente HTTP e interceptores
│       ├── components/
│       │   ├── layout/              Estructura de las pantallas
│       │   ├── roles/               Formulario de crear y editar roles
│       │   ├── users/               Diálogos de crear usuario y asignar roles
│       │   └── ui/                  Botón, campo, aviso, diálogo
│       ├── context/                 Estado global de la sesión
│       ├── hooks/
│       ├── pages/                   Login, registro, inicio, usuarios, roles, cuenta
│       ├── routes/                  Rutas y guards de permisos
│       ├── styles/                  Sistema de diseño
│       ├── types/                   Contrato con el backend
│       └── utils/                   Traducción de códigos a texto legible
│
└── docs/                            Guías de despliegue e integración
```

---

## Endpoints

### Públicos

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Crear una cuenta (recibe `ROLE_USER`) |
| `POST` | `/api/v1/auth/login` | Iniciar sesión y obtener los tokens |
| `POST` | `/api/v1/auth/refresh` | Renovar el token de acceso |
| `POST` | `/api/v1/auth/introspect` | **Validar un token** (lo usan otros microservicios) |

### Requieren sesión

| Método | Ruta | Permiso |
|---|---|---|
| `GET` | `/api/v1/auth/me` | — |
| `POST` | `/api/v1/auth/logout` | — |
| `POST` | `/api/v1/auth/logout-all` | — |
| `POST` | `/api/v1/auth/change-password` | — |
| `POST` | `/api/v1/users` | `USER_CREATE` |
| `GET` | `/api/v1/users` | `USER_READ` |
| `GET` | `/api/v1/users/{id}` | `USER_READ` |
| `PUT` | `/api/v1/users/{id}` | `USER_UPDATE` |
| `PATCH` | `/api/v1/users/{id}/status` | `USER_UPDATE` |
| `PUT` | `/api/v1/users/{id}/roles` | `ROLE_ASSIGN` |
| `DELETE` | `/api/v1/users/{id}` | `USER_DELETE` |
| `GET` | `/api/v1/roles` | `ROLE_READ` |
| `POST` | `/api/v1/roles` | `ROLE_WRITE` |
| `PUT` | `/api/v1/roles/{id}` | `ROLE_WRITE` |
| `DELETE` | `/api/v1/roles/{id}` | `ROLE_WRITE` |
| `GET` | `/api/v1/permissions` | `PERMISSION_READ` |
| `POST` | `/api/v1/permissions` | `PERMISSION_WRITE` |
| `DELETE` | `/api/v1/permissions/{id}` | `PERMISSION_WRITE` |
| `GET` | `/api/v1/menu` | — (el de cada uno) |
| `GET` | `/api/v1/menu/all` | `MENU_WRITE` |
| `POST` | `/api/v1/menu` | `MENU_WRITE` |
| `PUT` | `/api/v1/menu/{id}` | `MENU_WRITE` |
| `DELETE` | `/api/v1/menu/{id}` | `MENU_WRITE` |

### Qué se puede administrar desde la interfaz

| | Crear | Editar | Eliminar |
|---|---|---|---|
| Usuarios | sí | sí (datos, estado, roles) | sí |
| Roles | sí | sí (descripción y permisos) | sí, salvo los tres del sistema |
| Entradas del menú | sí | sí (texto, ruta, permiso, orden) | sí, salvo las del sistema |
| Permisos de interfaz | sí | — | sí, si nada los usa |
| Permisos del backend | no | no | no |

Al crear un usuario desde el panel sí se eligen sus roles; el registro público nunca
los acepta (usa un DTO que no tiene ese campo), así que no hay forma de auto-asignarse
`ROLE_ADMIN` desde el formulario abierto.

---

## Las dos clases de permiso

Esta distinción es la decisión de diseño más importante del sistema de autorización, y
conviene tenerla clara antes de añadir un permiso.

| | Permiso del backend | Permiso de solo interfaz |
|---|---|---|
| Dónde se declara | enum `PermissionName.java` | se crea desde el panel |
| Quién lo comprueba | el servidor, con `@PreAuthorize` | el frontend, por nombre |
| Qué protege | una operación real de la API | solo lo que se muestra |
| Añadir uno nuevo | editar el enum y reiniciar | desde la interfaz, al momento |
| Campo `uiOnly` | `false` | `true` |

**Por qué no se pueden crear permisos del backend desde el panel:** un permiso solo
protege algo si hay código que lo comprueba. Crear el nombre `FILE_DELETE_ALL` en la base
de datos no hace que aparezca ningún `@PreAuthorize` que lo exija, así que daría una
falsa sensación de seguridad. El servicio rechaza además los nombres que coincidan con
uno del enum, para que no haya dos cosas distintas con el mismo nombre.

**Por qué sí se pueden crear los de interfaz:** el filtrado del frontend es genérico —
compara por nombre cualquier permiso que venga en el token— así que un nombre nuevo
funciona sin recompilar nada. Lo que no hace es autorizar nada en el servidor.

```
Crear "Reportes" en el menú, de principio a fin y sin tocar código:

1. Roles → Nuevo de interfaz        → crea REPORT_VIEW
2. Roles → Nuevo rol "Analista"     → le marca REPORT_VIEW
3. Usuarios → Roles                 → asigna Analista a quien corresponda
4. Menú → Nueva entrada             → "Reportes", /panel/reportes, REPORT_VIEW

Esa persona ve "Reportes" en su menú la próxima vez que cargue. Nadie más lo ve.
```

**El menú vive en la base de datos.** La tabla `menu_items` guarda el texto, la ruta, el
orden y el permiso de cada entrada, y el backend ya devuelve el menú filtrado en
`GET /api/v1/menu`. El frontend solo lo pinta, así que reordenar o esconder secciones no
requiere desplegar nada.

Una limitación honesta: **crear la entrada crea el enlace, no la pantalla.** Si la ruta no
corresponde a una sección programada, quien entre verá un aviso de "sección sin
contenido" con la ruta y dónde añadirla, en lugar de un 404.

---

## Roles y permisos

| Rol | Permisos |
|---|---|
| `ROLE_ADMIN` | todos (13) |
| `ROLE_MANAGER` | `USER_READ` `USER_CREATE` `USER_UPDATE` `ROLE_READ` `FILE_UPLOAD` `FILE_READ` |
| `ROLE_USER` | `USER_READ` `FILE_UPLOAD` `FILE_READ` |

Los roles se crean y editan desde el panel. **Para añadir un permiso que el backend
verifique** se tocan dos archivos y al reiniciar la base de datos se actualiza sola:

1. `backend/.../domain/enums/PermissionName.java` → añade la constante
2. `backend/.../domain/enums/RoleName.java` → asígnala a los roles que corresponda

Para un permiso que solo controle lo que se ve, no hace falta nada de esto: se crea desde
la pantalla de Roles.

---

## Cómo funciona la sesión

```
1. LOGIN
   El frontend envía usuario y contraseña.
   El backend comprueba el hash BCrypt y devuelve:
     · accessToken  (JWT, 15 min, contiene roles y permisos)
     · refreshToken (valor aleatorio, 7 días, guardado en base de datos)

2. CADA PETICIÓN
   El frontend añade:  Authorization: Bearer <accessToken>
   JwtAuthenticationFilter verifica la firma, recarga el usuario de la base de
   datos y rellena el contexto de seguridad. Después, @PreAuthorize decide.

3. EL TOKEN CADUCA (a los 15 minutos)
   El backend responde 401.
   El interceptor de axios lo detecta, llama a /auth/refresh y reintenta la
   petición original. La persona no nota nada.
   Si llegan varias peticiones a la vez, se encolan: solo se hace UN refresco.

4. LOGOUT
   Se revoca el refreshToken en base de datos. El accessToken sigue siendo
   válido como mucho 15 minutos más, y ya no se puede renovar.
```

**Por qué dos tokens:** un JWT no se puede invalidar antes de que caduque. Si solo
hubiera un token de larga duración, no habría forma de cerrar sesión de verdad. Con este
esquema, el token de larga duración sí vive en la base de datos y se puede revocar al
instante; el de vida corta se valida sin consultar nada, lo que mantiene el sistema rápido
y capaz de escalar a varias instancias.

---

## Decisiones de diseño

**Servicios con interfaz + implementación**
Los controladores dependen de `AuthService`, no de `AuthServiceImpl`. Permite sustituir
la implementación en los tests sin levantar la base de datos (inversión de dependencias).

**Las entidades JPA no salen nunca por la API**
Todo pasa por un DTO (`record`). Así `password` no puede filtrarse por descuido y el
contrato HTTP no queda atado al modelo de base de datos.

**`User` no implementa `UserDetails`**
La adaptación a Spring Security está en `UserPrincipal`. El modelo de dominio se mantiene
libre del framework de seguridad.

**Toda la manipulación de JWT está en `JwtTokenProvider`**
Ninguna otra clase usa la librería JJWT. Cambiar de HMAC a RSA afecta a un solo archivo.

**Un único formato de error**
`GlobalExceptionHandler` convierte cualquier excepción en la misma estructura JSON, así
el frontend escribe un solo manejador (`getErrorMessage`) en lugar de adivinar.

**El contador de intentos fallidos está en la base de datos**
Si estuviera en memoria, bastaría reiniciar la aplicación o alternar entre instancias
para esquivar la protección contra fuerza bruta.

**La introspección consulta la base de datos**
Sería más rápido dejar que cada servicio verificase la firma por su cuenta, pero entonces
no detectarían que un administrador acaba de desactivar una cuenta. Se prefiere que los
cambios de permisos surtan efecto de inmediato.

---

## Variables de entorno

Todas tienen un valor por defecto para desarrollo. En producción hay que definir al menos
las marcadas con ⚠️.

| Variable | Por defecto | Para qué |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (H2) o `prod` (PostgreSQL) |
| ⚠️ `APP_JWT_SECRET` | clave de desarrollo | Firma de los tokens. Mínimo 32 caracteres |
| `APP_JWT_ACCESS_MS` | `900000` | Vida del token de acceso (15 min) |
| `APP_JWT_REFRESH_MS` | `604800000` | Vida del token de refresco (7 días) |
| ⚠️ `DB_HOST` | — | Endpoint de RDS |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| ⚠️ `DB_NAME` | — | Nombre de la base de datos |
| ⚠️ `DB_USERNAME` | — | Usuario de la base de datos |
| ⚠️ `DB_PASSWORD` | — | Contraseña de la base de datos |
| `APP_ALLOWED_ORIGINS` | `localhost:5173,localhost:3000` | Orígenes permitidos por CORS |
| ⚠️ `APP_SERVICE_API_KEY` | *(vacío)* | Clave que deben enviar los microservicios aliados. **Si está vacía, la introspección queda abierta** |
| ⚠️ `APP_ADMIN_PASSWORD` | `Admin.2026` | Contraseña del administrador inicial |
| `DDL_AUTO` | `update` | `update` la primera vez, `validate` después |

Genera los secretos con:

```bash
openssl rand -base64 48
```

---

## Antes de entregar o publicar

- [ ] `APP_JWT_SECRET` generado al azar y fuera del repositorio
- [ ] `APP_SERVICE_API_KEY` definida (si está vacía, cualquiera puede introspeccionar)
- [ ] `APP_ADMIN_PASSWORD` cambiada
- [ ] `DDL_AUTO=validate` una vez que las tablas existen
- [ ] RDS con **Public access: No** y su grupo de seguridad apuntando al del EC2
- [ ] Puerto 8080 cerrado en el grupo de seguridad (todo pasa por Nginx)
- [ ] HTTPS configurado si el proyecto sale de un entorno de clase
  (certificado gratuito con `sudo certbot --nginx`, requiere un dominio)

---

## Stack

**Backend** · Java 21 · Spring Boot 3.5 · Spring Security 6 · Spring Data JPA ·
Hibernate · JJWT 0.12 · Lombok · PostgreSQL / H2 · SpringDoc OpenAPI

**Frontend** · React 19 · TypeScript 5.8 · Vite 6 · Tailwind CSS 4 · React Router 7 · Axios
