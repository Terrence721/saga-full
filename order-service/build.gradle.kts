plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example.saga"
version = "1.0.0"

springBoot {
    mainClass.set("com.example.saga.order.OrderServiceApplication")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.projectreactor:reactor-core")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test> {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports.xml.required.set(true)
}


// Opt-in (pass -Pnullaway, ideally on a JDK 21 toolchain): runs ErrorProne + NullAway.
// Experimental due JDK 25 / Error Prone compatibility - hence continue-on-error in CI.
if (project.hasProperty("nullaway")) {
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    dependencies {
        annotationProcessor("com.google.errorprone:error_prone_core:2.43.0")
        annotationProcessor("com.uber.nullaway:nullaway:0.13.1")
    }
    tasks.withType<JavaCompile> {
        options.isFork = true
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
        options.forkOptions.jvmArgs!!.add("--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
        options.compilerArgs.addAll(listOf(
            "-XDcompilePolicy=simple",
            "--should-stop=ifError=FLOW",
            "-Xplugin:ErrorProne -Xep:NullAway:WARN -XepOpt:NullAway:AnnotatedPackages=com.example.saga -XepOpt:NullAway:HandleTestAssertionLibraries=false"
        ))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgumentProviders.add(object : CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val jar = project.configurations.testRuntimeClasspath.get()
                .resolve().firstOrNull { it.name.startsWith("mockito-core") }
            return if (jar != null) listOf("-javaagent:${jar.absolutePath}") else emptyList()
        }
    })
}
