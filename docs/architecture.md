# Architecture Decisions

Last updated: August 10, 2026

This document records the architectural decisions made in this repo — context, alternatives considered, what each decision actually cost — not a general tutorial on the Saga pattern. For the phase-by-phase build log, see [todo.md](../todo.md). For the portfolio-facing summary, see [case-study.md](case-study.md).

## Build tool: Gradle (Kotlin DSL) over Maven

**Status:** Done — repo bootstrap, Phase 2.

### Context: build tool

The source repository used as a directory-structure guide for this project builds with Maven, and Maven/XML is the more common default for Spring Boot microservice projects generally — the initial recommendation here, on the reasoning that its declarative XML is easier for someone skimming a portfolio repo to parse at a glance than a Gradle DSL, and that it maps cleanly onto a multi-module, one-`pom.xml`-per-service layout.

This is a six-module Maven-style multi-module build (`api-gateway-service`, `order-service`, `payment-service`, `restaurant-service`, `user-service`, `user-contract`), and both local dev loops and CI will rebuild/retest it repeatedly as services get added one at a time. Build turnaround on every one of those iterations is a real, recurring cost, not a one-time setup cost.

### Decision: build tool

Gradle with the Kotlin DSL (`build.gradle.kts` / `settings.gradle.kts`) replaces Maven for every module in this repo. The deciding factor is **build-time performance**: Gradle's incremental compilation and build caching only rebuild/retest what actually changed, where Maven's reactor rebuilds a module's full dependency chain on every invocation. In a six-module multi-module project rebuilt on every local iteration and every CI run, that difference compounds — it's specifically faster *builds*, not faster *runtime* application performance, which depends on the JVM and each service's own code, not the build tool. The multi-module layout itself carries over unchanged from the directory-structure guide; only the per-module build descriptor format changes, from `pom.xml` to `build.gradle.kts`.

### Consequences: build tool

- Every module gets a `build.gradle.kts` instead of a `pom.xml`; the root gets `settings.gradle.kts` instead of an aggregator `pom.xml`.
- Build/test commands documented in [CONTRIBUTING.md](../CONTRIBUTING.md) use `./gradlew`, not `mvn`.
- CI workflows (not yet added — see [todo.md](../todo.md)) will invoke Gradle tasks, not Maven goals, once there's code for them to run against, and can rely on Gradle's build cache to keep CI turnaround down as more modules are added.
- This decision has no effect on the running services' own performance — that's a separate set of decisions (JVM tuning, Spring Boot config, per-service design) made as each service is built out.

## Gradle wrapper version: 9.7.0, not the initially-generated 8.11

**Status:** Done — `user-contract` build verification, Phase 4.

### Context: Gradle version

Generating the wrapper with the first Gradle version tried, 8.11 (released November 2024), failed outright: its bundled Kotlin compiler — used to compile `build.gradle.kts`/`settings.gradle.kts` themselves — throws `IllegalArgumentException: 25.0.4` while parsing the running JVM's version string, because Gradle 8.11 predates Java 25's release and its Kotlin DSL tooling doesn't recognize the version format. This isn't a theoretical compatibility note; it's a build that fails to even generate a wrapper, confirmed by actually running it, not inferred from a compatibility matrix.

### Decision: Gradle version

The wrapper is generated against Gradle 9.7.0, which runs its Kotlin DSL compiler correctly on JDK 25 and was confirmed, by an actual `./gradlew :user-contract:build` run, to compile the module and generate working gRPC stubs from `user.proto`.

### Consequences: Gradle version

- `gradle/wrapper/gradle-wrapper.properties` pins `distributionUrl` to `gradle-9.7.0-bin.zip`. Any future module added to this repo inherits this via the shared wrapper — nobody building this repo needs Gradle installed system-wide, only a JDK.
- The `com.google.protobuf` Gradle plugin (0.9.4) emits a "multi-string dependency notation deprecated, fails in Gradle 10" warning on every build. Confirmed via `--warning-mode all` that this comes from inside the plugin's own artifact-resolution code, not from anything in this repo's `build.gradle.kts` files (which already use single-string notation) — noted here so it isn't mistaken for a local mistake, and revisited if a newer plugin version fixes it before a future Gradle 10 upgrade.

## Real build issue found and fixed: missing `javax.annotation.Generated`

**Status:** Done — `user-contract` build verification, Phase 4.

### Context: javax.annotation.Generated

