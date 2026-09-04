# Code Review Results

<!-- markdownlint-disable-next-line MD036 -->
**Last Updated: September 2, 2026**

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

**Third follow-up, [PR #118](https://github.com/Terrence721/saga-full/pull/118) (2026-08-28)**: even PR #59's unconditional fix stayed flagged (alert #3) — the code comment left behind noted "still flagged post-PR #55" without ever actually resolving it. Investigated properly this time: split the null-header and mismatched-header cases into two branches so the mismatch branch's `perimeterUserId` is narrowed non-null (fixed alert #3 for real, confirmed against a live CodeQL run), which revealed a new alert at the exact `.replaceAll()` call site; routed it through a small `sanitizeForLog(String)` helper method (fixed that one too, confirmed), which revealed yet another alert at the helper's own call site. Four structurally distinct fix attempts total, each verified against a real CodeQL run on a branch before touching `main` — every one cleared the prior alert only to reveal a new one at the next node in the same chain.

Researched CodeQL's actual `LogInjection.qll` source: the `.replaceAll("[\r\n]", "_")` sanitizer pattern used here is genuinely recognized as a barrier — and it's the exact same pattern that already clears this identical CWE-117 class for `itemCode` a few lines below (alerts #1/#2, both fixed). The likely difference: `itemCode`'s source is a Java record accessor on an `@RequestBody`-deserialized object, while `perimeterUserId` is a direct `@RequestHeader` string parameter — a more heavily-modeled, direct Spring MVC taint source, where it's plausible the record-accessor path was never fully tracked as tainted in the first place (a false negative there, not a successful sanitizer, which would explain why two "identical" patterns behave differently). CodeQL maintainers have publicly acknowledged recurring sanitizer-recognition gaps in this exact query (`github/codeql` discussions #10702, #12641), fixed piecemeal over time.

Given the value is provably, unconditionally sanitized in the running code across all 4 verified variants, the final alert (#5) was dismissed on GitHub as a documented false positive rather than continuing to guess at code shapes indefinitely — see the dismissal comment and PR #118 for the full investigation trail. The split-branch structure and shared `sanitizeForLog` helper are kept regardless, as genuine clarity improvements independent of the CodeQL outcome.

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

### [`OutboxRecord.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/domain/OutboxRecord.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #84](https://github.com/Terrence721/saga-full/issues/84))

Byte-for-byte identical to `order-service`'s `OutboxRecord.java` (already reviewed clean, #60/#61) — same fields, same `@Lob` on `payload`, same nullability constraints. A direct structural repeat, not a new design.

---

### [`Payment.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/domain/Payment.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #86](https://github.com/Terrence721/saga-full/issues/86))

JPA entity: Hibernate-native `UUID` id generation, `amount` a `BigDecimal(19,2)` and `status` an `EnumType.STRING` enum — matching `order-service`'s `Order.java` conventions (already reviewed clean, #52/#53). No custom `equals`/`hashCode` — the established safe default for entities in this repo. `orderId` carries a genuine `unique = true, nullable = false` constraint — a real DB-level backstop for the "one payment per order" invariant, matching `PaymentService.processPaymentSaga`'s own `existsByOrderId` idempotency check. Grepped every `Payment.builder`/`.setStatus`/`paymentRepository.` call site: every field maps to a real caller, nothing orphaned or unused.

---

### [`PaymentStatus.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/domain/PaymentStatus.java)

**medium · Reliability** — Fixed via [PR #89](https://github.com/Terrence721/saga-full/pull/89) ([issue #88](https://github.com/Terrence721/saga-full/issues/88))

**Real finding, fixed**: `PaymentStatus.FAILED` was declared but never producible or consumable anywhere in the codebase — `PaymentService.processPaymentSaga` unconditionally approved every payment, so there was no real decline path (the same dead-value shape as `user-contract`'s `ValidateToken`-no-caller structural note, #24). Per this repo's no-excused-findings policy, this was implemented rather than left as a permanent note: `processPaymentSaga` now declines (`FAILED`) any order whose `totalAmount` exceeds a configurable `app.payment.max-amount` limit (default $500, simulating a simple authorization/credit limit), still publishing `PaymentProcessedEvent` on the existing topic/eventType either way. `handleOrderCompensation` now short-circuits for a payment already `FAILED` — nothing was captured, so there's nothing to refund; without this guard a `RestaurantRejectedEvent` for a declined order would have incorrectly flipped it to `REFUNDED`.

This cascaded into `restaurant-service/RestaurantService.java` (ahead of that module's own audit, which hasn't started): `processRestaurantStep` now checks `PaymentProcessedEvent.status()` and short-circuits straight to a `RestaurantRejectedEvent` (reason: "Payment failed for order: ...") without touching inventory when status isn't `APPROVED` — reusing the existing rejection/compensation pathway end-to-end with no new topics or event types. Verified for real: 3 new tests added (`PaymentServiceTest` ×2, `RestaurantServiceTest` ×1), and the fix was deliberately reverted in both files to confirm all 3 fail (two assertion failures, one `NullPointerException` from the unguarded inventory call) before being restored — full repo suite green at 110/110 afterward.

`payment-service/PaymentService.java` and `restaurant-service/RestaurantService.java` are considered covered by this review; when the audit reaches them on its normal per-file schedule, they'll be marked as already reviewed here rather than re-reviewed from scratch.

---

### [`OrderCreatedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/dto/OrderCreatedEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #90](https://github.com/Terrence721/saga-full/issues/90))

Plain record, no custom logic. Its sole consumer, `PaymentConsumerConfig.onOrderCreated`, deserializes it and passes it straight to `PaymentService.processPaymentSaga`, which uses `orderId`/`totalAmount` directly and `customerId`/`itemCode`/`quantity` when building the outbound `PaymentProcessedEvent`. `status` is never read — always `PENDING` on every real instance, the same passthrough already reviewed and accepted on `order-service`'s identical sibling copy (#64). Its sole real producer is `order-service`'s own outbox, built from an already-Bean-Validated `CreateOrderRequest` — never bound from untrusted input directly, matching this repo's established reasoning for why no additional validation belongs on this record itself.

---

### [`OrderStatus.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/dto/OrderStatus.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #92](https://github.com/Terrence721/saga-full/issues/92))

3-value enum (`PENDING`, `CANCELLED`, `SUCCESS`), used only as the type of `OrderCreatedEvent.status` (reviewed clean, #90 — a field never read by `PaymentService`, an always-`PENDING` passthrough on every real instance). Order-service's own `OrderStatus.java` review already cross-checked this exact payment-service copy across the whole repo for dead values and found none: all three values are genuinely used in order-service's real saga transitions. This copy exists purely for deserialization type-compatibility with the incoming event.

---

### [`PaymentProcessedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/dto/PaymentProcessedEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #94](https://github.com/Terrence721/saga-full/issues/94))

Plain record, no custom logic, field-for-field identical to `restaurant-service`'s own copy of this event. Its sole construction site, `PaymentService.buildOutboxRecord`, was already reviewed and fixed under #88 (the payment-decline path) — every field is genuinely populated from a real `Payment`/`OrderCreatedEvent`, including `status`, now meaningful since `processPaymentSaga` can actually produce `FAILED`. Every field has a real downstream consumer in `restaurant-service`'s `RestaurantService` (also fixed under #88 to branch on `status`).

---

### [`RestaurantRejectedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/dto/RestaurantRejectedEvent.java)

**low · Reliability** — Fixed via [PR #97](https://github.com/Terrence721/saga-full/pull/97) ([issue #96](https://github.com/Terrence721/saga-full/issues/96))

**Real finding, fixed**: this record's sole real consumer, `PaymentService.handleOrderCompensation`, never read `event.customerId()` — unlike order-service's identical scenario (`OrderService.cancelOrder`, consuming its own copy of this same event), which cross-checks the event's `customerId` against its already-loaded entity as defense-in-depth against a corrupted/mismatched message on the shared Kafka topic (#68). The asymmetry existed because `Payment` never persisted a `customerId` to compare against — a genuine gap, not a live money-routing bug (`orderId` alone already determines the correct `Payment` row to refund), but real enough, and cheap enough to match, that it was fixed rather than just noted.

Fix: added a `customer_id` column to `Payment` (`nullable = false`), populated it in `processPaymentSaga` from `OrderCreatedEvent.customerId()`, and added `validateCustomerMatches` to `handleOrderCompensation`, mirroring `OrderService`'s exact pattern (name, message format, placement). This retroactively touches two already-closed files: `Payment.java` (#86 — its "no findings" verdict was correct as far as it went at the time, just missing a field this later review added) and `PaymentService.java` (already covered under #88). Verified via deliberate revert: the new mismatch test genuinely failed without the check, then passed once restored. Full repo suite green, 111/111.

---

### [`PaymentNotFoundException.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/exception/PaymentNotFoundException.java)

**n/a · Maintainability** — Reviewed, structural note recorded, not fixed here ([issue #98](https://github.com/Terrence721/saga-full/issues/98))

Trivial, itself-correct `RuntimeException` subclass. Thrown exactly once (`PaymentService.handleOrderCompensation`'s `orElseThrow`), reachable only from the `onRestaurantRejected` Kafka listener. Its message interpolates `event.orderId()`, `UUID`-typed on `RestaurantRejectedEvent`, so Jackson rejects malformed values before this code runs — no log-injection surface.

**Real finding, not fixed here**: the same gap already found and fixed in order-service (`OrderNotFoundException.java` review, #70, fixed at `OrderConsumerConfig.java`'s own turn, #76) — `PaymentConsumerConfig.java` has no `DefaultErrorHandler`/`CommonErrorHandler` bean at all, so an uncaught throw from either of its two `@KafkaListener` methods falls through to Spring Kafka's actual default (`FixedBackOff(0, 9)`: 10 rapid retries, zero backoff, then the record is logged and the offset committed — silently dropped, not retried forever, per #76's own correction of the original write-up). Reachable given independent producers and at-least-once delivery. `IllegalArgumentException` (from `validateCustomerMatches`, added under #97) has the identical exposure.

Not fixed here — the exception type itself is blameless; the fix belongs to `PaymentConsumerConfig.java`'s own review (2 files away in this module's queue), the file that owns the listener wiring, mirroring order-service's exact precedent.

---

### [`OutboxRepository.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/repository/OutboxRepository.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #100](https://github.com/Terrence721/saga-full/issues/100))

Byte-for-byte identical to `order-service`'s already-reviewed sibling (#74) — same `PESSIMISTIC_WRITE` + `lock.timeout=-2` (SKIP LOCKED) custom query. Grepped every call site: `save` (`PaymentService`), `findByOrderByCreatedTimeAsc`/`delete` (`OutboxPublisherService`) — all genuinely used. `findByOrderByCreatedTimeAsc` is tested against real embedded H2 with the same shape as order-service's own test (12 records inserted, confirms exactly the 10 oldest come back in order). The SKIP LOCKED semantic itself isn't independently proven under real concurrent transactions — same documented, deliberate scope decision already accepted for order-service's identical copy.

---

### [`PaymentRepository.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/repository/PaymentRepository.java)

**low · Reliability** — Fixed via [PR #103](https://github.com/Terrence721/saga-full/pull/103) ([issue #102](https://github.com/Terrence721/saga-full/issues/102))

**Real finding, fixed (test-coverage gap)**: 3 derived-query methods (`findByOrderId`, `existsByOrderId`, `existsByOrderIdAndStatus`), all genuinely used in `PaymentService.java`'s idempotency guards. Only `findByOrderId` had real `@DataJpaTest` coverage — `existsByOrderId`/`existsByOrderIdAndStatus` had none, unlike order-service's identical-shape `OrderRepository`, whose own `existsByIdAndStatus` already has dedicated real-database coverage (#72). Same class of gap already found and fixed twice in `user-service`'s audit (`SecurityConfig.java` #29, `UserRepository.java` #45). Fixed with 5 new `@DataJpaTest` tests mirroring `OrderRepositoryTest`'s exact shape — each derived query gets a genuine true/false pair against the real embedded H2 database, not a tautological single assertion. Full repo suite green, 116/116.

---

### [`OutboxPublisherService.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/service/OutboxPublisherService.java)

**low · Reliability** — Fixed via [PR #105](https://github.com/Terrence721/saga-full/pull/105) ([issue #104](https://github.com/Terrence721/saga-full/issues/104))

**Known finding, fixed**: pre-flagged before this file's review even started — the same unbounded `kafkaTemplate.send(message).get()` timeout gap already found and fixed in order-service (#80). This method is `@Transactional`, holding the outbox row's `PESSIMISTIC_WRITE` lock open for as long as the send blocks — an unbounded wait relies solely on Kafka's own undocumented 120s `delivery.timeout.ms` default, meaning a degraded broker could hold a DB connection and lock open per record for up to 120s, times up to `batchSize` records in the worst case. Fixed identically to order-service's #80: a configurable `app.outbox.send-timeout-ms` (default 10000ms), bounding the `.get()` call and catching `TimeoutException` alongside the existing `InterruptedException`/`ExecutionException` handling. Verified via deliberate revert: reverting to the unbounded `.get()` makes the new timeout test's `TimeoutException` catch block unreachable — a compile-time proof, not just a runtime one, that the fix is load-bearing. Full repo suite green, 117/117.

---

### [`PaymentConsumerConfig.java`](https://github.com/Terrence721/saga-full/blob/main/payment-service/src/main/java/io/github/terrence721/saga/payment/service/PaymentConsumerConfig.java) — last file in `payment-service`

**low · Reliability** — Fixed via [PR #107](https://github.com/Terrence721/saga-full/pull/107) ([issue #106](https://github.com/Terrence721/saga-full/issues/106))

**Known finding, fixed**: pre-flagged during `PaymentNotFoundException.java`'s review (#98), mirroring order-service's identical `OrderConsumerConfig.java` fix (#76) — no `DefaultErrorHandler`/`CommonErrorHandler` bean existed anywhere in this module, so an uncaught throw from either `@KafkaListener` method fell through to Spring Kafka's default (10 rapid retries, zero backoff, then silently dropped). Reachable given independent producers and at-least-once Kafka delivery — a missing payment (`PaymentNotFoundException`) or a `customerId` mismatch (`IllegalArgumentException`, added under #96) would previously retry 10 times for nothing before being silently dropped. Fixed identically to order-service's #76: an explicit `DefaultErrorHandler` `@Bean`, bounded `FixedBackOff(1000L, 2L)`, `addNotRetryableExceptions(PaymentNotFoundException.class, IllegalArgumentException.class)`, and an explicit `ERROR`-level recoverer naming topic/partition/offset. Grepped for a log-injection surface too — both listeners log only `UUID`-typed `event.orderId()`, no risk. Verified via deliberate revert: removing `addNotRetryableExceptions` makes both new non-retryable tests genuinely fail; a third new test proves generic exceptions still retry (not accidentally swallowed). Full repo suite green, 120/120.

**Module review complete — `payment-service`, 14/14 files reviewed, 4 real findings fixed** (a payment-decline path that never existed in `PaymentStatus.java`/#88, a refund compensation that never checked whose order it was refunding in `RestaurantRejectedEvent.java`/#96, a test-coverage gap on two idempotency-guard queries in `PaymentRepository.java`/#102, and an unbounded Kafka-send timeout in `OutboxPublisherService.java`/#104 — plus this file's known-finding fix), 0 findings left open. See [todo.md](../todo.md) for the full per-file table and [#21](https://github.com/Terrence721/saga-full/issues/21) for the closed tracking issue.

---

### [`RestaurantServiceApplication.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/RestaurantServiceApplication.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #108](https://github.com/Terrence721/saga-full/issues/108))

Byte-for-byte identical to the already-reviewed `order-service`/`payment-service` entry points (#48, #82). Grepping for `@Scheduled` confirms `@EnableScheduling` is genuinely needed — it turned up `OutboxPublisherService`'s real polling method in this same module. A real `RestaurantServiceApplicationTests.contextLoads()` exists and boots the module.

---

### [`InventoryItem.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/domain/InventoryItem.java)

**medium · Reliability** — Fixed via [PR #111](https://github.com/Terrence721/saga-full/pull/111) ([issue #110](https://github.com/Terrence721/saga-full/issues/110))

**Real finding, fixed**: this entity (`itemCode`/`stockCount`) had no path anywhere in the repo to ever get a row created in a real deployment. `InventoryItem.builder()` was used only in tests — the sole production `save()` call is inside `RestaurantInventoryService.verifyAndDeductStock`, which only updates a row that already exists (found via `findById`). No `data.sql`, no seed script, no admin/replenishment endpoint anywhere. In any real run (`bootRun`, docker-compose, a manual end-to-end test), `findById(itemCode)` would always return empty — every order would hit `ITEM_NOT_FOUND` and get rejected, unconditionally. This is the module's core function, unreachable in practice.

Fixed with a real `data.sql` seed (10 items, `PIZZA_01`–`PIZZA_10`, matching the item code already used throughout this module's own test fixtures), covering all three `InventoryStatus` outcomes for real manual testing: most well-stocked (`ALLOCATED`), two deliberately low-stock (`INSUFFICIENT_STOCK` on demand), any unlisted code still hits `ITEM_NOT_FOUND`. `application.yaml` gained `spring.jpa.defer-datasource-initialization: true` and `spring.sql.init.mode: always` so the seed actually runs, after table creation, on both profiles.

**Real portability bug found and fixed while verifying**: the first attempt used `ON CONFLICT (item_code) DO NOTHING` for idempotent re-runs against a persistent Postgres database. A real `./gradlew :restaurant-service:test` run failed with a genuine H2 syntax error — `@DataJpaTest`'s auto-configured embedded H2 swaps out the explicitly-configured `MODE=PostgreSQL` datasource for its own plain H2, which doesn't recognize `ON CONFLICT`. Rewrote using `INSERT ... SELECT ... WHERE NOT EXISTS`, portable across both H2 configurations and real Postgres.

Added a new real-boot test, `RestaurantServiceApplicationTests.dataSqlSeedsInventory_onARealBoot`, asserting all 10 seeded rows exist with correct stock counts against the actual `MODE=PostgreSQL` H2 context — proof this works end-to-end, not just that the YAML is plausible. Verified via deliberate revert: emptied `data.sql`, confirmed the new test genuinely fails, restored it. Full repo suite green, 121/121.

---

### [`InventoryStatus.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/domain/InventoryStatus.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #112](https://github.com/Terrence721/saga-full/issues/112))

3-value enum (`ALLOCATED`, `INSUFFICIENT_STOCK`, `ITEM_NOT_FOUND`). Grepped every reference: all 3 values are genuinely produced by `RestaurantInventoryService.verifyAndDeductStock` and exhaustively consumed by `RestaurantService.processRestaurantStep`'s switch, no dead values. Purely a transient service-return type — not a JPA-persisted field on any entity (`RestaurantTicket.status` uses the separate `RestaurantTicketStatus`), so there's no `@Enumerated` storage-compatibility concern like `order-service`'s/`payment-service`'s `OrderStatus.java` copies.

---

### [`OutboxRecord.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/domain/OutboxRecord.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #114](https://github.com/Terrence721/saga-full/issues/114))

Byte-for-byte identical (package declaration aside) to the already-reviewed `order-service`/`payment-service` copies (#74, #84) — same `UUID` id, `aggregateId`/`eventType`/`payload`/`createdTime` columns, same `@Lob` payload. Consumers (`RestaurantService.saveRestaurantTicketOutbox`, `OutboxPublisherService`, `OutboxRepository`) match the identical shape already reviewed in the sibling modules.

---

### [`RestaurantTicket.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/domain/RestaurantTicket.java)

**medium · Reliability** — Fixed via [PR #117](https://github.com/Terrence721/saga-full/pull/117) ([issue #116](https://github.com/Terrence721/saga-full/issues/116))

**Real finding, fixed**: the `order_id` column had no unique constraint, but `RestaurantService.processRestaurantStep` uses `ticketRepository.existsByOrderId(event.orderId())` as a check-then-act idempotency guard against duplicate `PaymentProcessedEvent` delivery — the same shape as `Payment.java`'s already-fixed `unique = true` constraint on its own `order_id` column (#86), added specifically to backstop that exact idempotency-guard pattern. `RestaurantConsumerConfig` is a plain `@KafkaListener` (default at-least-once semantics), so a redelivered/duplicate event is a real, expected occurrence, not a theoretical one. Without the constraint, two near-simultaneous deliveries could both pass the `existsByOrderId` check before either commits, producing two `RestaurantTicket` rows for one order.

Fixed by adding `unique = true` to the `order_id` `@Column`, matching `Payment.java`'s precedent. Added a new test, `RestaurantTicketRepositoryTest.save_rejectsDuplicateOrderId`, asserting a `DataIntegrityViolationException` on a second `saveAndFlush` with the same `orderId` — more rigorous than `Payment.java`'s own constraint, which had no dedicated test anywhere in the repo. Verified via deliberate revert: removed `unique = true`, confirmed the new test genuinely fails, restored it.

**Second, unrelated real bug found and fixed while verifying**: running the full suite as `./gradlew test aggregateTestReport` in one invocation failed intermittently with Gradle's "implicit dependency" validation error on every module — `aggregateTestReport` reads each module's `test-results/test` directory without any declared ordering relationship to the `test` task that writes it, so when Gradle happened to schedule `aggregateTestReport` before a module's `test` task had finished writing its output in the same build, the read was flagged as unsafe. Reproduced reliably: ran `:restaurant-service:test` alone first (so its results were freshly written but other modules' were untouched this Gradle daemon session), then `./gradlew test aggregateTestReport` — failed every time with that exact input/output ordering violation. Fixed in `build.gradle.kts` by adding `mustRunAfter(sub.tasks.named("test"))` per subproject — ordering only, not a real `dependsOn`, so it doesn't force `test` to run and doesn't fail if a module's tests fail, preserving the task's deliberate "merge whatever results exist, pass or fail" design. Verified by reproducing the exact failing sequence again after the fix — now succeeds. Full repo suite green, 122/122 (up from 121/121 — the one new duplicate-`orderId` test).

---

### [`RestaurantTicketStatus.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/domain/RestaurantTicketStatus.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #123](https://github.com/Terrence721/saga-full/issues/123))

2-value enum (`PREPARING`, `REJECTED`). Grepped every reference: both values are genuinely produced by `RestaurantService.processRestaurantStep`'s branches (`PREPARING` on `ALLOCATED`, `REJECTED` on every rejection path — payment failure, `INSUFFICIENT_STOCK`, `ITEM_NOT_FOUND`), no dead values. Persisted via `@Enumerated(EnumType.STRING)` on `RestaurantTicket.status` (reviewed in #116), so storage is name-based — reordering is safe.

---

### [`PaymentProcessedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/dto/PaymentProcessedEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #125](https://github.com/Terrence721/saga-full/issues/125))

Field-for-field identical (package/import aside) to `payment-service`'s already-reviewed copy (#94). All 6 fields are genuinely used: `RestaurantService.validate` checks every one (defense-in-depth against a corrupted or malformed message on shared Kafka infrastructure, the same pattern already established for `itemCode`/`customerId` elsewhere in this repo), and `orderId`/`status`/`itemCode`/`quantity`/`customerId` are each consumed further in `processRestaurantStep` — `status` specifically gates the payment-approval branch added under #88. `amount` is validated but not otherwise read, matching the same not-dead-just-defensive shape already accepted for other cross-service DTO fields in this audit.

---

### [`PaymentStatus.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/dto/PaymentStatus.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #127](https://github.com/Terrence721/saga-full/issues/127))

3-value enum (`APPROVED`, `REFUNDED`, `FAILED`), matching `payment-service`'s own domain enum shape. Traced `PaymentProcessedEvent`'s sole construction site in `payment-service` (`PaymentService.buildOutboxRecord`, called only from `processPaymentSaga`): `payment.getStatus()` there is only ever `APPROVED` or `FAILED` (set at the top of `processPaymentSaga`, #88) — `REFUNDED` is set exclusively inside the separate `handleOrderCompensation` method, which never constructs or publishes a new `PaymentProcessedEvent`. So `REFUNDED` is structurally unreachable through this specific DTO, even though it's a real, actively-used value in `payment-service`'s own `Payment.status` column. Same shape as `OrderStatus.java`'s already-accepted precedent (#56/#92) — a shared-shape DTO enum that needs to match the broader domain type even though only a subset of values is reachable via this particular event. `RestaurantService.processRestaurantStep`'s `!= APPROVED` check already treats `REFUNDED` the same as `FAILED` (reject, fail-closed) if it ever somehow arrived, so there's no mishandling risk either way — kept as-is, not a finding.

---

### [`RestaurantApprovedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/dto/RestaurantApprovedEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #129](https://github.com/Terrence721/saga-full/issues/129))

3-field record (`orderId`, `customerId`, `ticketId`), field-for-field identical to `order-service`'s already-reviewed consuming copy (#66) except implementing this module's own `RestaurantEvent` sealed interface for `saveRestaurantTicketOutbox`'s polymorphism. All 3 fields are genuinely populated at the sole construction site (`RestaurantService.processRestaurantStep`'s `ALLOCATED` branch: `event.orderId()`, `event.customerId()`, `ticket.getId()` — a real, just-generated `RestaurantTicket` id, not a placeholder) and all 3 are consumed on the receiving end in `OrderService.confirmOrder` (`orderId` for the idempotency guard and lookup, `ticketId` null-checked as required, `customerId` cross-checked against the loaded order).

---

### [`RestaurantEvent.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/dto/RestaurantEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #131](https://github.com/Terrence721/saga-full/issues/131))

Sealed interface, `permits RestaurantApprovedEvent, RestaurantRejectedEvent` — checked both implement it for real (grepped both files), so the seal is accurate and exhaustive, not stale. Its sole use is `RestaurantService.saveRestaurantTicketOutbox(RestaurantEvent event, ...)`, which only calls `event.orderId()` polymorphically (three times — the exception message, `aggregateId`, and the log line); `event.customerId()`, also declared on the interface, is never invoked through a `RestaurantEvent`-typed reference anywhere in the codebase. Not a finding — `customerId` is still a genuinely used field on both concrete records (constructed with real values, serialized into the outbox payload via Jackson's record reflection, and read by real consumers in `order-service`/`payment-service`), it's just accessed through the concrete types rather than the interface. Removing it from the interface wouldn't fix anything, only shrink the documented shared shape.

---

### [`RestaurantRejectedEvent.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/dto/RestaurantRejectedEvent.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #133](https://github.com/Terrence721/saga-full/issues/133))

3-field record (`orderId`, `customerId`, `reason`), field-for-field identical to both `payment-service`'s (#96) and `order-service`'s (#68) already-reviewed copies. All 3 fields are genuinely populated at all 3 real construction sites in `RestaurantService.processRestaurantStep` (payment-failure rejection, `INSUFFICIENT_STOCK`, `ITEM_NOT_FOUND`, each with a distinct, real descriptive `reason` string) and consumed on both receiving ends — `payment-service`'s `handleOrderCompensation` (`orderId`/`customerId` cross-check, `reason` logged) and `order-service`'s `cancelOrder` (same shape, plus the CWE-117 sanitization already fixed under #78).

---

### [`InventoryItemRepository.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/repository/InventoryItemRepository.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #135](https://github.com/Terrence721/saga-full/issues/135))

Pure `JpaRepository<InventoryItem, String>` passthrough, zero custom query methods declared. Both methods actually used in production (`RestaurantInventoryService.verifyAndDeductStock`'s `findById`/`save`) have real `@DataJpaTest` coverage against a genuine embedded H2, including a true/false pair for `findById` (found vs. unknown item code).

---

### [`OutboxRepository.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/repository/OutboxRepository.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #137](https://github.com/Terrence721/saga-full/issues/137))

Byte-for-byte identical (module naming in comments/package aside) to both `order-service`'s (#74) and `payment-service`'s (#100) already-reviewed copies — same `SKIP LOCKED` pessimistic-lock hint on `findByOrderByCreatedTimeAsc`. Real test coverage: an empty-case test plus a genuine ordering+page-limit test (12 records inserted, verifies exactly the oldest 10 come back in chronological order).

---

### [`RestaurantTicketRepository.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/repository/RestaurantTicketRepository.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #139](https://github.com/Terrence721/saga-full/issues/139))

`existsByOrderId` is the fast-path idempotency guard used in `RestaurantService.processRestaurantStep`, correctly paired with `RestaurantTicket.order_id`'s unique constraint (#116) as a defense-in-depth DB-level backstop — the same shape already established for `Payment`/`PaymentRepository`. Both the `existsByOrderId` true/false behavior and the constraint-rejection behavior already have real test coverage, verified via deliberate revert as part of #116/PR #117's own review — not re-verified here since nothing about this interface changed since then.

---

### [`OutboxPublisherService.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/service/OutboxPublisherService.java)

**medium · Reliability** — Fixed via [PR #142](https://github.com/Terrence721/saga-full/pull/142) ([issue #141](https://github.com/Terrence721/saga-full/issues/141))

**Real finding, fixed — the third occurrence of the same gap**: `kafkaTemplate.send(message).get()` blocked unbounded, implicitly relying on Kafka's own undocumented 120s `delivery.timeout.ms` default, while `@Transactional` held the outbox row's `PESSIMISTIC_WRITE` lock open the entire time — a degraded broker could hold that lock for up to 20 minutes across a full 10-record batch. Identical structural gap already fixed in `order-service` (#80) and `payment-service` (#104/PR #105); pre-flagged before this file's own review even started (see `todo.md`'s Phase 31 log and the payment-service audit notes).

Fixed with the identical pattern: a new `app.outbox.send-timeout-ms` config property (default 10000ms, `application.yaml`), `.get(sendTimeoutMs, TimeUnit.MILLISECONDS)` replacing the unbounded call, and a `TimeoutException` catch that logs and leaves the record for the next poll. New test `publishPendingOutboxRecords_leavesRecordForRetry_onTimeout` (a service instance with a 50ms timeout against a `CompletableFuture` that never completes). Verified via deliberate revert — reverting to the unbounded `.get()` makes the new test's `TimeoutException` catch block unreachable, so the module fails to *compile*, not just fails a test, a genuine compile-time proof. Full repo suite green, 123/123 (up from 122/122).

---

### [`RestaurantConsumerConfig.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/service/RestaurantConsumerConfig.java)

**medium · Reliability** — Fixed via [PR #144](https://github.com/Terrence721/saga-full/pull/144) ([issue #143](https://github.com/Terrence721/saga-full/issues/143))

**Real finding, fixed**: no `DefaultErrorHandler` bean anywhere in this module — an uncaught exception from `onPaymentProcessed` (a malformed payload, or `RestaurantService.validate`'s `IllegalArgumentException`) fell through to Spring Boot's autoconfigured default (10 retries at 0ms backoff, then silent log-and-skip). Identical gap already fixed in `order-service`'s (#76) and `payment-service`'s (#106) own `*ConsumerConfig` files, expected here per the audit's own precedent.

Fixed with the identical pattern: a `kafkaErrorHandler()` `@Bean` with a bounded `FixedBackOff(1000L, 2L)` (3 attempts total, 1s apart), `IllegalArgumentException` marked non-retryable (retrying can't fix a permanently malformed message — restaurant-service has no custom "not found"-style exception the way `order-service`/`payment-service` do, since `processRestaurantStep` never loads an existing entity by id), and an explicit `ERROR`-level recoverer naming the topic/partition/offset. Two new tests mirroring the sibling modules' exact shape. Verified via deliberate revert: removed `addNotRetryableExceptions(...)`, confirmed the new test genuinely fails, restored it. Full repo suite green, 125/125 (up from 123/123).

---

### [`RestaurantInventoryService.java`](https://github.com/Terrence721/saga-full/blob/main/restaurant-service/src/main/java/io/github/terrence721/saga/restaurant/service/RestaurantInventoryService.java) — last file in `restaurant-service`

**medium · Reliability** — Fixed via [PR #146](https://github.com/Terrence721/saga-full/pull/146) ([issue #145](https://github.com/Terrence721/saga-full/issues/145))

**Real finding, fixed**: `verifyAndDeductStock`'s read-modify-write on `InventoryItem.stockCount` (`findById` → check → `setStockCount` → `save`) issued no lock. Two concurrent deductions against the same `itemCode` — a real occurrence once `restaurant-service` scales past one instance, since the Kafka message key on `payment-processed-topic` is `orderId`, not `itemCode`, so two different orders for the same item can land on different partitions/instances — could both read the same pre-deduction `stockCount` before either wrote back, both return `ALLOCATED`, and both save a decrement: a classic lost update that oversells stock below zero.

Fixed by adding `InventoryItemRepository.findByItemCodeForUpdate` (`@Lock(PESSIMISTIC_WRITE)` + a JPQL `@Query`, the same locking primitive this repo already uses on `OutboxRepository`) and switching `verifyAndDeductStock` to call it instead of plain `findById`. This blocks a second transaction reading the same row until the first commits, so it always sees the real post-deduction count rather than a stale one.

Verified with a new `RestaurantInventoryServiceConcurrencyTest` (`@SpringBootTest`, not `@DataJpaTest` — needs the real transaction manager and connection pool so two threads can each hold a genuine, independent transaction against the same row): seeds one item with exactly enough stock for one request, fires two real concurrent calls via an `ExecutorService` released together off a `CountDownLatch`, and asserts exactly one comes back `ALLOCATED` and the other `INSUFFICIENT_STOCK`, with the row landing on exactly 0, never negative. Verified via deliberate revert, run 3 times to rule out flakiness in both directions: with the fix, 4/4 real runs green; reverted to plain `findById`, 3/3 real runs failed with the exact predicted bug (`[ALLOCATED, ALLOCATED]` — both threads wrongly allocated against stock for one), restored. Full repo suite green, 126/126 (up from 125/125).

**Module review complete — `restaurant-service`, 18/18 files reviewed, 5 real findings fixed, 0 findings left open** (an unreachable inventory-seeding gap in `InventoryItem.java`/#110, a missing `order_id` unique constraint in `RestaurantTicket.java`/#116, an unbounded Kafka-send timeout in `OutboxPublisherService.java`/#141 — the third occurrence of a gap already fixed in `order-service`/`payment-service` — a missing `DefaultErrorHandler` in `RestaurantConsumerConfig.java`/#143 — likewise the third occurrence — and an unlocked stock-deduction race in this file). `RestaurantService.java` was covered early under `payment-service`'s `PaymentStatus.java` review (#88), ahead of this module's own audit turn. Full multi-module suite: 126/126 tests passing. See [todo.md](../todo.md) for the full per-file table and [#22](https://github.com/Terrence721/saga-full/issues/22) for the closed tracking issue.

---

### [`ApiGatewayServiceApplication.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/ApiGatewayServiceApplication.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #147](https://github.com/Terrence721/saga-full/issues/147))

13 lines: package declaration, two imports, a single `@SpringBootApplication`-annotated class with a `main()` calling `SpringApplication.run()`. Identical in shape to `OrderServiceApplication.java`/`PaymentServiceApplication.java`/`UserServiceApplication.java`, all already reviewed with no findings. A real `ApiGatewayServiceApplicationTests.contextLoads` boots the actual `ApplicationContext` (not a sliced/mocked test), so this class's only real responsibility — wiring the app up at all — has genuine coverage.

---

### [`CommonAppConfig.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/config/CommonAppConfig.java)

**n/a · Maintainability** — Reviewed, no findings in this file's own code ([issue #149](https://github.com/Terrence721/saga-full/issues/149))

This file's only job — wiring a `UserIdentityServiceBlockingStub` bean off the named `userService` gRPC channel — is correct and matches its own `application.yaml` config. Choosing a *blocking* stub here isn't itself wrong; it's a legitimate pattern as long as its caller isolates the blocking call off Reactor Netty's event-loop threads.

**Real finding discovered while reviewing this file's only consumer, deferred to `AuthenticationController.java`'s own turn (the very next file)**: `AuthenticationController.login` wraps the blocking `userGrpcClient.login(...)` call in `Mono.fromCallable(...)` with a comment claiming that, since `spring.threads.virtual.enabled=true` is set repo-wide, the call runs on a virtual thread and "WebFlux's Netty event loop is never occupied by it." Verified this claim empirically with a real `@SpringBootTest(webEnvironment = RANDOM_PORT)` hitting `/auth/login` through an actual running Netty server and capturing `Thread.currentThread().getName()` inside the mocked gRPC call: it ran on `webflux-http-nio-2`, a genuine Reactor Netty event-loop thread, not a virtual thread. `spring.threads.virtual.enabled` retargets Spring's own servlet/Tomcat-style executors and `@Async`/scheduling — it has no effect on Reactor Netty's independently-managed event-loop group. Every `/auth/login` call currently blocks one of a small, fixed pool of event-loop threads shared by the entire gateway (including the reactive `order-service` proxy route) for the duration of the gRPC call — not visible under today's light, single-request testing, but a real concurrency bug at production traffic levels, the same class of "real but not-yet-triggered" finding as `restaurant-service`'s stock-deduction race. Pre-flagged here so it isn't lost or re-discovered as fresh; fix lands on `AuthenticationController.java`'s own review, immediately next.

---

### [`AuthenticationController.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/controller/AuthenticationController.java)

**medium · Reliability** — Fixed via [PR #152](https://github.com/Terrence721/saga-full/pull/152) ([issue #151](https://github.com/Terrence721/saga-full/issues/151))

**Real finding, fixed — carried over, already verified, from #149's review of `CommonAppConfig.java`**: `login`'s blocking gRPC call was wrapped in `Mono.fromCallable(...)` with no scheduler override, on the strength of a comment claiming `spring.threads.virtual.enabled=true` kept it off Reactor Netty's event loop. That claim was already disproved during #149's review — a real running server showed the call executing on `webflux-http-nio-2`.

Fixed by adding `.subscribeOn(Schedulers.boundedElastic())` to the `Mono.fromCallable(...)` chain, the standard interop pattern for isolating a genuinely blocking call from a reactive pipeline, and correcting the comment to state the real mechanism rather than the disproved one.

Verified with a new `AuthenticationControllerThreadingTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, not `@WebFluxTest` — the event-loop group only exists once Netty is actually running): captures `Thread.currentThread().getName()` inside the mocked gRPC call through a real HTTP round-trip and asserts it's not a `webflux-http-nio-*`/`reactor-http-nio-*` thread. Verified via deliberate revert: with `subscribeOn` removed, the test genuinely failed (the captured thread name matched the event-loop pattern); restored, and it passes. Full repo suite green, 127/127 (up from 126/126).

---

### [`GatewayFallbackController.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/controller/GatewayFallbackController.java)

**medium · Test coverage** — Fixed via [PR #154](https://github.com/Terrence721/saga-full/pull/154) ([issue #153](https://github.com/Terrence721/saga-full/issues/153))

**Real finding, fixed (test-coverage gap)**: this is the actual fallback target Resilience4j routes to when `orderServiceCircuitBreaker` trips (`application.yaml`'s `order-service-route`, `fallbackUri: forward:/fallback/orders`) — the concrete implementation behind the README's own "circuit breaker on the downstream hop" claim — yet had zero test coverage anywhere in the repo. No test verified the endpoint actually returns `503 SERVICE_UNAVAILABLE` with a well-formed `ErrorResponse` body, the exact behavior the whole circuit-breaker story depends on when the real `order-service` is unreachable. Same class of gap as `PaymentRepository.java`'s missing `existsByOrderId` coverage (#102) earlier in this audit — a real production code path with no test proving it does what it claims.

The controller's own logic checked out otherwise: `@RequestMapping("/orders")` with no method restriction means it also answers non-`POST` requests, but since it's a static, side-effect-free informational response with no auth bypass or data exposure, that's a legitimate, intentional choice for a fallback endpoint, not a finding.

Added `GatewayFallbackControllerTest` (`@WebFluxTest`, matching `AuthenticationControllerTest`'s existing pattern) asserting the real response shape: status `503`, `error` = `"SERVICE_UNAVAILABLE"`, non-empty `message`, numeric `timestamp` — three genuine field checks, not a tautological single assertion. No deliberate-revert step needed (test-only addition, no production code changed), matching the same precedent set at #102. Full repo suite green, 128/128 (up from 127/127).

---

### [`AuthRequest.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/dto/AuthRequest.java)

**n/a · Maintainability** — Reviewed, no findings in this file's own code ([issue #155](https://github.com/Terrence721/saga-full/issues/155))

`@NotBlank`-only validation on both `email` and `password` is the right call for a login DTO — this is a credential *check*, not an account-creation flow, so format/complexity constraints don't belong here; malformed input just fails authentication normally further downstream, not a real bug.

**Real finding discovered while checking this record's real consumers, deferred to `UserGrpcClient.java`'s own turn (later in this module's file order)**: `email` is unrestricted free text (no `@Email` format check) and `UserGrpcClient.login` logs it raw, twice, at `log.info` level (`"Initiating gRPC login call for email: {}"` / `"Authenticated user successfully for email: {}"`) — on `/auth/login`, an unauthenticated endpoint, so any caller controls this value entirely. A classic CWE-117 log-injection vector: embedded CR/LF can forge fake log lines. Same class of gap already fixed in this repo's `order-service` (`OrderService.java`'s `sanitizedReason`, stripped via `.replaceAll("[\r\n]", "_")` for the same reason — "content-unrestricted" free text reaching a log call), and exactly the class of finding CodeQL's own precision gap won't catch here (see [`project-codeql-log-injection-header-source-gap`](../docs/code-review.md) — record accessors sourced from `@RequestBody` don't clear CodeQL's `java/log-injection` taint tracking the way `@RequestHeader` strings do, but this repo's own audit process still needs to catch them). Pre-flagged here so it isn't lost; fix (the same `.replaceAll("[\r\n]", "_")` pattern) lands on `UserGrpcClient.java`'s own review.

---

### [`WebTokenResponse.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/dto/WebTokenResponse.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #157](https://github.com/Terrence721/saga-full/issues/157))

Plain output record, no validation needed since it's a response, not a request. Sole construction site is the already-reviewed `AuthenticationController.login` (#151/#152); all 3 fields (`token`, `type`, `expiresIn`) are genuinely populated from the real gRPC response and already have real coverage via `AuthenticationControllerTest`'s existing JSON-path assertions.

---

### [`DependencyUnavailableException.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/exception/DependencyUnavailableException.java)

**n/a · Maintainability** — Reviewed, no findings in this file's own shape ([issue #159](https://github.com/Terrence721/saga-full/issues/159))

Plain `RuntimeException` subclass, matching every other custom exception in this repo (`PaymentNotFoundException.java`, `OrderNotFoundException.java`, etc.). Genuinely thrown from `UserGrpcExceptionTranslator.translate` (both `UNAVAILABLE`/`DEADLINE_EXCEEDED`/`INTERNAL` and the `default` gRPC status branches, always via the `(message, cause)` constructor) and caught by `GlobalExceptionHandler.handleDependencyFailure`, which returns `503 SERVICE_UNAVAILABLE`.

**Real finding discovered while checking real usages, deferred to `GlobalExceptionHandler.java`'s own turn**: no test anywhere exercises `handleDependencyFailure`, or this exception's handling at all — same class of gap as `GatewayFallbackController.java` (#153/#154), a real production error-handling path with zero coverage. Since the untested behavior actually lives in `GlobalExceptionHandler.java` (an `@ExceptionHandler` method, not anything defined in this file), the fix belongs there — coming up in 2 files, where the whole exception-handling matrix can be assessed at once rather than piecemeal per exception class.

---

### [`ErrorResponse.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/exception/ErrorResponse.java)

**n/a · Maintainability** — Reviewed, no findings ([issue #161](https://github.com/Terrence721/saga-full/issues/161))

Plain output record, no validation needed. The shared error body across the whole module's failure surface — every `GlobalExceptionHandler` handler builds one via `buildResponse`, and `GatewayFallbackController.orderFallback` constructs one directly. Already has real coverage via `GatewayFallbackControllerTest` and `AuthenticationControllerTest`'s UNAUTHORIZED-path assertions.

---

### [`GlobalExceptionHandler.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/exception/GlobalExceptionHandler.java)

**medium · Test coverage** — Fixed via [PR #164](https://github.com/Terrence721/saga-full/pull/164) ([issue #163](https://github.com/Terrence721/saga-full/issues/163))

**Real finding, fixed — carried over, already flagged, from #159's review of `DependencyUnavailableException.java`**: of this class's 9 `@ExceptionHandler` methods, only `handleInvalidCredentials` and `handleReactiveValidationExceptions` had any real coverage (indirectly, via `AuthenticationControllerTest`). The other 7 — `handleNotFound`, `handleForbidden`, `handleDependencyFailure`, `handleTokenExpired`, `handleJwtFailure`, `handleBadRequest`, `handleGenericException` — had none.

Two of those (`handleTokenExpired`/`handleJwtFailure`) carried a real, unverified architectural assumption, stated as fact in `JwtPerimeterGuardGatewayFilterFactoryTest`'s own comment: that `@RestControllerAdvice` catches exceptions thrown inside a Spring Cloud Gateway filter's reactive chain at all. `@RestControllerAdvice` is fundamentally an MVC/WebFlux *controller*-dispatch mechanism, and Spring Cloud Gateway's filter chain is a separate reactive pipeline — the assumption was reasonable but never actually proven. Verified empirically with a new `JwtPerimeterGuardIntegrationTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, hitting the real `/orders` route through a real running server, not a filter-only unit test): it does apply — a missing token produces a real `401`/`UNAUTHORIZED` body, a genuinely expired one a real `403`/`FORBIDDEN` body, both matching `GlobalExceptionHandler`'s documented contract exactly.

The remaining 5 handlers each have a real, syntactically-present translation branch in `UserGrpcExceptionTranslator`. Added 5 new tests to `AuthenticationControllerTest`, mirroring its existing `InvalidCredentialsException` test's exact pattern (mock `userGrpcClient.login` to throw, assert the real HTTP status and body): `UserNotFoundException` → 404, `UserInactiveException` → 403, `DependencyUnavailableException` → 503, `IllegalArgumentException` → 400, and a generic `RuntimeException` → 500 (the catch-all). These verify the gateway's own translation-to-HTTP-status mapping is internally correct; whether `user-service`'s real `Login` RPC can actually produce the gRPC status each one is translated from is a separate question, resolved during `InvalidCredentialsException.java`'s own review (#165/#166) — see that entry below.

No production code changed — test-only addition, matching the same precedent set at #102/#153. Full repo suite green, 135/135 (up from 128/128).

---

### [`InvalidCredentialsException.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/exception/InvalidCredentialsException.java)

**Reviewed via [PR #166](https://github.com/Terrence721/saga-full/pull/166) ([issue #165](https://github.com/Terrence721/saga-full/issues/165))**

No findings in its own code — a plain `RuntimeException` subclass, matching every other custom exception in this repo.

**Structural nuance found while checking real usages, corrects a slightly overstated claim in `GlobalExceptionHandler.java`'s own write-up above**: `UserGrpcExceptionTranslator`'s `NOT_FOUND → UserNotFoundException` and `PERMISSION_DENIED → UserInactiveException` branches cannot actually fire from any real call today. `user.proto` exposes only `Login` and `ValidateToken`; `ValidateToken` never throws; and `GrpcExecutor.java` (fixed for CWE-203 login-enumeration during `user-service`'s own audit) deliberately collapses `UserNotFoundException`/`InvalidCredentialsException`/`UserInactiveException` into a single `Status.UNAUTHENTICATED` — by design, so a caller can't distinguish "unknown email" from "wrong password" from "inactive account" over the wire. `INVALID_ARGUMENT → IllegalArgumentException` is similarly unreachable through `login()`'s real flow today (it throws no such exception; DTO-level validation is handled separately, upstream, by `AuthenticationController`'s own `@Valid` binding). Only `UNAUTHENTICATED → InvalidCredentialsException` and the `UNAVAILABLE`/`DEADLINE_EXCEEDED`/`INTERNAL`/`default → DependencyUnavailableException` branches are genuinely reachable via a real `Login` call right now.

This is **not a bug** — the CWE-203 collapse is a correct, deliberate security decision, and reopening those branches to make them "reachable" would mean re-exposing exactly the enumeration signal that fix closed. The translator being more granular than `user-service` currently exercises is defensive/forward-compatible, not wrong; `GlobalExceptionHandler`'s tests for those 3 handlers remain valid as unit-level proof the translation-to-HTTP-status mapping is internally correct, independent of whether today's `Login` RPC happens to reach them. No code change made. Full repo suite unchanged, 135/135.

---

### [`UserInactiveException.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/exception/UserInactiveException.java)

**Reviewed via [PR #168](https://github.com/Terrence721/saga-full/pull/168) ([issue #167](https://github.com/Terrence721/saga-full/issues/167))**

No findings — same shape as `InvalidCredentialsException.java` (a plain `RuntimeException` subclass), correctly mapped by `GlobalExceptionHandler.handleForbidden` to `403 FORBIDDEN`, with real coverage already added in #163/#164's `AuthenticationControllerTest`. Same `PERMISSION_DENIED`-branch reachability nuance already documented on `InvalidCredentialsException.java`'s entry above applies here too — not re-litigated per file, this is its one canonical writeup. No code change made.

---

### [`UserNotFoundException.java`](https://github.com/Terrence721/saga-full/blob/main/api-gateway-service/src/main/java/io/github/terrence721/saga/gateway/exception/UserNotFoundException.java)

**Reviewed via [PR #170](https://github.com/Terrence721/saga-full/pull/170) ([issue #169](https://github.com/Terrence721/saga-full/issues/169))**

No findings — same shape as `InvalidCredentialsException.java`/`UserInactiveException.java`, correctly mapped by `GlobalExceptionHandler.handleNotFound` to `404 NOT_FOUND`, with real coverage already added in #163/#164's `AuthenticationControllerTest`. Same `NOT_FOUND`-branch reachability nuance already documented on `InvalidCredentialsException.java`'s entry applies here too, its canonical writeup. No code change made.

---

*More findings are appended here as each file's PR merges. See [todo.md](../todo.md) for the per-file tracking table of whichever module is currently in progress.*
