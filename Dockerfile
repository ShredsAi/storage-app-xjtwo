# Multi-stage build for Image Ingestion and Management Shred
# Builder stage uses official Maven image to avoid relying on mvnw wrapper
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy pom and sources
COPY pom.xml ./
RUN mvn -q -e -B dependency:go-offline
COPY src ./src

# Build the application (skip tests for faster container builds)
RUN mvn -q -e -B clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -ms /bin/bash appuser
USER appuser
WORKDIR /app

# Copy jar
COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
