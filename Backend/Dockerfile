# =============================================================================
# SATELLITE PLATFORM BACKEND - PRODUCTION RUNTIME CONTAINER
# Base: Eclipse Temurin JRE 17 Alpine (Lightweight, secure, non-root)
# =============================================================================

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 1. Create runtime directories and least-privilege non-root user
RUN mkdir -p /app/upload-dir /app/logs /app/tmp && \
    addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app

# 2. Copy the pre-compiled Spring Boot JAR
COPY target/*.jar app.jar

# 3. Enforce non-root execution (Security Best Practice)
USER spring:spring

# 4. Expose application port
EXPOSE 8080

# 5. Production JVM container tuning
ENV JAVA_OPTS="-Xmx2g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

# 6. Container Health Check (Spring Boot Actuator)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 7. Entrypoint execution
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
