# Code Review Results

<!-- markdownlint-disable-next-line MD036 -->

**Last Updated: August 13, 2026**

> [!CAUTION]
> This is a simulation of real-world code review.

Every finding below goes through a real GitHub Pull Request: a branch, an issue documenting the finding, and a real merge — see [issue #17](https://github.com/Terrence721/saga-full/issues/17) for the live parent tracking card, and its 6 sub-issues (one per module) for the per-module cards. This page is a readable historical index, not the live mechanism.

**Process**: review one module at a time, not in parallel. Within a module, every `.java` source file under `src/main/java` (plus `user-contract`'s `.proto` contract) gets its own row in [todo.md](../todo.md)'s per-module table (file path + last commit SHA at time of review), its own sub-issue nested under that module's tracking issue documenting findings, and its own PR — every file gets a PR, whether it carries a real fix or just records a clean review. The repo owner (Senior) reviews and merges every PR. Check [todo.md](../todo.md) for which module/file is next.

**Order**: modules are reviewed in the same order they were originally built — `user-contract` → `user-service` → `order-service` → `payment-service` → `restaurant-service` → `api-gateway-service` — since that's the dependency order everything else in this repo already follows (each service was itself built against the ones before it), not an arbitrary choice.

**Severity/category** follow the same scheme used on this author's other projects (see [coolify-full](https://github.com/Terrence721/coolify-full), [platform-main](https://github.com/Terrence721/platform-main)): severity is `critical`/`high`/`medium`/`low`, category is one of `Security`, `Reliability`, `Correctness`, `Maintainability`.

---

## Findings

### [`user.proto`](https://github.com/Terrence721/saga-full/blob/main/user-contract/src/main/proto/user.proto)

**n/a · Maintainability** — Reviewed, no code defect, structural note recorded ([issue #24](https://github.com/Terrence721/saga-full/issues/24))

Message/service definitions checked against every real consumer across all six modules: field numbering is sequential with no gaps, naming matches proto3 style, and the versioned package (`saga.user.v1`) leaves room for a future v2. `LoginRequest`/`LoginResponse` match `UserGrpcServiceImpl.login()` (`user-service`) and `UserGrpcClient`/`AuthenticationController` (`api-gateway-service`) field-for-field on both sides of the wire. `ValidateTokenRequest`/`ValidateTokenResponse` match `UserGrpcServiceImpl.validateToken()`'s two branches, confirmed by `UserContractSerializationTest`'s dedicated round-trip tests for both.

**Structural note**: `ValidateToken` has zero actual callers anywhere in this repo. `docs/architecture.md`'s own Phase 7 entry states its consumer would be "eventually `api-gateway-service`" — but Phase 39's `JwtPerimeterGuardGatewayFilterFactory` verifies tokens locally against a shared HMAC secret instead, a direct port of the source's design that never revisited that plan. Not a defect — the RPC is correctly implemented and tested, and local verification is itself a legitimate pattern — but it's an API surface this repo built and tested against its own stated intent for who'd use it, without ever actually wiring that caller in. Flagged for a decision, not fixed here: either wire a real caller, or document local verification as the deliberate final design.

---

_More findings are appended here as each file's PR merges. See [todo.md](../todo.md) for the per-file tracking table of whichever module is currently in progress._
