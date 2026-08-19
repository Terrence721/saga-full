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

### [`UserServiceApplication.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/UserServiceApplication.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #26](https://github.com/Terrence721/saga-full/issues/26))

12 lines: package declaration, two imports, a single `@SpringBootApplication`-annotated class with a `main()` calling `SpringApplication.run()`. No custom logic, no branching, nothing to get wrong. Identical in shape to every other module's entry-point class already in this repo.

---

### [`SecurityConfig.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/config/SecurityConfig.java)

**low · Maintainability** — Fixed via [PR #29](https://github.com/Terrence721/saga-full/pull/29) ([issue #28](https://github.com/Terrence721/saga-full/issues/28))

The 8-line `@Configuration` class exposing a `BCryptPasswordEncoder`-backed `PasswordEncoder` bean is itself correct — nothing to fix in its logic. But grepping the whole repo for `.encode(` turned up zero matches, in production code or tests: `UserGrpcServiceImpl` only ever calls `.matches()` during login, and every test that touches `PasswordEncoder` mocks it, setting `User.passwordHash` to a literal placeholder like `"hashed-password"` rather than a real bcrypt hash. No registration/user-creation RPC exists anywhere in `user.proto` (a deliberate scope decision, not an oversight), so `BCryptPasswordEncoder.encode()`'s actual behavior had never been exercised by a single line of code in this repo — assumed correct, never verified. Added `SecurityConfigTest` (2 tests): a real `encode()`/`matches()` round trip, plus a negative case proving a wrong password is rejected. Verified with a real `./gradlew :user-service:test` run, and confirmed the suite genuinely catches wrong data by deliberately flipping the negative assertion's expected boolean and re-running before reverting.

---

### [`User.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/domain/User.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #30](https://github.com/Terrence721/saga-full/issues/30))

JPA entity: Hibernate-native `UUID` id generation, `email` unique/not-null, `password_hash` explicitly column-named, `active` a primitive `boolean` matching its `nullable = false` constraint. No custom `equals`/`hashCode` — the safe default for a JPA entity, matching every other entity in this repo. All 4 real `User.builder()` call sites (all in test code) explicitly set `.active(...)`. Cross-checked structurally against `order-service`'s `Order.java` — identical shape, no new pattern introduced.

---

### [`InvalidCredentialsException.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/exception/InvalidCredentialsException.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #32](https://github.com/Terrence721/saga-full/issues/32))

Trivial `RuntimeException` subclass. Confirmed correct usage end to end: thrown by `UserGrpcServiceImpl.login()` on a password mismatch, caught by `GrpcExecutor` and mapped to gRPC `UNAUTHENTICATED`, matching `UserGrpcServiceImplErrorTest`'s coverage.

**Related observation, not a defect here**: `login()`'s branching returns a different status for "unknown email" (`NOT_FOUND`) versus "wrong password" (this class, `UNAUTHENTICATED`) — a user-enumeration pattern (CWE-203). The root cause lives in `UserGrpcServiceImpl`/`GrpcExecutor`, not this exception type — flagged for when those files come up.

---

### [`UserInactiveException.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/exception/UserInactiveException.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #34](https://github.com/Terrence721/saga-full/issues/34))

Identical trivial shape to `InvalidCredentialsException.java`. Confirmed correct usage: thrown when `User.isActive()` is false (checked before the password check), mapped to gRPC `PERMISSION_DENIED`. Same enumeration observation as #32 applies, one more facet: an inactive account with a wrong password still returns `PERMISSION_DENIED` rather than `UNAUTHENTICATED`, also revealing account-active status to anyone who already knows the email. Same root cause, same file to fix — not this one.

---

### [`UserNotFoundException.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/exception/UserNotFoundException.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #36](https://github.com/Terrence721/saga-full/issues/36))

Third and last of the three sibling exception types. Confirmed correct usage: thrown when `findByEmail` finds no match, mapped to gRPC `NOT_FOUND`. All three individually are correct — the enumeration observation they collectively drive (`NOT_FOUND`/`UNAUTHENTICATED`/`PERMISSION_DENIED` all distinguishable) will be written up as a real finding on `GrpcExecutor.java`, the file that actually owns the mapping.

---

### [`GrpcExecutor.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/infra/grpc/GrpcExecutor.java)

**medium · Security** — Fixed via [PR #39](https://github.com/Terrence721/saga-full/pull/39) ([issue #38](https://github.com/Terrence721/saga-full/issues/38))

`execute()` mapped three exceptions to three *distinguishable* gRPC statuses on `Login`: unknown email → `NOT_FOUND`, wrong password → `UNAUTHENTICATED`, inactive account → `PERMISSION_DENIED`. Any caller could enumerate registered emails — and their active status — purely from the login response, without ever guessing a password (CWE-203). This is the finding flagged across #32/#34/#36.

Discussed the tradeoff before fixing: collapsing `PERMISSION_DENIED` too has a real cost (some systems deliberately tell a user their account was deactivated as a UX courtesy). Decision: full enumeration protection — collapsed all three into one generic `UNAUTHENTICATED` / `"Invalid email or password"` response via a multi-catch. The specific reason is still logged server-side; only the wire response is generic. Updated `UserGrpcServiceImplErrorTest` (3 tests) and `UserGrpcServiceIntegrationTest`'s real-wire-round-trip test to match. Verified with a real `./gradlew :user-service:test` run, confirmed the suite genuinely catches wrong data by deliberately reverting one assertion and re-running before reverting back, and confirmed the full multi-module suite (88 tests) stays green.

**Known cross-module consequence, not fixed here**: `api-gateway-service`'s `UserGrpcExceptionTranslator` still maps `NOT_FOUND`/`PERMISSION_DENIED` to their own exception types — those branches are now unreachable via the login path specifically, since `user-service` never returns those codes for `Login` anymore. Left as-is, since the translator is generic infrastructure for any future RPC, not login-specific logic; `api-gateway-service`'s own code-review pass is the right place to revisit it.

---

### [`UserGrpcServiceImpl.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/infra/grpc/UserGrpcServiceImpl.java)

**medium · Security** — Fixed via [PR #41](https://github.com/Terrence721/saga-full/pull/41) ([issue #40](https://github.com/Terrence721/saga-full/issues/40))

`login()` short-circuited via `orElseThrow()` when the email wasn't found, and threw on an inactive account before ever calling `passwordEncoder.matches()`. BCrypt is deliberately slow (~100ms at cost factor 10); skipping it for "unknown email" and "inactive account" meant those cases responded measurably faster than a real login attempt — a timing side-channel (CWE-208) surviving even after #38/PR #39 already made all three cases return the identical `UNAUTHENTICATED` status.

Chose the full fix, matching #38's scope. Restructured `login()` so `passwordEncoder.matches()` runs exactly once on every path — found-active, found-inactive, and not-found (against a fixed, real, unused bcrypt hash, generated for real via `BCryptPasswordEncoder`, not hand-typed) — before any branching. The original precedence (unknown-email > inactive > wrong-password, for server-side logging) is preserved; only the timing is normalized. Added 2 tests verifying via Mockito `verify()` that `passwordEncoder.matches()` is genuinely invoked on both previously-fast-path branches. Verified for real: reverted the fix temporarily, confirmed both new tests fail against the old code, restored it, confirmed everything passes again. Full multi-module suite (90 tests) verified green.

---

### [`JwtTokenProvider.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/infra/security/JwtTokenProvider.java)

**low · Maintainability** — Fixed via [PR #43](https://github.com/Terrence721/saga-full/pull/43) ([issue #42](https://github.com/Terrence721/saga-full/issues/42))

`createToken()`/`verifyToken()` logic is correct, already covered by `JwtTokenProviderTest`'s 5 tests. But `verifyToken()` rebuilt a fresh `JWTVerifier` on every call, while the sibling code in `api-gateway-service`'s `JwtPerimeterGuardGatewayFilterFactory` deliberately builds it once in the constructor for exactly this reason. Since `algorithm`/`issuer` are immutable fields here too, moved the `.build()` call into the constructor as a cached field, matching the established pattern — zero behavior change, existing test suite passed unchanged as proof.

---

### [`UserRepository.java`](https://github.com/Terrence721/saga-full/blob/main/user-service/src/main/java/io/github/terrence721/saga/user/repository/UserRepository.java) — last file in `user-service`

**low · Maintainability** — Fixed via [PR #45](https://github.com/Terrence721/saga-full/pull/45) ([issue #44](https://github.com/Terrence721/saga-full/issues/44))

A 4-line Spring Data JPA repository, one derived query method (`findByEmail`). `Optional<User>` return type is only safe because `User.email` carries a genuine `@Column(unique = true)` constraint — confirmed, not assumed.

**Structural note, not fixed**: email lookups are case-sensitive (no normalization anywhere in `user-service`). Not fixed — this repo has no registration/write flow to normalize at intake (#28/#30), so a read-time fix alone would just be guessing at how out-of-band-seeded data is cased. Worth revisiting if a registration flow is ever built.

**Real finding, fixed (test-coverage gap)**: `user-service` had zero `@DataJpaTest` repository coverage, unlike `order-service`/`payment-service`/`restaurant-service`, which all established this as standard practice. `findByEmail` and the `email` unique constraint had never been verified against a real database. Added `UserRepositoryTest` (3 tests, mirroring `OrderRepositoryTest`'s shape): found/not-found by email, and a real proof the unique constraint is genuinely enforced (`saveAndFlush` a duplicate, expect `DataIntegrityViolationException`). Verified with a real `./gradlew :user-service:test` run, confirmed the unique-constraint test genuinely catches wrong data by deliberately using a non-duplicate email and re-running before reverting.

---

**`user-service` module review complete — 10/10 files reviewed, 5 real findings fixed, 2 structural notes recorded.** A test-coverage gap in `SecurityConfig.java`, a login-enumeration security fix (CWE-203) in `GrpcExecutor.java`, its timing-side-channel sibling (CWE-208) in `UserGrpcServiceImpl.java`, a minor efficiency fix in `JwtTokenProvider.java`, and a test-coverage gap in `UserRepository.java`. Both structural notes (`ValidateToken`'s unused RPC, case-sensitive email lookups) are blocked on a registration flow that doesn't exist in this repo yet. Full multi-module suite: 95/95 tests passing. See [todo.md](../todo.md) for the full per-file table and the next module in the audit.

### [`OrderServiceApplication.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/OrderServiceApplication.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #48](https://github.com/Terrence721/saga-full/issues/48))

15 lines: package declaration, two imports, a single `@SpringBootApplication`-annotated class with a `main()` calling `SpringApplication.run()`. The one difference from the other modules' entry points, `@EnableScheduling`, is genuinely needed — confirmed by grepping for `@Scheduled`, which turned up `OutboxPublisherService`'s real polling method in this same module. Identical in shape otherwise to every other module's entry-point class already reviewed.

---

### [`OrderController.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/controller/OrderController.java)

**medium · Security** — Fixed via [PR #51](https://github.com/Terrence721/saga-full/pull/51) ([issue #50](https://github.com/Terrence721/saga-full/issues/50))

`docs/architecture.md`'s own Phase 23 entry already recorded this as a tracked gap: the perimeter-header trust boundary "needs to be added once `api-gateway-service` exists to actually inject a verified identity." That gateway now exists (Phase 37-40) and its `JwtPerimeterGuardGatewayFilterFactory` injects a verified `X-Perimeter-User-Id` header on every request routed to `POST /orders` — but `OrderController` never read it. Since `CreateOrderRequest.customerId` is only `@NotNull`, any authenticated caller could set it to an arbitrary UUID and create an order attributed to a different customer; the gateway's identity verification was completely inert for this endpoint's actual authorization decision. Confirmed via a repo-wide grep for `X-Perimeter-User-Id`: only the gateway injects it, nothing downstream ever read it.

Discussed the implementation shape before fixing, since `order-service` has no exception-handling infrastructure at all (no `@ControllerAdvice` anywhere in the module — not even for the existing `OrderNotFoundException`). Chose a `ResponseStatusException(FORBIDDEN)` thrown inline over the source's dedicated `ClientIdentityMismatchException` + handler — a whole new exception-handling layer for one check was more infrastructure than the module needs today. Rejects with `403` when the header is missing entirely or doesn't match `request.customerId()`, failing closed for any call that bypassed the gateway. Added 2 tests (missing header, mismatched header); the existing happy-path test now sends a matching header. Verified for real: ran the suite with the fix (4/4 passing), reverted the fix only, confirmed both new tests genuinely fail against the old code, restored it and confirmed green again.

**Follow-up, fixed via [PR #55](https://github.com/Terrence721/saga-full/pull/55) ([issue #54](https://github.com/Terrence721/saga-full/issues/54))**: CodeQL flagged the fix above's own `log.warn` call — `perimeterUserId` is a raw, client-controlled header value whenever the check actually fires, and it was being logged unsanitized, the same CWE-117 class Phase 41 already fixed for `itemCode` in this same file. Sanitized with the identical `.replaceAll("[\r\n]", "_")` pattern, handling the `null` case since the header is optional. No dedicated test added, matching Phase 41's own precedent of relying on the full suite staying green rather than a log-content assertion.

**Second follow-up, fixed via [PR #59](https://github.com/Terrence721/saga-full/pull/59) ([issue #58](https://github.com/Terrence721/saga-full/issues/58))**: PR #55's ternary-based sanitization (`perimeterUserId == null ? "null" : perimeterUserId.replaceAll(...)`) resolved that CodeQL alert but a new one opened on the next scan — its sanitizer detection doesn't treat a `.replaceAll()` call nested inside only one ternary branch as a barrier for the whole expression. Restructured to `String.valueOf(perimeterUserId).replaceAll("[\r\n]", "_")`, where the sanitizing call runs unconditionally on every path (`String.valueOf(null)` returns the literal `"null"`), rather than being conditionally applied.

---

### [`Order.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/domain/Order.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #52](https://github.com/Terrence721/saga-full/issues/52))

JPA entity: Hibernate-native `UUID` id generation, `total_amount` `BigDecimal` with `precision = 19, scale = 2` matching `payment-service`'s `Payment` entity exactly, no custom `equals`/`hashCode` per this repo's established safe default. Table name `customer_orders` (not `orders`) is a deliberate, already-tested choice, not an accident. The only production construction site is `OrderService.createOrder`, built entirely from an already-validated `CreateOrderRequest`.

Considered flagging the class-level `@Setter` leaving `setCustomerId`/`setTotalAmount`/`setItemCode`/`setQuantity` as unused dead surface (only `setStatus` and a test-only `setId` are ever actually called) — but `User.java`'s already-closed review has the identical shape and passed clean. Raising it here would relitigate an already-accepted repo-wide Lombok convention, not flag something specific to this file.

---

### [`OrderStatus.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/domain/OrderStatus.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #56](https://github.com/Terrence721/saga-full/issues/56))

3-value enum (`PENDING`, `CANCELLED`, `SUCCESS`). Grepped every reference across the whole repo: all 3 values are genuinely used in `order-service`'s own saga transitions, no dead values. Cross-checked against `payment-service`'s local copy — matches exactly, consistent with Phase 30's documented correction. `Order.java`'s `@Enumerated(EnumType.STRING)` (reviewed in #52) means storage is name-based, so reordering is safe.

---

### [`OutboxRecord.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/domain/OutboxRecord.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #60](https://github.com/Terrence721/saga-full/issues/60))

JPA entity backing the transactional outbox pattern. `aggregateId`'s `String` typing (vs. `Order.id`'s `UUID`) is deliberate — it's used directly as the Kafka message key `OutboxPublisherService` sends, not a typing inconsistency. `@Lob` on the JSON `payload` is a known PostgreSQL/Hibernate gotcha area (can map to the `oid` large-object type instead of `text` depending on dialect/version) — not flagging it as unverified, since Phase 26's real `bootRun` against live Postgres already exercised this exact mapping end-to-end. Matches `payment-service`/`restaurant-service`'s identical `OutboxRecord` shape.

Checked every setter call site across the module: zero. `OutboxRecord` is built once via `.builder()` and only ever read or deleted — a stronger case of the same class-level-`@Setter` pattern already reviewed and accepted on `Order.java` (#52), not a new issue.

---

### [`CreateOrderRequest.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/dto/CreateOrderRequest.java)

**low · Reliability** — Fixed via [PR #63](https://github.com/Terrence721/saga-full/pull/63) ([issue #62](https://github.com/Terrence721/saga-full/issues/62))

`itemCode` had `@NotBlank` but no upper bound, while `Order.itemCode` maps to Hibernate's default `VARCHAR(255)`. `totalAmount` had `@NotNull`/`@Positive` but no `@Digits`, while `Order.totalAmount` is `NUMERIC(19,2)`. Grepped the whole repo for `@Size`/`@Digits`: zero matches — a genuine gap, not an inconsistency with an established convention. Since `order-service` has no exception-handling infrastructure, a request that passes this DTO's validation but violates the entity's actual column constraints currently reaches the database raw and surfaces as an unhandled `500`, not a clean `400` — a real system boundary (`POST /orders` is public behind gateway auth, otherwise unrestricted body content), not a scenario that can't happen.

Added `@Size(max = 255)` to `itemCode` and `@Digits(integer = 17, fraction = 2)` to `totalAmount`, matching `Order`'s schema exactly. 2 new tests, both expecting `400`. Verified for real: ran the suite with the fix (8/8 passing), reverted the fix only, confirmed both new tests genuinely fail against the old code, restored it and confirmed green again.

---

### [`OrderCreatedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/dto/OrderCreatedEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #64](https://github.com/Terrence721/saga-full/issues/64))

Outbox event record, no custom logic. Its only construction site is `OrderService.buildOutboxRecord`, called exactly once from `createOrder`, built directly off an already-persisted `Order` entity whose fields already passed `CreateOrderRequest`'s validation — never bound from untrusted input directly, so no Bean Validation is needed here. Field-for-field identical to `payment-service`'s mirrored copy (confirmed in #56). Since `buildOutboxRecord` is only ever reached from `createOrder`, `status` is always `PENDING` on every real instance, consistent with `payment-service`'s already-reviewed assumption (Phase 30).

---

### [`RestaurantApprovedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/dto/RestaurantApprovedEvent.java)

**n/a · Maintainability** — Reviewed, structural note recorded, not fixed here ([issue #66](https://github.com/Terrence721/saga-full/issues/66))

Inbound event record. Grepped every `.customerId()`/`.ticketId()` call site: `OrderService.confirmOrder` only ever reads `event.orderId()` — both other fields are deserialized but never consumed. Confirmed they aren't placeholder data: `restaurant-service`'s producer populates them with a real persisted `RestaurantTicket.id` and the real saga `customerId`. A genuine asymmetry against `restaurant-service`'s own inbound event (`PaymentProcessedEvent`), which validates/uses every field defensively against malformed Kafka messages (Phase 35) — `order-service`'s consumers do zero validation on any field of either inbound event.

Not fixed here — the record itself correctly mirrors the real wire contract; the fix belongs to `OrderService.java`'s own review, the file that actually owns `confirmOrder`.

---

### [`RestaurantRejectedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/dto/RestaurantRejectedEvent.java)

**n/a · Maintainability** — Reviewed, structural note recorded, not fixed here ([issue #68](https://github.com/Terrence721/saga-full/issues/68))

Same unused-field note as `RestaurantApprovedEvent.java` (#66): `customerId` is deserialized but never read anywhere in `order-service`.

**Real finding, traced end-to-end, not fixed here**: `reason` is a genuine CWE-117 log-injection sink. Client-controlled `itemCode` (length-capped by #62, but content unrestricted) survives unmodified through `Order`/`OrderCreatedEvent`, gets passed straight through `payment-service`'s `PaymentProcessedEvent`, then gets raw-concatenated into `restaurant-service`'s rejection reason (`"Invalid item code: " + event.itemCode()`) on `RestaurantRejectedEvent`, which `order-service`'s `OrderService.cancelOrder` logs unsanitized — the same vulnerability class already fixed three times in this module (Phase 41, #54, #58), reached through a different, cross-service path. `ITEM_NOT_FOUND` is trivially triggerable (almost any string won't match a real `InventoryItem`), so this is a live, reachable path, not a hypothetical.

Not fixed here — the record correctly carries the wire contract, and the vulnerable `log.warn` call lives in `OrderService.cancelOrder`, a file not yet reached in this review's order. Matches the precedent set by the CWE-203 enumeration finding (flagged across #32/#34/#36, fixed only when `GrpcExecutor.java` reached its own turn): documented now, will be fixed when `OrderService.java` comes up.

---

### [`OrderNotFoundException.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/exception/OrderNotFoundException.java)

**n/a · Maintainability** — Reviewed, structural note recorded, not fixed here ([issue #70](https://github.com/Terrence721/saga-full/issues/70))

Trivial, itself-correct `RuntimeException` subclass. Thrown exactly once (`OrderService.findOrder`), reachable only from `confirmOrder`/`cancelOrder` — never the HTTP path. Its message interpolates `orderId`, but that field is `UUID`-typed on both inbound events, so Jackson rejects malformed values before this code runs — no log-injection surface here, unlike `reason`/`itemCode` (#68).

**Real finding, not fixed here**: this exception is caught nowhere — no `@ControllerAdvice`, no try/catch in `OrderConsumerConfig`, and no Kafka listener error-handling configuration anywhere in the module (`application.yaml`'s `kafka:` section has none). Since it's only reachable from `@KafkaListener` methods, an uncaught throw propagates out of the listener with no `CommonErrorHandler`/`DefaultErrorHandler` bean configured to handle it — a scenario triggered by any restaurant-service event referencing an order ID this service doesn't recognize, a genuinely reachable case given independent producers and at-least-once Kafka delivery. ~~Spring Kafka's default error handling retries indefinitely with no backoff limit~~ **Correction** (verified during `OrderConsumerConfig.java`'s own review, [#76](https://github.com/Terrence721/saga-full/issues/76)): the actual default (`FixedBackOff(0, 9)`) is 10 rapid retries with zero backoff, then the record is logged and the offset committed — the message is silently **dropped**, not retried forever. Still a real bug (silent data loss, no DLQ, no visibility beyond one log line), just not the poison-pill-blocks-the-partition-forever framing originally written here.

Not fixed here — the exception type itself is blameless; the fix belongs to `OrderConsumerConfig.java`'s own review, the file that owns the listener wiring.

---

### [`OrderRepository.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/repository/OrderRepository.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #72](https://github.com/Terrence721/saga-full/issues/72))

2-line Spring Data JPA interface, one custom derived query (`existsByIdAndStatus`). Grepped every call site: `save`/`findById` (inherited) and `existsByIdAndStatus` (the idempotency guard) — all genuinely used, nothing unused or missing. `existsByIdAndStatus` is tested against real embedded H2 across all 3 branches, not assumed. Matches `UserRepository`'s already-reviewed precedent closely in shape and test rigor.

---

### [`OutboxRepository.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/repository/OutboxRepository.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #74](https://github.com/Terrence721/saga-full/issues/74))

Spring Data JPA interface, one custom query (`findByOrderByCreatedTimeAsc`, `PESSIMISTIC_WRITE` + `lock.timeout=-2` → `SKIP LOCKED`). Grepped every call site: `save`, `findByOrderByCreatedTimeAsc`, `delete` — all genuinely used. `findByOrderByCreatedTimeAsc` is tested against real embedded H2 with a real assertion (12 records inserted, confirms exactly the 10 oldest come back in correct order), not a count check. The `SKIP LOCKED` semantic itself isn't independently proven under real concurrent transactions — already a documented, deliberate scope decision from Phase 19, not a new gap.

---

### [`OrderConsumerConfig.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/service/OrderConsumerConfig.java)

**medium · Reliability** — Fixed via [PR #77](https://github.com/Terrence721/saga-full/pull/77) ([issue #76](https://github.com/Terrence721/saga-full/issues/76))

Two `@KafkaListener` methods (`onRestaurantApproved`/`onRestaurantRejected`), each deserializing the raw JSON payload and delegating to `OrderService.confirmOrder`/`cancelOrder`. Grepped for callers: the Kafka container invokes this class; nothing else in the repo does.

**Real finding, closing the gap deferred from `OrderNotFoundException.java`'s review (#70)**: neither listener method caught anything — not a malformed payload's `JsonProcessingException`, not `OrderService.findOrder`'s `OrderNotFoundException` for an event referencing an order ID this instance doesn't recognize (reachable given independent producers and at-least-once Kafka delivery). No `CommonErrorHandler`/`DefaultErrorHandler` bean existed anywhere in the module, so an uncaught throw fell through to Spring Boot's autoconfigured default with nothing in the code announcing that. That default turned out to be milder than #70's write-up claimed — see the correction on that entry above — but the underlying gap (accidental, undocumented reliance on a library default) was real.

**Fix**: added an explicit `DefaultErrorHandler` `@Bean` — Spring Boot auto-applies any single `CommonErrorHandler` bean to the autoconfigured listener container factory, no other wiring needed. A bounded `FixedBackOff(1000L, 2L)` (3 attempts total, 1s apart) replaces the accidental 10x/0ms default; `addNotRetryableExceptions` marks `OrderNotFoundException` non-retryable since retrying can't make a missing order appear; an explicit `ERROR`-level recoverer logs topic/partition/offset/exception on final failure, trading an invisible default for a loud, intentional one. No DLQ topic — discussed with the repo owner first, since a real DLQ has no precedent anywhere in this repo yet and would set one for three still-unreviewed sibling modules; matches this module's existing precedent (`OrderController.java`'s inline `ResponseStatusException` over a dedicated exception+handler layer, #50) of not building more infrastructure than one finding needs.

Added 2 tests exercising the handler directly via `CommonErrorHandler.handleOne(...)`: the handler recovers `OrderNotFoundException` on the first call (returns `true`, confirmed by the real recovery log line firing), while a generic exception gets a retry instead of an on-the-spot give-up (first call returns `false`). Verified for real: full `order-service` suite green with the fix (including `contextLoads()` — the new bean doesn't break context startup); reverted the production fix while leaving the tests in place, confirmed both new tests genuinely fail to compile against the old code, then restored the fix and confirmed green again.

---

### [`OrderService.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/service/OrderService.java)

**medium · Security** — Fixed via [PR #79](https://github.com/Terrence721/saga-full/pull/79) ([issue #78](https://github.com/Terrence721/saga-full/issues/78))

`createOrder`/`confirmOrder`/`cancelOrder` — the saga's actual state-machine owner. Grepped every call site of both listener-facing methods: `OrderConsumerConfig` is the sole caller, confirming this file owns both findings deferred to it.

**Real finding #1, closing the gap deferred from `RestaurantRejectedEvent.java`'s review (#68)**: `event.reason()` reached `cancelOrder`'s `log.warn` raw and unsanitized — a confirmed CWE-117 log-injection sink traced end-to-end through 3 service hops (client `itemCode` → `order-service` → `payment-service` → `restaurant-service`'s raw string-concatenated rejection reason → back here). Same vulnerability class already fixed twice in this module (`OrderController.java`, #50/#54/#58), reached through a different, cross-service path this time.

**Real finding #2, closing the gap deferred from `RestaurantApprovedEvent.java`/`RestaurantRejectedEvent.java`'s review (#66)**: `customerId` (both events) and `ticketId` (approved event) reached `confirmOrder`/`cancelOrder` through deserialization but neither method read them — a real asymmetry against this repo's own established pattern (`restaurant-service`'s `PaymentProcessedEvent` validates every field defensively, Phase 35).

**Fix**: sanitizes `event.reason()` with the exact pattern `OrderController.java` already established — `String.valueOf(event.reason()).replaceAll("[\r\n]", "_")` — before the `log.warn` call. `confirmOrder`/`cancelOrder` now cross-check `event.customerId()` against the already-loaded `order.getCustomerId()` and throw `IllegalArgumentException` on mismatch — a real integrity check against a corrupted or mismatched message on the shared topic, reusing data already in hand rather than a second lookup. `ticketId` has no field on `Order` to cross-reference, so `confirmOrder` applies a null check alone (`IllegalArgumentException` if missing), matching `restaurant-service`'s own `validate()` precedent for fields it can't cross-check either, rather than adding a column purely to store a value nothing reads. Three real options existed here — cross-check defensively, drop the fields, or persist `ticketId` for real — so the repo owner picked the approach before implementation started.

Ripple-effect fix, since fixing this exposed a second gap: registered the new `IllegalArgumentException` as non-retryable in `OrderConsumerConfig.kafkaErrorHandler()` (added in #76) alongside `OrderNotFoundException` — retrying a genuine data mismatch can't fix it, so it should skip straight to the recoverer instead of wasting 3 retries.

No dedicated exception type or `ticket_id` column — matches this module's existing precedent of using what's already in hand (`OrderController.java`'s inline `ResponseStatusException`, restaurant-service's plain `IllegalArgumentException` in `validate()`) over new infrastructure for a single check.

Added 6 tests: `confirmOrder` throws on missing `ticketId` and on `customerId` mismatch, `cancelOrder` throws on `customerId` mismatch and still cancels the order when `reason` contains CR/LF, plus a matching non-retryable-recovery test for `IllegalArgumentException` in `OrderConsumerConfigTest`. CodeQL's `java/log-injection` query proves the CR/LF sanitization statically — the same gate that caught this exact vulnerability class twice already in this module — rather than a log-capture assertion, matching how `OrderController.java`'s identical-shaped fix got verified. Verified for real: full `order-service` suite green with the fix; reverted the production fixes alone, confirmed the 4 behavior-asserting new tests fail against the old code, restored them and confirmed green again.

---

### [`OutboxPublisherService.java`](https://github.com/Terrence721/saga-full/blob/main/order-service/src/main/java/io/github/terrence721/saga/order/service/OutboxPublisherService.java)

**low · Reliability** — Fixed via [PR #81](https://github.com/Terrence721/saga-full/pull/81) ([issue #80](https://github.com/Terrence721/saga-full/issues/80))

Last file in this module. `@Scheduled`/`@Transactional` poller: pulls a batch via the already-reviewed `SKIP LOCKED` query (#74), sends each record's raw payload to Kafka, deletes on confirmed send, leaves the record for retry next poll on failure. Cross-checked against the two sibling implementations `todo.md`'s Phase 31/36 log describes (`payment-service`/`restaurant-service` share this identical shape) — the same gap probably exists there too, out of scope until their own reviews reach this file. No log-injection surface (`record.getEventType()` is a hardcoded literal, not client input) and no ordering bug (already proven correct in #74).

**Real finding**: `kafkaTemplate.send(message).get()` blocked with no explicit timeout, bounded solely by Kafka's own client default (`delivery.timeout.ms`, 120s), undocumented anywhere in this code. This method is `@Transactional` (required to hold the outbox row's `PESSIMISTIC_WRITE` lock across the batch), so an unbounded wait meant a degraded broker could hold a DB connection and row lock open for up to the full 120s per record — worst case ~20 minutes for a full batch of 10 stuck records.

**Fix**: added an explicit `get(sendTimeoutMs, TimeUnit.MILLISECONDS)` bound, defaulting to 10s (`app.outbox.send-timeout-ms`, a new `application.yaml` property matching the existing `app.outbox.*` naming convention), with a new `TimeoutException` catch mirroring the existing `ExecutionException` handling. Trades a small increase in duplicate-publish likelihood on timeout (the send may still succeed server-side after the client gives up waiting) for a much shorter, bounded worst-case hold on the DB connection/lock — an acceptable tradeoff since this system already runs at-least-once, guarded by consumer-side idempotency checks that already tolerate duplicate delivery. Two rejected alternatives: leaving it as an accepted structural note (matching the `SKIP LOCKED` untested-concurrency precedent) — this repo's standard now fixes what it finds instead of documenting around it; and redesigning to avoid blocking inside the transaction entirely — a much bigger architectural change spanning all 3 sibling implementations, out of scope for a single-file review. Discussed with the repo owner before implementing.

Added 1 test: a `CompletableFuture` that never completes, against a short-timeout (50ms) instance of the service, proves the explicit bound fires rather than blocking indefinitely. Existing tests use already-resolved futures, so they're unaffected by the change. Verified for real: full `order-service` suite green with the fix; reverted the production fix alone, confirmed the change fails to compile against the old constructor, restored it and confirmed green again.

---

**`order-service` module review complete — 15/15 files reviewed, 5 issues fixed across 5 files, 0 findings left open.** An IDOR fix and 2 CodeQL log-injection follow-ups in `OrderController.java` (#50/#54/#58), a DTO-validation fix in `CreateOrderRequest.java` (#62), a Kafka poison-pill gap fixed in `OrderConsumerConfig.java` (#76, deferred from #70), a CWE-117 log-injection fix plus `customerId`/`ticketId` validation in `OrderService.java` (#78, two findings deferred from #66/#68, both fixed), and an unbounded-Kafka-timeout fix in `OutboxPublisherService.java` (#80). Every deferred finding raised during this module's review landed as a real fix by the time the module closed — including a correction to #70's own write-up once #76's review verified Spring Kafka's actual default retry behavior differed from what was first documented. Full multi-module suite: 107/107 tests passing. See [todo.md](../todo.md) for the full per-file table and the next module in the audit.

---

### [`PaymentServiceApplication.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/PaymentServiceApplication.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #82](https://github.com/Terrence721/saga-full/issues/82))

12 lines: package declaration, two imports, a single `@SpringBootApplication`-annotated class with a `main()` calling `SpringApplication.run()`. Grepping for `@Scheduled` confirms `@EnableScheduling` is genuinely needed — it turned up `OutboxPublisherService`'s real polling method in this same module. Identical in shape to `OrderServiceApplication.java`/`UserServiceApplication.java`, both already reviewed with no findings.

---

_More findings are appended here as each file's PR merges. See [todo.md](../todo.md) for the per-file tracking table of whichever module is currently in progress._
