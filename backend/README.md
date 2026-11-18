# CleanSupply Backend

NestJS modular-monolith API for CleanSupply. This application is intentionally introduced alongside the existing Android `:app` module; it does not replace or modify the Android client.

## Requirements

- Node.js 20.11+
- npm 10+
- PostgreSQL 15+
- Firebase project credentials are required when the authentication module is introduced. They are not needed for the initial health-check scaffold.

## Local setup

```bash
cd backend
cp .env.example .env
npm install
npm run prisma:generate
npm run start:dev
```

The API listens on `http://localhost:3000`. Health endpoint: `GET /api/v1/health`.

## Database

Set `DATABASE_URL` in `.env`, then create and apply the first migration:

```bash
npm run prisma:validate
npm run prisma:migrate:dev -- --name init
```

For deployment, use `npm run prisma:migrate:deploy`; never run `migrate dev` against production.

The initial schema is a foundation, not the final commerce schema. It deliberately models users/RBAC, customer profiles, addresses, catalog, tier prices, one-or-many warehouses, inventory balances and ledger, orders, payments, and audit logs. Later phases will add reservations, purchasing/GRN, batches/expiry, coupon and loyalty ledgers, invoices, credit ledger, shipments, carriers, drivers, and support workflows.

## Security notes

- Do not commit `.env`, service-account JSON, Firebase private keys, or provider secrets.
- Authentication and authorization guards are not enabled yet; protected business endpoints must not be added until the Firebase token verifier and database-backed RBAC phase is implemented.
- The global request rate limit is a baseline and must be tuned before production.
- CORS origins must be explicitly configured per environment.
- No pricing, inventory, payment, or order mutation endpoints are exposed in this scaffold.

## Current scope

Phase 1 foundation and Phase 2 initial Prisma data model. Build/test results must be recorded from a real local or CI run before this backend is considered verified.


## Firebase authentication and authorization

- Send Firebase ID tokens as `Authorization: Bearer <id-token>`.
- The global guard verifies tokens with Firebase Admin and checks revocation.
- On Google Cloud, use workload Application Default Credentials and set `FIREBASE_PROJECT_ID`.
- For local development, set `GOOGLE_APPLICATION_CREDENTIALS` to a service-account JSON file stored outside this repository.
- `GET /api/v1/auth/me` creates or refreshes the internal user profile from verified Firebase claims.
- Business controllers must declare permissions with `@RequirePermissions('catalog.read')`; the global permissions guard checks active status and PostgreSQL role-permission assignments.
- Only endpoints explicitly marked `@Public()` bypass Firebase authentication. Health is public.
- No user is automatically granted an administrative role; initial administrator assignment must be a controlled provisioning operation.
