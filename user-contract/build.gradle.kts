plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
}

group = "io.github.terrence721.saga"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.68.1"
val protobufVersion = "4.28.2"

dependencies {
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-stub:$grpcVersion")
    api("com.google.protobuf:protobuf-java:$protobufVersion")

    // grpc-java's generated code still annotates with @javax.annotation.Generated,
    // which was removed from the JDK itself when Java EE modules dropped out in
    // JDK 11. compileOnly is enough: the annotation has source retention, so it's
    // never needed at runtime.
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    // Pinned to match the JUnit Jupiter/AssertJ versions Spring Boot's BOM
    // manages for the other 5 modules (bumped alongside Spring Boot 3.5.16),
    // rather than adopting a newer JUnit major version here and splitting
    // the repo across two.
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}
