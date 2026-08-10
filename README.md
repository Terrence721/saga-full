# 🔀 Distributed Saga Microservice Platform

**[📜 View the portfolio page →](https://terrence721.github.io/saga-full/portfolio.html)** · [More of my work ↗](https://terrence721.github.io/)

Last updated: August 10, 2026

This repository is a from-scratch demonstration of the Distributed Saga pattern for coordinating long-running business transactions across independent microservices — order placement, payment settlement, and fulfillment, each owned by its own service, coordinated without a shared database transaction.

This is an original implementation, not a fork of any existing project. The module boundaries and general shape of the problem (order → payment → fulfillment, with compensation on failure) are common territory for this class of system; the code, design decisions, and tradeoffs recorded here are this repo's own.

## 🧭 Start Here

- **[`todo.md`](todo.md)** — the phase-by-phase log of everything done and everything still open. This is the source of truth for progress.
- **[`docs/architecture.md`](docs/architecture.md)** — the reasoning behind this repo's architectural decisions (context, alternatives, what each one actually cost), not just what changed.
- **[`docs/case-study.md`](docs/case-study.md)** — problem, constraints, tradeoffs, and results, for anyone scanning this repo as a portfolio piece rather than reading it as documentation.
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — development setup, testing commands, commit conventions.

## 🧭 Why This Matters

Saga orchestration is a common interview-whiteboard topic and an uncommon thing to actually build end to end: compensating transactions, event ordering, idempotency, and the failure modes that only show up once services genuinely run independently. The point of this repo is working through those problems for real, one service at a time, and writing down which decisions were load-bearing and why.

## 🏗 What's Here So Far

Repository bootstrap only — no services added yet. See `todo.md` for the build-out plan.

```text
(planned, mirrors the shape of the problem — not final)
  api-gateway-service/   inbound edge, routing, auth
  order-service/         saga orchestrator / state machine
  payment-service/       payment ledger + settlement
  restaurant-service/    fulfillment-side processor
  user-service/          identity + auth
  user-contract/         shared API contract types
```

## 🖥 Getting Started

Not yet runnable — services haven't been added. This section will be filled in as the first service lands.
