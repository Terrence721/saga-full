plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.terrence721.saga"
version = "1.0.0"

springBoot {
    mainClass.set("io.github.terrence721.saga.user.UserServiceApplication")
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

dependencies {
    implementation(project(":user-contract"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.grpc:spring-grpc-server-spring-boot-starter")

    // Spring Security's crypto module for password hashing, instead of the
    // unmaintained standalone jbcrypt library the reference structure used -
    // actively maintained and already audited as part of the Spring ecosystem.
    implementation("org.springframework.security:spring-security-crypto")

    implementation("com.auth0:java-jwt:4.4.0")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Pinned above Spring Boot 3.4.1's managed 1.18.36: that version predates
    // JDK 25 javac-internals support and fails annotation processing with
    // NoSuchFieldException: TypeTag :: UNKNOWN. 1.18.42 fixes it.
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.grpc:grpc-testing")

    // spring-boot-starter-test doesn't pull this in on its own; without it,
    // Gradle's useJUnitPlatform() has nothing to actually launch tests with.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
