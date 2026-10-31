# Local development

This guide explains how to run and browse the Saga platform locally with Docker Compose.

## Local URLs

Backend gateway (primary entrypoint):
- Base URL: `http://localhost:8090`
- Health: `http://localhost:8090/actuator/health`
- Menu: `http://localhost:8090/menu`
- Orders: `http://localhost:8090/orders`
- Orders by ID: `http://localhost:8090/orders/{id}`

Direct service URLs:
- User service gRPC: `localhost:9090`
- Order service: `http://localhost:8081`
- Payment service: `http://localhost:8082`
- Restaurant service: `http://localhost:8083`
- Reservation service: `http://localhost:8084`

Infrastructure:
- Postgres: `localhost:5433`
- Kafka: `localhost:9092`

Frontend (optional):
- Vite dev server: `http://localhost:5180`

## Start the stack

```bash
export JWT_SECRET=local-dev-jwt-secret
export POSTGRES_PASSWORD=postgres
docker compose up -d --build
```

Check status:
```bash
export JWT_SECRET=local-dev-jwt-secret
export POSTGRES_PASSWORD=postgres
docker compose ps
```

Stop:
```bash
docker compose down
```

## Useful API commands

Health:
```bash
curl http://localhost:8090/actuator/health
```

Menu:
```bash
curl http://localhost:8090/menu
```

Create an order (`Authorization: Bearer <token>` is required):
```bash
curl -X POST http://localhost:8090/orders \
  -H 'Authorization: Bearer <jwt-token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId":"11111111-1111-1111-1111-111111111111",
    "totalAmount":12.50,
    "itemCode":"MARGHERITA",
    "quantity":1,
    "paymentMethod":"CASH"
  }'
```

## Frontend development

Run the React/Vite frontend separately:
```bash
cd frontend
corepack enable
yarn install --immutable
yarn dev
```

The frontend dev server usually runs at:
```text
http://localhost:5180
```

It connects to the gateway configured by the frontend API layer.

## Notes
- Full stack compose requires `JWT_SECRET` and `POSTGRES_PASSWORD`.
- The gateway does not serve a web UI at `/`; that is expected for an API gateway.
- The first startup may take longer due to Flyway migrations and service initialization.