The first real `./gradlew :user-contract:build` run failed at `compileJava`, not at proto generation: the generated `UserIdentityServiceGrpc.java` annotates its generated-code marker with `@javax.annotation.Generated`, a class that doesn't exist on the classpath by default on modern JDKs — `javax.annotation.*` was part of Java EE and was removed from the JDK itself starting with Java 11, well before this repo's Java 25 target.

### Decision: javax.annotation.Generated

Added `javax.annotation:javax.annotation-api:1.3.2` to `user-contract/build.gradle.kts` as a `compileOnly` dependency, not `api`/`implementation`: the annotation has source retention, so nothing needs it on the runtime classpath, only the compiler needs it present to resolve the symbol.

### Consequences: javax.annotation.Generated

- `./gradlew :user-contract:build` now passes end-to-end: proto generation, Java compilation, jar assembly all verified with a real build run, not assumed from the source files alone.
- Every future module that generates gRPC stubs (`order-service`, `payment-service`, `restaurant-service`, `user-service`, `api-gateway-service`) will hit this exact same missing-symbol error and needs the same `compileOnly` dependency.

## Root package/group namespace: `io.github.terrence721.saga`

**Status:** Done — `user-contract` bootstrap, Phase 3.

### Context: root namespace

Java package names and Gradle group IDs conventionally follow reverse-domain notation, which by convention corresponds to a domain the author actually controls (e.g. `com.google.protobuf`). This repo doesn't sit behind a real registered domain. The source repository used as a structural guide uses `dev.tunmin.saga` — that namespace is effectively the original author's identity and isn't something to reuse here, even structurally, since none of this repo's actual code is copied from that source (see `docs/case-study.md` and the README's non-affiliation framing).

The first concrete proposal was `com.sagafull.saga`, matching this repo's own name. That works mechanically — Java doesn't enforce domain ownership — but it implies ownership of `sagafull.com`, a domain nobody here owns, which is the exact convention a reverse-domain package name is supposed to signal.

### Decision: root namespace

The root namespace is `io.github.terrence721.saga`, following the common convention (used widely for GitHub-hosted-only projects, e.g. Maven Central's own guidance for `io.github.<username>` group IDs) of anchoring the package to an identity actually owned — this repo's own GitHub account — rather than a domain that isn't owned. `saga` is appended as the project-specific segment, so `user-contract`'s generated gRPC classes live under `io.github.terrence721.saga.user.grpc`, `order-service` code would live under `io.github.terrence721.saga.order`, and so on per module.

### Consequences: root namespace

- Every module's `build.gradle.kts` uses `group = "io.github.terrence721.saga"`.
- Generated protobuf/gRPC Java sources use `io.github.terrence721.saga.<module>.grpc` as the `java_package` option, not the source's `dev.tunmin.saga.*` namespace.

## CI: quality (build+test) and CodeQL workflows

**Status:** Done — CI setup, Phase 6.

### Context: CI

`quality.yml` and `codeql.yml` were added following platform-main's structure (separate build/test jobs, a scheduled CodeQL scan), but adapted for Gradle/Java instead of yarn/Node: `actions/setup-java` (JDK 25, Temurin) plus the official `gradle/actions/setup-gradle` action for build caching. `quality.yml` splits `build` (`./gradlew assemble`) and `test` (`./gradlew test`) into separate parallel jobs rather than one job running both, since `assemble` doesn't need tests to produce a package. `codeql.yml` runs an actual `./gradlew build -x test` before the CodeQL analyze step, since CodeQL needs real compiled bytecode to scan a compiled language like Java — unlike JavaScript/TypeScript, which it can read as source directly. The language identifier is `java-kotlin`, CodeQL's current unified identifier, since this repo's production code is Java but its build scripts are Kotlin DSL.

### Decision: CI

Both workflows were pushed and their first real run confirmed — not assumed to work from the YAML alone.

### Consequences: CI

- The first real run of both workflows failed immediately: `./gradlew: Permission denied`, exit code 126. `gradlew` was committed from Windows, which doesn't track Unix executable permissions, so git stored it as file mode `100644` instead of `100755` — the Linux CI runner couldn't execute it at all. Fixed with `git update-index --chmod=+x gradlew`, confirmed by a second real run that both workflows pass.
- This is a real risk for every future module and every future contributor committing from Windows: a script added without its executable bit will build fine locally (Windows doesn't enforce the bit) and fail silently in CI until someone actually runs it there.
- Hand-written service code follows the same root, e.g. `io.github.terrence721.saga.<module>`.
