# Runbook: HighLatency

1. Confirm whether latency is isolated to one endpoint or service.
2. Check saturation: CPU, memory, thread pools, DB connections, Kafka consumer lag.
3. Review recent traffic spikes and HPA scaling.
4. Inspect slow traces in Tempo.
5. Scale manually if autoscaling is lagging, then tune HPA thresholds.
