# Multi-stage build para optimizar el tamaño de la imagen
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivo de configuración de Maven
COPY pom.xml .

# Descargar dependencias (esto se cachea si no cambia el pom.xml)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# Imagen de producción
FROM eclipse-temurin:21-jre-alpine

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Instalar dependencias necesarias
RUN apk add --no-cache \
    tzdata \
    curl \
    && rm -rf /var/cache/apk/*

# Establecer zona horaria
ENV TZ=America/Argentina/Cordoba
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Crear directorios necesarios
RUN mkdir -p /var/log/lago-escondido && \
    chown -R appuser:appgroup /var/log/lago-escondido

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar ownership al usuario no-root
RUN chown appuser:appgroup app.jar

# Cambiar al usuario no-root
USER appuser

# Exponer puerto
EXPOSE 8080

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

