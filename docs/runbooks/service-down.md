# Runbook: ServiceDown

1. Check pod status and events: `kubectl -n saga-ecosystem get pods`, `kubectl describe pod`.
2. Inspect recent logs: `kubectl -n saga-ecosystem logs deployment/<service> --tail=200`.
3. Verify dependencies: Postgres health, Kafka broker health, and network policies.
4. If a bad deploy caused it, roll back: `kubectl rollout undo deployment/<service>`.
5. Confirm `/actuator/health` returns 200 after restart.
