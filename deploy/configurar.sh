#!/usr/bin/env bash
# =============================================================================
#  Genera el archivo .env del despliegue preguntando solo lo imprescindible.
#
#  Las claves aleatorias las crea el script, la IP publica la detecta sola y el
#  resto de valores ya vienen bien por defecto. Solo tienes que responder a los
#  datos de la base de datos y a tu usuario de Docker Hub.
#
#  Uso, dentro de ~/login-deploy/ en la instancia EC2:
#      bash configurar.sh
# =============================================================================
set -euo pipefail

DESTINO=".env"

# -----------------------------------------------------------------------------
#  No pisar una configuracion que ya funciona sin avisar
# -----------------------------------------------------------------------------
if [ -f "$DESTINO" ]; then
    echo "Ya existe un archivo .env en esta carpeta."
    read -r -p "Quieres reemplazarlo? Se guardara una copia como .env.anterior [s/N]: " confirmar
    case "$confirmar" in
        s|S|si|SI|Si) cp "$DESTINO" ".env.anterior" ;;
        *) echo "Cancelado. No se ha cambiado nada."; exit 0 ;;
    esac
fi

# -----------------------------------------------------------------------------
#  Funciones de apoyo
# -----------------------------------------------------------------------------

# Pregunta algo mostrando un valor por defecto entre corchetes.
# Si la persona pulsa Enter sin escribir, se queda el valor por defecto.
#
#   $1 texto de la pregunta   $2 valor por defecto
preguntar() {
    local pregunta="$1" porDefecto="${2:-}" respuesta
    if [ -n "$porDefecto" ]; then
        read -r -p "$pregunta [$porDefecto]: " respuesta
        echo "${respuesta:-$porDefecto}"
    else
        # Sin valor por defecto no se admite dejarlo vacio: son datos obligatorios
        while [ -z "${respuesta:-}" ]; do
            read -r -p "$pregunta: " respuesta
        done
        echo "$respuesta"
    fi
}

# Pregunta sin mostrar lo que se escribe, para las contrasenas.
preguntarOculto() {
    local respuesta=""
    while [ -z "$respuesta" ]; do
        read -r -s -p "$1: " respuesta
        echo >&2
    done
    echo "$respuesta"
}

# Genera una clave aleatoria. Usa openssl y, si no estuviera, el generador
# de numeros aleatorios del propio kernel.
generarClave() {
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -base64 48 | tr -d '\n'
    else
        head -c 48 /dev/urandom | base64 | tr -d '\n'
    fi
}

# Pregunta a AWS por la IP publica de esta instancia.
# Usa IMDSv2 (el de los tokens), que es el que viene activado por defecto.
# Si falla (por ejemplo si esto no se ejecuta en un EC2), devuelve vacio.
detectarIpPublica() {
    local token ip
    token=$(curl -s -m 2 -X PUT "http://169.254.169.254/latest/api/token" \
        -H "X-aws-ec2-metadata-token-ttl-seconds: 60" 2>/dev/null) || return 0
    [ -z "$token" ] && return 0
    ip=$(curl -s -m 2 -H "X-aws-ec2-metadata-token: $token" \
        "http://169.254.169.254/latest/meta-data/public-ipv4" 2>/dev/null) || return 0
    echo "$ip"
}

# -----------------------------------------------------------------------------
#  Preguntas
# -----------------------------------------------------------------------------
echo
echo "================================================================"
echo "  Configuracion del despliegue"
echo "================================================================"
echo
echo "Pulsa Enter para aceptar el valor que aparece entre corchetes."
echo

echo "--- Imagenes de Docker ---"
IMAGE_REPO=$(preguntar "Tu usuario de Docker Hub" "")
IMAGE_REPO="${IMAGE_REPO%/login}/login"   # admite que escriban el /login o no

echo
echo "--- Base de datos ---"
echo "Si todavia no te han pasado los datos del RDS, puedes responder que NO:"
echo "se configurara una base de datos H2 en memoria para que puedas probar el"
echo "despliegue completo ya mismo. Los datos se pierden al reiniciar, asi que"
echo "es solo para probar; cuando tengas el RDS, vuelve a ejecutar este script."
echo
read -r -p "Ya tienes los datos de la base de datos? [S/n]: " TIENE_BD

