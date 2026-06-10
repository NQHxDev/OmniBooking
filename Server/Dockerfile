# Stage 1: Cache dependencies to optimize build speed
FROM maven:3.9.16-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
# Pre-download dependencies in batch mode to leverage Docker layer caching
RUN mvn dependency:go-offline -B

# Stage 2: Build source code and extract layers
FROM maven:3.9.16-eclipse-temurin-21 AS builder
WORKDIR /app
# Copy the downloaded dependencies cache from the first stage
COPY --from=deps /root/.m2 /root/.m2
COPY pom.xml .
COPY src ./src
# Compile, package, and verify in batch mode (skipping tests for production build image)
RUN mvn clean package -DskipTests -B
# Extract layers using Spring Boot 3.4+ 'tools' jarmode into an isolated subdirectory
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# Stage 3: Production-hardened, lightweight runtime JRE
FROM eclipse-temurin:21-jre-alpine-3.23
WORKDIR /app

# Create a dedicated non-root user and group
RUN addgroup -S spring && adduser -S spring -G spring

# Pre-create the logs directory and ensure the 'spring' user owns it.
# This prevents permission errors at runtime when Logback attempts to create or write log files.
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Copy the extracted layers from the builder stage with correct owner permissions
COPY --from=builder --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/application/ ./

# Define JVM parameters and Spring Boot production environments
# -XX:MaxRAMPercentage=75.0 allows the JVM to scale dynamically based on the container memory limit
# -XX:+ExitOnOutOfMemoryError ensures the container terminates immediately on OOM so it can be restarted by Compose
# server.shutdown=graceful ensures that in-flight requests finish before JVM terminates
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError" \
    SERVER_SHUTDOWN="graceful" \
    SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE="30s"

# Expose default application port
EXPOSE 8080

# Switch execution context to the non-root user
USER spring

# Configure Docker HEALTHCHECK utilizing Spring Boot Actuator endpoint (port 8080)
# - Start period of 60s allows application compilation/startup logs to settle down before healthcheck cycles begin
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Launch using JarLauncher with exec shell command to ensure SIGTERM signals are forwarded directly to the JVM (PID 1)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
