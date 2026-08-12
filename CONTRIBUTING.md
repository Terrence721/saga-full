# Developing

## Setup

```shell
./gradlew build
```

## Testing

```shell
./gradlew test
```

### Testing a specific service module

```shell
./gradlew :order-service:test
./gradlew :<module-name>:test
```

### Consolidated test report (all modules, one file)

Each module's own HTML report lives at `<module>/build/reports/tests/test/index.html`. To see every module's results merged into a single file instead:

```shell
./gradlew test --continue
./gradlew aggregateTestReport
```

Output: `build/reports/tests/aggregate/index.html`. Run as two separate commands, not one — `--continue` lets every module's tests run independently of one another, but if `aggregateTestReport` depended on the `test` tasks directly, a single failing module would block it from running at all (a failed dependency always prevents a dependent task from executing). Running it standalone means it always reflects whatever's currently on disk, pass or fail.

## Running against real Postgres + Kafka (Docker)

Tests use H2 (in-memory) and don't need this. To run a module against real infrastructure instead (e.g. `./gradlew :order-service:bootRun`), start the local stack:

```shell
docker compose up -d
```

This brings up `postgres-db` and `kafka-broker` only — no per-service containers yet (see [todo.md](todo.md)). A schema-init script (`docker/init-schemas.sql`) creates each service's Postgres schema automatically on first startup.

`postgres-db` maps to host port **5433**, not the standard 5432 — some dev machines already have another project's Postgres container bound to 5432. Activate the `postgres` profile and point at that port:

```shell
SPRING_PROFILES_ACTIVE=postgres DATABASE_PORT=5433 ./gradlew :order-service:bootRun
```

`kafka-broker` uses the standard `9092` and needs no override.

Tear down with `docker compose down` when done.

## Submitting pull requests

Please follow these steps to simplify review:

- Rebase your branch against the current `main`.
- Run `./gradlew build` to make sure the project still builds cleanly.
- Run the test suite before submitting.
- Add tests for any new functionality.

## Submitting bug reports

- Search existing issues on this repo before opening a new one.
- Include a small reproduction where possible.
- State the JDK version and OS in use.

## Submitting new features

- Keep each service's API surface small and concise.
- Open an issue describing the proposal before submitting a PR.

## Commit message guidelines

Commit messages follow a fixed format so history stays readable.

### Format

Each commit message has a header, an optional body, and an optional footer:

```text
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The header is mandatory; scope is optional. No line may exceed 100 characters.

Example:

```text
fix(order-service): avoid duplicate compensation event on retry
```

### Type

One of: `build`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `style`, `test`.

### Scope

The service module affected, matching the module layout as it gets built out:

- **api-gateway-service**
- **order-service**
- **payment-service**
- **restaurant-service**
- **user-service**
- **user-contract**

### Subject and body

Imperative, present tense ("change" not "changed"/"changes"), no capital letter or trailing period on the subject. The body explains motivation and contrasts with previous behavior.

### Footer

Reference closed issues and note breaking changes here, starting with `BREAKING CHANGE:`.
