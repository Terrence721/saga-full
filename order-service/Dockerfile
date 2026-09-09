# syntax=docker/dockerfile:1
#
# Build context must be the repo root (`docker build -f order-service/Dockerfile .`),
# not this directory - settings.gradle.kts include()s all 6 modules, and Gradle
# refuses to configure a multi-project build if any included module's directory
# doesn't physically exist (verified: :order-service:bootJar alone, with only this
# module's own files present, fails with "Configuring project ':user-contract'
# without an existing directory is not allowed" even though order-service has zero
# dependency on user-contract).

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

# Cache mount, not a plain layer: without it, any change under order-service/ (even a
# one-line source edit) invalidates the COPY above and forces this RUN to start from a
# completely empty filesystem - re-downloading the Gradle distribution itself and
# re-resolving every dependency from Maven Central from scratch, observed costing 10+
# minutes per rebuild during real debugging. A cache mount persists independently of
# layer invalidation, keyed by its target path across builds on this machine's BuildKit
# instance.
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew :order-service:bootJar --no-daemon

# Runtime stage: JRE only, no Gradle/JDK/source in the shipped image.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN groupadd --system saga && useradd --system --gid saga saga
COPY --from=build /workspace/order-service/build/libs/order-service-1.0.0.jar app.jar
USER saga:saga

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
