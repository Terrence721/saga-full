# Chaos and failover experiments

## Automated smoke experiments
- Pod delete and observe recovery:
  - `kubectl -n saga-ecosystem delete pod -l app=order-service`
  - verify HPA/PDB keep at least 2 pods available
- Node drain:
  - `kubectl drain <node> --ignore-daemonsets --delete-emptydir-data`
  - verify all Deployments reschedule
- Database restart:
  - restart managed Postgres or test instance
  - verify Flyway validates and services reconnect
- Kafka broker restart:
  - restart one Kafka broker in a 3-broker cluster
  - verify producer/consumer availability and lag recovery
- DLQ replay:
  - publish a deliberately invalid event
  - verify consumer does not retry indefinitely and DLQ receive works

## Steady-state checks
- HTTP success rate above 99.5%
- p99 latency under 2 seconds
- no pod restart loop for 10 minutes
- no consumer lag above 1000 for more than 5 minutes
