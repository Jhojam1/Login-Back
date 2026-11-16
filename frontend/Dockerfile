# syntax=docker/dockerfile:1


# -----------------------------------------------------------------------------
#  Etapa 1: compilar
# -----------------------------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /build

# Igual que en el backend: primero los manifiestos, para que Docker reutilice la
# capa de dependencias mientras no cambien.
COPY package.json package-lock.json ./
# "npm ci" instala exactamente las versiones del package-lock, sin actualizarlas.
# Es lo correcto en un build automatico: dos ejecuciones dan el mismo resultado.
RUN npm ci

COPY . .

# VITE_API_URL se incrusta en el JavaScript durante la compilacion. Se deja vacio
# a proposito: el frontend llama a /api en su propio dominio y es el Nginx de la
# etapa siguiente quien lo reenvia al backend. Asi la misma imagen vale para
# cualquier servidor, sin recompilar, y no hay problemas de CORS.
ENV VITE_API_URL=""
RUN npm run build


# -----------------------------------------------------------------------------
#  Etapa 2: servir
# -----------------------------------------------------------------------------
# La variante "unprivileged" corre con un usuario normal y escucha en el 8080.
FROM nginxinc/nginx-unprivileged:1.27-alpine

# Al arrancar, la imagen sustituye las variables de las plantillas de este
# directorio y deja el resultado en /etc/nginx/conf.d/
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

COPY --from=build /build/dist /usr/share/nginx/html

# Direccion del backend. En el compose se sobreescribe con el nombre del servicio.
ENV BACKEND_URL="http://backend:8080"
# Limita la sustitucion a esa unica variable, para que no se toque nada mas de la
# configuracion de Nginx por accidente.
ENV NGINX_ENVSUBST_FILTER="^BACKEND_URL$"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
