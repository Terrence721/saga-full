# Saga POS Frontend

Register/POS client for the Distributed Saga platform. React 19 + TypeScript, built with Vite, styled with Tailwind CSS 4. Calls `api-gateway-service` directly (no BFF).

## Scripts

```bash
yarn install
yarn dev       # http://localhost:5180
yarn build     # type-check + production bundle
yarn test      # Vitest + React Testing Library (20 tests)
yarn lint      # oxlint
```

The gateway must be running first (`http://localhost:8090` by default). Set `FRONTEND_ORIGIN` on the gateway if you change the dev port.
