# NullAway follow-up (not yet viable on JDK 25)

## What landed
- `org.jspecify:jspecify:1.0.0` is on the classpath of every module (and the `@NullMarked` package-info).
- Runtime guards (`Objects.requireNonNull` / explicit null checks) exist where the IDE flagged
  unchecked paths.

## What did not land
Compile-time NullAway enforcement. Two attempts failed on JDK 25 with Error Prone 2.43.0 because the
javac internals Error Prone touches (`com.sun.tools.javac.comp`, `...tree`, …) are not exported to the
unnamed module, and opening them all with `--add-exports` (via `forkOptions.jvmArgs`) plus
`-XDcompilePolicy=simple` / `--should-stop=ifError=FLOW` still left Error Prone throwing on
`OutboxRecordFactory.java`.

## Recommended resume path
1. Wait for a NullAway release whose Error Prone dependency supports JDK 25 unmodified, or pin
   the build to a JDK 21/24 toolchain *just for compileJava* while keeping runtime on 25.
2. Prove it on a single module (`order-service`) behind an opt-in `-Pnullaway` flag.
3. Only roll it out once a green build on one module is confirmed.

## Interim (advisory) option
Eclipse JDT null analysis is configured in `.vscode/settings.json` for IDE feedback without changing
the build; keep `@SuppressWarnings("null")` where JDT can't infer non-null (especially mocked deps).

## Proposed (currently non-compiling) snippet

```kotlin
// opt-in: ./gradlew ... -Pnullaway (run on JDK 21 toolchain to avoid Error Prone internals issues)
if (project.hasProperty("nullaway")) {
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
            "-Xplugin:ErrorProne -Xep:NullAway:WARN " +
                "-XepOpt:NullAway:AnnotatedPackages=com.example.saga " +
                "-XepOpt:NullAway:HandleTestAssertionLibraries=false"
        ))
    }
}
```

## Continuation criteria

- Wait for an ErrorProne release that declares first-party support for JDK 25.
- Alternatively down-grade *only the compile task* toolchain to Java 21 while keeping runtime on 25.
- Any non-opt-in enablement requires a clean green run on at least one module (currently `order-service`)
  before rolling out to the rest.
