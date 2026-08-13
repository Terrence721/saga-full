plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.terrence721.saga"
version = "1.0.0"

springBoot {
    mainClass.set("io.github.terrence721.saga.gateway.ApiGatewayServiceApplication")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

extra["springGrpcVersion"] = "0.7.0"
extra["springCloudVersion"] = "2025.0.0"

dependencies {
    implementation(project(":user-contract"))

    // Same real conflict user-service hit in Phase 12: spring-grpc's BOM forces
    // protobuf-java down below what user-contract's generated code needs.
    implementation("com.google.protobuf:protobuf-java:4.28.2")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    implementation("com.auth0:java-jwt:4.4.0")

    implementation("org.springframework.grpc:spring-grpc-client-spring-boot-starter")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
