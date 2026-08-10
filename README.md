# 🔀 Distributed Saga Microservice Platform

[![Quality](https://github.com/Terrence721/saga-full/actions/workflows/quality.yml/badge.svg)](https://github.com/Terrence721/saga-full/actions/workflows/quality.yml)
[![CodeQL](https://github.com/Terrence721/saga-full/actions/workflows/codeql.yml/badge.svg)](https://github.com/Terrence721/saga-full/actions/workflows/codeql.yml)

**[📜 View the portfolio page →](https://terrence721.github.io/saga-full/portfolio.html)**

Last updated: August 10, 2026 (`user-service` error-path tests)

This repository is a from-scratch demonstration of the Distributed Saga pattern for coordinating long-running business transactions across independent microservices — order placement, payment settlement, and fulfillment, each owned by its own service, coordinated without a shared database transaction.

This is an original implementation, not a fork of any existing project. The module boundaries and general shape of the problem (order → payment → fulfillment, with compensation on failure) are common territory for this class of system; the code, design decisions, and tradeoffs recorded here are this repo's own.

**At a glance:** 17/17 tests passing across `user-contract` + `user-service` — see the **[consolidated test report](https://terrence721.github.io/saga-full/test-report.html)**, a single file that CI keeps current on every push to `main` as more test cases are added. To generate it locally instead, run `./gradlew test --continue && ./gradlew aggregateTestReport` — see [CONTRIBUTING.md](CONTRIBUTING.md#consolidated-test-report-all-modules-one-file) for details.

## 🧭 Start Here

- **[`todo.md`](todo.md)** — the phase-by-phase log of everything done and everything still open. This is the source of truth for progress.
- **[GitHub Project board](https://github.com/users/Terrence721/projects/3)** — a Scrum-style Backlog/Planned/In Progress/Verification & QA/Done view of the same work, for a quick at-a-glance status without reading the full log. Kept in sync with [`todo.md`](todo.md).
- **[`docs/architecture.md`](docs/architecture.md)** — the reasoning behind this repo's architectural decisions (context, alternatives, what each one actually cost), not just what changed.
- **[`docs/case-study.md`](docs/case-study.md)** — problem, constraints, tradeoffs, and results, for anyone scanning this repo as a portfolio piece rather than reading it as documentation.
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — development setup, testing commands, commit conventions.

## 🧭 Why This Matters

Saga orchestration is a common interview-whiteboard topic and an uncommon thing to actually build end to end: compensating transactions, event ordering, idempotency, and the failure modes that only show up once services genuinely run independently. The point of this repo is working through those problems for real, one service at a time, and writing down which decisions were load-bearing and why.

## 🏗 What's Here So Far

`user-contract` (shared gRPC contract) and the core of `user-service` (identity + auth: login, token issuance, token validation) are implemented, both with passing test suites (see the [consolidated test report](https://terrence721.github.io/saga-full/test-report.html)). See `todo.md` for the full build-out plan.

```text
(planned, mirrors the shape of the problem — not final)
  api-gateway-service/   inbound edge, routing, auth
  order-service/         saga orchestrator / state machine
  payment-service/       payment ledger + settlement
  restaurant-service/    fulfillment-side processor
  user-service/          identity + auth                  ✅ core implementation
  user-contract/         shared API contract types         ✅ done
```

## 🖥 Getting Started

```shell
./gradlew :user-service:compileJava
```

A full runnable stack needs at least one more service to talk to `user-service` over gRPC — this section will expand as that lands.
