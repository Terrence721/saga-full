# Architecture Decisions

Last updated: August 25, 2026 (all six backend modules complete through `api-gateway-service`, Phase 40; a CodeQL-flagged log-injection fix, Phase 41; the code-review audit has closed out `user-contract`, `user-service`, and `order-service`, `payment-service` 12/14 files in — see [`docs/code-review.md`](code-review.md))

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

## `user-service`: password hashing via Spring Security Crypto, not the source's `jbcrypt`

**Status:** Done — `user-service` core implementation, Phase 7.

### Context: password hashing library

The structural-reference source hashes passwords with the standalone `org.mindrot:jbcrypt` library. That project has had no release in years and isn't part of any actively maintained ecosystem — a real concern for anything touching credential storage, where an unpatched library is a standing liability rather than a one-time inconvenience.

### Decision: password hashing library

`user-service` uses `spring-security-crypto`'s `BCryptPasswordEncoder` instead — same BCrypt algorithm underneath, but shipped and security-audited as part of the actively maintained Spring Security project, and already pulled in transitively by nothing else here, so it's an explicit, deliberate dependency, not an accident.

### Consequences: password hashing library

- `user-service/build.gradle.kts` depends on `org.springframework.security:spring-security-crypto` directly, without pulling in all of `spring-boot-starter-security` (no request-level security filter chain exists or is needed here — this service exposes gRPC only).
- A `PasswordEncoder` bean (`SecurityConfig`) is required where the reference had none, since Spring Security's encoder is dependency-injected rather than called as a static utility method.

## `user-service`: UUID primary keys, not the source's un-generated `Long`

**Status:** Done — `user-service` core implementation, Phase 7.

### Context: `User` primary key

The reference `User` entity declares `@Id private Long id` with no `@GeneratedValue` strategy at all — every insert would need the ID assigned by hand, which only works there because rows are seeded via `data.sql`, not created through normal application code.

### Decision: `User` primary key

`user-service`'s `User` entity uses `UUID id` with `@GeneratedValue(strategy = GenerationType.UUID)`, Hibernate's native UUID generation (available since Hibernate 6, bundled with Spring Boot 3.4). A UUID is also what actually crosses the gRPC boundary — `LoginResponse.user_id` in `user.proto` is a `string`, so an opaque UUID converted with `.toString()` fits that contract more naturally than a sequential integer would, and doesn't leak how many users have signed up.

### Consequences: `User` primary key

- `UserRepository extends JpaRepository<User, UUID>`, not `<User, Long>`.
- No `data.sql` seed file is used for `user-service`; rows are created through normal application code, which is what the generated-ID strategy is for.

## `user-service`: `ValidateToken` returns `valid: false`, never a gRPC error

**Status:** Done — `user-service` core implementation, Phase 7.

### Context: token validation failure handling

The reference source never actually implements a token-validation RPC — only login/token-issuance. This repo's `user.proto` (Phase 4) deliberately added `ValidateToken` as a second RPC, so its failure-handling behavior had to be decided from scratch. The naive approach mirrors `Login`: throw a domain exception and let it become a gRPC error status (`UNAUTHENTICATED`, etc.).

### Decision: token validation failure handling

An expired, malformed, or forged token is a normal answer for a validation endpoint to give, not a failure of the endpoint itself — the same reasoning behind OAuth2 token-introspection (`RFC 7662`) returning `active: false` rather than an HTTP error for an invalid token. `JwtTokenProvider.verifyToken` returns `Optional<DecodedJWT>`, empty on any verification failure, and `UserGrpcServiceImpl.validateToken` maps that directly to `ValidateTokenResponse{ valid: false }` — it never throws, so it never goes through `GrpcExecutor`'s exception-to-`Status` mapping at all.

### Consequences: token validation failure handling

