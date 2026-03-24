# Fast runtime-only image build.
# Build the jar first on host/CI:
#   ./mvnw -DskipTests clean package
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/upload-dir /app/logs /app/tmp && \
    addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app

# Copy prebuilt JAR from host build context
COPY target/*.jar app.jar

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
