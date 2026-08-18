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

_More findings are appended here as each file's PR merges. See [todo.md](../todo.md) for the per-file tracking table of whichever module is currently in progress._
