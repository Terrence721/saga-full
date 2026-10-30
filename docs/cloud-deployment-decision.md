<!-- Status (Oct 2026): Login/auth entrypoints and the register/POS login flow were removed; the public site is order/reservation. Test totals: 314 backend, 69 Vitest/RTL + 4 Playwright E2E frontend. Historical sections below may predate these changes. -->
# Cloud Deployment Decision — Issue #229 (docs-only phase)

**Status:** No live deployment yet. Manifests already landed under [`manifests/`](../manifests)
(manifests-only, app images are expected to be built/pushed by the operator).

## Goal

Decide whether to actually deploy the saga chain to a managed container platform
(one container per service) as a CI step, mirroring the idea raised for `conduit-full`.

## Options considered

| Option | Pros | Cons | Verdict |
|---|---|---|---|
| **Azure Container Apps** | Scale-to-zero, cheap for demos, managed Postgres/Event Hubs option, simpler secrets via Key Vault | Less familiar to AWS shops; Event Hubs semantics differ from Kafka slightly | **Recommended if we ship** |
| **AWS ECS/Fargate** | Strong AWS story, pairs with RDS + MSK, plenty of ecosystem | Higher ops overhead, costier at idle, more YAML/Terraform | Good for enterprise parity, overkill for portfolio |
| **Manifests-only (current)** | Zero hosting cost, reusable K8s story already tells the deployment tale | No live demo URL | **Current state** |

## What a real deploy would require

1. Image registry (GHCR or ECR/ACR) + per-service build/push in CI.
2. Managed Postgres (with `reservation_service_schema` + existing four schemas) and Kafka-equivalent
   (MSK / Event Hubs) or self-hosted Kafka inside the cluster.
3. Secret wiring for `JWT_SECRET`, DB creds, and any OTel OTLP endpoint.
4. Hosting-target decision (above) plus a cost ceiling.

## Decision

Remain **manifests-only for now**. The manifests + runbook (`docs/kubernetes.md`,
`manifests/README` pending) already provide a deployable story; live hosting can be added later once
a budget/region decision lands. No code changes are needed to support it — only infra.
