# Dockerfile for Nexus IAM (Spring Boot backend)
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .
COPY lib lib

# Install system-scoped dependency into local Maven repo
RUN ./mvnw install:install-file -Dfile=lib/nexus-encryption.jar -DgroupId=com.nexus -DartifactId=NexusEncryption -Dversion=1.0-SNAPSHOT -Dpackaging=jar

RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
