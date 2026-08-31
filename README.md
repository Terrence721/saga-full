# 🔀 Distributed Saga Microservice Platform

[![Quality](https://github.com/Terrence721/saga-full/actions/workflows/quality.yml/badge.svg)](https://github.com/Terrence721/saga-full/actions/workflows/quality.yml)
[![CodeQL](https://github.com/Terrence721/saga-full/actions/workflows/codeql.yml/badge.svg)](https://github.com/Terrence721/saga-full/actions/workflows/codeql.yml)

**[📜 View the portfolio page →](https://terrence721.github.io/saga-full/portfolio.html)**

Last updated: August 31, 2026 (code-review audit: `api-gateway-service`'s audit in progress, 8/15 files done — a real event-loop-blocking concurrency bug found and fixed in `AuthenticationController.java`, plus a real test-coverage gap fixed in `GatewayFallbackController.java`; 22 real findings fixed audit-wide so far)

This repository is a from-scratch demonstration of the Distributed Saga pattern for coordinating long-running business transactions across independent microservices — order placement, payment settlement, and fulfillment, each owned by its own service, coordinated without a shared database transaction.

This is an original implementation, not a fork of any existing project. The module boundaries and general shape of the problem (order → payment → fulfillment, with compensation on failure) are common territory for this class of system; the code, design decisions, and tradeoffs recorded here are this repo's own.

**At a glance:** 128/128 tests passing across `user-contract` + `user-service` + `order-service` + `payment-service` + `restaurant-service` + `api-gateway-service` — see the **[consolidated test report](https://terrence721.github.io/saga-full/test-report.html)**, a single file that CI keeps current on every push to `main` as more test cases are added. To generate it locally instead, run `./gradlew test --continue && ./gradlew aggregateTestReport` — see [CONTRIBUTING.md](CONTRIBUTING.md#consolidated-test-report-all-modules-one-file) for details.

## 🧭 Start Here

- **[Architecture Overview](https://terrence721.github.io/saga-full/diagrams/system-architecture.html)** — the module map, the two transports (gRPC + Kafka), and how the one entry point (`api-gateway-service`) fits together
- **[Saga Flow](https://terrence721.github.io/saga-full/diagrams/saga-flow.html)** — how a single order actually moves through all five saga-participating services, happy path and compensation
- **[Services Reference](https://terrence721.github.io/saga-full/diagrams/services-reference.html)** — the shared outbox-then-poll shape every service follows, and what's actually different per service (ports, topics, schemas)
- **[Testing Strategy](https://terrence721.github.io/saga-full/diagrams/testing-strategy.html)** — the four test layers this repo runs together, each with a real bug example it alone caught

The rest of the [wiki](https://github.com/Terrence721/saga-full/wiki) goes deeper per-module (one page per service).

- **[`todo.md`](todo.md)** — the phase-by-phase log of everything done and everything still open, plus a [Milestones](todo.md#-milestones) section for the high-level story arc. This is the source of truth for progress.
- **[GitHub Project board](https://github.com/users/Terrence721/projects/3)** — a Scrum-style Backlog/Planned/In Progress/Verification & QA/Done view of the same work, for a quick at-a-glance status without reading the full log. Kept in sync with [`todo.md`](todo.md).
- **[`docs/architecture.md`](docs/architecture.md)** — the reasoning behind this repo's architectural decisions (context, alternatives, what each one actually cost), not just what changed.
- **[`docs/code-review.md`](docs/code-review.md)** — a per-module, per-file code-review audit of the whole codebase, one real GitHub PR per file (findings or not). `user-contract`, `user-service`, `order-service`, `payment-service`, and `restaurant-service` are complete; `api-gateway-service` is now in progress, the last of the 6 modules, 8/15 files done — a real event-loop-blocking concurrency bug found and fixed in `AuthenticationController.java`, plus a real test-coverage gap in `GatewayFallbackController.java`. 22 real findings fixed audit-wide so far, including three real security issues, a Kafka poison-pill fix, a payment-decline path that never existed, and an unlocked stock-deduction race that could oversell inventory once the service scales past one replica — see [todo.md](todo.md) for the tracking table.
- **[`docs/case-study.md`](docs/case-study.md)** — problem, constraints, tradeoffs, and results, for anyone scanning this repo as a portfolio piece rather than reading it as documentation.
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — development setup, testing commands, commit conventions.

On AI-assisted development: Commits co-authored as Claude are AI-assisted implementations directed, reviewed, and merged by Terrence Daniels — same process as every other change, documented in docs/code-review.md.

## 🧭 Why This Matters

Saga orchestration is a common interview-whiteboard topic and an uncommon thing to actually build end to end: compensating transactions, event ordering, idempotency, and the failure modes that only show up once services genuinely run independently. The point of this repo is working through those problems for real, one service at a time, and writing down which decisions were load-bearing and why.

## 🏗 What's Here So Far

`user-contract` (shared gRPC contract), `user-service` (identity + auth: login, token issuance, token validation), `order-service` (order creation, transactional outbox, Kafka publish/consume, the saga's create/confirm/cancel lifecycle), `payment-service` (charge on order creation, refund on restaurant rejection), `restaurant-service` (inventory allocation, ticket creation, the saga's approve/reject decision), and `api-gateway-service` (reactive WebFlux edge — JWT-guarded routing to `order-service`, gRPC-backed login, a Resilience4j circuit breaker on the downstream hop) are complete, all with passing test suites (see the [consolidated test report](https://terrence721.github.io/saga-full/test-report.html)). The full saga chain is wired end-to-end behind a real security perimeter: order → payment → restaurant, with compensation flowing back on rejection. This completes all five originally-planned backend modules — what's left is a register/POS frontend and an unscoped `reservation-service` addition. See `todo.md` for the full build-out plan.

```text
  api-gateway-service/   inbound edge, JWT perimeter guard, routing         ✅ done
  order-service/         saga orchestrator / state machine                 ✅ done
  payment-service/       payment ledger + settlement                       ✅ done
  restaurant-service/    fulfillment-side processor                        ✅ done
  user-service/          identity + auth                                   ✅ done
  user-contract/         shared API contract types                         ✅ done
```

## 🖥 Getting Started

```shell
./gradlew :api-gateway-service:compileJava
```

A full runnable stack needs Postgres + Kafka up (`docker-compose up`, see [CONTRIBUTING.md](CONTRIBUTING.md)) and each service started with the `postgres` profile — `user-service` and `order-service` first, since `api-gateway-service` and `payment-service`/`restaurant-service` depend on them being reachable over gRPC and Kafka respectively.
