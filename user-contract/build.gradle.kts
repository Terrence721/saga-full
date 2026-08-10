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
