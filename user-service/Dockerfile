# syntax=docker/dockerfile:1
#
# Build context must be the repo root (`docker build -f user-service/Dockerfile .`),
# not this directory - settings.gradle.kts include()s all 6 modules, and Gradle
# refuses to configure a multi-project build if any included module's directory
# doesn't physically exist. user-service also actually depends on user-contract
# (project(":user-contract")), so its own module directory is load-bearing here,
# not just present to satisfy Gradle's directory check.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY user-contract ./user-contract
COPY user-service ./user-service
COPY order-service ./order-service
COPY payment-service ./payment-service
COPY restaurant-service ./restaurant-service
COPY api-gateway-service ./api-gateway-service

# Cache mount, not a plain layer: without it, any change under user-service/ (even a
# one-line source edit) invalidates the COPY above and forces this RUN to start from a
# completely empty filesystem - re-downloading the Gradle distribution itself and
# re-resolving every dependency from Maven Central from scratch, observed costing 10+
# minutes per rebuild during real debugging. A cache mount persists independently of
# layer invalidation, keyed by its target path across builds on this machine's BuildKit
# instance.
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew :user-service:bootJar --no-daemon

# Runtime stage: JRE only, no Gradle/JDK/source in the shipped image.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN groupadd --system saga && useradd --system --gid saga saga
COPY --from=build /workspace/user-service/build/libs/user-service-1.0.0.jar app.jar
USER saga:saga

# No HTTP server - this service only exposes a gRPC endpoint (see application.yaml's
# spring.grpc.server.port). JWT_SECRET has no default in application.yaml and must
# be supplied at runtime (see docker-compose.yml / CONTRIBUTING.md).
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
