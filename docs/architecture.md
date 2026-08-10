# Architecture Decisions

Last updated: August 10, 2026

This document records the architectural decisions made in this repo — context, alternatives considered, what each decision actually cost — not a general tutorial on the Saga pattern. For the phase-by-phase build log, see [todo.md](../todo.md). For the portfolio-facing summary, see [case-study.md](case-study.md).

## Build tool: Gradle (Kotlin DSL) over Maven

**Status:** Done — repo bootstrap, Phase 2.

### Context

The source repository used as a directory-structure guide for this project builds with Maven, and Maven/XML is the more common default for Spring Boot microservice projects generally — the initial recommendation here, on the reasoning that its declarative XML is easier for someone skimming a portfolio repo to parse at a glance than a Gradle DSL, and that it maps cleanly onto a multi-module, one-`pom.xml`-per-service layout.

This is a six-module Maven-style multi-module build (`api-gateway-service`, `order-service`, `payment-service`, `restaurant-service`, `user-service`, `user-contract`), and both local dev loops and CI will rebuild/retest it repeatedly as services get added one at a time. Build turnaround on every one of those iterations is a real, recurring cost, not a one-time setup cost.

### Decision

Gradle with the Kotlin DSL (`build.gradle.kts` / `settings.gradle.kts`) replaces Maven for every module in this repo. The deciding factor is **build-time performance**: Gradle's incremental compilation and build caching only rebuild/retest what actually changed, where Maven's reactor rebuilds a module's full dependency chain on every invocation. In a six-module multi-module project rebuilt on every local iteration and every CI run, that difference compounds — it's specifically faster *builds*, not faster *runtime* application performance, which depends on the JVM and each service's own code, not the build tool. The multi-module layout itself carries over unchanged from the directory-structure guide; only the per-module build descriptor format changes, from `pom.xml` to `build.gradle.kts`.

### Consequences

- Every module gets a `build.gradle.kts` instead of a `pom.xml`; the root gets `settings.gradle.kts` instead of an aggregator `pom.xml`.
- Build/test commands documented in [CONTRIBUTING.md](../CONTRIBUTING.md) use `./gradlew`, not `mvn`.
- CI workflows (not yet added — see [todo.md](../todo.md)) will invoke Gradle tasks, not Maven goals, once there's code for them to run against, and can rely on Gradle's build cache to keep CI turnaround down as more modules are added.
- This decision has no effect on the running services' own performance — that's a separate set of decisions (JVM tuning, Spring Boot config, per-service design) made as each service is built out.
