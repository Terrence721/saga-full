<!-- Status (Oct 2026): Login/auth entrypoints and the register/POS login flow were removed; the public site is order/reservation. Test totals: 314 backend, 69 Vitest/RTL + 4 Playwright E2E frontend. Historical sections below may predate these changes. -->
# Frontend Verification (issue #228)

Manual + automated checklist for the register/POS UI (`frontend/`, dev server `http://localhost:5180`,
gateway `http://localhost:8090`).

## Automated (must pass before any run-through)

```bash
cd frontend
yarn install
yarn test   # Vitest + React Testing Library (22 tests currently)
yarn lint
yarn build
```

Status: **build + lint + 22/22 tests passing.**

## Manual click-through at a tablet viewport (target: 1024-1280 px wide)

Launch `yarn dev` (pinned to `http://localhost:5180`), open DevTools and lock the viewport to 1280x800:

1. **Login**
   - `LoginForm` renders card + `auth.logout` is not yet visible
   - Type email/password, toggle **Show/Hide** password, confirm input type flips and
     the form does **not** submit on toggle
   - Submitting shows the spinner + “Signing in…”, then authenticates
   - Invalid credentials surface an `role="alert"` message and stay logged out
2. **Order entry**
   - Quantity stepper increments/decrements; decrement disabled at 1
   - Submit with a seeded item (`MARGHERITA`, qty 1, total e.g. 5.00) locks the form
     and shows the spinner (“Placing…”)
3. **Live status**
   - Status card renders order id, badge, item/qty/total, and “Live updates connected”
   - SSE updates flip the badge `PENDING → SUCCESS` without a page reload
4. **Logout**
   - `AppShell` header “Log out” is present and clears auth state back to `LoginForm`

## Runtime notes

- All interactions hit `api-gateway-service` at `http://localhost:8090`; `FRONTEND_ORIGIN`
  must match the dev port (default `http://localhost:5180`).
- An unseeded item code (e.g. `BURGER`) exercises the compensation path (`CANCELLED`,
  payment refunded, reservation cancelled) — verify each in the gateway logs.


## Verification record

- Date: October 27, 2026
- `yarn test`: **22/22 passing**
- `yarn lint`: clean (exit 0)
- `yarn build`: passes (Vite prod bundle emitted)
- Frontend dev server (`http://localhost:5180`) and backend gateway (`http://localhost:8090`) both respond `200 OK`.
- End-to-end saga (`PENDING → SUCCESS`) verified via the gateway with `MARGHERITA`, and the compensation path (`CANCELLED`) verified with `BURGER`.
