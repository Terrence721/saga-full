# HA validation runbook

## Scope
Validate the availability and failover behavior declared by the 3-replica Deployments, HPAs, and PDBs.

## Prerequisites
- Apply `manifests/high-availability/*`.
- Use managed or multi-node Postgres/Kafka, not single-node local Compose.
- Prometheus and Grafana are available.

## Tests
1. Rolling update restart:
   - `kubectl rollout restart deployment/<service>`
   - verify no 5xx spike and readiness stays healthy.
2. Pod eviction:
   - `kubectl delete pod -l app=<service>`
   - confirm PDB prevents simultaneous disruption and service stays available.
3. Node drain:
   - `kubectl drain <node> --ignore-daemonsets --delete-emptydir-data`
   - verify pods reschedule and traffic continues.
4. Postgres failover:
   - fail over to replica or restart managed instance
   - verify service reconnects and no data loss from the outbox.
5. Kafka broker loss:
   - restart one broker in a 3-broker cluster
   - verify producers/consumers continue and consumer lag recovers.
6. Backup/PITR:
   - run a scheduled backup and restore to a test database.
   - verify application starts and Flyway validation passes.

## Suggested SLO checks
- availability: 99.9% during failover
- error rate: below 5% for 5 minutes
- p99 latency: under 2 seconds
