# 📊 Observability Runbook

Minimal, runnable triage playbook for the saga stack. Complements the Kubernetes manifests under
[`manifests/monitoring/`](../manifests/monitoring), which carry the full OTel / Prometheus / Loki / Tempo / Grafana
configuration.

## What's already wired in the app

Every service module depends on `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, and
`opentelemetry-exporter-otlp`. In Docker the compose file sets:

- `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus` on every service (so `/actuator/prometheus`
  is scrapeable), and
- `MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4318/v1/traces` (OTLP HTTP),
- `OTEL_SERVICE_NAME=<service>` plus `OTEL_RESOURCE_ATTRIBUTES=service.namespace=saga-ecosystem,deployment.environment=local`.

Spring Boot's Actuator exposes `/actuator/health` and `/actuator/prometheus` on each service's own port
(gateway 8080, order 8081, payment 8082, restaurant 8083, reservation 8084, user 9090).

## Enabling the Grafana stack (Kubernetes)

Apply the monitoring manifests:

```bash
kubectl apply -f manifests/monitoring/namespace.yaml 2>/dev/null || true
kubectl apply -R -f manifests/monitoring/
```

Pin the data sources inside Grafana (web UI on the service's NodePort) to `Prometheus`, `Loki`, and `Tempo` using
the endpoints defined in `manifests/monitoring/config/*-config.yaml`.

## Day-0 triage

1. **Metrics (Prometheus, Grafana port 9095)** — scrape `saga-*` targets. Useful queries:
   - HTTP route latency: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))`
   - Circuit breaker trips on the gateway: `resilience4j_circuitbreaker_state`
   - JVM/GC memory: `jvm_memory_used_bytes{area="heap"}`
2. **Traces (Tempo via Grafana Explore)** — search by the order id (`trace_id` extracted from the `POST /orders`
   response); the tree should read `api-gateway-service → order-service → payment-service → restaurant-service`
   (and `reservation-service` when an order commits).
3. **Logs (Loki)** — `{service_name="order-service"} |= "ORDER_ID"` to stitch a trace together.

## End-to-end sanity check

```bash
# Healthy happy path lands in SUCCESS on the SSE stream
curl -sS -X POST http://localhost:8090/orders -H "Authorization: Bearer <jwt>" \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"<id>","itemCode":"MARGHERITA","quantity":1,"totalAmount":5}'
# Then GET /orders/{id}/stream and watch PENDING -> SUCCESS.
```

Compensation can be exercised with an unseeded item code (e.g. `BURGER`): the order moves to `CANCELLED`,
the payment is refunded, and the reservation (if any) is cancelled — verify each leg in Loki/Tempo.

## Costs / scope

This is intentionally manifests-first: there is no live cluster deploy target yet (issue #229 cloud decision
pending) and the OTel collector URL defaults to `otel-collector:4318` so it only resolves under Kubernetes or when
the compose profile is wired up. Follow-up work includes NodePort/Ingress wiring and dashboards as code.

## Verification (completed 2026-10-27, local smoke)

- Stack rebuilt with `micrometer-registry-prometheus` on all six services.
- `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus` is set in `docker-compose.yml`.
- `OTEL_SERVICE_NAME` + `OTEL_RESOURCE_ATTRIBUTES` are wired per service.
- Prometheus (`http://localhost:9096`, host-mapped) successfully scrapes:
  - order:8081 → up
  - payment:8082 → up
  - restaurant:8083 → up
  - gateway:8090 → up
- Grafana responds at `http://localhost:3000`.
- Prometheus datasource can be added in Grafana at `http://host.docker.internal:9096`.
- Remaining: live Kubernetes run-through, end-to-end trace extraction, dashboards-as-code, live deploy decision.
