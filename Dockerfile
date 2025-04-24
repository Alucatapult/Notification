FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (better caching)
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime image with minimal footprint
FROM eclipse-temurin:17-jre-jammy as trimmer
WORKDIR /app
COPY --from=build /app/target/notification-service-*.jar app.jar
# Extract application layers for improved layering
RUN mkdir -p extracted && \
    java -Djarmode=layertools -jar app.jar extract --destination extracted

# Final runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Add curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1

# Create non-root user with minimal permissions
RUN groupadd -r spring && useradd -r -g spring spring && \
    mkdir -p /app/logs /app/config && \
    chown -R spring:spring /app

# Set security permissions
RUN chmod -R 550 /app && \
    chmod -R 770 /app/logs /app/config

# Switch to non-root user
USER spring:spring

# Copy application layers from the trimmer stage with optimized layering
COPY --from=trimmer --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=trimmer --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=trimmer --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=trimmer --chown=spring:spring /app/extracted/application/ ./

# Add application insights agent
ARG APPLICATIONINSIGHTS_VERSION=3.4.12
ADD --chown=spring:spring https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPLICATIONINSIGHTS_VERSION}/applicationinsights-agent-${APPLICATIONINSIGHTS_VERSION}.jar applicationinsights-agent.jar

# Set environment variables for JVM tuning
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Xms256m -Xmx1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"

EXPOSE 8080
ENTRYPOINT ["java", "${JAVA_OPTS}", "-javaagent:applicationinsights-agent.jar", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.JarLauncher"] 