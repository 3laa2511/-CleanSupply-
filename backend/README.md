# CleanSupply Backend

NestJS modular-monolith API for CleanSupply. This application is intentionally introduced alongside the existing Android `:app` module; it does not replace or modify the Android client.

## Requirements

- Node.js 20.11+
- npm 10+
- PostgreSQL 15+
- A Firebase project for authenticated API requests

## Local setup

```bash
cd backend
cp .env.example .env
npm install
npm run prisma:generate
npm run start:dev
```

Set `DATABASE_URL` to a reachable PostgreSQL database before starting the API. The API listens on `http://localhost:3000`; health endpoint: `GET /api/v1/health`.

## Database

Validate and generate the Prisma client:

```bash
npm run prisma:validate
npm run prisma:generate
```

The first reviewed SQL migration is a required next database-delivery step. Do not run `prisma migrate dev` against production. Once migration files are committed, deployments must use `npm run prisma:migrate:deploy`.

The current schema models users/RBAC, customer profiles, addresses, catalog, tier prices, one-or-many warehouses, inventory balances and ledger, orders, payments, and audit logs. Later phases will add reservations, purchasing/GRN, batches/expiry, coupon and loyalty ledgers, invoices, credit ledger, shipments, carriers, drivers, and support workflows.

## Authentication and authorization

- Send Firebase ID tokens as `Authorization: Bearer <id-token>`.
- The global guard verifies tokens with Firebase Admin and checks revocation.
- On Google Cloud, use workload Application Default Credentials and set `FIREBASE_PROJECT_ID`.
- For local development, set `GOOGLE_APPLICATION_CREDENTIALS` to a service-account JSON file stored outside this repository.
- `GET /api/v1/auth/me` creates or refreshes the internal user profile from verified Firebase claims.
- Missing optional Firebase claims do not erase previously stored profile fields.
- Business controllers must declare permissions with `@RequirePermissions('catalog.read')`; the global permissions guard checks active status and PostgreSQL role-permission assignments.
- Only endpoints explicitly marked `@Public()` bypass Firebase authentication. Health is public.
- No user is automatically granted an administrative role; initial administrator assignment must be a controlled provisioning operation.

## Security and operational notes

- Do not commit `.env`, service-account JSON, Firebase private keys, or provider secrets.
- The global request rate limit is a baseline and must be tuned before production; a Redis-backed distributed limiter is still pending.
- CORS origins must be explicitly configured per environment.
- No pricing, inventory, payment, or order mutation endpoints are exposed yet.
- No production deployment or production database mutation has been performed.

## Current scope

Backend foundation and Firebase authentication/RBAC are implemented in the open pull requests. CI validates the Prisma schema, generates Prisma Client, builds the NestJS API, and runs unit tests. This is not yet production-ready: reviewed SQL migrations, domain services/endpoints, integration tests against PostgreSQL, Redis-backed idempotency, GCP infrastructure, and Android API integration remain future work.
