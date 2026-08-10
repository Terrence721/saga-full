# 📝 TODO

**Last Updated:** August 10, 2026 (CI green: quality + CodeQL workflows both passing)

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

**Actually still open, right now:** every remaining service module and `portfolio.html`. See the **Still to do** table below.

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

### CI

| Phase | Date | What |
| - | - | - |
| 5 | 2026-08-10 | `.github/workflows/quality.yml` (parallel `build`/`test` jobs, JDK 25 Temurin, `gradle/actions/setup-gradle`) and `codeql.yml` (java-kotlin, real build before analyze) added. First real push failed both workflows: `./gradlew: Permission denied`, exit 126 — `gradlew` was committed from Windows without its executable bit, so git stored mode `100644` instead of `100755`. Fixed with `git update-index --chmod=+x gradlew`; a second real run confirmed both workflows green. Full reasoning in [docs/architecture.md](docs/architecture.md) |

## 🔧 Still to do

| Item | Detail |
| - | - |
| Remaining service modules | `user-service`, `order-service`, `payment-service`, `restaurant-service`, `api-gateway-service` — added one file/folder at a time, conferring at each step |
| `portfolio.html` | Case-study page for the portfolio site, written once there are real results/metrics to show |
