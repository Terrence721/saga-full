# syntax=docker/dockerfile:1
#
# Build context must be the repo root (`docker build -f restaurant-service/Dockerfile .`),
# not this directory - settings.gradle.kts include()s all 6 modules, and Gradle
# refuses to configure a multi-project build if any included module's directory
# doesn't physically exist (verified: a targeted bootJar for a module with zero
# inter-module dependencies still fails without every included module present).

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

RUN chmod +x gradlew && ./gradlew :restaurant-service:bootJar --no-daemon

# Runtime stage: JRE only, no Gradle/JDK/source in the shipped image.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN groupadd --system saga && useradd --system --gid saga saga
COPY --from=build /workspace/restaurant-service/build/libs/restaurant-service-1.0.0.jar app.jar
USER saga:saga

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
