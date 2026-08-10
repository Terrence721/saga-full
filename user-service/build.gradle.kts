plugins {
    java
    id("org.springframework.boot") version "3.5.16"
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

    // spring-grpc's BOM (below) forces protobuf-java down to 3.25.6 on this
    // module's runtime classpath, even though user-contract's generated
    // classes are compiled against 4.28.2 - a real binary incompatibility
    // (not just a test issue): those classes' <clinit> reference
    // RuntimeVersion$RuntimeDomain, added in protobuf-java 4.27+, and crash
    // with NoClassDefFoundError the instant a message class loads. Forced
    // back up to what user-contract actually needs.
    implementation("com.google.protobuf:protobuf-java:4.28.2")

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

    // grpc-testing's own POM declares grpc-inprocess at runtime scope only,
    // so InProcessServerBuilder/InProcessChannelBuilder resolve at test
    // runtime but aren't visible to testCompileClasspath without this.
    testImplementation("io.grpc:grpc-inprocess:1.70.0")

    // Pinned above Spring Boot 3.4.1's managed 5.14.2 (ByteBuddy 1.15.11):
    // that ByteBuddy only officially supports up to Java 23 and fails
    // mocking on JDK 25 with IllegalArgumentException in OpenedClassReader.
    // 5.16+ bundles ByteBuddy 1.17+ with native JDK 25 support.
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")

    // Spring Boot's dependency-management BOM still forces byte-buddy back
    // down to 1.15.11 even after bumping mockito-core above, since the BOM's
    // constraint outranks the version mockito-core itself asks for. Pinned
    // explicitly to what mockito-core:5.23.0 actually requires.
    testImplementation("net.bytebuddy:byte-buddy:1.17.7")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.17.7")

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
