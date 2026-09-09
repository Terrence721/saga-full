# Case Study: Distributed Saga Microservice Platform

Last updated: September 9, 2026

For the portfolio-facing version of this page, see [portfolio.html](https://terrence721.github.io/saga-full/portfolio.html). This document is the shorter, docs-oriented summary — problem, constraints, tradeoffs, results — for anyone scanning this repo rather than reading it end to end.

## Problem

Coordinate one business transaction — placing an order, charging payment, and confirming fulfillment — across three independently-owned services with no shared database transaction. A traditional two-phase commit isn't available once each service owns its own datastore; the Distributed Saga pattern replaces it with a chain of local transactions, each publishing an event that triggers the next step, plus a compensating action (refund, cancellation) if any step downstream fails.

This is common territory as an interview-whiteboard topic and an uncommon thing to actually build end to end: compensating transactions, event ordering, idempotent consumers, and the failure modes (poison-pill messages, duplicate delivery, a cold first request under load) that only surface once services genuinely run as separate processes talking over a real broker, not as one deployable with an in-process function call standing in for "the saga."

## Constraints

- **Original implementation, not a fork.** A separate repository was used as a directory-structure and scope guide (which modules exist, roughly what each owns) — see the README's non-affiliation framing. None of this repo's code, package namespace, or build tooling is copied from it; every one of the deviations below was a decision made here, not inherited.
- **No shared database across services.** `order-service`, `payment-service`, and `restaurant-service` each own their own schema. The only way one step's outcome reaches the next is a Kafka event, published transactionally via the outbox pattern (write the event and the state change in the same local transaction, poll and publish it after commit) — never a direct cross-service call for anything that has to survive a crash mid-step.
- **One security perimeter, not per-service auth.** `api-gateway-service` is the only component that terminates a JWT; every service behind it trusts a `X-Perimeter-User-Id` header the gateway attaches after verifying the token. That trust boundary is real and load-bearing — the [code-review audit](code-review.md) treated a missing check on it (an IDOR in `order-service`, an ownership check missing in `payment-service`'s refund path) as a genuine security bug, not a style nit.
- **A JDK 25 / Gradle multi-module toolchain that was, at the time this repo started, ahead of the ecosystem's own JDK 25 support.** Lombok, Mockito's ByteBuddy-based mock maker, and Spring Boot 3.4.x's bundled ASM (both the Gradle plugin's `resolveMainClassName` scan and Spring Framework's own component-scan) all failed outright on real JDK 25 bytecode until pinned/upgraded — not a hypothetical compatibility note, a build and boot failure hit for real on the first module that exercised each code path. See [architecture.md](architecture.md) for each incident.
- **Self-imposed correctness bar**: every module gets a full file-by-file code-review pass and a structural test-coverage cross-reference before being considered done, and every real finding gets an actual fix rather than being dismissed as minor. This shaped the schedule as much as any technical constraint: six backend modules complete, then a 73-file audit, then a separate coverage-gap scan, before frontend work started.

## Architecture

Full reasoning for every decision below lives in [architecture.md](architecture.md); the four diagram pages give the visual version:

- **[Architecture Overview](https://terrence721.github.io/saga-full/diagrams/system-architecture.html)** — the module map and the two transports
- **[Saga Flow](https://terrence721.github.io/saga-full/diagrams/saga-flow.html)** — one order's actual path through all five saga-participating services, happy path and compensation
- **[Services Reference](https://terrence721.github.io/saga-full/diagrams/services-reference.html)** — the shared outbox-then-poll shape and what differs per service
- **[Testing Strategy](https://terrence721.github.io/saga-full/diagrams/testing-strategy.html)** — the four test layers, each with a real bug it alone caught

In short: `api-gateway-service` is a reactive WebFlux edge — JWT verification, gRPC-backed login against `user-service`, a Resilience4j circuit breaker on the one downstream HTTP hop. `order-service` owns the saga's lifecycle state (`PENDING`/`CANCELLED`/`SUCCESS`) and is the only service the gateway talks to directly. `payment-service` and `restaurant-service` are pure Kafka consumers/producers with no inbound HTTP surface at all — they only ever hear about an order through an event. Every one of the three saga participants uses the identical outbox-then-poll shape (`OutboxRecord` written in the same transaction as the state change, a `@Scheduled` poller with `SKIP LOCKED` publishing it via `KafkaTemplate`), so a bug found in one (a missing `DefaultErrorHandler` for poison-pill messages, an unbounded send timeout) was checked and fixed in the other two as the same pattern, not three separate investigations.

The whole stack now runs as seven containers (`docker compose up -d --build`: five services, Postgres, Kafka) — the shift from "boots against local infra" to "boots as the thing that would actually deploy" surfaced two of the real bugs in the Results section below.

## Tradeoffs

A sample of the deliberate deviations from the structural-reference source, each made for a stated reason rather than by default — full list and reasoning in [architecture.md](architecture.md):

| Decision | Instead of | Why |
| - | - | - |
| Gradle (Kotlin DSL) | Maven | Incremental/cached builds matter more here than XML being easier to skim, once six modules rebuild repeatedly across local dev and CI |
| `BigDecimal` for money | `double` | Binary floating point can't exactly represent most decimal fractions — a real correctness concern once amounts are added, refunded, or compared |
| UUID primary keys | un-generated `Long` | The source's `Long id` had no `@GeneratedValue`, workable only because the source seeds rows by hand; this repo creates rows through real application code |
| `spring-security-crypto`'s `BCryptPasswordEncoder` | `jbcrypt` | Same algorithm, but actively maintained rather than an abandoned standalone library sitting on the credential-storage path |
| `spring-kafka` directly | Spring Cloud Stream's Kafka binder | This project only ever targets Kafka and never needs broker-swappability — the extra abstraction had no payoff here |
| `ValidateToken` returns `valid: false` | throwing a gRPC error | An expired/forged token is a normal answer for a validation endpoint, not a failure of the endpoint — same reasoning as OAuth2 token introspection (RFC 7662) |
| SSE streamed directly from `order-service` (`Sinks.Many`), proxied by the gateway | a new Kafka consumer added to the gateway | The gateway has no existing Kafka wiring and doesn't need one just to relay status the order service already knows the instant it changes |

Two tradeoffs cost real debugging time rather than just design discussion, both only found by actually running the containerized stack, not by writing or reviewing the config:

- **Kafka's advertised listener.** `PLAINTEXT://localhost:9092` is correct for a host process connecting in, but a container reconnecting via `kafka-broker:9092` gets told, post-handshake, to use `localhost:9092` — meaningless inside its own network namespace. Fixed with the standard dual-listener pattern (`PLAINTEXT_HOST` unchanged, a new `PLAINTEXT_INTERNAL` on `kafka-broker:29092`).
- **The gateway's circuit breaker had no explicit `TimeLimiter`**, silently defaulting to Resilience4j's 1-second timeout — too tight for a cold cross-container HTTP call. Measured 1.29s on the very first real order request (cold DNS + connection-pool warmup); fixed with an explicit 5s timeout, then re-verified from a genuinely cold stack restart, not the same warm containers.

## Results / Impact

- **161/161 tests passing** across all six modules — see the [consolidated test report](https://terrence721.github.io/saga-full/test-report.html), regenerated and committed by CI on every push to `main`.
- **Code-review audit complete: 73/73 files, 26 real findings fixed, 0 left open** — including three real security issues (a login-enumeration timing side-channel, an IDOR on order lookup, a missing ownership check on payment refunds), an unlocked stock-deduction race that could oversell inventory once `restaurant-service` scales past one replica, a payment-decline path that was dead code (`PaymentStatus.FAILED` was never reachable), and a Kafka poison-pill gap fixed once and then found again twice more as the same unhandled-deserialization-error pattern repeated across services. Full narrative in [code-review.md](code-review.md).
- **A separate, structural test-coverage scan closed 9 further real gaps** (dead branches, untested error paths) after the audit above had already run its course — cross-referencing every source class against its test class rather than relying on the audit's per-file read to catch everything.
- **Verified against the actual deployable artifact, not just `bootRun` against local infra**: the full 7-container stack was brought up for real, a test user inserted directly via `psql` (no registration endpoint exists — this is a login-only identity service by design), and a real order driven through the complete saga across container boundaries — including the compensation path, where an unseeded item code triggered a genuine restaurant rejection that correctly fired both a payment refund and an order cancellation.
- **CI itself had a real, repo-wide-blocking bug found and fixed**: enabling Gradle's build cache for faster CI turnaround let CodeQL's build step restore `compileJava`'s output from cache instead of invoking `javac`, so its tracer saw zero real compilation and failed every scan, scheduled and on-push, across all of `main`. Fixed by scoping `--no-build-cache` to just that one workflow step.

## What's next

The register/POS frontend (React/TS/Vite/Tailwind, streamed order status via SSE) is in progress — see [todo.md](../todo.md) for the current step. An unscoped `reservation-service` addition remains open on the board. Neither changes anything recorded above; this page will get another pass once the frontend build closes out.
