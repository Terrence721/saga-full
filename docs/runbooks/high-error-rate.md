# Runbook: HighErrorRate

1. Identify the affected service and status code from Grafana/Prometheus.
2. Inspect application logs for stack traces and downstream dependency failures.
3. Check recent deployments and config changes.
4. If database/Kafka is degraded, follow the dependency runbooks.
5. Roll back if the errors correlate with a new release.
