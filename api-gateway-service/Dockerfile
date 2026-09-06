# syntax=docker/dockerfile:1
#
# Build context must be the repo root (`docker build -f api-gateway-service/Dockerfile .`),
# not this directory - settings.gradle.kts include()s all 6 modules, and Gradle
# refuses to configure a multi-project build if any included module's directory
# doesn't physically exist. api-gateway-service also actually depends on
# user-contract (project(":user-contract")), for the generated gRPC stub/message
# types UserGrpcClient uses.

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

RUN chmod +x gradlew && ./gradlew :api-gateway-service:bootJar --no-daemon

# Runtime stage: JRE only, no Gradle/JDK/source in the shipped image.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN groupadd --system saga && useradd --system --gid saga saga
COPY --from=build /workspace/api-gateway-service/build/libs/api-gateway-service-1.0.0.jar app.jar
USER saga:saga

# JWT_SECRET has no default in application.yaml and must be supplied at runtime
# (see docker-compose.yml / CONTRIBUTING.md) - the same secret user-service signs with.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