case "${TIENE_BD:-s}" in
    n|N|no|NO|No)
        PERFIL="dev"
        echo
        echo ">> Modo prueba: base de datos H2 en memoria, sin PostgreSQL."
        ;;
    *)
        PERFIL="prod"
        echo
        DB_HOST=$(preguntar "Endpoint del RDS")
        DB_PORT=$(preguntar "Puerto" "5432")
        DB_NAME=$(preguntar "Nombre de la base de datos" "authdb")
        DB_USERNAME=$(preguntar "Usuario" "auth_app")
        DB_PASSWORD=$(preguntarOculto "Contrasena (no se vera al escribir)")
        ;;
esac

echo
echo "--- Administrador de la aplicacion ---"
APP_ADMIN_PASSWORD=$(preguntarOculto "Contrasena para el usuario 'admin'")

# -----------------------------------------------------------------------------
#  Valores que no hace falta preguntar
# -----------------------------------------------------------------------------
echo
echo "Generando las claves de seguridad..."
APP_JWT_SECRET=$(generarClave)
APP_SERVICE_API_KEY=$(generarClave)

IP_PUBLICA=$(detectarIpPublica)
if [ -n "$IP_PUBLICA" ]; then
    echo "IP publica detectada: $IP_PUBLICA"
    APP_ALLOWED_ORIGINS="http://$IP_PUBLICA"
else
    echo "No se ha podido detectar la IP publica (esto no parece un EC2)."
    APP_ALLOWED_ORIGINS=$(preguntar "Escribe la URL del frontend" "http://localhost")
fi

# -----------------------------------------------------------------------------
#  Escribir el archivo
# -----------------------------------------------------------------------------
cat > "$DESTINO" <<ENV
# Generado por configurar.sh el $(date '+%Y-%m-%d %H:%M')
# NO subir este archivo a Git.

IMAGE_REPO=$IMAGE_REPO
SPRING_PROFILES_ACTIVE=$PERFIL
ENV

# El bloque de la base de datos solo tiene sentido en modo produccion: en modo
# prueba el perfil "dev" usa H2 en memoria e ignora cualquier dato de conexion.
if [ "$PERFIL" = "prod" ]; then
cat >> "$DESTINO" <<ENV

DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
DB_SSLMODE=require
DDL_AUTO=update
ENV
else
cat >> "$DESTINO" <<ENV

# Modo prueba: base de datos H2 en memoria, los datos se borran al reiniciar.
# Cuando tengas el RDS, vuelve a ejecutar configurar.sh y responde que SI.
ENV
fi

cat >> "$DESTINO" <<ENV

APP_JWT_SECRET=$APP_JWT_SECRET
APP_SERVICE_API_KEY=$APP_SERVICE_API_KEY

APP_ADMIN_USERNAME=admin
APP_ADMIN_EMAIL=admin@proyecto.com
APP_ADMIN_PASSWORD=$APP_ADMIN_PASSWORD

APP_ALLOWED_ORIGINS=$APP_ALLOWED_ORIGINS
ENV

# Solo el dueno puede leerlo: dentro hay contrasenas
chmod 600 "$DESTINO"

# -----------------------------------------------------------------------------
#  Resumen
# -----------------------------------------------------------------------------
echo
echo "================================================================"
echo "  Listo: .env creado"
echo "================================================================"
echo
echo "Imagenes:       $IMAGE_REPO:backend-latest / :frontend-latest"
if [ "$PERFIL" = "prod" ]; then
    echo "Base de datos:  $DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"
else
    echo "Base de datos:  H2 en memoria (MODO PRUEBA)"
    echo "                Los datos se borran al reiniciar el contenedor."
    echo "                Cuando tengas el RDS, ejecuta este script otra vez."
fi
echo "Frontend:       $APP_ALLOWED_ORIGINS"
echo
echo "Esta clave es la que debe usar el equipo del servicio de S3 para"
echo "validar tokens. Pasasela por un canal privado:"
echo
echo "   X-Service-Key: $APP_SERVICE_API_KEY"
echo
echo "Ahora arranca con:"
echo "   docker compose pull && docker compose up -d"
echo
