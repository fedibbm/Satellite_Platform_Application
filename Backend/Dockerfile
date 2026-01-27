# Multi-stage build for optimized Spring Boot container
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/upload-dir /app/logs /app/tmp && \
    addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app

# Copy the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Use non-root user
USER spring:spring

# Expose application port
EXPOSE 8080

# JVM configuration for containers
ENV JAVA_OPTS="-Xmx2g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
