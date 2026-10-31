# Service-level objectives

Recommended starting SLOs for the saga ecosystem:

- Availability: 99.9% successful responses over 30 days
- Latency: p99 request latency under 2 seconds
- Error budget: 0.1% of requests may fail
- Consumer lag: no Kafka consumer group lag above 1000 messages for more than 5 minutes
- Data durability: 99.99% successful outbox publish within 30 seconds
