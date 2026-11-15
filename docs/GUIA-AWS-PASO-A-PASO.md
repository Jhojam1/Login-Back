# Guía paso a paso: desplegar el servicio de login en AWS

> Para alguien que **nunca** ha usado AWS. Cada paso dice exactamente dónde hacer clic
> y qué escribir. Si algo falla, al final hay una sección de problemas comunes.

**Tiempo estimado:** 2–3 horas la primera vez.

---

## Índice

1. [Qué vamos a construir](#1-qué-vamos-a-construir)
2. [Antes de empezar](#2-antes-de-empezar)
3. [Crear los grupos de seguridad (el cortafuegos)](#3-crear-los-grupos-de-seguridad-el-cortafuegos)
4. [Crear la base de datos (RDS PostgreSQL)](#4-crear-la-base-de-datos-rds-postgresql)
5. [Crear el servidor (EC2)](#5-crear-el-servidor-ec2)
6. [Subir el repositorio al servidor](#6-subir-el-repositorio-al-servidor)
7. [Preparar el servidor e instalar Java](#7-preparar-el-servidor-e-instalar-java)
8. [Desplegar el backend como servicio](#8-desplegar-el-backend-como-servicio)
9. [Desplegar el frontend con Nginx](#9-desplegar-el-frontend-con-nginx)
10. [Conectar con el equipo de S3 y el otro EC2](#10-conectar-con-el-equipo-de-s3-y-el-otro-ec2)
11. [Comprobaciones finales](#11-comprobaciones-finales)
12. [Actualizar el código más adelante](#12-actualizar-el-código-más-adelante)
13. [Problemas comunes](#13-problemas-comunes)
14. [Costes y cómo no gastar de más](#14-costes-y-cómo-no-gastar-de-más)

---

## 1. Qué vamos a construir

```
                          Internet
                              │
                              ▼
         ┌────────────────────────────────────────┐
         │   EC2 "auth-server"  (VUESTRO equipo)  │
         │                                        │
         │   Nginx  :80                           │
         │     ├── /          → React (frontend)  │
         │     └── /api/      → Spring Boot :8080 │
         └───────────────┬────────────────────────┘
                         │ (red privada, puerto 5432)
                         ▼
         ┌────────────────────────────────────────┐
         │   RDS PostgreSQL "auth-db"             │
         │   usuarios · roles · permisos · tokens │
         └────────────────────────────────────────┘

         ┌────────────────────────────────────────┐
         │   EC2 del OTRO equipo (servicio S3)    │
         │                                        │
         │   Para saber si un usuario es válido   │
         │   llama a:                             │
         │   POST http://<IP-vuestra>/api/v1/auth/introspect │
         └────────────────────────────────────────┘
```

**Piezas que vais a crear:**

| Pieza | Servicio AWS | Para qué sirve |
|---|---|---|
| `auth-db` | RDS PostgreSQL | Guardar usuarios, roles, permisos y tokens |
| `auth-server` | EC2 (Ubuntu) | Ejecutar Spring Boot + servir el React |
| 3 grupos de seguridad | VPC | Controlar quién puede hablar con quién |

> **Nota sobre el bucket S3:** el bucket de archivos lo crea el **otro equipo**, no vosotros.
> Vuestra única relación con ellos es el endpoint de introspección (paso 10).

---

## 2. Antes de empezar

### 2.1 Cuenta de AWS

1. Entra en <https://aws.amazon.com/free/> y crea una cuenta (necesitas tarjeta, pero
   todo lo de esta guía entra en la **capa gratuita** si eliges los tamaños indicados).
2. Una vez dentro, arriba a la derecha **elige una región y no la cambies nunca más**.
   Recomendado: **`us-east-1` (N. Virginia)** — es la más barata y la que más servicios tiene.

> ⚠️ El error nº1 de quien empieza: crear la base de datos en una región y el servidor
> en otra. Luego no se ven entre sí. **Todo en la misma región.**

### 2.2 Crear el par de claves (para entrar al servidor por SSH)

1. Busca **EC2** en la barra superior → entra.
2. Menú izquierdo → **Network & Security** → **Key Pairs**.
3. Botón **Create key pair**:
   - **Name:** `auth-key`
   - **Key pair type:** RSA
   - **Private key file format:** `.pem` (si usas Windows con PowerShell/Git Bash) o
     `.ppk` (si vas a usar PuTTY)
4. **Create key pair** → se descarga `auth-key.pem`.

**Guárdalo bien: AWS no te lo dará otra vez.** Muévelo a una carpeta estable, por ejemplo
`C:\Users\TuUsuario\.ssh\auth-key.pem`.

En Windows, da permisos correctos al archivo (si no, SSH lo rechaza):

```powershell
icacls "C:\Users\TuUsuario\.ssh\auth-key.pem" /inheritance:r
icacls "C:\Users\TuUsuario\.ssh\auth-key.pem" /grant:r "$env:USERNAME:R"
```

---

## 3. Crear los grupos de seguridad (el cortafuegos)

Un **grupo de seguridad** (Security Group, o "SG") es una lista de reglas que dice qué
tráfico entra a una máquina. Vamos a crear dos, y es **importante hacerlo en este orden**.

Ve a **EC2** → menú izquierdo → **Network & Security** → **Security Groups**.

### 3.1 SG del servidor: `sg-auth-server`

**Create security group**:

- **Security group name:** `sg-auth-server`
- **Description:** `Servidor de autenticacion`
- **VPC:** deja la que viene por defecto

**Inbound rules** (tráfico que ENTRA) → añade estas tres:

| Type | Port | Source | Para qué |
|---|---|---|---|
| SSH | 22 | **My IP** | Que puedas entrar tú por consola |
| HTTP | 80 | `0.0.0.0/0` (Anywhere-IPv4) | Que cualquiera abra la web |
| Custom TCP | 8080 | `0.0.0.0/0` | Probar Spring Boot directamente (lo cerraremos luego) |

**Outbound rules:** déjalas como están (permitir todo).

→ **Create security group**

> 💡 **Por qué SSH solo desde "My IP":** si lo abres a todo internet, en cuestión de horas
> empezarás a recibir miles de intentos de acceso automatizados. Si tu IP cambia (router
> doméstico), vuelve aquí y edita la regla.

### 3.2 SG de la base de datos: `sg-auth-db`

**Create security group** otra vez:

- **Security group name:** `sg-auth-db`
- **Description:** `Base de datos del servicio de autenticacion`

**Inbound rules** → **una sola regla**:

| Type | Port | Source |
|---|---|---|
| PostgreSQL | 5432 | **escribe `sg-auth-server`** y selecciónalo de la lista |

> 🔑 **Esto es lo más importante de toda la guía.** En "Source" NO pongas una IP: pon el
> **otro grupo de seguridad**. Así la base de datos solo acepta conexiones desde tu
> servidor, y **nunca** desde internet. Una base de datos abierta a internet es el fallo
> de seguridad más grave y más frecuente en proyectos de estudiantes.

→ **Create security group**

---

## 4. Crear la base de datos (RDS PostgreSQL)

1. Busca **RDS** en la barra superior → **Create database**.

2. **Choose a database creation method:** `Standard create`

3. **Engine options:** `PostgreSQL`
   - **Engine Version:** deja la más reciente que ofrezca (16.x o superior)

4. **Templates:** `Free tier`
   *(si no aparece, elige `Dev/Test` y en el paso 7 elige el tamaño `db.t4g.micro`)*

5. **Settings:**
   - **DB instance identifier:** `auth-db`
   - **Master username:** `authadmin`
   - **Credentials management:** `Self managed`
   - **Master password:** inventa una contraseña fuerte y **apúntala ahora**
     (ejemplo: `Rds.Auth.2026#seg`) — la vas a necesitar en el paso 8

6. **Instance configuration:**
   - **DB instance class:** `db.t4g.micro` (o `db.t3.micro`)

7. **Storage:**
   - **Storage type:** `gp3`
   - **Allocated storage:** `20` GB
   - **Enable storage autoscaling:** ❌ **desmárcalo** (evita sustos en la factura)

8. **Connectivity:** ⚠️ la sección clave
   - **Compute resource:** `Don't connect to an EC2 compute resource`
   - **VPC:** la de por defecto
   - **Public access:** **`No`**
   - **VPC security group (firewall):** `Choose existing` → **quita `default`** y
     selecciona **`sg-auth-db`**
   - **Availability Zone:** cualquiera, apunta cuál

9. **Database authentication:** `Password authentication`

10. **Additional configuration** (despliega la sección):
    - **Initial database name:** `authdb` ← **¡no lo dejes vacío!** Si lo dejas vacío,
      RDS no crea ninguna base de datos y tendrás que crearla a mano luego.
    - **Backup retention period:** `1 day` (o `0` si no te importa)
    - **Enable Enhanced monitoring:** ❌ desmárcalo (cuesta dinero)

11. **Create database**

⏳ Tarda **5–10 minutos**. Cuando el estado sea **Available**, haz clic en `auth-db` y
copia el **Endpoint**. Tendrá esta pinta:

```
auth-db.c9xxxxxxxx.us-east-1.rds.amazonaws.com
```

**Apunta estos cuatro datos**, los usarás en el paso 8:

| Dato | Valor |
|---|---|
| `DB_HOST` | `auth-db.c9xxxxxxxx.us-east-1.rds.amazonaws.com` |
| `DB_NAME` | `authdb` |
| `DB_USERNAME` | `authadmin` |
| `DB_PASSWORD` | la que inventaste |

> **¿Hay que crear las tablas a mano?** No. Al arrancar, Spring Boot (Hibernate) crea
> las tablas solo, y el `DataSeeder` inserta los roles, los permisos y el usuario `admin`.
> El esquema que se genera está documentado en [`ESQUEMA-BASE-DATOS.md`](ESQUEMA-BASE-DATOS.md).

---

## 5. Crear el servidor (EC2)

1. **EC2** → **Instances** → **Launch instances**

2. **Name:** `auth-server`

3. **Application and OS Images:**
   - `Ubuntu` → **Ubuntu Server 24.04 LTS (HVM), SSD Volume Type**
   - Arquitectura: `64-bit (x86)`

4. **Instance type:** `t2.micro` o `t3.micro` (los que dicen *Free tier eligible*)

   > ⚠️ `t2.micro` tiene **1 GB de RAM**. Compilar con Maven ahí puede quedarse sin
   > memoria. En el paso 7 añadimos memoria de intercambio (swap) para evitarlo.

5. **Key pair (login):** selecciona `auth-key`

6. **Network settings** → **Edit**:
   - **Auto-assign public IP:** `Enable`
   - **Firewall (security groups):** `Select existing security group` → marca
     **`sg-auth-server`**

7. **Configure storage:** `30` GiB, `gp3` (la capa gratuita cubre hasta 30 GB)

8. **Launch instance**

Cuando aparezca como **Running**, entra en la instancia y copia su **Public IPv4 address**
(ejemplo: `52.14.23.45`). Esa es la dirección de vuestro servicio.

> 📌 **La IP pública cambia cada vez que apagas y enciendes la instancia.** Si eso te
> molesta: **EC2 → Elastic IPs → Allocate Elastic IP → Associate** con tu instancia.
> Es gratis **mientras esté asociada a una instancia encendida**.

### 5.1 Conectarte por SSH

Desde PowerShell o Git Bash en tu equipo:

```bash
ssh -i "C:/Users/TuUsuario/.ssh/auth-key.pem" ubuntu@52.14.23.45
```

La primera vez pregunta `Are you sure you want to continue connecting?` → escribe `yes`.

Si ves `ubuntu@ip-172-31-x-x:~$`, ya estás dentro del servidor. 🎉

---

## 6. Subir el repositorio al servidor

La forma recomendada es **GitHub**: así actualizar el servidor es un `git pull`.

### 6.1 Subir el proyecto a GitHub (desde tu PC)

Si el repositorio todavía no está en GitHub:

1. Entra en <https://github.com/new>, crea un repositorio llamado `login` (puede ser privado).
2. En tu PC, dentro de `D:\Repositorio\Login`:

```bash
git add .
git commit -m "Servicio de autenticacion con JWT, roles y permisos"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/login.git
git push -u origin main
```

> 🔒 **Comprueba antes de hacer push** que no subes secretos. El archivo `.gitignore`
> ya excluye `.env`, `target/`, `node_modules/` y `*.pem`.

### 6.2 Descargarlo en el servidor

Ya conectado por SSH:

```bash
sudo apt update
sudo apt install -y git
git clone https://github.com/TU-USUARIO/login.git ~/login
```

Si el repositorio es **privado**, GitHub pedirá usuario y contraseña, pero la contraseña
normal ya no funciona: hay que usar un **token**. Créalo en
<https://github.com/settings/tokens> → *Generate new token (classic)* → marca el permiso
`repo` → y pega ese token cuando pida la contraseña.

> **Alternativa sin GitHub** (copiar desde tu PC directamente):
> ```bash
> scp -i "C:/Users/TuUsuario/.ssh/auth-key.pem" -r D:/Repositorio/Login ubuntu@52.14.23.45:~/login
> ```

---

## 7. Preparar el servidor e instalar Java

Todo lo siguiente se ejecuta **dentro del servidor** (por SSH).

### 7.1 Añadir memoria de intercambio (swap)

Imprescindible en `t2.micro`: sin esto, Maven o npm se quedan sin memoria a mitad de la
compilación y el proceso muere con `Killed`.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h          # debe mostrar 2Gi en la fila Swap
```

### 7.2 Instalar Java 21, Maven, Node y Nginx

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven nginx

# Node.js 20 (el que trae Ubuntu por defecto es demasiado antiguo para Vite)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

Comprueba que todo quedó instalado:

```bash
java -version     # openjdk version "21..."
mvn -version      # Apache Maven 3.x
node -v           # v20.x
nginx -v          # nginx/1.24 o superior
```

---

## 8. Desplegar el backend como servicio

### 8.1 Compilar el JAR

```bash
cd ~/login/backend
mvn clean package -DskipTests
```

Tarda unos minutos la primera vez (descarga dependencias). Al terminar debe existir:

```bash
ls -lh target/auth-service.jar
```

### 8.2 Crear el archivo de secretos

Aquí van las contraseñas. **Nunca** se escriben en el código ni se suben a Git.

Primero genera una clave aleatoria para firmar los tokens:

```bash
openssl rand -base64 48
```

Copia el resultado. Ahora crea el archivo:

```bash
sudo nano /etc/auth-service.env
```

Pega esto **sustituyendo** los valores por los tuyos:

```ini
# --- Base de datos (los apuntaste en el paso 4) ---
DB_HOST=auth-db.c9xxxxxxxx.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=authdb
DB_USERNAME=authadmin
DB_PASSWORD=Rds.Auth.2026#seg
# RDS con PostgreSQL 15+ exige conexión cifrada; "require" lo garantiza
DB_SSLMODE=require

# --- Firma de los tokens: pega aquí lo que generó openssl ---
APP_JWT_SECRET=PEGA_AQUI_LA_CLAVE_DE_48_BYTES
APP_JWT_ACCESS_MS=900000
APP_JWT_REFRESH_MS=604800000

# --- Desde dónde se permite llamar a la API (la IP de vuestro EC2) ---
APP_ALLOWED_ORIGINS=http://52.14.23.45,http://localhost:5173

# --- Clave compartida con el equipo de S3 (paso 10). Genera otra con openssl ---
APP_SERVICE_API_KEY=PEGA_AQUI_OTRA_CLAVE_DISTINTA

# --- Administrador inicial ---
APP_ADMIN_USERNAME=admin
APP_ADMIN_EMAIL=admin@proyecto.com
APP_ADMIN_PASSWORD=CambiaEstaClave.2026

# --- Perfil y base de datos ---
SPRING_PROFILES_ACTIVE=prod
DDL_AUTO=update

# --- Guardar los logs también en archivo (además de journalctl) ---
LOG_FILE=/var/log/auth-service/application.log
```

> 💡 **¿Dónde van exactamente las credenciales?** En este archivo y en ningún otro
> sitio. `application-prod.yml` no contiene ninguna contraseña: solo lee estas
> variables (`${DB_HOST}`, `${DB_PASSWORD}`...). El servicio de systemd del paso 8.3
> carga el archivo con la línea `EnvironmentFile=/etc/auth-service.env`.
>
> La plantilla con todas las variables posibles está en `backend/.env.example`.
> Si prefieres, también puedes copiarla como `backend/.env` dentro del servidor: el
> backend la lee sola al arrancar. Usa **una** de las dos opciones, no ambas; si una
> variable aparece en los dos, gana la de `/etc/auth-service.env`.

Guarda con `Ctrl+O`, `Enter`, y sal con `Ctrl+X`.

Protege el archivo para que solo lo lea `root`:

```bash
sudo chmod 600 /etc/auth-service.env
```

> ℹ️ **`DDL_AUTO=update`** hace que Hibernate cree las tablas que falten. Déjalo así la
> primera vez. Cuando el sistema ya funcione, cámbialo a `validate` (paso 12) para que
> nadie modifique el esquema por accidente.

### 8.3 Crear el servicio de systemd

Esto hace que Spring Boot arranque solo al encender el servidor y se reinicie si se cae.

```bash
sudo nano /etc/systemd/system/auth-service.service
```

Pega exactamente esto:

```ini
[Unit]
Description=Servicio de autenticacion (Spring Boot)
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/login/backend
EnvironmentFile=/etc/auth-service.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /home/ubuntu/login/backend/target/auth-service.jar
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

> `-Xmx512m` limita la memoria de Java. En una máquina de 1 GB es necesario: sin el
> límite, Java intenta reservar más de lo que hay y el sistema lo mata.

Crea la carpeta de logs, activa el servicio y arráncalo:

```bash
sudo mkdir -p /var/log/auth-service
sudo chown ubuntu:ubuntu /var/log/auth-service

sudo systemctl daemon-reload
sudo systemctl enable auth-service
sudo systemctl start auth-service
sudo systemctl status auth-service
```

Debe decir **`active (running)`** en verde.

### 8.4 Comprobar que funciona

```bash
# Desde el propio servidor
curl http://localhost:8080/actuator/health
# → {"status":"UP",...}

# Probar el login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin","password":"CambiaEstaClave.2026"}'
# → debe devolver accessToken y refreshToken
```

Si algo falla, mira los logs en vivo:

```bash
sudo journalctl -u auth-service -f
```

(sal con `Ctrl+C`)

---

## 9. Desplegar el frontend con Nginx

### 9.1 Compilar el React

```bash
cd ~/login/frontend
npm install
npm run build
```

Esto genera la carpeta `dist/` con HTML, CSS y JavaScript ya optimizados.

> El frontend se construye con `VITE_API_URL` vacío a propósito: así llama a `/api/...`
> en el mismo dominio y es Nginx quien reparte. Es más simple y **evita por completo los
> problemas de CORS**.

### 9.2 Copiar el frontend a su sitio

```bash
sudo mkdir -p /var/www/auth-frontend
sudo cp -r dist/* /var/www/auth-frontend/
sudo chown -R www-data:www-data /var/www/auth-frontend
```

### 9.3 Configurar Nginx

```bash
sudo nano /etc/nginx/sites-available/auth
```

Pega esto:

```nginx
server {
    listen 80;
    server_name _;

    # ---------- Frontend (React) ----------
    root /var/www/auth-frontend;
    index index.html;

    location / {
        # Clave para una SPA: si la ruta no existe como archivo, se devuelve
        # index.html y React Router decide qué pintar. Sin esta línea, recargar
        # la página en /panel/usuarios daría un 404.
        try_files $uri $uri/ /index.html;
    }

    # Los archivos con hash en el nombre no cambian nunca: se cachean un año
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # ---------- Backend (Spring Boot) ----------
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        # Sin esta cabecera, la auditoría registraría siempre 127.0.0.1
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 30s;
        proxy_read_timeout    30s;
    }

    # Health check para el balanceador, si lo añadís más adelante
    location /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
    }
}
```

Activa el sitio y desactiva el de ejemplo:

```bash
sudo ln -sf /etc/nginx/sites-available/auth /etc/nginx/sites-enabled/auth
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t          # debe decir "syntax is ok" y "test is successful"
sudo systemctl restart nginx
```

### 9.4 Probar desde tu navegador

Abre `http://52.14.23.45` (tu IP pública). Debe aparecer la pantalla de login.
Entra con `admin` y la contraseña que pusiste en `APP_ADMIN_PASSWORD`.

### 9.5 Cerrar el puerto 8080

Ya no hace falta exponerlo: todo pasa por Nginx en el puerto 80.

**EC2 → Security Groups → `sg-auth-server` → Inbound rules → Edit** → borra la regla
del puerto 8080 → **Save rules**.

---

## 10. Conectar con el equipo de S3 y el otro EC2

Esta es la parte que une los tres equipos. La idea es sencilla:

> **Vuestro servicio es el único que sabe quién es cada persona.**
> Los demás servicios no guardan contraseñas: le preguntan a vuestro servicio
> *"¿este token es válido y qué puede hacer su dueño?"*.

### 10.1 El flujo completo

```
1. La persona entra en vuestro frontend y hace login.
   → recibe un accessToken (JWT).

2. Quiere subir un archivo. El frontend llama al servicio de S3
   enviando ese mismo token:
      POST http://<IP-EC2-S3>/api/archivos
      Authorization: Bearer eyJhbGciOi...

3. El servicio de S3 NO sabe si ese token es bueno. Os pregunta:
      POST http://<IP-VUESTRA>/api/v1/auth/introspect
      X-Service-Key: <clave compartida>
      { "token": "eyJhbGciOi..." }

4. Vosotros respondéis:
      { "active": true, "username": "jhojam",
        "permissions": ["FILE_UPLOAD", "FILE_READ"], ... }

5. El servicio de S3 ve FILE_UPLOAD en la lista → sube el archivo a su bucket.
   Si no estuviera → devuelve 403.
```

### 10.2 Qué tenéis que darle al otro equipo

| Dato | Ejemplo |
|---|---|
| URL de introspección | `http://52.14.23.45/api/v1/auth/introspect` |
| Clave de servicio | el valor de `APP_SERVICE_API_KEY` |
| Documentación del contrato | el archivo [`CONTRATO-INTEGRACION.md`](CONTRATO-INTEGRACION.md) |

> 🔐 **Pasadles la clave por un canal privado** (mensaje directo, no por el repositorio
> ni por un grupo público).

### 10.3 Abrir el cortafuegos para su servidor

Para que su EC2 pueda llamaros:

1. **EC2 → Security Groups → `sg-auth-server` → Inbound rules → Edit inbound rules**
2. **Add rule:**
   - **Type:** `HTTP` (puerto 80)
   - **Source:** `Custom` → escribe el **ID del grupo de seguridad del otro EC2**
     (pídeselo: tiene la forma `sg-0a1b2c3d4e5f`), o su **IP elástica** con `/32`
3. **Save rules**

> Si los dos EC2 están en la misma VPC, usar el ID del grupo de seguridad es mejor que
> la IP: sigue funcionando aunque reinicien su máquina y cambie su IP.

### 10.4 Permisos de archivos ya preparados

El sistema ya incluye tres permisos pensados para ellos, que viajan dentro del token:

| Permiso | Qué autoriza | Lo tienen |
|---|---|---|
| `FILE_UPLOAD` | Subir archivos al bucket | ADMIN, MANAGER, USER |
| `FILE_READ` | Listar y descargar | ADMIN, MANAGER, USER |
| `FILE_DELETE` | Borrar archivos | solo ADMIN |

Si necesitan permisos distintos, se añaden en
`backend/src/main/java/com/proyecto/auth/domain/enums/PermissionName.java` y se asignan a
los roles en `RoleName.java`. Al reiniciar, el `DataSeeder` los crea en la base de datos solo.

### 10.5 Sobre el bucket S3

El bucket lo crea el otro equipo. Solo dos recomendaciones que os afectan:

- El bucket debe tener **Block all public access: ON**. Los archivos no se sirven
  públicamente: su backend genera **URLs prefirmadas** temporales tras comprobar el token
  con vosotros.
- Su EC2 debe acceder al bucket con un **rol de IAM** asignado a la instancia, nunca con
  claves de acceso escritas en el código.

---

## 11. Comprobaciones finales

Ejecuta esto desde **tu PC** (no desde el servidor), sustituyendo la IP:

```bash
IP=52.14.23.45

# 1. ¿Responde el frontend?
curl -s -o /dev/null -w "Frontend: %{http_code}\n" http://$IP/

# 2. ¿Responde el backend a través de Nginx?
curl -s http://$IP/actuator/health

# 3. ¿Funciona el login?
curl -s -X POST http://$IP/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin","password":"CambiaEstaClave.2026"}'

# 4. ¿La base de datos NO está abierta a internet? (debe fallar o quedarse colgado)
curl -s -m 5 telnet://auth-db.xxxxx.us-east-1.rds.amazonaws.com:5432 \
  && echo "⚠️ PELIGRO: la base de datos es accesible desde fuera" \
  || echo "✅ Correcto: la base de datos no es accesible desde internet"
```

### Lista de verificación

- [ ] `http://<IP>` muestra la pantalla de login
- [ ] Se puede entrar con `admin`
- [ ] El panel muestra los roles y permisos
- [ ] Al registrar un usuario nuevo, recibe `ROLE_USER`
- [ ] Un usuario normal NO ve el botón de eliminar en la lista de usuarios
- [ ] `sudo systemctl status auth-service` dice `active (running)`
- [ ] El puerto 8080 **ya no** está abierto en el grupo de seguridad
- [ ] La base de datos tiene **Public access: No**
- [ ] `APP_ADMIN_PASSWORD` ya **no** es la de ejemplo
- [ ] El otro equipo puede llamar a `/api/v1/auth/introspect` con su clave

---

## 12. Actualizar el código más adelante

Cuando cambiéis algo y lo subáis a GitHub:

```bash
ssh -i ~/.ssh/auth-key.pem ubuntu@52.14.23.45

cd ~/login
git pull

# Si cambió el backend
cd backend
mvn clean package -DskipTests
sudo systemctl restart auth-service

# Si cambió el frontend
cd ../frontend
npm install
npm run build
sudo cp -r dist/* /var/www/auth-frontend/
```

Para ahorrarte escribirlo cada vez, crea `~/desplegar.sh`:

```bash
cat > ~/desplegar.sh <<'EOF'
#!/bin/bash
set -e
echo "==> Descargando cambios..."
cd ~/login && git pull

echo "==> Compilando backend..."
cd ~/login/backend && mvn clean package -DskipTests -q

echo "==> Compilando frontend..."
cd ~/login/frontend && npm install --silent && npm run build

echo "==> Publicando..."
sudo cp -r ~/login/frontend/dist/* /var/www/auth-frontend/
sudo systemctl restart auth-service

echo "==> Listo. Estado del servicio:"
sudo systemctl is-active auth-service
EOF

chmod +x ~/desplegar.sh
```

A partir de ahora, desplegar es: `~/desplegar.sh`

### Endurecer el esquema de la base de datos

Cuando el sistema ya funcione y las tablas existan, edita `/etc/auth-service.env`:

```ini
DDL_AUTO=validate
```

y reinicia (`sudo systemctl restart auth-service`). Así Hibernate comprueba el esquema
pero no lo modifica, que es lo correcto en producción.

---

## 13. Problemas comunes

### «Connection timed out» al conectar por SSH
La regla de SSH apunta a una IP antigua. Tu IP doméstica cambió.
→ **Security Groups → `sg-auth-server` → Edit inbound rules** → en la regla SSH,
vuelve a elegir **My IP** → Save.

### El servicio no arranca: `Connection refused` a la base de datos
```bash
sudo journalctl -u auth-service -n 50 --no-pager
```
Casi siempre es una de estas tres:
1. El `DB_HOST` está mal escrito en `/etc/auth-service.env`.
2. El grupo de seguridad `sg-auth-db` no tiene como origen a `sg-auth-server`.
3. La base de datos aún estaba creándose. Comprueba que ponga **Available** en RDS.

### El servicio no arranca: `The specified key byte array is ... bits which is not secure enough`
`APP_JWT_SECRET` es demasiado corto. Debe tener al menos 32 caracteres.
→ Genera uno nuevo con `openssl rand -base64 48` y reinicia.

### `mvn package` se queda colgado o muere con `Killed`
Falta memoria. Vuelve al paso 7.1 y crea el swap.
Como alternativa: compila el JAR en tu PC y súbelo con `scp`.

### La web carga pero el login da «No se pudo conectar con el servidor»
Nginx no está redirigiendo `/api`.
```bash
sudo nginx -t
sudo systemctl status nginx
curl http://localhost:8080/actuator/health   # ¿responde el backend?
```

### Recargar `/panel/usuarios` da 404
Falta `try_files $uri $uri/ /index.html;` en la configuración de Nginx (paso 9.3).

### Error de CORS en la consola del navegador
Solo pasa si sirves el frontend desde un dominio distinto al del backend.
→ Añade ese dominio a `APP_ALLOWED_ORIGINS` en `/etc/auth-service.env` y reinicia.

### Me he quedado fuera: bloqueé la cuenta de admin
El bloqueo dura 15 minutos y se quita solo. Si no quieres esperar, conéctate a la base
de datos desde el servidor:

```bash
sudo apt install -y postgresql-client
psql -h <DB_HOST> -U authadmin -d authdb
```
```sql
UPDATE users SET account_locked = false, failed_attempts = 0 WHERE username = 'admin';
```

---

## 14. Costes y cómo no gastar de más

Con los tamaños de esta guía, y durante los **12 meses de capa gratuita**, el coste
debería ser **0 €**. Aun así:

1. **Pon una alarma de facturación.** Busca **Billing** → **Budgets** → **Create budget**
   → *Zero spend budget* → pon tu correo. Te avisa en cuanto algo empiece a costar.

2. **Apaga lo que no uses.** Si el proyecto es solo para una entrega:
   - EC2: **Instance state → Stop** (al parar, deja de cobrarse el cómputo)
   - RDS: **Actions → Stop temporarily** (se reactiva sola a los 7 días)

3. **Cuando termines el curso, borra todo** para no llevarte sorpresas:
   - EC2 → Instance state → **Terminate**
   - RDS → Actions → **Delete** (desmarca "create final snapshot")
   - Elastic IPs → **Release** ← **una IP elástica sin instancia asociada SÍ se cobra**
   - Security Groups → borra los que creaste

---

## Resumen de lo que hay que apuntar

Rellena esta tabla mientras avanzas; la necesitarás para la entrega y para el otro equipo:

| Dato | Valor |
|---|---|
| Región de AWS | `us-east-1` |
| IP pública del EC2 | `__________________` |
| Endpoint de RDS | `__________________` |
| URL del frontend | `http://__________` |
| URL de la API | `http://__________/api/v1` |
| URL de introspección | `http://__________/api/v1/auth/introspect` |
| Clave de servicio (`X-Service-Key`) | *(pásala en privado)* |
| Documentación interactiva | `http://__________/swagger-ui.html` |
