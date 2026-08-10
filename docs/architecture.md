# Architecture Decisions

Last updated: August 10, 2026 (consolidated test report)

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
