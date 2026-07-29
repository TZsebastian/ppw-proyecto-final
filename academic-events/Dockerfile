# syntax=docker/dockerfile:1.7

# ============================================
# ETAPA 1: BUILD
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace/app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar -x test --no-daemon

RUN mkdir -p build/dependency \
    && cd build/dependency \
    && jar -xf ../libs/*.jar


# ============================================
# ETAPA 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

ARG DEPENDENCY=/workspace/app/build/dependency

COPY --from=builder --chown=spring:spring \
    ${DEPENDENCY}/BOOT-INF/lib /app/lib

COPY --from=builder --chown=spring:spring \
    ${DEPENDENCY}/META-INF /app/META-INF

COPY --from=builder --chown=spring:spring \
    ${DEPENDENCY}/BOOT-INF/classes /app

USER spring:spring

EXPOSE 8080

ENV TZ=America/Guayaquil

HEALTHCHECK --interval=30s \
    --timeout=5s \
    --start-period=60s \
    --retries=3 \
    CMD curl --fail --silent --show-error \
    http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75", \
    "-cp", \
    "/app:/app/lib/*", \
    "ec.edu.ups.icc.academic_events.AcademicEventsApplication"]