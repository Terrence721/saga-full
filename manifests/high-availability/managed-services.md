# Managed/high-availability Postgres and Kafka

For production, use managed clusters rather than the single-node compose defaults.

## Postgres
- Set `DATABASE_HOST`, `DATABASE_PORT`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to the managed endpoint and least-privilege credentials.
- Each service already targets its own schema via `currentSchema=..._service_schema`; managed Postgres should expose those schemas and users.
- Disable `sql.init.mode` in production (`never`) and let Flyway own migrations. Enable automated backups/PITR.

## Kafka
- Set `KAFKA_BOOTSTRAP_SERVERS` to a comma-separated broker list, for example `kafka-1:9092,kafka-2:9092,kafka-3:9092`.
- Use a replicated topic configuration (replication factor >= 3) and multiple partitions for saga throughput.
- Enable TLS/SASL according to the managed provider and enforce ACLs.
- Monitor consumer lag, DLQ depth, and broker disk usage.

## Application settings
- Run services with at least 3 replicas behind the existing health probes.
- Apply the PDBs in this directory before rolling updates or node drains.
