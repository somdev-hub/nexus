# Multi-stage Dockerfile for Nexus Core (Spring Boot backend)
# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and configuration
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .
COPY lib lib

# Install system-scoped dependency into local Maven repo
RUN ./mvnw install:install-file -Dfile=lib/nexus-encryption.jar -DgroupId=com.nexus -DartifactId=NexusEncryption -Dversion=1.0-SNAPSHOT -Dpackaging=jar

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
