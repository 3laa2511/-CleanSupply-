# CleanSupply — Implementation Roadmap (Phase Zero Audit)

**Audit Date:** 2026-11-18  
**Execution Principle:** Dependency-ordered migration. Keep the existing Android Customer App compiling and preserved while building the authoritative backend, database, and admin portal before cutting over Android repositories.

---

## Phase Overview & Dependency Sequence

| Phase | Name | Primary Deliverables | Exit Criteria / Verification |
| :--- | :--- | :--- | :--- |
| **Phase 0** | **Repository Audit (Current)** | 6 audit/architecture docs (`docs/*.md`), test & build baseline, blockers list | Build & existing unit/rules tests verified; owner sign-off on open decisions |
| **Phase 1** | **NestJS Backend Foundation** | Modular monolith scaffold (`/backend`), `/api/v1`, Swagger, Config/Env validation, Pino structured logs, Correlation ID, Global Exception Filter, Rate Limiting, Idempotency middleware, Health checks | Backend boots, `/api/v1/health` passes, Swagger renders |
| **Phase 2** | **PostgreSQL + Prisma Schema** | Full normalized schema (Identity, Catalog, Warehouses/Inventory, Orders/Finance, Delivery/Support, Audit Logs), initial migrations, seed script (1 central warehouse, roles/permissions, baseline catalog) | `prisma migrate deploy` & seed run cleanly against PostgreSQL |
| **Phase 3** | **Auth & Server-Side RBAC** | Firebase Admin SDK ID token guard, user auto-provisioning in PostgreSQL, 7 Admin RBAC roles + permission guard, Customer segment/KYC endpoints, account suspension checks | Integration tests verify revoked/spoofed tokens & RBAC denials (`403`) |
| **Phase 4** | **Catalog & Server-Authoritative Pricing** | Multi-tier pricing (`RETAIL`, `WHOLESALE`, `BUSINESS`), `ApprovedTier` rebates, volume slabs, contract prices, unit/carton pack math, MOQ enforcement, `POST /checkout/quote` | Unit & API tests match/exceed `PricingEngineTest` and block unverified B2B pricing/coupons |
| **Phase 5** | **Inventory Ledger, Purchasing & Atomic Checkout** | Suppliers, POs, GRNs, `inventory_balances`, `inventory_batches`, `inventory_reservations` (with expiry sweep via Cloud Tasks/Redis), `inventory_ledger`, concurrent `POST /orders` with deterministic lock ordering & idempotency keys, order cancel stock release | Concurrency integration test (parallel checkouts on last stock unit) guarantees zero overselling & zero duplicate orders |
| **Phase 6** | **Payments, B2B Credit, Tax Invoices & Returns** | Decoupled `payments` state machine, webhook HMAC verifier (`payment_webhooks_log`), COD reconciliation, manual transfer verification, B2B credit limit + overdue invoice checks (`credit_ledger`), sequential VAT `sales_invoices`, `returns` & `refunds` | Integration tests for webhook spoofing rejection, credit exhaustion, partial/full refunds, invoice numbering |
| **Phase 7** | **Delivery & Carrier Adapters** | `delivery_zones`, `shipments`, `drivers`, `delivery_proofs`, `CarrierAdapter` (Internal Fleet + 3rd-party courier abstraction), customer tracking APIs, FCM status notifications | Shipment lifecycle & adapter contract tests pass |
| **Phase 8** | **Next.js Admin Dashboard** | Separate `/admin-web` Next.js TypeScript app with Firebase Auth + 7-role RBAC views (Dashboard, Catalog/Pricing, KYC & Credit Approval, Purchasing/GRN/Inventory, Orders/Reconciliation, Dispatch/Drivers, Support, Audit Logs) | E2E/role-boundary tests verify Finance vs. Inventory vs. Support isolation |
| **Phase 9** | **Android App Integration** | Retrofit/OkHttp + Firebase ID token interceptor, Room offline cache, Phone OTP + Google Auth, Photo Picker KYC/slip upload, server-backed Cart/Checkout/Orders/Addresses/Tickets, removal of Firestore business ops & `OperationsConsoleTab` | Android unit + Robolectric + MockWebServer repository tests pass; `compile_applet` succeeds |
| **Phase 10** | **Security, QA & GCP Deployment Prep** | Vulnerability/secret scan, load/concurrency tests, backup/restore runbook, Cloud Run / Cloud SQL / Memorystore / Cloud Storage / Secret Manager IaC/configs, Cloud Monitoring alerts | Full QA checklist signed off; staging deployment ready |
| **Phase 11** | **Driver Android App (Dedicated App)** | Separate `/driver-app` module/project: Driver auth, assigned manifest, active-delivery foreground location service, POD capture, COD collection receipt | Driver app builds & passes delivery lifecycle tests |

---

## Blockers & Decisions Required Before Phase 1–6 Implementation

| # | Decision Needed | Impacted Phases |
| :--- | :--- | :--- |
| **B1** | **Country, Currency & Tax / e-Invoicing Scope** (confirm `EGP`, 14% VAT, and whether Egyptian Tax Authority (ETA) e-invoicing integration is Phase 1 or Phase 3) | Phase 2, 4, 6 |
| **B2** | **Payment Gateway Selection** (Paymob vs. Fawry vs. Geidea; Card + Mobile Wallet; whether manual Bank/InstaPay slip upload is enabled at launch) | Phase 6, 9 |
| **B3** | **Third-Party Shipping Carrier Selection** (Bosta, Aramex, Mylerz, or manual AWB entry at launch behind `CarrierAdapter`) | Phase 7 |
| **B4** | **Phone OTP & Google Maps Billing** (Confirm Firebase Phone Auth SMS quota/region + whether Google Maps SDK & Routes API key will be provisioned) | Phase 3, 7, 9 |
| **B5** | **Inventory Batch / Expiry (FEFO) Day-1 Requirement** (Confirm whether chemical/detergent shelf-life batch tracking is mandatory on Day 1 or schema-ready for Phase 2) | Phase 2, 5 |
| **B6** | **Monorepo vs. Multi-Repo Workspace Layout** (Confirm placing `/backend` and `/admin-web` alongside `/app` in this repo vs. separate repositories; note AI Studio Android build system builds `:app` from root `settings.gradle.kts`) | Phase 1, 8 |
| **B7** | **Final Brand Name, Logo & Domain** (Or keep neutral `CleanSupply / كلين سبلاي` until store launch) | Phase 8, 9, 10 |
