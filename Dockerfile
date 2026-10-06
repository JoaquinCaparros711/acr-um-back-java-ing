# =============================================================================
# Stage 1: Build
# =============================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first for layer caching
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies separately (cached layer)
RUN ./mvnw dependency:go-offline -q

# Copy source and build the fat JAR (skip tests — run in CI)
COPY src ./src
RUN ./mvnw package -DskipTests -q

# =============================================================================
# Stage 2: Runtime — minimal JRE Alpine image
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# Copy the built JAR
COPY --from=builder /build/target/*.jar app.jar

# Set file ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose default Spring Boot port
EXPOSE 8080

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Build args for metadata labels
ARG BUILD_DATE
ARG VCS_REF
ARG IMAGE_TAG

LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.version="${IMAGE_TAG}" \
      org.opencontainers.image.title="dental-management-backend" \
      org.opencontainers.image.description="Spring Boot REST API for dental clinic management"

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
