# Chaos execution checklist

Before production cutover, record evidence for:
- Pod deletion recovery time and HTTP error rate
- Node drain and rescheduling time
- Managed Postgres failover and outbox durability
- Kafka broker restart and consumer lag recovery
- DLQ replay behavior
- p99 latency during all experiments
- HPA scale-up/down timing

Store the resulting dashboards, logs, and command transcripts with the release record.
