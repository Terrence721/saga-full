# 📝 TODO

**Last Updated:** August 10, 2026 (consolidated test report across modules)

A phase-by-phase log of what's been done on this repo and what's still open. This is the source of truth for progress.

**What this repo is:** a from-scratch personal demonstration workspace implementing the Distributed Saga pattern across independent microservices. This is not a fork or continuation of any existing project — the module boundaries and general shape of the problem are common territory for this class of system, but the code, design decisions, and tradeoffs recorded here are original. See [README.md](README.md) for the project description and [LICENSE](LICENSE) for licensing.

## At a glance

**Done, in full:**

| Item | Detail | Phases |
| - | - | - |
| Repo bootstrap | git init, README, MIT LICENSE (Terrence Daniels), docs/ skeletons, .gitignore, CONTRIBUTING.md | Phase 1 |
| Build tool: Gradle | Chosen over Maven (source's own tool) for incremental/cached builds across this six-module project — a build-time win, not a runtime one; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 2 |
| Root namespace: `io.github.terrence721.saga` | Anchors the package/group ID to an identity actually owned (this repo's GitHub account) rather than an unowned domain like the first proposal, `com.sagafull.saga`; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 3 |
| `user-contract` module | `settings.gradle.kts`, `build.gradle.kts`, and an original `user.proto` (gRPC login + token validation contract) written; `./gradlew :user-contract:build` verified green end-to-end, including a real Gradle-version incompatibility and a real missing-symbol compile error found and fixed along the way, not just written on faith; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 4 |
| CI: quality + CodeQL | `quality.yml` (build/test jobs) and `codeql.yml` (java-kotlin) added; first real run failed (`gradlew` committed without its executable bit from Windows), fixed and confirmed green on a second real run; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 5 |
| `user-service` core | Entry point, config, `User` entity (UUID id), `UserRepository`, exception types, `JwtTokenProvider`, and `UserGrpcServiceImpl` (`Login` + `ValidateToken`) written; hit and fixed a real Lombok/JDK 25 incompatibility along the way; `./gradlew :user-service:compileJava` verified green; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 7 |
| CI fix: `resolveMainClassName` | First real CI run of `quality.yml` against `user-service` failed (`Unsupported class file major version 69` — Spring Boot Gradle plugin's bundled ASM can't read JDK 25 bytecode). Fixed with an explicit `springBoot { mainClass.set(...) }`, skipping the auto-detection scan entirely; reproduced the failure locally and confirmed both `./gradlew assemble` and `./gradlew test` green before pushing; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 8 |
| `user-contract` tests | JUnit 5.11.4 + AssertJ 3.26.3 added (pinned to match what Spring Boot manages for `user-service`, not the new JUnit 6.1.3); 6 serialization round-trip tests written since there's no hand-written Java to unit test in a proto-only module; verified genuinely green with a real `./gradlew :user-contract:test` run, and verified the suite actually catches a wrong value by deliberately breaking one assertion and re-running before reverting; reasoning in [docs/architecture.md](docs/architecture.md) | Phase 9 |
| Consolidated test report | Custom root `aggregateTestReport` task merges every module's JUnit XML into one HTML file (`build/reports/tests/aggregate/index.html`), auto-picking up future modules with no edits needed. Gradle's built-in `test-report-aggregation` plugin was tried first and abandoned — it needs every module's full dependency graph (Spring Boot's BOM, etc.) resolvable from the root, an ongoing maintenance burden. Caught and fixed two real bugs along the way: a failing test silently blocking the report from generating at all, and a stale-report bug (no declared task inputs, so Gradle skipped regenerating it). Wired into CI: `quality.yml`'s `Test` job now runs it and uploads the merged report as a downloadable GitHub Actions artifact on every run. Reasoning in [docs/architecture.md](docs/architecture.md), usage in [CONTRIBUTING.md](CONTRIBUTING.md) | Phase 10 |

**Actually still open, right now:** `user-service` tests, remaining service modules, and `portfolio.html`. See the **Still to do** table below.

## 🧪 Test Coverage Ledger

Every test suite added to this repo, and its last confirmed real run. Updated as suites are added or re-run — this table only reflects an actual `./gradlew :<module>:test` run, never an assumed result.

| Module | Test class | Tests | Result | Phase | Date |
| - | - | - | - | - | - |
| `user-contract` | `UserContractSerializationTest` | 6 | ✅ passing | Phase 9 | 2026-08-10 |

## ✅ Done

### Repository bootstrap

| Phase | Date | What |
| - | - | - |
| 1 | 2026-08-10 | `git init`; README.md written describing the project and its scope; MIT LICENSE added under Terrence Daniels; `docs/architecture.md` and `docs/case-study.md` skeletons added; `.gitignore` added; `CONTRIBUTING.md` added |
| 2 | 2026-08-10 | Build tool decided: Gradle (Kotlin DSL) over Maven, for incremental compilation and build caching across this six-module project — faster local/CI build turnaround, with no effect on the running services' own performance. `.gitignore` and `CONTRIBUTING.md` updated from Maven to Gradle conventions; full reasoning in [docs/architecture.md](docs/architecture.md) |
| 3 | 2026-08-10 | Root package/group namespace decided: `io.github.terrence721.saga`, after ruling out `com.sagafull.saga` (implies ownership of an unowned domain) in favor of the GitHub-identity convention. Full reasoning in [docs/architecture.md](docs/architecture.md) |

### `user-contract` module

| Phase | Date | What |
| - | - | - |
| 4 | 2026-08-10 | Root `settings.gradle.kts` written, including `user-contract`. `user-contract/build.gradle.kts` written (Java 25 toolchain, `com.google.protobuf` plugin 0.9.4, grpc/protobuf deps exposed via `api`). `user.proto` written from scratch — `UserIdentityService` with `Login` and `ValidateToken` RPCs, deliberately not a field-for-field mirror of the source's single-RPC contract. JDK 25 (Eclipse Temurin) and a Gradle wrapper installed/generated; hit and fixed a real Gradle 8.11/JDK 25 Kotlin-DSL incompatibility (moved to Gradle 9.7.0) and a real missing-`javax.annotation.Generated` compile error (added `javax.annotation-api` as `compileOnly`). `./gradlew :user-contract:build` verified green. Full reasoning in [docs/architecture.md](docs/architecture.md) |
| 9 | 2026-08-10 | `user-contract/build.gradle.kts` gained JUnit 5.11.4, AssertJ 3.26.3, and `junit-platform-launcher` (also added to `user-service`, missing from both). `UserContractSerializationTest` written: 6 serialize/parse round-trip tests covering all 4 message types plus an int64 boundary case, since there's no hand-written Java in a proto-only module to unit test otherwise. Verified with a real `./gradlew :user-contract:test` run, and confirmed the suite genuinely catches wrong data by deliberately breaking one assertion and re-running before reverting it. Full reasoning in [docs/architecture.md](docs/architecture.md) |

### CI

| Phase | Date | What |
| - | - | - |
| 5 | 2026-08-10 | `.github/workflows/quality.yml` (parallel `build`/`test` jobs, JDK 25 Temurin, `gradle/actions/setup-gradle`) and `codeql.yml` (java-kotlin, real build before analyze) added. First real push failed both workflows: `./gradlew: Permission denied`, exit 126 — `gradlew` was committed from Windows without its executable bit, so git stored mode `100644` instead of `100755`. Fixed with `git update-index --chmod=+x gradlew`; a second real run confirmed both workflows green. Full reasoning in [docs/architecture.md](docs/architecture.md) |

### `user-service` module

| Phase | Date | What |
| - | - | - |
| 7 | 2026-08-10 | `settings.gradle.kts` updated to include `user-service`; `build.gradle.kts` written (actuator, data-jpa, spring-grpc-server, spring-security-crypto instead of the source's unmaintained `jbcrypt`, java-jwt, H2/Postgres runtime drivers). `UserServiceApplication` entry point; `application.yaml` (default H2 / `postgres` profiles, no baked-in JWT secret default); `User` entity with a Hibernate-native `UUID` id instead of the source's un-generated `Long`; `UserRepository`; `UserNotFoundException`/`InvalidCredentialsException`/`UserInactiveException`; `SecurityConfig` exposing a `BCryptPasswordEncoder` bean; `GrpcExecutor` (domain-exception → gRPC `Status` mapping, using `java.util.function.Supplier` instead of the source's Guava dependency); `JwtTokenProvider` (token creation and verification split out into its own component, unlike the source, which never actually implemented token validation); `UserGrpcServiceImpl` implementing both `Login` and `ValidateToken` — the latter returns `valid: false` rather than a gRPC error on a bad token. Hit and fixed a real Lombok 1.18.36/JDK 25 incompatibility (`NoSuchFieldException: TypeTag :: UNKNOWN`), pinned to 1.18.42. `./gradlew :user-service:compileJava` verified green. Full reasoning in [docs/architecture.md](docs/architecture.md) |
| 8 | 2026-08-10 | First real CI run against `user-service` failed at `:user-service:resolveMainClassName` (`Unsupported class file major version 69`) — Spring Boot 3.4.1's Gradle plugin bundles an ASM that predates JDK 25 and can't read its bytecode when auto-detecting the main class. Fixed with an explicit `springBoot { mainClass.set(...) }` in `user-service/build.gradle.kts`, skipping the scan. Reproduced the exact failure locally with `./gradlew assemble`, confirmed the fix with a second real run, and confirmed `./gradlew test` also passes clean with no tests yet. Full reasoning in [docs/architecture.md](docs/architecture.md) |

### Consolidated test reporting

| Phase | Date | What |
| - | - | - |
| 10 | 2026-08-10 | Tried Gradle's built-in `test-report-aggregation` plugin at the root project first; abandoned after it required resolving every subproject's full dependency graph from the root (missing repositories, then unresolvable Spring-managed versions) — an ongoing burden that would grow with every future module. Replaced with a custom root `build.gradle.kts` task, `aggregateTestReport`, that reads each module's already-written JUnit XML with the JDK's own XML parser (zero extra dependencies) and merges it into one HTML file. Caught and fixed two real bugs via actual runs: a failing test blocking the report from generating at all (fixed by decoupling `aggregateTestReport` from the `test` tasks — documented two-step workflow in [CONTRIBUTING.md](CONTRIBUTING.md)), and a stale-report bug where the task had no declared inputs and Gradle silently kept serving an outdated report (fixed by declaring each module's `test-results/test` directory as an input once it exists). Full reasoning in [docs/architecture.md](docs/architecture.md) |

## 🔧 Still to do

| Item | Detail |
| - | - |
| `user-service` tests | Unit/integration test coverage for `UserGrpcServiceImpl` and `JwtTokenProvider` — not yet written |
| Remaining service modules | `order-service`, `payment-service`, `restaurant-service`, `api-gateway-service` — added one file/folder at a time, conferring at each step |
| `portfolio.html` | Case-study page for the portfolio site, written once there are real results/metrics to show |
