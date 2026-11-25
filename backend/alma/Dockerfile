# syntax=docker/dockerfile:1

# --- Etapa 1: compilar con Maven + JDK 21 ---
# Se usa mvn de la imagen (no ./mvnw): no depende de los finales de línea ni baja Maven en cada build.
# Los tests no corren acá: usan las bases de desarrollo (ver CLAUDE.md).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom: las dependencias quedan en caché mientras el pom no cambie.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# --- Etapa 2: ejecutar con JRE 21, sin root ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios y carpeta de imágenes subidas (se monta como volumen en el compose).
RUN addgroup -S alma && adduser -S -G alma alma \
    && mkdir -p /data/uploads \
    && chown -R alma:alma /data

COPY --from=build /build/target/alma-*.jar app.jar

USER alma
ENV ALMA_UPLOADS_DIR=/data/uploads
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