- Callers of `ValidateToken` (eventually `api-gateway-service`, checking a caller's bearer token) get a normal, successful gRPC response either way and branch on the `valid` field — they don't need gRPC-status error handling just to check a token.
- `Login`, by contrast, still throws through `GrpcExecutor`: a wrong password or unknown email during an explicit login attempt is treated as a genuine client error worth a gRPC status (`UNAUTHENTICATED`/`NOT_FOUND`/`PERMISSION_DENIED`), since the caller is actively asking "let me in," not "is this thing still valid."

## Real build issue found and fixed: Lombok 1.18.36 incompatible with JDK 25's javac internals

**Status:** Done — `user-service` core implementation, Phase 7.

### Context: Lombok/JDK 25 incompatibility

The first real `./gradlew :user-service:compileJava` run — the first time any class in this repo actually used a Lombok annotation (`@Getter`/`@Setter`/`@Builder`/`@Slf4j`) — failed with `java.lang.ExceptionInInitializerError` → `NoSuchFieldException: com.sun.tools.javac.code.TypeTag :: UNKNOWN`. Lombok hooks into `javac`'s internal, undocumented classes to rewrite the AST at compile time; Spring Boot 3.4.1's dependency management pins Lombok to 1.18.36, which predates JDK 25 (GA September 2025) and doesn't know about that JDK build's internal layout.

### Decision: Lombok/JDK 25 incompatibility

`user-service/build.gradle.kts` pins `org.projectlombok:lombok:1.18.42` explicitly on both `compileOnly` and `annotationProcessor`, overriding Spring Boot's managed version. 1.18.40 added JDK 25 support; 1.18.42 fixed a Javadoc-parsing regression from that release. Confirmed by an actual second `./gradlew :user-service:compileJava` run succeeding, not inferred from the changelog alone.

### Consequences: Lombok/JDK 25 incompatibility

- Every future module using Lombok on this repo's JDK 25 toolchain (`order-service`, `payment-service`, `restaurant-service`, `api-gateway-service`) will hit this same failure and needs the identical explicit version pin until Spring Boot's own managed dependencies catch up to a JDK-25-compatible Lombok release.

## Real build issue found and fixed: `resolveMainClassName` can't read JDK 25 class files

**Status:** Done — `user-service` CI fix, Phase 8.

### Context: `resolveMainClassName` failure

The first real CI run of `quality.yml`'s `build` job (`./gradlew assemble`) against `user-service` failed at `:user-service:resolveMainClassName` with `Unsupported class file major version 69` (major version 69 = Java 25). That task, added by the Spring Boot Gradle plugin, auto-detects a module's main class by scanning its compiled `.class` files with a bundled ASM `ClassReader` when `springBoot.mainClass` isn't set explicitly. Spring Boot 3.4.1's Gradle plugin predates JDK 25's GA (September 2025), so its bundled ASM doesn't recognize a class file compiled to major version 69 — this is independent of which JDK actually runs Gradle itself (already JDK 25, per Phase 5/6's CI setup), since the scan uses the plugin's own bundled bytecode reader, not the running JVM's.

### Decision: `resolveMainClassName` failure

`user-service/build.gradle.kts` sets `springBoot { mainClass.set("io.github.terrence721.saga.user.UserServiceApplication") }`, which tells the plugin the answer instead of asking it to derive one by reading bytecode — this skips the ASM scan entirely rather than working around a bug inside it. Confirmed locally by reproducing the exact CI failure with `./gradlew assemble` before the fix, then a second run succeeding (through `resolveMainClassName` → `bootJar` → `assemble`) after it.

### Consequences: `resolveMainClassName` failure

- Every future Spring Boot module in this repo (`order-service`, `payment-service`, `restaurant-service`, `api-gateway-service`) needs the same explicit `mainClass` declaration for the identical reason, until the Spring Boot Gradle plugin ships a release built against a JDK-25-aware ASM.

## Testing framework: JUnit 5 (Jupiter) + AssertJ, pinned to Spring Boot's managed versions

**Status:** Done — `user-contract` test suite, Phase 9.

### Context: testing framework

`user-service` gets JUnit 5, AssertJ, and Mockito for free via `spring-boot-starter-test`'s `testImplementation` dependency, already resolved (confirmed by inspecting `:user-service:dependencies`) to JUnit Jupiter 5.11.4 and AssertJ 3.26.3 through Spring Boot 3.4.1's dependency management. `user-contract` has no such starter and no test dependencies at all. JUnit 6.1.3 is the actual latest release as of this decision (three days old), but adopting it in `user-contract` while `user-service` stays on the Spring-Boot-managed 5.x line would split the repo across two JUnit major versions for no functional reason.

### Decision: testing framework

`user-contract/build.gradle.kts` adds `junit-jupiter:5.11.4` and `assertj-core:3.26.3` directly — matching, not guessing at, what Spring Boot already manages for `user-service` — plus `junit-platform-launcher`, added to **both** modules after discovering neither had it: `spring-boot-starter-test` doesn't pull it in on its own, and without it on the test runtime classpath, Gradle's `useJUnitPlatform()` has no launcher to actually run discovered tests with. `user-contract/build.gradle.kts` also gained an explicit `tasks.test { useJUnitPlatform() }`, since nothing else in that module configures it (Spring Boot's plugin does this automatically for `user-service`).

### Consequences: testing framework

- Every module added to this repo should default to this same JUnit 5.11.4 + AssertJ 3.26.3 pairing unless a future Spring Boot BOM bump changes what's managed — at which point both modules should move together, not drift apart.
- Mockito wasn't added to `user-contract`: nothing there has collaborators to mock (see the next section).

## `user-contract` test coverage: serialization round-trips, not generated-code tests

**Status:** Done — `user-contract` test suite, Phase 9.

### Context: what's actually testable in a contract-only module

`user-contract` has exactly one hand-written source file, `user.proto` — every `.java` class (`LoginRequest`, `LoginResponse`, `ValidateTokenRequest`, `ValidateTokenResponse`, `UserIdentityServiceGrpc`) is generated by `protoc`/the gRPC codegen plugin into the gitignored `build/` directory at compile time. Writing unit tests against those generated builders/getters would exercise `protoc`'s own code generation, not anything written in this repo — there's no hand-written logic here to protect a regression in.

### Decision: what's actually testable in a contract-only module

`UserContractSerializationTest` (`user-contract/src/test/java/io/github/terrence721/saga/user/grpc/`) tests the one thing about a proto contract that's actually load-bearing: that a message survives being serialized to bytes and parsed back with its data intact, i.e. the wire format itself hasn't silently broken. Six tests, each building a message, round-tripping it through `toByteArray()` → `parseFrom()`, and asserting the result equals the original:

- `loginRequestRoundTripsThroughSerialization` — `LoginRequest` (`email`, `password`) survives a round trip with both string fields intact.
- `loginResponseRoundTripsThroughSerialization` — `LoginResponse` (`user_id`, `access_token`, `token_type`, `expires_in_seconds`) survives a round trip with realistic values (a UUID string, a JWT-shaped token, `3600` seconds).
- `loginResponseRoundTripsAtInt64Boundary` — the same message with `expires_in_seconds` set to `Long.MAX_VALUE`, specifically to catch a varint-encoding bug that a small "happy path" number like `3600` could never expose.
- `validateTokenRequestRoundTripsThroughSerialization` — `ValidateTokenRequest` (`access_token`) survives a round trip.
- `validateTokenResponseRoundTripsWhenValid` — `ValidateTokenResponse` with `valid = true` and a populated `user_id` survives a round trip, covering the "token accepted" branch `user-service`'s `ValidateToken` RPC actually returns.
- `validateTokenResponseRoundTripsWhenInvalid` — the same message with `valid = false` and no `user_id` set, covering protobuf's default/empty-field behavior on the "token rejected" branch, and asserting `user_id` comes back empty rather than null (protobuf strings never deserialize to null).

Verified as real, not just "written and assumed to pass": a genuine `./gradlew :user-contract:test` run confirmed all 6 green, and the suite was then deliberately broken (asserting `expires_in_seconds` against a wrong value) and re-run to confirm it actually fails on wrong data, before being reverted to its correct, passing form.

### Consequences: what's actually testable in a contract-only module

- No test exists for `UserIdentityServiceGrpc` itself — it's a pure stub/service-base class with no data or logic of its own to assert against; its behavior is exercised indirectly once `user-service`'s own tests call through it.
- Every result in [todo.md](../todo.md)'s Test Coverage Ledger reflects an actual test run, not an assumption — that ledger is the source of truth for "is this actually tested," updated only after a real `./gradlew :<module>:test` execution.

## Consolidated test report: a custom root task, not Gradle's `test-report-aggregation` plugin

**Status:** Done — consolidated test report, Phase 10.

### Context: consolidated test report

With more modules and test suites planned across this repo, checking each module's own `build/reports/tests/test/index.html` separately doesn't scale. Gradle ships a built-in `test-report-aggregation` plugin for exactly this, applied at the root project with `testReportAggregation(project(...))` dependencies pointing at each subproject.

In practice, that plugin's resolution needs each subproject's *full* dependency graph resolvable from the root, not just its already-written JUnit XML. Root-level resolution failed twice: first with "no repositories are defined" (fixed by declaring `mavenCentral()` at the root), then with unresolved Spring-managed dependency versions (`spring-boot-starter-actuator:.`, etc.) — because `user-service`'s versions come from Spring Boot's Gradle plugin auto-importing its BOM, which isn't applied at the root. Making that work would mean the root project also applying Spring Boot's plugin and BOM (and later, whatever `order-service`/`payment-service`/etc. add) — an ongoing duplication burden that grows every time any module's dependencies change, not a one-time setup cost.

### Decision: consolidated test report

A custom `aggregateTestReport` task in the new root `build.gradle.kts` reads the JUnit XML each module's `test` task already writes (`<module>/build/test-results/test/*.xml`) using the JDK's own `javax.xml.parsers.DocumentBuilderFactory` — no third-party dependency, no dependency-graph resolution of any kind — and merges every `<testcase>` into one self-contained HTML file at `build/reports/tests/aggregate/index.html`, with a pass/fail/skipped summary at the top.

It deliberately does **not** `dependsOn` the subprojects' `test` tasks. A real test failure was used to verify this: with a dependency in place, `:user-contract:test` failing blocked `aggregateTestReport` from running at all, even with `--continue` (a failed dependency always prevents a dependent task from executing — `--continue` only lets *other, independent* tasks keep going). The documented workflow ([CONTRIBUTING.md](../CONTRIBUTING.md)) is therefore two separate commands: `./gradlew test --continue` (every module's tests run regardless of one another failing), then `./gradlew aggregateTestReport` (merges whatever's on disk, pass or fail).

A second real bug was caught the same way: the task's first version declared `outputs.file(...)` but no inputs, so after one run Gradle marked it `UP-TO-DATE` on every later invocation and silently kept serving a stale report — it still showed a test that had since been fixed as failing. Fixed by declaring each subproject's `test-results/test` directory as a task input (only once it actually exists on disk, since a module with no tests yet never creates one, and Gradle's directory-input validation requires existence).

### Consequences: consolidated test report

- Every module added to this repo (`order-service`, `payment-service`, `restaurant-service`, `api-gateway-service`) is picked up automatically the moment it's added to `settings.gradle.kts`, since the task iterates `subprojects` rather than naming modules individually — no edit to this file needed as the repo grows.
- The two-step `test --continue` / `aggregateTestReport` workflow is the only way to get an always-current merged report; running `aggregateTestReport` alone just reflects whatever the last `test` run produced, and running it as part of a single failing `./gradlew test` invocation without `--continue` would skip modules after the first failure.
- Verified end-to-end with real runs, not assumed: confirmed 6/6 passing, then deliberately broke a `user-contract` assertion and confirmed the report showed `Failed: 1` with the real failure message, then confirmed reverting it flipped the report back to 6/6 (catching the staleness bug in the process), all before trusting the task.
- Wired into CI: `quality.yml`'s `Test` job runs `./gradlew test --continue`, then `./gradlew aggregateTestReport` and an `actions/upload-artifact@v4` step, both marked `if: always()` so they run even when a test fails, uploading `build/reports/tests/aggregate/` as a downloadable artifact on every CI run — not just something you'd have to reproduce locally to see.

## `user-service` gRPC service tests: a hand-written `StreamObserver`, not `io.grpc.testing.StreamRecorder`

**Status:** Done — `user-service` gRPC service tests, Phase 12.

### Context: capturing a unary gRPC response in a unit test

`UserGrpcServiceImplTest` needs to call `UserGrpcServiceImpl.login`/`.validateToken` directly (with `UserRepository`/`PasswordEncoder`/`JwtTokenProvider` mocked via Mockito) and inspect the response without a real network call. `io.grpc:grpc-testing` (already a `build.gradle.kts` dependency since Phase 7, previously unused) ships `io.grpc.testing.StreamRecorder` for exactly this. The IDE flagged it deprecated; checking upstream confirmed it's deprecated with **no official replacement** — grpc-java's own maintainers describe it as "not for public use," with community guidance to either use blocking stubs against a real (in-process) server or hand-roll the capturing logic.

### Decision: capturing a unary gRPC response in a unit test

A small package-private `RecordingStreamObserver<T>` implements `io.grpc.stub.StreamObserver<T>` directly, capturing the value passed to `onNext` and the error passed to `onError`. No latch or `awaitCompletion()` is needed: `GrpcExecutor` (the shared exception-to-`Status` mapper both RPCs go through) always calls `onNext`/`onError`/`onCompleted` synchronously, in the same thread, before `login`/`validateToken` returns — this isn't a streaming or async call.

### Consequences: capturing a unary gRPC response in a unit test

- `RecordingStreamObserver` is reused by both `UserGrpcServiceImplTest` (happy path) and `UserGrpcServiceImplErrorTest` (error path) rather than duplicated.
- If a future RPC in this repo is genuinely asynchronous or streaming (unlike `Login`/`ValidateToken`), this pattern doesn't apply as-is — that would need real synchronization, not just a capturing observer.

## Real production bug found and fixed: `spring-grpc`'s BOM silently downgrades `protobuf-java` below what generated code needs

**Status:** Done — `user-service` gRPC service tests, Phase 12.

### Context: protobuf-java version conflict

Running `UserGrpcServiceImplTest` for the first time — the first time anything in `user-service` actually constructed a `LoginRequest`/`ValidateTokenRequest` message at runtime, not just at compile time — failed with `NoClassDefFoundError: com/google/protobuf/RuntimeVersion$RuntimeDomain` the instant the message class's static initializer ran. `com.google.protobuf.RuntimeVersion$RuntimeDomain` is a class protobuf-java only added in 4.27+, referenced by code `protoc` 4.28.2 generates (used in `user-contract`) as part of its own runtime-version validation.

`./gradlew :user-service:dependencyInsight --dependency protobuf-java --configuration testRuntimeClasspath` showed the actual cause: `org.springframework.grpc:spring-grpc-dependencies` (the BOM imported in Phase 7 for `spring-grpc-server-spring-boot-starter`) forces `protobuf-java` down to `3.25.6` — overriding not just Gradle's normal "highest version wins" resolution, but overriding it *downward*, even below `user-contract`'s own direct `4.28.2` requirement. Checking `runtimeClasspath` (not just `testRuntimeClasspath`) confirmed this wasn't a test-only artifact: **the actual production application had the identical broken dependency graph** since Phase 7. It went unnoticed because every verification since then (`compileJava`, `assemble`, `build`, `bootJar`) only compiles code or packages a jar — none of them actually construct a generated protobuf message at runtime, which is the one thing that triggers the class-loading failure. This test is the first thing in the repo that ever did.

### Decision: protobuf-java version conflict

`user-service/build.gradle.kts` adds an explicit `implementation("com.google.protobuf:protobuf-java:4.28.2")` — on the main `implementation` configuration, not `testImplementation`, so it fixes `runtimeClasspath` and `testRuntimeClasspath` identically, the same way the bug affected both identically. An explicit direct dependency declaration outranks a BOM-forced transitive one in Gradle's conflict resolution, the same mechanism that made the Lombok and Mockito pins (below) take effect. Confirmed via `dependencyInsight` that `protobuf-java` now resolves to `4.28.2` on both configurations, then confirmed the actual test (which directly calls `LoginRequest.newBuilder()...build()`, the exact call that crashed) passes — proving the fix at the same point that exposed the bug, not just at the dependency-resolution level.

### Consequences: protobuf-java version conflict

- This was a real, user-facing bug: had `user-service` been deployed and actually received a gRPC call before this was caught, it would have crashed on the first request. No amount of `compileJava`/`assemble`/`bootJar` verification would have caught it — only a test (or a real request) that actually constructs a generated message does.
- Every future module in this repo that both generates protobuf code (via `user-contract`-style modules) and depends on `spring-grpc-server-spring-boot-starter` needs the identical explicit `protobuf-java` pin, until `spring-grpc-dependencies` ships a BOM that doesn't force protobuf-java below what its own declared grpc-protobuf version needs.
- This is the strongest argument yet in this repo for writing tests that actually exercise generated code paths, not just ones that compile against them — see [todo.md](../todo.md)'s Test Coverage Ledger.

## `user-service` test tooling: Mockito 5.23.0 + ByteBuddy 1.17.7, pinned for JDK 25

**Status:** Done — `user-service` gRPC service tests, Phase 12.

### Context: Mockito/JDK 25 incompatibility

The first real test run using `@Mock`/`@InjectMocks` (`UserGrpcServiceImplTest`) failed with `MockitoException` → `IllegalStateException` in `InlineBytecodeGenerator` → `IllegalArgumentException` in `OpenedClassReader` — the same *category* of failure as the Lombok/JDK 25 issue from Phase 7 and the `resolveMainClassName`/JDK 25 issue from Phase 8, but in a third tool: Mockito's inline mock maker uses ByteBuddy to generate mock subclasses at runtime, and Spring Boot 3.4.1 manages Mockito at `5.14.2`, which bundles ByteBuddy `1.15.11` — a version that only officially supports class files up to Java 23 (major version 67), not JDK 25's 69.

### Decision: Mockito/JDK 25 incompatibility

Pinned `org.mockito:mockito-core`/`mockito-junit-jupiter` to `5.23.0` directly. That alone wasn't sufficient: `dependencyInsight` showed Spring Boot's BOM still forcing `byte-buddy` back down to `1.15.11` even after the Mockito bump, the identical "BOM overrides the version its own dependency asks for" pattern as the protobuf-java bug above — so `net.bytebuddy:byte-buddy`/`byte-buddy-agent` were also pinned explicitly, to `1.17.7`, the version `mockito-core:5.23.0` itself requests. Confirmed via `dependencyInsight` that both now resolve correctly, then confirmed the actual mocked test passes.

### Consequences: Mockito/JDK 25 incompatibility

- A pattern is now visible across three separate incidents (Lombok, Spring Boot's `resolveMainClassName`, Mockito/ByteBuddy): tools that read or generate JVM bytecode via bundled ASM/ByteBuddy consistently lag JDK 25 support, and Spring Boot 3.4.1's dependency management consistently pins the pre-JDK-25 version of each. Any *new* bytecode-touching tool added to this repo (code coverage, additional static analysis, etc.) should be assumed to need the same treatment until proven otherwise.
- Bumping a BOM-managed library's version is not always enough on its own — its own transitive dependencies (like ByteBuddy here) may need pinning too, since the BOM's constraint can outrank what the newly-bumped library itself declares it needs.

## Consolidated test report visibility: a committed static file kept current by CI, not a Pages-deployment pipeline

**Status:** Done — README test-status visibility, Phase 13.

### Context: making test status visible from the README

`aggregateTestReport` (Phase 10) produces a real merged report, but only into the gitignored `build/` directory — nothing a reader browsing the repo or its portfolio page could click into. `platform-main`'s README pointed at the actual precedent: a full-suite HTML test report deployed live to GitHub Pages, kept current automatically on every push, with a dedicated `pages.yml` workflow (`actions/upload-pages-artifact` + `actions/deploy-pages`) building it fresh on each deploy.

That mechanism was tried directly: a `pages.yml` workflow was added, and this repo's GitHub Pages source was switched from its existing legacy branch-build (which auto-renders `README.md` as the site's Jekyll homepage) to the Actions-based build type needed for `deploy-pages`. Both steps worked. It was then deliberately reverted: the actual requirement was a plain, single committed HTML file the README could link to directly — not a live-rendered site replacing the README-based homepage, and not a report regenerated fresh by a workflow at deploy time.

### Decision: making test status visible from the README

`quality.yml`'s `Test` job gained one more step: after generating the aggregate report, it's copied to `test-report.html` at the repo root and committed back to `main` automatically — but only on an actual `push` to `main` (not `pull_request` or manual `workflow_dispatch` runs), only when the file's content actually changed (`git diff --quiet` guard, avoiding empty commits), and with `[skip ci]` in the commit message so the auto-commit doesn't re-trigger `quality.yml`/`codeql.yml` against itself. GitHub Pages was switched back to the legacy branch build, so `test-report.html` is served as a plain static file at `https://terrence721.github.io/saga-full/test-report.html` alongside the README-rendered homepage, not in place of it. The README's "At a glance" line and "What's Here So Far" section both link directly to that URL.

### Consequences: making test status visible from the README

- The file is real, committed, and reviewable in normal `git log`/PR diffs like any other tracked file — not an artifact that only exists inside a CI run's ephemeral storage or a separately-deployed site with its own history.
- Verifying the guard conditions took an actual `workflow_dispatch` run: it confirmed the new step correctly shows as `skipped` (`github.event_name != 'push'`), proving the condition works as written rather than assuming it from the YAML alone. A genuine `push`-triggered run is still the real end-to-end proof of the commit-back path itself.
- A real anomaly, noted but not chased further: the `git push --force-with-lease` used to fold this work into an already-pushed commit did not trigger `quality.yml`/`codeql.yml` automatically (only GitHub's own internal Pages rebuild fired) — `gh workflow run` was used to verify the workflow directly instead. Force-pushes are expected to trigger `on: push` workflows normally; this looked like a one-off GitHub webhook gap rather than an Actions permissions or configuration problem (`actions/permissions` confirmed Actions fully enabled). Confirmed one-off, not a persistent issue: the next genuine (non-force) push triggered `Quality`/`CodeQL` normally and produced a real `chore: update consolidated test report [skip ci]` auto-commit, the actual end-to-end proof this mechanism works.

## `user-service` gRPC integration test: `InProcessServerBuilder`/`InProcessChannelBuilder`, and a compile-vs-runtime dependency-scope gap

**Status:** Done — `user-service` gRPC integration test, Phase 15.

### Context: proving the real wire contract, not just internal Java calls

`UserGrpcServiceImplTest`/`UserGrpcServiceImplErrorTest` (Phases 11-12, 14) call `UserGrpcServiceImpl.login`/`.validateToken` as plain Java method calls — real coverage of the business logic, but they never actually serialize a message or send a `Status` code across any transport. One thing only an end-to-end call proves: that the generated client stub, the real gRPC server registration, and `Status` codes all survive an actual (if in-process) network round trip.

Writing `UserGrpcServiceIntegrationTest` hit a real, if minor, dependency-resolution gap: `io.grpc.inprocess.InProcessServerBuilder`/`InProcessChannelBuilder` come from a separate artifact, `grpc-inprocess`, already present on `user-service`'s `testRuntimeClasspath` (pulled in transitively by `grpc-testing`, Phase 7) but absent from `testCompileClasspath` — confirmed by checking both configurations directly via `./gradlew :user-service:dependencies`, not assumed from the "it's already a dependency" intuition. `grpc-testing`'s own POM declares `grpc-inprocess` at Maven's `runtime` scope, which Gradle's `testRuntimeClasspath` inherits but `testCompileClasspath` correctly does not, since nothing at compile time needs a runtime-only dependency — until this test tried to import its types directly.

### Decision: proving the real wire contract, not just internal Java calls

`UserGrpcServiceIntegrationTest` starts a real `Server`/`ManagedChannel` pair per test (`@BeforeEach`/`@AfterEach`), both built with `.directExecutor()` — grpc-java's own recommended pattern for tests, making every call synchronous within the test thread so no latch/timeout handling is needed — registers the real `UserGrpcServiceImpl` (still with the same three collaborators mocked, keeping this test independent of a real database), and talks to it through the actual generated `UserIdentityServiceGrpc.UserIdentityServiceBlockingStub`, not a captured `StreamObserver`. Three tests: `login` succeeding over the wire, `login` propagating a real `NOT_FOUND` `StatusRuntimeException` for an unknown user (asserted via `Status.fromThrowable`-equivalent extraction on the thrown exception, proving the status code itself survives serialization, not just the Java exception type), and `validateToken` returning `valid: true` over the wire. `grpc-inprocess` was added as an explicit `testImplementation`, matching the `1.70.0` already resolved elsewhere on this module's classpath.

### Consequences: proving the real wire contract, not just internal Java calls

- This is now the one test in the repo that would catch a real wire-level regression the other two suites structurally cannot — e.g. a proto field number collision or a `Status` code that gets lost in a serialization round trip.
- The `testRuntimeClasspath`-vs-`testCompileClasspath` distinction is a useful general lesson for this repo: "the JAR is already being pulled in" isn't the same question as "is the *class* available where I'm trying to use it," and the two can silently diverge based on how an upstream POM scopes its own transitive dependencies.
- With this test in place, all three planned `user-service` test suites (unit, error-path, integration) are complete — see [todo.md](../todo.md)'s Test Coverage Ledger.

## Real production risk found and fixed: Spring Boot 3.4.1 can't boot a JDK 25 application at all — bumped to 3.5.16, repo-wide

**Status:** Done — `order-service` scaffold + `user-service` context-load proof, Phases 16-17.

### Context: a gap none of the earlier tests could have caught

`order-service`'s first test, a minimal `@SpringBootTest` `contextLoads()` smoke test, failed immediately with `BeanDefinitionStoreException` → `ClassFormatException` → `IllegalArgumentException: Unsupported class file major version 69` (major version 69 = Java 25). Unlike the earlier `resolveMainClassName` incompatibility (Phase 8, scoped to the Spring Boot *Gradle plugin's* bundled ASM, sidestepped by declaring `mainClass` explicitly), this failure came from **Spring Framework's own runtime ASM** (`org.springframework.asm.ClassReader`, shaded inside `spring-core:6.2.1`, the version Boot 3.4.1 manages), hit inside `ClassPathScanningCandidateComponentProvider` — the exact machinery every `@SpringBootApplication`'s `@ComponentScan` depends on to read a class file's annotations before deciding whether it's a bean candidate. There is no equivalent "just tell it the answer" escape hatch for this one: any class file compiled to major version 69 anywhere in a scanned package trips it, not just the one class the scan happens to be looking for.

That raised an uncomfortable question about `user-service`: had it ever actually been proven to boot a real `ApplicationContext` under JDK 25? Checking the answer directly (not assuming): no. `JwtTokenProviderTest` needs no Spring context at all. `UserGrpcServiceImplTest`/`UserGrpcServiceImplErrorTest` construct `UserGrpcServiceImpl` by hand with Mockito-mocked collaborators. `UserGrpcServiceIntegrationTest` (Phase 15) starts a real in-process gRPC `Server`/`ManagedChannel` pair, but registers the service instance directly — no `@SpringBootTest`, no component scan, no real context. Every verification step since Phase 7 (`compileJava`, `assemble`, `build`, `bootJar`) only compiles or packages code; none of them execute `SpringApplication.run(...)`'s component-scanning path. `user-service` had a live, previously undiscovered risk of failing to start entirely, identical in shape to the Phase 12 `protobuf-java` BOM bug: a real defect on the actual runtime path, invisible to every check performed so far because none of them exercised the one code path that triggers it.

### Decision: bump both Spring Boot modules from 3.4.1 to 3.5.16

Verified empirically, not assumed from a changelog: bumping `order-service/build.gradle.kts`'s Spring Boot plugin version from `3.4.1` to `3.5.16` (the latest available release on Maven Central at the time) made `contextLoads()` pass outright — a full context boot, JPA `EntityManagerFactory` and the Hikari pool both initializing and shutting down cleanly. The same bump applied to `user-service/build.gradle.kts` compiled and passed its full existing suite unchanged (including against `spring-grpc-dependencies:0.7.0`, confirming that BOM tolerates the newer Boot line), and a new `UserServiceApplicationTests.contextLoads()` — the first test in this repo to actually boot `user-service`'s real `ApplicationContext` — passed with genuine proof in the log output: `Completed gRPC server shutdown`, plus the same JPA/Hikari lifecycle messages. `spring.grpc.server.port=0` and a test-only `app.jwt.secret` override are supplied as inline `@SpringBootTest(properties = ...)` values so the test doesn't depend on a fixed port or the unset `JWT_SECRET` environment variable Kubernetes deployment expects.

### Consequences: bumping to Boot 3.5.16

- Every future Spring Boot module in this repo (`payment-service`, `restaurant-service`, `api-gateway-service`) should start on `3.5.16`, not `3.4.1` — the version this repo's earlier phases pinned is now known to be fundamentally incompatible with actually running under this repo's JDK 25 toolchain, not just a source of isolated tool-specific workarounds like the earlier Lombok/Mockito/ASM findings.
- This is the second real "nothing before this test exercised the actual failing code path" bug found in this repo (after the Phase 12 `protobuf-java` BOM downgrade) — both reinforce the same lesson already recorded in [todo.md](../todo.md)'s Test Coverage Ledger: a module isn't proven to work until a test exists that would actually fail if it didn't, and `compileJava`/`assemble`/`bootJar` succeeding is not that proof for anything involving Spring's component scan.
- A minimal `@SpringBootTest` `contextLoads()` test is now the first test written for every future service module in this repo, before any business logic — it's the cheapest possible check that actually exercises real context startup, and it's what caught this.

## `order-service` domain model: `BigDecimal` for money, not the source's `double`

**Status:** Done — `order-service` domain model, Phase 18.

### Context: representing `Order.totalAmount`

The source's `Order` entity stores `totalAmount` as a primitive `double`. Binary floating-point types can't exactly represent most decimal fractions (`0.1` has no exact `double` representation, for instance) — a well-known, real source of rounding drift in money arithmetic that compounds the more a value gets added to, multiplied, or compared across a system, not a theoretical concern specific to this repo.

### Decision: `BigDecimal` for `Order.totalAmount`

`Order.totalAmount` is a `java.math.BigDecimal`, mapped with explicit JPA `precision = 19, scale = 2` (19 total digits, 2 after the decimal point — enough range for real currency amounts with exact cent precision, the same shape Hibernate itself defaults to for `BigDecimal` columns when left unspecified, made explicit here rather than relied on implicitly). Also carried over from `user-service`'s Phase 7 precedent rather than re-litigated: `Order.id` and the new `customerId` field both use `UUID` (`@GeneratedValue(strategy = GenerationType.UUID)` for `id`), not the source's un-generated `String id` / loosely-typed `String customerId` — `customerId` specifically should reference `user-service`'s `User.id`, itself a `UUID`.

### Consequences: `BigDecimal` for `Order.totalAmount`

- Equality/comparison on `totalAmount` must use `compareTo`/`equals` semantics correctly (`BigDecimal.equals` is scale-sensitive — `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is `false` — a real gotcha to keep in mind once tests are written against this field), not `==`, which isn't available for object types anyway but is an easy habit to carry over incorrectly from primitive `double` code.
- Any future service that also handles money (`payment-service` is the obvious one) should follow the identical `BigDecimal` convention for the same reason.

## `order-service` `OutboxRecord`: `traceId`/`spanId` columns deferred, not carried over from the source

**Status:** Done — `order-service` domain model, Phase 18.

### Context: trace-context columns with no tracer to populate them

The source's `OutboxRecord` includes `traceId`/`spanId` columns, populated from OpenTelemetry's active span context at the point an outbox row is written, so a downstream consumer can continue the same distributed trace. This repo already deliberately deferred OTel export wiring for `order-service` (Phase 16) — no collector exists yet to send traces to.

### Decision: leave `traceId`/`spanId` out of `OutboxRecord` for now

Adding non-nullable trace-context columns with no active tracer to populate them would mean either forcing placeholder values into every row or making the columns nullable purely to accommodate infrastructure that doesn't exist yet — both worse than not having the columns at all until they'd carry real data.

### Consequences: deferring `traceId`/`spanId`

- When OTel export wiring actually gets added to this repo, `OutboxRecord` (here and in every other module with an outbox table) needs these two columns added at that point, as a real schema change, not something to backfill quietly.

## `order-service` repositories: `OutboxRepository`'s `SKIP LOCKED` query carried over as-is, tested for ordering only

**Status:** Done — `order-service` repositories, Phase 19.

### Context: safe concurrent outbox polling

`OutboxRepository.findByOrderByCreatedTimeAsc` carries over the source's `@Lock(PESSIMISTIC_WRITE)` + `@QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})` combination unchanged — Hibernate's documented signal to append `FOR UPDATE SKIP LOCKED` to the generated query. This is the standard mechanism for safely running multiple instances of the same service polling one outbox table: each instance's poll skips rows another instance already has locked instead of blocking on them, so the same event never gets published twice by two instances racing on the same row. (This is also why `order_db`'s H2 connection URL, once `application.yaml` is written, needs `MODE=PostgreSQL` — plain H2 doesn't parse `SKIP LOCKED` without it.)

### Decision: carry the query over, test ordering/pagination directly, don't attempt to reprove the locking guarantee itself

`OrderRepositoryTest` (3 tests: `existsByIdAndStatus` true/false/not-found) and `OutboxRepositoryTest` (2 tests: empty result, oldest-10-first ordering) are both `@DataJpaTest`s against a real embedded H2 database — not mocked repositories, so the actual generated SQL and Hibernate mapping are what's under test. What they deliberately don't attempt is proving the `SKIP LOCKED` semantic itself under real concurrent transactions (two threads, one holding a lock while another polls and confirms it skips rather than blocks) — that would need manual transaction management outside `@DataJpaTest`'s default single-transaction-per-test wrapping, and the source's own equivalent test (`restaurant-service`'s `OutboxRepositoryTest`) doesn't attempt this either. The locking behavior is trusted as documented Hibernate/PostgreSQL-dialect behavior, the same way a library's own contract is trusted rather than re-verified from scratch.

Verified as real, not just written and assumed: both suites pass against actual Hibernate DDL (`customer_orders`/`outbox_record` tables genuinely created and dropped per test, visible in the run output), and `OutboxRepositoryTest`'s ordering assertion was deliberately broken (scrambled the expected order) and re-run to confirm it fails on wrong data, before being reverted.

### Consequences: `order-service` repositories

- A future concurrency test for the `SKIP LOCKED` guarantee itself remains open if it's ever worth the complexity — not attempted here, consistent with the source's own scope.
- `OrderRepository`/`OutboxRepository` both extend `JpaRepository<_, UUID>`, matching the Phase 18 domain model's UUID ids throughout — `OrderRepository` diverges from the source's `JpaRepository<Order, String>` for the same reason.

## `order-service` DTOs: UUID/BigDecimal typing carried through, one dead enum dropped

**Status:** Done — `order-service` DTOs, Phase 20.

### Context: request/event record typing, and an unreferenced enum

`CreateOrderRequest`, `OrderCreatedEvent`, `RestaurantApprovedEvent`, and `RestaurantRejectedEvent` are the first Java records in this repo. The source types their id fields as raw `String` and money as `double`. Separately, the source's `order-service` also defines a `RestaurantTicketStatus` enum (`PREPARING`/`REJECTED`) in the same `dto` package — grepping `order-service/src` there confirms it's referenced by nothing else in that module at all, only defined. The same enum also exists standalone in the source's `restaurant-service` and `payment-service` `dto` packages, which suggests the whole `dto` package gets copy-pasted across services without pruning what each one actually uses.

### Decision: extend existing typing conventions, drop the unreferenced enum

`CreateOrderRequest.customerId`, and the `orderId`/`customerId`/`ticketId` fields across `OrderCreatedEvent`/`RestaurantApprovedEvent`/`RestaurantRejectedEvent`, are all `UUID`, matching `Order`'s Phase 18 typing rather than the source's `String`. `OrderCreatedEvent.totalAmount` is `BigDecimal`, matching `Order.totalAmount`'s Phase 18 decision. `CreateOrderRequest` validates with `@NotNull` on the `UUID`/`BigDecimal` fields (plus `@Positive` on `totalAmount`, since Bean Validation constraints other than `@NotNull` silently pass on a `null` value rather than failing) and `@NotBlank` on `itemCode`. `RestaurantTicketStatus` is not carried over into `order-service` — nothing there would reference it, so adding it would just be reintroducing the same dead code found in the source.

### Consequences: `order-service` DTOs

- No dedicated tests were written for these — they're plain records with no behavior of their own, the same reasoning already applied to not testing `Order`/`OutboxRecord`/`OrderStatus` directly (Phase 18). Their shape gets exercised indirectly once `OrderController`/`OrderService`/`OutboxPublisherService` actually use them.
- Any future module in this repo that copies from the source's `dto` packages should check for the same kind of cross-service copy-paste leftovers before carrying a type over, not assume everything in a `dto` folder is actually load-bearing.

## `order-service`: `OrderService` combines create + saga transitions, with a dedicated `OrderNotFoundException`

**Status:** Done — `order-service` service layer, Phase 21.

### Context: order-service business logic entry point

The source's `OrderService` bundles `createOrder`, `confirmOrder` (restaurant-approved), and `cancelOrder` (restaurant-rejected) in one class, wrapped in manual OpenTelemetry span code and using a plain `IllegalArgumentException` when an order isn't found. This repo already deferred OTel export wiring (Phase 16) and dropped `OutboxRecord`'s `traceId`/`spanId` columns (Phase 18), so there's no trace context to restore here. Separately, `OrderRepository.existsByIdAndStatus` (Phase 19) was written specifically as an idempotency guard for exactly this kind of duplicate-Kafka-redelivery check, but sat unused until this phase gave it a caller. The source's controller layer also reads `customerId` from a gateway-injected header rather than trusting the request body — but no `api-gateway-service` or auth wiring exists yet in this repo to supply that trusted identity separately.

### Decision: order-service business logic entry point

`OrderService.createOrder` builds a `PENDING` `Order`, saves it, and stages a matching `OutboxRecord` in the same `@Transactional` method — trusting `CreateOrderRequest.customerId` directly rather than a separate authenticated-identity parameter, deferred until a real gateway/auth layer exists to enforce that boundary. `confirmOrder`/`cancelOrder` carry over the source's double-guard idempotency pattern unchanged in shape (a cheap `existsByIdAndStatus` check first, then `findById`, then a second status check after load, before mutating) but with all tracing code stripped, and a new `OrderNotFoundException` in place of the source's generic `IllegalArgumentException` — matching `user-service`'s established convention (`UserNotFoundException`, Phase 7) of a dedicated unchecked exception type per domain failure, rather than a generic JDK exception standing in for one.

### Consequences: order-service business logic entry point

- `OrderServiceTest` (7 tests) uses a real `ObjectMapper` instance rather than mocking it like the source does — mocking `writeValueAsString` to return a canned string would never prove the outbox payload actually serializes correctly, the same class of gap the Phase 12 `protobuf-java` bug slipped through. The test round-trips the real JSON back into `OrderCreatedEvent` and asserts on the deserialized fields. Verified with a real `./gradlew :order-service:test` run, and confirmed the suite genuinely catches wrong data by deliberately asserting a wrong `OrderStatus` and re-running before reverting.
- The `customerId`-trust-boundary gap is a real, tracked deviation from the source's security posture, not an oversight — it needs revisiting once `api-gateway-service` exists to inject a verified identity.

## `order-service`: `OutboxPublisherService` ships raw JSON via `KafkaTemplate`, not the source's `StreamBridge`

**Status:** Done — `order-service` service layer, Phase 22.

### Context: publishing staged outbox rows to Kafka

The source's `OutboxPublisherService` deserializes each `OutboxRecord.payload` back into `OrderCreatedEvent`, wraps it in a `Message`, and sends it through Spring Cloud Stream's `StreamBridge` with Kafka exactly-once-semantics config (`transaction-id-prefix`, `enable.idempotence`) and manual Micrometer tracing around every span. This repo chose `spring-kafka` directly over Cloud Stream back in Phase 16 specifically to avoid a binder abstraction this single-broker project doesn't need, and has no tracer to feed (Phase 18). The deserialize-then-reserialize step is also structurally pointless here: `OutboxRecord.payload` is already the exact JSON string that needs to end up on the wire — nothing about `KafkaTemplate` requires a typed Java object first.

### Decision: publishing staged outbox rows to Kafka

`publishPendingOutboxRecords` polls a batch via `OutboxRepository.findByOrderByCreatedTimeAsc` (Phase 19's `SKIP LOCKED` query) inside one `@Transactional`/`@Scheduled` method, then publishes each record's raw JSON payload as-is through `KafkaTemplate<String, String>.send(Message<String>)` — keyed on `aggregateId` for per-order partition affinity, with an `eventType` Kafka header even though only one event type exists today. The send is blocked on (`.get()` on the returned `CompletableFuture`) so a record is only deleted after a confirmed successful publish. Failures are caught **per record** and logged, not allowed to propagate and roll back the whole batch's transaction the way the source's rethrow does — since `confirmOrder`/`cancelOrder` (Phase 21) are already idempotency-guarded against duplicate delivery, catching per-record avoids needlessly re-publishing already-successful sends in the same batch.

### Consequences: publishing staged outbox rows to Kafka

- `OutboxPublisherServiceTest` (4 tests) covers: an empty batch doing nothing, a successful publish asserting the real message payload/topic/key/header values then deleting the record, a failed send leaving the record undeleted for retry, and — the specific proof for the per-record-catch decision — a two-record batch where one send fails and the other still gets deleted, showing the batch doesn't roll back as a unit. Verified with a real `./gradlew :order-service:test` run, and confirmed the suite genuinely catches wrong data by deliberately asserting a wrong Kafka key header and re-running before reverting.
- This repo now has no Kafka transactional/exactly-once guarantee at all — messages are published at-least-once, and a crash between a confirmed send and the delete could republish. That's an accepted tradeoff given Phase 21's idempotency guards downstream, not an oversight.

## `order-service`: `OrderController` has no perimeter-header trust boundary yet, and returns 201 over the source's 200

**Status:** Done — `order-service` service layer, Phase 23.

### Context: the REST entry point for order creation

The source's `OrderController` extracts a gateway-injected `X-Perimeter-User-Id` header and cross-checks it against the request body's `customerId`, throwing a `ClientIdentityMismatchException` (mapped to HTTP 403) on mismatch — a real security boundary, but one that assumes an `api-gateway-service` upstream that doesn't exist yet in this repo, and a `ClientIdentityMismatchException` type this repo has no reason to define with nothing to throw it. `OrderService.createOrder` (Phase 21) already made the corresponding call to trust `CreateOrderRequest.customerId` directly rather than take a separately-verified identity parameter.

### Decision: the REST entry point for order creation

`OrderController` stays minimal: `@PostMapping` on `/orders`, `@Valid @RequestBody CreateOrderRequest`, straight into `OrderService.createOrder`. It returns the `Order` entity directly in the response body rather than a dedicated response DTO — `Order` has no lazy associations to leak, and a DTO that would mirror it field-for-field with exactly one caller right now is a speculative abstraction, not a real decoupling need yet. The response status is `201 Created`, correct REST semantics for a resource-creating POST, deviating from the source's `200 OK`.

### Consequences: the REST entry point for order creation

- The perimeter-header trust boundary is a tracked gap, not a silent omission — it needs to be added once `api-gateway-service` exists to actually inject a verified identity; until then, anything calling this endpoint directly can claim any `customerId`.
- `OrderControllerTest` (2 tests, `@WebMvcTest`) covers the happy path (asserting the real `201` status and JSON body fields) and a bean-validation failure (`400`). No header/auth-boundary test exists yet, since no such boundary exists yet either. Verified with a real `./gradlew :order-service:test` run, and confirmed the suite genuinely catches wrong data by deliberately asserting a wrong `status` field value and re-running before reverting.

## `order-service`: `OrderConsumerConfig` uses `@KafkaListener`, not Cloud Stream's functional `Consumer<Message<T>>` beans

**Status:** Done — `order-service` service layer, Phase 24.

### Context: consuming restaurant approval/rejection events

The source's `OrderConsumerConfig` declares two `@Bean Consumer<Message<T>>` functions, wired through Spring Cloud Stream's functional binding model and wrapped in manual OpenTelemetry span code around each invocation. None of that machinery exists here: this repo chose `spring-kafka` directly over Cloud Stream (Phase 16) and has no tracer to feed (Phase 18). Separately, `OutboxPublisherService` (Phase 22) publishes raw JSON strings with no Spring `JsonSerializer` type headers (`__TypeId__`), so a consumer-side typed `JsonDeserializer` would have nothing to key its type resolution off without extra fixed-default-type configuration per listener.

### Decision: consuming restaurant approval/rejection events

`OrderConsumerConfig` is a plain `@Component` with two `@KafkaListener` methods, one per topic (`restaurant-approved-topic`/`restaurant-rejected-topic`, consumer groups `order-approved-group`/`order-rejected-group` — topic and group names carried over from the source's own config for continuity). Each method takes the raw JSON `String` payload and deserializes it manually via `ObjectMapper.readValue`, symmetric with how `OutboxPublisherService` publishes, before delegating straight to `OrderService.confirmOrder`/`cancelOrder` (Phase 21).

### Consequences: consuming restaurant approval/rejection events

- Unlike the source, which has no dedicated test for this class (relying entirely on its `OrderServiceTest` for the confirm/cancel logic it delegates to), `OrderConsumerConfigTest` (2 tests, new to this repo) was added specifically to prove real JSON deserializes into the right DTO and dispatches to the right `OrderService` method per topic — a wiring mistake (e.g. the wrong DTO type paired with the wrong topic) is exactly what a purely logic-focused test like `OrderServiceTest` structurally can't catch, since it never touches JSON at all. Verified with a real `./gradlew :order-service:test` run, and confirmed the suite genuinely catches wrong data by deliberately asserting a wrong `reason` field and re-running before reverting.
- Confirmed via a real `./gradlew :order-service:test` run (not assumed) that adding two `@KafkaListener` consumers and the `@Scheduled` outbox poller doesn't break `OrderServiceApplicationTests.contextLoads()` even with no live Kafka broker running locally — Spring Kafka's consumer containers just retry joining their consumer group in the background rather than failing application startup, since no `spring.kafka.admin.fail-fast` is configured.

## `order-service/application.yaml`: `default`/`postgres` profiles, plain String Kafka (de)serializers

**Status:** Done — `order-service` service layer, Phase 25.

### Context: wiring config for a module that had none yet

No `application.yaml` existed for `order-service` before this phase — Phase 16 deferred it since nothing needed real config until the service layer and Kafka wiring actually existed. The source's version targets infrastructure this repo doesn't have: `host.minikube.internal` for Kafka, an OTLP collector, and a `k8sdb` profile for a Minikube-hosted Postgres — plus Spring Cloud Stream binder config (`transaction-id-prefix`, `enable.idempotence`, DLQ bindings) that doesn't apply now that `order-service` uses plain `spring-kafka` (Phase 16, 22, 24).

### Decision: wiring config for a module that had none yet

Follows `user-service`'s established `default`/`postgres` profile split (Phase 7) rather than the source's `default`/`k8sdb`, with `order_db`/`order_service_schema` naming mirroring `user_db`/`user_service_schema`. Kafka `bootstrap-servers` defaults to `localhost:9092` (no Minikube infra here), with plain `StringSerializer`/`StringDeserializer` configured on both producer and consumer sides — matching `OutboxPublisherService`/`OrderConsumerConfig`'s raw-JSON-string approach (Phase 22, 24) rather than Spring's typed `JsonSerializer`. The `app.outbox.topic`/`batch-size`/`polling-delay-ms` properties reuse the source's own naming and defaults (`order-created-topic`, `10`, `500`ms), declared explicitly here even though `OutboxPublisherService`'s `@Value` defaults already cover them, for discoverability and env-var overridability without touching code.

### Consequences: wiring config for a module that had none yet

- Confirmed with a real `./gradlew :order-service:test` run that the full module (all 21 tests, including `contextLoads()`) stays green with this config in place and no live Kafka broker or Postgres running — H2 covers the JPA layer, and Kafka's consumer/producer clients tolerate a missing broker at startup by retrying rather than failing fast.
- This closes out the "`order-service` service layer + Kafka wiring" item tracked in [todo.md](../todo.md) since Phase 17 — `OrderService`, `OutboxPublisherService`, `OrderController`, `OrderConsumerConfig`, and this config file are all in place with real, verified test coverage (Phases 21-25).
- `payment-service`/`restaurant-service`, once built, should follow this same `default`/`postgres` profile convention and plain-String Kafka (de)serializer choice for consistency, rather than each re-deciding it from the source's Minikube-oriented config.

## `docker-compose.yml`: Postgres + Kafka only, scoped to what actually needs proving right now

**Status:** Done — local Docker infrastructure, Phase 26.

### Context: what Docker is actually for at this point

The source's `docker-compose.yml` bundles Postgres, a dual-listener KRaft Kafka broker (one listener for containers on the Docker network, one advertised at `host.minikube.internal` for host-machine tools), and a full OTel/Prometheus/Loki/Tempo/Grafana observability stack, plus a per-service `Dockerfile` for each of its four Spring Boot services. This repo's own `todo.md` had already scoped this down before this phase started: the OTel/observability stack was deferred back at Phase 16 (no collector exists to feed), and per-service `Dockerfile`s were explicitly deferred "until there's a complete multi-service stack worth deploying" — only `user-service` and `order-service` exist so far, with `payment-service`/`restaurant-service` still ahead. Right now, the only thing Docker actually needs to prove is `order-service`'s `postgres` profile and its Kafka wiring (Phases 21-25) against real infrastructure instead of H2/mocks.

### Decision: what actually needs proving right now

`docker-compose.yml` has exactly two services: `postgres-db` (matching `application.yaml`'s `postgres` profile: `saga_db`, user/password `postgres`) and `kafka-broker` (KRaft mode, single `PLAINTEXT` listener rather than the source's dual internal/external setup — with no app containers on the Docker network yet, there's no "internal" side to serve, so the dual-listener complexity has nothing to do). A new `docker/init-schemas.sql`, mounted via Postgres's `docker-entrypoint-initdb.d` convention, creates `user_service_schema` and `order_service_schema` on first startup — without it, `application.yaml`'s `currentSchema=...` postgres-profile URLs would fail against a fresh container, since Postgres doesn't auto-create schemas referenced only in a JDBC connection string.

A real, machine-specific conflict surfaced while verifying this: this dev machine already runs `coolify-full` (a separate, permanently-running containerized project) with its own Postgres bound to host port 5432. `postgres-db` maps to host port **5433** instead, documented in a compose comment and in [CONTRIBUTING.md](../CONTRIBUTING.md), so running `saga-full`'s stack never touches `coolify-full`'s.

### Consequences: what actually needs proving right now

- Verified for real, not just written and assumed to work: `docker compose up -d` started both containers; `\dn` inside `postgres-db` confirmed both schemas exist; `kafka-topics --list` inside `kafka-broker` confirmed the broker answers client requests. `order-service` was then actually booted (`SPRING_PROFILES_ACTIVE=postgres DATABASE_PORT=5433 ./gradlew :order-service:bootRun`) against both containers: the log showed a real `HikariPool` connection to `PgConnection`, `Started OrderServiceApplication`, and — the strongest proof — both `@KafkaListener` consumer groups (`order-approved-group`/`order-rejected-group`) actually joining their consumer group and getting real partition assignments against the live broker, not the retry-forever behavior seen in every test run so far with no broker present.
- The port-5432-vs-5433 conflict was caught *before* any container collision happened — the first `docker compose up -d` attempt was stopped mid-image-pull once `docker ps` showed `coolify-db` already on 5432, and the compose file was fixed before any port bind was actually attempted against a running system.
- No app-service containers or `Dockerfile`s exist yet — `order-service`/`user-service` still run as host JVM processes (`bootRun` or the IDE) connecting into these two containers, not as containers themselves. That's the natural next step once `payment-service`/`restaurant-service` exist and there's a complete stack worth containerizing.

## `payment-service` scaffold: the actual next link in the saga chain, not an arbitrary pick

**Status:** Done — `payment-service` scaffold, Phase 27.

### Context: which service module comes next

`order-service`'s `OrderConsumerConfig` (Phase 24) already consumes `restaurant-approved-topic`/`restaurant-rejected-topic`, which made `restaurant-service` look like the natural next module to build — it's the one `order-service` is already waiting on. Checking the source's actual event wiring (`RestaurantConsumerConfig`/`PaymentConsumerConfig`) showed otherwise: `payment-service` consumes `OrderCreatedEvent` first and publishes `PaymentProcessedEvent`, which is what `restaurant-service` actually consumes to decide approval — `restaurant-service` can't be meaningfully built before `payment-service` exists, since its own consumer depends on an event type only `payment-service` would ever produce. `payment-service` also consumes `RestaurantRejectedEvent` to issue a refund — a genuine compensating transaction, the same pattern `order-service`'s `cancelOrder` already implements on the order side. Right now, `OrderCreatedEvent` is being published into a void: nothing in this repo consumes `order-created-topic` yet.

### Decision: which service module comes next

`payment-service` is built next, mirroring `order-service`'s own Phase 16 scaffold shape: `spring-kafka` direct instead of the source's Cloud Stream binder, Spring Boot 3.5.16 from the start (not 3.4.1, learned the hard way in Phase 17), Java 25 toolchain, Lombok pinned to 1.18.42 (Phase 7's lesson), and an explicit `springBoot { mainClass.set(...) }` (Phase 8's lesson) — so this module starts already avoiding every JDK-25-compatibility issue found the hard way building `user-service`/`order-service`, rather than rediscovering them one at a time. `PaymentServiceApplicationTests.contextLoads()` was written immediately, before any business logic, per the Phase 17 lesson that this is the cheapest real check that actually exercises context startup.

### Consequences: which service module comes next

- `payment-service` has no REST controller in the source at all — it's pure Kafka consumer/producer plus JPA, unlike `order-service`. `actuator` + `web` are still included for the same reason as `order-service`: actuator's HTTP health endpoints need a web server to actually serve them over HTTP, even with no application-level controller. `spring-boot-starter-validation` is deliberately not added yet — there's no request DTO to validate without a controller, unlike `order-service`'s `CreateOrderRequest`.
- Verified with a real `./gradlew :payment-service:test` run: `contextLoads()` passes with a genuine `HikariPool`/JPA boot in the log, not assumed from the config alone — this module never has the Phase 17-shaped gap (a module compiling/packaging fine while never actually proving `ApplicationContext` boots), since the check exists from its very first commit.

## `payment-service` domain model: same `UUID`/`BigDecimal` conventions as `order-service`, no new fork

**Status:** Done — `payment-service` domain model, Phase 28.

### Context: Payment/PaymentStatus/OutboxRecord

The source's `Payment` entity declares `@Id private String id` with no `@GeneratedValue` at all — the same un-generated-primary-key pattern already corrected for `User.id` (Phase 7) and `Order.id` (Phase 18); its own service code (`PaymentService.deductFunds`) works around this by manually calling `UUID.randomUUID().toString()` before construction. `Payment.orderId` is also a loose `String`, and `amount` is a boxed `Double` — the same money-as-floating-point concern `Order.totalAmount` already resolved with `BigDecimal` (Phase 18). `OutboxRecord` again carries `traceId`/`spanId` columns, populated from OpenTelemetry, that this repo's `payment-service` (Phase 27) has no tracer to feed — identical to `order-service`'s Phase 18 outbox decision.

### Decision: Payment/PaymentStatus/OutboxRecord

None of this is a new fork — it's the same typing conventions Phase 18 already established, applied to a second module: `Payment.id` uses Hibernate-native `UUID` generation (`@GeneratedValue(strategy = GenerationType.UUID)`), `orderId` is typed `UUID` to match `order-service`'s `Order.id` directly rather than a loosely-typed string copy of it, `amount` is `BigDecimal` (`precision = 19, scale = 2`, matching `Order.totalAmount`'s mapping exactly), and `orderId` keeps the source's `unique = true` constraint — `PaymentService.processPaymentSaga`'s idempotency check (`findByOrderId`) depends on at most one payment ever existing per order, worth enforcing at the schema level, not just in application code. `OutboxRecord` is a direct copy of `order-service`'s Phase 18 version, minus the trace columns. `PaymentStatus` (`APPROVED`/`REFUNDED`/`FAILED`) is unchanged from the source.

### Consequences: Payment/PaymentStatus/OutboxRecord

- `./gradlew :payment-service:compileJava` and `contextLoads()` both verified green with no regression — Hibernate now has two real entities to map, not just an empty context.
- No dedicated tests were written for these — plain entities with no behavior of their own, the same reasoning already applied to `order-service`'s domain model (Phase 18). Their shape gets exercised indirectly once `PaymentRepository`/`OutboxRepository`/`PaymentService` actually use them.

## `payment-service` repositories: same shape as `order-service`'s, re-typed to `UUID`

**Status:** Done — `payment-service` repositories, Phase 29.

### Context: PaymentRepository/OutboxRepository

The source's `PaymentRepository` (`findByOrderId`) and `OutboxRepository` (`findByOrderByCreatedTimeAsc`, with the same `PESSIMISTIC_WRITE` + `SKIP LOCKED` query hint as `order-service`'s) map directly onto this repo's Phase 28 domain model — no new design question here, just the same `String`-vs-`UUID` typing gap Phase 28 already decided to close.

### Decision: PaymentRepository/OutboxRepository

`PaymentRepository extends JpaRepository<Payment, UUID>` with `findByOrderId(UUID orderId)`, and `OutboxRepository` carries over `order-service`'s `SKIP LOCKED` query unchanged, both re-typed to `UUID` matching Phase 28. `PaymentRepositoryTest` (2 tests: found/not-found by `orderId`) and `OutboxRepositoryTest` (2 tests: empty result, oldest-10-first ordering) are both real `@DataJpaTest`s against embedded H2, mirroring `order-service`'s `OrderRepositoryTest`/`OutboxRepositoryTest` (Phase 19) test shape exactly.

### Consequences: PaymentRepository/OutboxRepository

- Verified as real, not just written and assumed: both suites pass against actual Hibernate DDL (`payments`/`outbox_record` tables genuinely created and dropped per test, visible in the run output — `Hibernate: drop table if exists outbox_record cascade` / `payments cascade`), and `OutboxRepositoryTest`'s ordering assertion was deliberately scrambled and re-run to confirm it fails on wrong data, before being reverted — the same verification discipline as Phase 19.
- Like `order-service`'s Phase 19 decision, the `SKIP LOCKED` locking guarantee itself isn't independently reproven under real concurrent transactions here either — trusted as documented Hibernate/PostgreSQL-dialect behavior, consistent with both the source's own test scope and this repo's prior decision.

## `payment-service` DTOs: two dead event types dropped, one enum's drift from `order-service` corrected

**Status:** Done — `payment-service` DTOs, Phase 30.

### Context: which cross-service events actually need a DTO

`payment-service` needs its own local copies of the events it consumes/produces — `order-service`'s `OrderCreatedEvent`, `RestaurantRejectedEvent`, and its own `PaymentProcessedEvent` — since services in this repo share no event-contract library, only the wire format. The source's `dto` package for this module has five records, not three: `PaymentFailedEvent` and `RestaurantPreparedEvent`/`RestaurantTicketStatus` are also present. Grepping the source's actual `payment-service` code (not just the `dto` package) showed `PaymentFailedEvent` is referenced exactly once outside its own file — a comment in `OutboxPublisherService` ("Ready for gateway rejections!") marking a dispatch case that's never actually reached, since `PaymentService.deductFunds` always succeeds unconditionally and never constructs one. `RestaurantPreparedEvent`/`RestaurantTicketStatus` are referenced nowhere outside their own files at all — even `PaymentService`'s own class-level Javadoc describes "`RestaurantPreparedEvent`s indicat[ing] a rejection," a concept the actual code never uses (it consumes `RestaurantRejectedEvent` instead). The source's local `OrderStatus` copy in this package also has a fourth value, `PAID`, that `order-service`'s actual `OrderStatus` enum (Phase 18: `PENDING`/`CANCELLED`/`SUCCESS`) never produces.

### Decision: which cross-service events actually need a DTO

Only the three DTOs actually consumed/produced by `PaymentConsumerConfig`/`PaymentService` are built: `OrderCreatedEvent`, `PaymentProcessedEvent`, `RestaurantRejectedEvent` — the same "don't carry over unreferenced source cruft" call already made for `order-service`'s dead `RestaurantTicketStatus` in Phase 20, applied here to two DTOs and one enum instead of one enum. `payment-service`'s local `OrderStatus` copy matches `order-service`'s real 3-value enum, not the source's inconsistent 4-value one. All `UUID`/`BigDecimal` typing follows the conventions already established in Phases 18/28: `OrderCreatedEvent`/`RestaurantRejectedEvent` get their own local `orderId`/`customerId: UUID` fields (unavoidable duplication across service boundaries in an event-driven system with no shared contract library), while `PaymentProcessedEvent.status` reuses `payment-service`'s own domain `PaymentStatus` enum directly rather than a separate DTO-local copy, since `payment-service` is the one publishing it — the same pattern `order-service`'s `OrderCreatedEvent.status: OrderStatus` already uses (Phase 20).

### Consequences: which cross-service events actually need a DTO

- If a real payment-failure or restaurant-preparation-tracking need shows up later, the matching DTO gets added at that point, as a real addition driven by an actual code path — not speculatively pre-built now to mirror a source package that itself never wires them up.

## `payment-service`: `PaymentService` reuses `order-service`'s idempotency-guard shape, drops one defensive check the source's code never actually needs

**Status:** Done — `payment-service` service layer, Phase 30.

### Context: processPaymentSaga/handleOrderCompensation

The source's `PaymentService.processPaymentSaga` throws `IllegalArgumentException` if the incoming `OrderCreatedEvent.status` isn't `PENDING`, and its idempotency guards use a single `findByOrderId` call followed by an in-memory check, rather than `order-service`'s two-step "cheap `existsBy` check, then load" pattern (Phase 21). It also uses `IllegalArgumentException` for "payment not found," the same generic-exception pattern `order-service`'s Phase 21 already moved away from in favor of dedicated types.

### Decision: processPaymentSaga/handleOrderCompensation

The defensive `PENDING`-only check is dropped: `OrderService.createOrder` (Phase 21) is the only code in this repo that ever constructs an `OrderCreatedEvent`, and it's always `PENDING` — there's no code path in this system where a non-`PENDING` event could ever arrive, making the check unreachable validation rather than real defense. `PaymentRepository` gained `existsByOrderId`/`existsByOrderIdAndStatus` (extending Phase 29's repository) specifically so `processPaymentSaga`/`handleOrderCompensation` can use the same cheap-check-before-load idempotency shape as `order-service`'s `confirmOrder`/`cancelOrder` — `existsByOrderIdAndStatus(orderId, REFUNDED)` short-circuits before ever loading the full `Payment` row for the common duplicate-event case, and a second in-memory status check after `findByOrderId` protects against a genuine TOCTOU race between two service instances both passing the cheap check before either commits. A new `PaymentNotFoundException` replaces the source's `IllegalArgumentException`, matching `OrderNotFoundException`'s (Phase 21) convention.

### Consequences: processPaymentSaga/handleOrderCompensation

- `PaymentServiceTest` (6 tests) uses a real `ObjectMapper` instance, not a mock, for the same reason as `OrderServiceTest` (Phase 21) — round-tripping the real outbox JSON payload back into `PaymentProcessedEvent` actually proves serialization correctness rather than trusting a canned mock string. Covers: the happy path with real payload assertions, the duplicate-payment skip, both compensation idempotency guards (pre-load `existsBy` short-circuit and the post-load race-condition re-check), the status transition to `REFUNDED`, and `PaymentNotFoundException`. Verified with a real `./gradlew :payment-service:test` run, and confirmed the suite genuinely catches wrong data by deliberately asserting a wrong `PaymentStatus` on the deserialized outbox payload and re-running before reverting. Full multi-module suite (53 tests across 4 modules) also verified green with no regressions.
- `payment-service` now has real business logic wired end-to-end for both saga directions it participates in — charge on order creation, refund on restaurant rejection — matching `order-service`'s `confirmOrder`/`cancelOrder` on the other side of the same compensating-transaction pair.

## `payment-service` Kafka wiring: `OutboxPublisherService`, `PaymentConsumerConfig`, `application.yaml`

**Status:** Done — `payment-service` Kafka wiring, Phase 31.

### Context: closing the loop from OutboxRecord to a live broker

`PaymentService` (Phase 30) stages `OutboxRecord`s and reacts to consumed events, but nothing yet moved those records onto Kafka or subscribed to the topics `PaymentConsumerConfig` needs. The source's `OutboxPublisherService` for this module is structurally identical to `order-service`'s (Phase 22) — `StreamBridge`, Micrometer tracing, a `PaymentProcessed`/`PaymentFailed` dispatch switch — none of which applies here for the same reasons already established. Its `application.yaml` also targets Minikube-hosted infrastructure (`host.minikube.internal`, a `k8sdb` profile) this repo doesn't have.

### Decision: closing the loop from OutboxRecord to a live broker

All three files are direct structural repeats of `order-service`'s Phase 22/24/25, with no new forks: `OutboxPublisherService` ships each `OutboxRecord`'s raw JSON payload via `KafkaTemplate<String, String>` to `payment-processed-topic`, per-record try/catch so one failure doesn't roll back the batch. `PaymentConsumerConfig` is a plain `@Component` with two `@KafkaListener` methods — `order-created-topic` (group `payment-group`) → `processPaymentSaga`, `restaurant-rejected-topic` (group `payment-compensation-group`) → `handleOrderCompensation` — each deserializing the raw JSON payload manually via `ObjectMapper`, symmetric with the publish side. `application.yaml` follows `user-service`/`order-service`'s `default`/`postgres` profile split (not the source's `default`/`k8sdb`), `payment_db`/`payment_service_schema` naming, and the same plain `StringSerializer`/`StringDeserializer` Kafka config. `docker/init-schemas.sql` (Phase 26) gained a `payment_service_schema` line to match.

### Consequences: closing the loop from OutboxRecord to a live broker

- `OutboxPublisherServiceTest` (4 tests) and `PaymentConsumerConfigTest` (2 tests, new to this repo like `order-service`'s Phase 24 equivalent — the source has no dedicated consumer test) mirror `order-service`'s Phase 22/24 suites exactly. Verified with a real `./gradlew :payment-service:test` run (17 tests in this module now), and confirmed both new suites genuinely catch wrong data by deliberately breaking a Kafka key header assertion and a deserialized `reason` field, re-running to confirm failure, then reverting.
- Verified end-to-end against real infrastructure, not just unit-level mocks: `docker compose up -d` (recreated so the updated `init-schemas.sql` actually ran — the script only executes on a container's first startup, so the already-running containers from Phase 26 didn't pick up the new schema line automatically), confirmed all three schemas present via `\dn`, then booted `payment-service` for real (`SPRING_PROFILES_ACTIVE=postgres DATABASE_PORT=5433 ./gradlew :payment-service:bootRun`) — a genuine `HikariPool`→`PgConnection` against `payment_service_schema`, and both `payment-group`/`payment-compensation-group` consumers actually joining and getting real partition assignments (`order-created-topic-0`/`restaurant-rejected-topic-0`) against the live broker, the same standard of proof Phase 26 established for `order-service`.
- Both saga participants built so far (`order-service`, `payment-service`) are now fully wired for their half of the compensating-transaction pair: `order-service` creates and reacts to restaurant decisions, `payment-service` charges and refunds. `restaurant-service` is the missing middle link — it consumes `PaymentProcessedEvent` and is what actually produces the `RestaurantApproved`/`RejectedEvent`s both existing services already wait on.

## `restaurant-service` scaffold: third repeat of the same established shape

**Status:** Done — `restaurant-service` scaffold, Phase 32.

### Context: starting the saga's third participant

The source's `restaurant-service` `pom.xml` and entry point are structurally identical to `order-service`'s and `payment-service`'s own source `pom.xml`s — actuator, data-jpa, H2/Postgres, Cloud Stream Kafka, OTel, Lombok — and it also has no REST controller. By this third module, every adaptation decision (`spring-kafka` direct, Boot 3.5.16 from the start, Lombok 1.18.42 pinned, explicit `mainClass`, `actuator`+`web` without a controller, `contextLoads()` written first) is already established precedent from Phases 16/27, not a new judgment call.

### Decision: starting the saga's third participant

`restaurant-service` scaffolded identically to `payment-service`'s Phase 27: `settings.gradle.kts` registration, `build.gradle.kts` matching the same dependency set and JDK-25-compatibility pins, `RestaurantServiceApplication` entry point, and `RestaurantServiceApplicationTests.contextLoads()` written immediately, before any business logic.

### Consequences: starting the saga's third participant

- Verified with a real `./gradlew :restaurant-service:test` run: `contextLoads()` passes with a genuine `HikariPool`/JPA boot in the log, avoiding the Phase 17-shaped gap from its very first commit, same as `payment-service`. Full multi-module suite (60 tests across 5 modules) also verified green with no regressions.
- With three of four planned service modules now scaffolded (`api-gateway-service` remains unstarted), the pattern established in Phases 16/27/32 — mirror the source's dependency set, apply the same JDK-25 pins, write `contextLoads()` first — should need no further re-litigation for `api-gateway-service` either.

## `restaurant-service` domain model: natural-key `InventoryItem`, the one `RestaurantTicketStatus` that isn't dead code

**Status:** Done — `restaurant-service` domain model, Phase 33.

### Context: InventoryItem/InventoryStatus/RestaurantTicket/RestaurantTicketStatus/OutboxRecord

`RestaurantTicket` repeats the same un-generated-primary-key pattern already corrected twice (`User.id` Phase 7, `Order.id` Phase 18, `Payment.id` Phase 28): `@Id private String id` with no `@GeneratedValue`, worked around in `RestaurantService.createAndSaveTicket` by manually calling `UUID.randomUUID().toString()`. `RestaurantTicket.orderId` is a loose `String` that should reference `order-service`'s `Order.id`. `OutboxRecord` again carries the `traceId`/`spanId` columns this repo's `restaurant-service` (Phase 32) has no tracer to feed. `InventoryItem`, by contrast, uses `itemCode` as its primary key — a genuine natural business key (the same string identifying an item across `order-service`'s `Order.itemCode`, `payment-service`'s DTOs, etc.), not a surrogate id standing in for one, so it doesn't fit the `UUID`-generation pattern applied everywhere else. `RestaurantTicketStatus` (`PREPARING`/`REJECTED`) is genuinely used here — unlike the dead copies of the same enum name already found and dropped from `order-service`'s (Phase 20) and `payment-service`'s (Phase 30) `dto` packages, this is the real domain enum `RestaurantTicket.status` and `RestaurantService`'s dispatch logic actually consume.

### Decision: InventoryItem/InventoryStatus/RestaurantTicket/RestaurantTicketStatus/OutboxRecord

`RestaurantTicket.id` uses Hibernate-native `UUID` generation, `orderId` is typed `UUID` matching `Order.id`, and this identifier is what actually crosses the wire — `order-service`'s own `RestaurantApprovedEvent.ticketId` (Phase 20) is already `UUID`-typed, so this decision was effectively already made by the consuming side. `InventoryItem` is carried over unchanged (`itemCode` `String` primary key, `stockCount` `int`) — the natural-key exception to the UUID convention, not an oversight. `OutboxRecord` is a direct copy of the established shape minus trace columns. `InventoryStatus` (`ALLOCATED`/`INSUFFICIENT_STOCK`/`ITEM_NOT_FOUND`) is unchanged from the source.

### Consequences: InventoryItem/InventoryStatus/RestaurantTicket/RestaurantTicketStatus/OutboxRecord

- `./gradlew :restaurant-service:compileJava` and `contextLoads()` both verified green with no regression; no dedicated tests written for these plain entities, the same reasoning already applied in Phases 18/28.
- `RestaurantService`'s own validation logic (deferred to the next phase) includes a `PaymentProcessedEvent.status != APPROVED` check that may turn out to be unreachable given `payment-service`'s actual behavior — `PaymentService` (Phase 30) only ever publishes a `PaymentProcessedEvent` once, immediately after creating a `Payment` with status `APPROVED`, never at `REFUNDED` time — the same shape as the defensive check dropped in Phase 30. Worth re-examining then, not decided here.

## `restaurant-service` repositories: same shape as `order-service`/`payment-service`'s, no new decisions

**Status:** Done — `restaurant-service` repositories, Phase 34.

### Context: InventoryItemRepository/RestaurantTicketRepository/OutboxRepository

The source's three repositories map directly onto Phase 33's domain model. `InventoryItemRepository` needs no custom query at all — `JpaRepository.findById` on the natural-key `itemCode` covers everything `RestaurantInventoryService` needs. `RestaurantTicketRepository.existsByOrderId` is exactly the idempotency-guard shape `order-service`/`payment-service` already established. `OutboxRepository` carries the same `SKIP LOCKED` query as both other modules.

### Decision: InventoryItemRepository/RestaurantTicketRepository/OutboxRepository

All three repositories are direct ports, re-typed to match Phase 33 (`RestaurantTicketRepository extends JpaRepository<RestaurantTicket, UUID>` with `existsByOrderId(UUID orderId)`; `InventoryItemRepository extends JpaRepository<InventoryItem, String>` unchanged, since `itemCode` stayed a natural-key `String`). `InventoryItemRepositoryTest` (2: found/not-found by `itemCode`), `RestaurantTicketRepositoryTest` (2: `existsByOrderId` true/false), and `OutboxRepositoryTest` (2: empty result, oldest-10-first ordering) mirror the test shape already established in Phases 19/29.

### Consequences: InventoryItemRepository/RestaurantTicketRepository/OutboxRepository

- Verified as real, not just written and assumed: all three suites pass against actual Hibernate DDL (`restaurant_inventory`/`restaurant_tickets`/`outbox_record` tables genuinely created and dropped per test), and `OutboxRepositoryTest`'s ordering assertion was deliberately scrambled and re-run to confirm it fails on wrong data, before being reverted. Full multi-module suite (66 tests across 5 modules) also verified green.
- `RestaurantTicketRepository` deliberately has no `findByOrderId` (unlike `PaymentRepository`, Phase 29) — nothing in `RestaurantService` needs to load and mutate an existing ticket; `restaurant-service` produces the saga's approval/rejection decision, it isn't itself the target of a later compensating action the way `order-service`/`payment-service` are.

## `restaurant-service` DTOs + `RestaurantService`: real validation checks kept, one unreachable check dropped

**Status:** Done — `restaurant-service` service layer, Phase 35.

### Context: the saga's decision point

Unlike `order-service`'s and `payment-service`'s `dto` packages, all five of the source's `restaurant-service` DTOs (`PaymentProcessedEvent`, `PaymentStatus`, `RestaurantApprovedEvent`, `RestaurantRejectedEvent`, `RestaurantEvent`) are genuinely used — no dead code to drop this time. `RestaurantEvent` is a sealed interface (`permits RestaurantApprovedEvent, RestaurantRejectedEvent`) that lets `saveRestaurantTicketOutbox` accept either event type polymorphically through one `orderId()`/`customerId()` contract — a clean, real use of Java's sealed interfaces, not source cruft to strip.

`RestaurantService.processRestaurantStep` validates several things about the incoming `PaymentProcessedEvent`: required-field null checks, `quantity`/`amount` positivity, and `status != APPROVED`. Phase 33 flagged the last one as suspicious — `PaymentService` (Phase 30) only ever publishes a `PaymentProcessedEvent` immediately after creating a `Payment` with status `APPROVED`, never at `REFUNDED` time, so that branch can't currently fire.

### Decision: the saga's decision point

The null-field and `quantity`/`amount` checks are kept — unlike the `OrderCreatedEvent`-must-be-`PENDING` check dropped in Phase 30 (an intra-repo invariant with exactly one producer, fully controlled), these guard against a genuinely different failure mode: a malformed or corrupted message on a shared Kafka topic, which no single service in this repo fully controls end-to-end. The `status != APPROVED` check is dropped, matching Phase 30's precedent exactly — it's the same shape of "unreachable given this repo's own producer" branch. All five DTOs get the established `UUID`/`BigDecimal` typing; `RestaurantEvent.orderId()`/`customerId()` are typed `UUID` accordingly. `RestaurantInventoryService.verifyAndDeductStock` and `RestaurantService.processRestaurantStep`/`createAndSaveTicket`/`saveRestaurantTicketOutbox` are otherwise direct ports of the source's logic, with the idempotency guard (`existsByOrderId`), tracing removal, and real-`ObjectMapper` serialization following the same conventions as `OrderService`/`PaymentService`.

### Consequences: the saga's decision point

- `RestaurantServiceTest` (6 tests) and a new `RestaurantInventoryServiceTest` (3 tests, no source equivalent) were both written — the latter because `RestaurantInventoryService.verifyAndDeductStock` has real branching logic and an actual stock-deduction side effect that `RestaurantServiceTest`'s mocked-inventory-service approach structurally can't exercise, the same reasoning as `OrderConsumerConfigTest`/`PaymentConsumerConfigTest` going beyond the source. Verified with a real `./gradlew :restaurant-service:test` run, and confirmed both new suites genuinely catch wrong data — a wrong deserialized `ticketId` and a wrong deducted stock count — by deliberately breaking each assertion and re-running before reverting. Full multi-module suite (75 tests across 5 modules) also verified green.
- All three saga participants built so far now have their core business logic in place: `order-service` creates and reacts, `payment-service` charges and refunds, `restaurant-service` decides and allocates inventory. What's left for `restaurant-service` is purely wiring — repositories and domain model already exist (Phases 33-34), so the next phase is `OutboxPublisherService`, consumer config, and `application.yaml`, the same three files that closed out `order-service`/`payment-service`.

## `restaurant-service` Kafka wiring: two-topic routing by `eventType` string, not deserialization

**Status:** Done — `restaurant-service` Kafka wiring, Phase 36. Closes the full saga chain end-to-end.

### Context: one outbox table, two destination topics

`order-service`/`payment-service`'s `OutboxPublisherService`s (Phase 22, 31) each publish to exactly one fixed topic, so shipping the outbox row's already-serialized JSON payload as-is needed no knowledge of the event's type at all. `restaurant-service` breaks that assumption: the same `outbox_record` table holds both `RestaurantApprovedEvent`s and `RestaurantRejectedEvent`s, destined for two different topics (`restaurant-approved-topic`/`restaurant-rejected-topic`). The source's own `OutboxPublisherService` handles this by deserializing each payload into a typed `RestaurantEvent` via `convertToEvent`, then pattern-matching on the concrete type (`case RestaurantApprovedEvent _ ->`) purely to pick a topic — reintroducing the deserialize-then-reserialize round trip this repo deliberately avoided in Phase 22.

### Decision: one outbox table, two destination topics

`OutboxPublisherService.resolveTopic` switches on `OutboxRecord.getEventType()` — a plain string column already on the row — to pick `restaurant-approved-topic` or `restaurant-rejected-topic`, then ships `record.getPayload()` raw exactly as before. No `ObjectMapper` dependency in this class at all, keeping the "ship the already-serialized payload, never deserialize just to republish" principle intact even with two destinations instead of one. An unrecognized `eventType` throws `IllegalArgumentException`, matching the source's own defensive default case — this one's a real guard, not a currently-unreachable branch, since nothing in this class controls what `eventType` strings ever land in the table. `RestaurantConsumerConfig` is a single `@KafkaListener` on `payment-processed-topic` (group `restaurant-group`) → `processRestaurantStep`, the same shape as `order-service`/`payment-service`'s consumer configs. `application.yaml` follows the established `default`/`postgres` profile split, `restaurant_db`/`restaurant_service_schema` naming. `docker/init-schemas.sql` gained a `restaurant_service_schema` line.

### Consequences: one outbox table, two destination topics

- `OutboxPublisherServiceTest` (5 tests) adds real coverage the single-topic modules never needed: routing to each topic correctly by event type, and an unrecognized `eventType` throwing rather than silently misrouting. `RestaurantConsumerConfigTest` (1 test) mirrors `order-service`/`payment-service`'s consumer test shape. Verified with a real `./gradlew :restaurant-service:test` run (22 tests in this module now), and confirmed both new suites genuinely catch wrong data by deliberately swapping the expected topic and a deserialized `amount` value, re-running to confirm failure, then reverting.
- Verified end-to-end against real infrastructure, the same standard established in Phase 26/31: recreated the Docker containers so the updated `init-schemas.sql` actually ran, confirmed all four service schemas via `\dn`, then booted `restaurant-service` for real (`bootRun`, `postgres` profile) — a genuine `HikariPool`→`PgConnection` against `restaurant_service_schema`, and `restaurant-group` actually joining and getting a real partition assignment (`payment-processed-topic-0`) against the live broker.
- **This closes the full saga chain end-to-end**: `order-service` creates an order and publishes `OrderCreatedEvent` → `payment-service` charges and publishes `PaymentProcessedEvent` → `restaurant-service` allocates inventory and publishes `RestaurantApproved`/`RejectedEvent` → both `order-service` (confirm/cancel) and `payment-service` (refund on rejection) react. Every hop in that chain now has a real, tested producer and a real, tested consumer on both ends — not just individually-verified services with untested integration points. `api-gateway-service` remains the only unbuilt module from the original four-service plan.

## `api-gateway-service` scaffold: WebFlux + Spring Cloud Gateway, no OTel, no Kubernetes DNS

**Status:** Done — `api-gateway-service` scaffold, Phase 37.

### Context: the last module, and the first genuinely different tech stack

Every module so far (`user-service`, `order-service`, `payment-service`, `restaurant-service`) is a servlet-based Spring MVC app on Boot 3.5.16. The source's `api-gateway-service` is reactive from the ground up — Spring WebFlux/Netty plus Spring Cloud Gateway's `webflux` server variant — because a gateway's whole job is proxying many concurrent downstream calls without tying up a thread per request, the one place in this repo where that distinction actually matters. The source also runs Spring Boot 4.0.6 with Spring Cloud 2025.1.1, OpenTelemetry export (`ObservationConfig`, `TracingConfig`, OTLP endpoints), and a `k8sdb` profile routing to Kubernetes-DNS hostnames (`order-service`, `user-service`) instead of `localhost`.

### Decision: the last module, and the first genuinely different tech stack

Stayed on Boot 3.5.16 (Phase 17's repo-wide floor) instead of jumping to 4.0.6, paired with Spring Cloud's matching **2025.0.0** BOM (the train that actually targets Boot 3.5.x) and the same spring-grpc **0.7.0** milestone `user-service` already depends on — resolved and compiled clean on the first attempt, no version conflicts. OTel is dropped entirely, the same call made for every other module since Phase 16: no collector infrastructure exists in this repo, so `TracingConfig`, `ObservationConfig`, and the OTLP config blocks would just be unused code. The `k8sdb` profile is dropped too — no Kubernetes manifests exist yet, and no other module has per-environment routing profiles; `application.yaml` just defaults every downstream target to `localhost` (`order-service` on `:8081`, `user-service`'s gRPC port on `:9090`), consistent with how every other module already assumes local dev. No `Dockerfile` yet either, matching `order-service`/`payment-service`/`restaurant-service` — none of the four are containerized as app images, only their infrastructure (Postgres/Kafka) is. Package root `io.github.terrence721.saga.gateway`, module name `api-gateway-service` as already named in the four-service plan.

### Consequences: the last module, and the first genuinely different tech stack

- `./gradlew :api-gateway-service:compileJava` verified green immediately after scaffolding — no repeat of the Phase 8/17 JDK-25 ASM incompatibilities, since this module starts on 3.5.16 from day one rather than migrating onto it later.
- Every prior module's `contextLoads()` was written and run before business logic (Phase 17's lesson); this one goes further, verified in Phase 40 below once the gRPC client, filter, and route config all exist together, since a gateway's config wiring is the actual product here, not incidental to it.

## `api-gateway-service`: auth flow adapted to this repo's own `Login` RPC, not the source's `authenticateUser`

**Status:** Done — `api-gateway-service` gRPC auth client, Phase 38.

### Context: two different `user-service` contracts

The source's `api-gateway-service` calls a `UserServiceBlockingStub.authenticateUser(AuthRequest)` returning `AuthResponse{token, token_type, expires_in}` — its own `user-contract`'s shape. This repo's `user-contract` (Phase 4) was deliberately written from scratch, not a mirror: `UserIdentityServiceGrpc`'s `Login(LoginRequest{email, password})` returns `LoginResponse{user_id, access_token, token_type, expires_in_seconds}`, matching what `user-service`'s real `UserGrpcServiceImpl.login()` (Phase 7) actually implements. The source's own `AuthRequest` DTO also names its field `username`, but every actual value passed into it — including its own test's `"developer@tunmin.dev"` — is an email address; a real naming inconsistency, not a deliberate distinction.

### Decision: two different `user-service` contracts

`CommonAppConfig` wires a `UserIdentityServiceBlockingStub` (not `UserServiceBlockingStub`) against the `userService` named gRPC channel. `UserGrpcClient.login()` calls the stub's real `login()` RPC and returns `LoginResponse` directly; `AuthenticationController` reads `getAccessToken()`/`getTokenType()`/`getExpiresInSeconds()` off it into `WebTokenResponse`. The gateway's own `AuthRequest` DTO field is named `email`, not `username`, since that's what it actually is. `UserGrpcExceptionTranslator`'s status-code mapping (`UNAUTHENTICATED`→`InvalidCredentialsException`, `NOT_FOUND`→`UserNotFoundException`, `PERMISSION_DENIED`→`UserInactiveException`, `INVALID_ARGUMENT`→`IllegalArgumentException`, `UNAVAILABLE`/`DEADLINE_EXCEEDED`/`INTERNAL`→`DependencyUnavailableException`) carries over unchanged from the source — confirmed against `user-service`'s own `GrpcExecutor` (Phase 7), which throws exactly those gRPC statuses for exactly those conditions, so the mapping is provably correct rather than just copied on faith. `GlobalExceptionHandler` also carries over the source's exception-to-HTTP-status table, minus the OTel span-enrichment code (`Span.current()`, `StatusCode`) dropped along with the rest of OTel in Phase 37.

### Consequences: two different `user-service` contracts

- `./gradlew :api-gateway-service:compileJava` verified green with the adapted proto types; no fallout from the field-name/RPC-name differences once every call site was updated together.

## `api-gateway-service`: `JwtPerimeterGuardGatewayFilterFactory` shares `user-service`'s signing secret, plus a real bug found in the source's own filter test

**Status:** Done — `api-gateway-service` JWT filter, route config, and tests, Phases 39-40. Completes the module.

### Context: verifying tokens `user-service` actually signs, and a test that could never have passed

`JwtPerimeterGuardGatewayFilterFactory` reads `app.jwt.secret`/`app.jwt.issuer` and verifies the `Authorization: Bearer` header against them, extracting a `user-id` claim into an `X-Perimeter-User-Id` downstream header — this needs to be the *same* secret/issuer `user-service`'s `JwtTokenProvider` (Phase 7) signs tokens with, or nothing verifies. Porting the source's own `JwtPerimeterGuardGatewayFilterFactoryTest`, then actually running it (per this repo's standing practice of never assuming a ported test passes), surfaced a real bug: two of its three tests asserted `exchange.getResponse().getStatusCode()` after calling the filter directly against a mocked `GatewayFilterChain`, expecting `401`/`403`. But the filter itself never sets a response status on missing or invalid tokens — it emits `Mono.error(...)` for `GlobalExceptionHandler`'s `@ExceptionHandler(JWTVerificationException.class)` to translate further up the real request pipeline, a layer that doesn't exist in an isolated unit test with no `@RestControllerAdvice` present. Both tests failed on the very first real run with `expectation "expectComplete" failed (actual: onError(...))` — proof the source's own test was never actually executed. The "Forbidden" test name was also wrong on its own terms: a tampered-signature token throws `JWTDecodeException` (a `JWTVerificationException` subtype), which `GlobalExceptionHandler` maps to `401`; `403` is reserved specifically for `TokenExpiredException`.

### Decision: verifying tokens `user-service` actually signs, and a test that could never have passed

`JwtPerimeterGuardGatewayFilterFactory` itself is a direct, unmodified port — its logic was never in question, only the test asserting it wrong. Rewrote both broken tests to assert what the filter actually, verifiably does: `StepVerifier.expectErrorMatches`/`expectError(JWTVerificationException.class)` on the emitted signal, plus `verifyNoInteractions(filterChain)` confirming the request never proceeds — the real, provable contract, rather than a response-status transformation that belongs to a different class. The source test's Guava `com.google.common.net.HttpHeaders` import was also dropped in favor of Spring's own `org.springframework.http.HttpHeaders`, already used everywhere else in this repo — no reason to add a Guava dependency for one header constant. `application.yaml`'s route table (`auth-token-route` forwarding to the local controller, `order-service-route` proxying to `localhost:8081` guarded by `JwtPerimeterGuard` + `RemoveRequestHeader=Cookie` + a Resilience4j `CircuitBreaker` falling back to `GatewayFallbackController`) and the `orderServiceCircuitBreaker` instance config carry over from the source unchanged — no other REST-facing route exists in this repo yet, since `payment-service`/`restaurant-service` are purely Kafka-driven.

### Consequences: verifying tokens `user-service` actually signs, and a test that could never have passed

- `ApiGatewayServiceApplicationTests.contextLoads()` (1 test) was added once the gRPC client, filter, and route config all existed together, and passed on its first real run — genuine log evidence the filter initialized with the right issuer, Spring Cloud Gateway loaded its full `RoutePredicateFactory` set (proving `application.yaml`'s route table parsed without error), and the gRPC stub bean wired, all with no live `order-service`/`user-service` running (gRPC channels connect lazily, matching every other module's Kafka-listener-without-a-broker precedent from Phase 24).
- `AuthenticationControllerTest` (3) and the corrected `JwtPerimeterGuardGatewayFilterFactoryTest` (3) verified with a real `./gradlew :api-gateway-service:test` run — 7 tests in this module, all passing. Confirmed the suite genuinely catches wrong data by deliberately asserting a wrong token value in the happy-path test, re-running to confirm failure, then reverting. Full multi-module suite (88 tests across all 6 modules) also verified green with no regressions.
- **This completes all five originally-planned backend modules** (`user-service`, `order-service`, `payment-service`, `restaurant-service`, `api-gateway-service`) — the saga chain now has a real security perimeter and single entry point in front of it, not just direct service-to-service access. What's left, per `todo.md`, is infrastructure (per-service `Dockerfile`s/Kubernetes manifests) and net-new scope (a register/POS frontend, a `reservation-service`), not further backend modules from the original plan.

## `order-service`: `OrderController` logs individual fields, not the raw request, after a real CodeQL log-injection finding

**Status:** Done — Phase 41.

### Context: a genuine CWE-117 finding, not a false positive

CodeQL's code scanning flagged `OrderController.createOrder` (`log.info("Received create order request: {}", request)`, Phase 23) as Medium-severity log injection. `CreateOrderRequest.itemCode` is a `@NotBlank String` with no length cap or character allow-list, taken directly off the public `POST /orders` body — the request record's `toString()` interpolates it raw. A client could set `itemCode` to a string containing `\r`/`\n` to forge fake log lines (spoofed timestamps, fabricated `ERROR`/admin-looking entries) or inject control characters into whatever ingests this service's logs. Checked every other `log.info`/`log.warn` call site across all five modules for the same pattern: every other one already logs individual, type-safe fields (`event.orderId()` — a `UUID`, `event.reason()` — always one of `RestaurantService`'s internally-generated messages, never raw user text) rather than a whole DTO's `toString()`. This was the one call site that slipped through, not a repo-wide pattern.

### Decision: a genuine CWE-117 finding, not a false positive

Replaced the single `{}`-on-`request` log call with four separate placeholders for `customerId`/`itemCode`/`quantity`/`totalAmount`, matching the "log safe fields individually" convention already used everywhere else. `itemCode` specifically gets `.replaceAll("[\r\n]", "_")` before logging — the one field with no character restriction and therefore the only one actually capable of forging a log line; `customerId` (`UUID`), `totalAmount` (`BigDecimal`), and `quantity` (`int`) have no string-injection surface by their types alone, so no sanitization needed there.

### Consequences: a genuine CWE-117 finding, not a false positive

- `./gradlew :order-service:test` verified green with no regressions (`OrderControllerTest` doesn't assert on log output, so the fix was purely additive from a test-coverage standpoint).
- No dedicated test added for the sanitization itself — matching this repo's own precedent of not writing tests for straightforward one-line defensive fixes with no branching logic (e.g. `DependencyUnavailableException`'s constructors, Phase 38) — verified instead by reading the fixed line directly.