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
