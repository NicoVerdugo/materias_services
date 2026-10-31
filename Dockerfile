# syntax=docker/dockerfile:1

# ---- Etapa 1: builder ----
# JDK de Corretto + Maven: solo se usa para compilar, no llega a la imagen final.
FROM amazoncorretto:21-alpine AS builder
WORKDIR /build

# Maven oficial sobre el JDK de Corretto (el paquete apk de maven trae otro JDK)
ARG MAVEN_VERSION=3.9.11
RUN wget -qO- https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xz -C /opt \
  && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# Primero solo el pom.xml: si no cambia, la capa de dependencias queda en caché.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Después el código fuente: un cambio en un .java solo invalida desde aquí.
COPY src ./src
RUN mvn -B clean package -DskipTests \
  && cp target/materias-service-*.jar /build/app.jar

# ---- Etapa 2: runtime ----
FROM amazoncorretto:21-alpine
WORKDIR /app

# Usuario sin privilegios
RUN addgroup -S app && adduser -S app -G app

# Solo el .jar resultante (sin Maven ni código fuente)
COPY --from=builder --chown=app:app /build/app.jar ./app.jar

USER app

# La JVM ajusta el heap al límite de memoria del contenedor
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Configuración por variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME,
# DB_PASSWORD (o SPRING_DATASOURCE_*) y AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY,
# AWS_REGION, AWS_BUCKET_NAME
EXPOSE 3002

# wget viene en Alpine (busybox); no hace falta instalar curl
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:3002/actuator/health || exit 1

# Forma exec (lista JSON): java es el PID 1 y recibe SIGTERM directamente.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
