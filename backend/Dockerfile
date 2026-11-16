# syntax=docker/dockerfile:1

# =============================================================================
#  Imagen del backend (Spring Boot).
#
#  Construccion en dos etapas: la primera compila con Maven y la segunda solo se
#  queda con el JAR. Asi la imagen final no arrastra Maven, el codigo fuente ni el
#  repositorio de dependencias: la imagen final pesa unos 420 MB en vez de mas de 1 GB,
#  y ademas no publica el codigo fuente en el registro.
#
#  Construir a mano:  docker build -t login:backend-latest ./backend
# =============================================================================


# -----------------------------------------------------------------------------
#  Etapa 1: compilar
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Primero SOLO el pom.xml. Docker guarda en cache el resultado de cada instruccion,
# asi que mientras no cambien las dependencias esta capa se reutiliza y no hay que
# volver a descargarlas en cada build. Si se copiara todo de golpe, cualquier cambio
# en una clase invalidaria la cache y la descarga empezaria de cero.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

# Los tests se ejecutan en un paso aparte del workflow de CI, donde un fallo se ve
# con claridad; repetirlos aqui solo alargaria la construccion de la imagen.
RUN mvn -B -ntp clean package -DskipTests


# -----------------------------------------------------------------------------
#  Etapa 2: ejecutar
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Un usuario sin privilegios: si alguien lograra ejecutar codigo dentro del
# contenedor, no seria root dentro de el.
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build --chown=spring:spring /build/target/auth-service.jar app.jar

USER spring

EXPOSE 8080

# MaxRAMPercentage hace que la JVM calcule su memoria sobre el limite del contenedor
# y no sobre la RAM de la maquina entera. Sin esto, en un EC2 pequeno la JVM intenta
# reservar mas de lo que tiene asignado y el sistema acaba matando el proceso.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENV SPRING_PROFILES_ACTIVE=prod

# Docker marca el contenedor como "healthy" solo cuando Spring responde de verdad.
# El compose lo usa para no arrancar el frontend antes de tiempo.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# "exec" hace que Java sea el proceso 1 del contenedor y reciba las senales de
# parada. Sin esto, docker stop tardaria 10 segundos y mataria el proceso de golpe.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
