# CleanSupply — Architecture Decisions Record (Phase Zero Audit)

**Date:** 2026-11-18  
**Status:** Baseline Architecture Decisions (Pre-Implementation)

---

## ADR-001: Replace Client-Direct Firestore with NestJS Modular Monolith + PostgreSQL + Prisma
- **Status:** Approved
- **Context:** The existing MVP reads/writes catalog, carts, orders, stock, loyalty points, and B2B credit directly from Android to Cloud Firestore. Multi-tier B2B pricing, MOQ enforcement, atomic inventory reservations, inventory ledgers, purchasing/GRN, sequential VAT invoicing, B2B credit aging, and payment webhook verification cannot be safely enforced in Firestore Security Rules or on the client.
- **Decision:** Rebuild the backend as a **NestJS (TypeScript) Modular Monolith** exposing REST endpoints under `/api/v1` with Swagger/OpenAPI, backed by **PostgreSQL + Prisma ORM** for transactional ACID truth and **Memorystore Redis** for rate limiting, idempotency locks, short-lived caching, and reservation/job queues.
- **Consequences:**
  - Android `CommerceRepository` (Firestore) will be replaced by Retrofit/OkHttp API repositories.
  - All pricing, discounts, taxes, credit checks, stock reservations, and order state transitions become strictly server-authoritative.
  - Firestore rules/collections will be retired (`allow read, write: if false;`) after backend cutover.

---

## ADR-002: Hosting & Infrastructure on Google Cloud Platform (GCP)
- **Status:** Approved
- **Decision:** Target GCP managed services:
  - **Compute:** Cloud Run (stateless NestJS API container + Next.js Admin container)
  - **Database:** Cloud SQL for PostgreSQL
  - **Cache / Locks / Queues:** Memorystore for Redis
  - **Object Storage:** Google Cloud Storage (KYC documents, product images, PDF VAT invoices, delivery POD photos, bank transfer slips) via signed URLs
  - **Async Jobs & Schedules:** Cloud Tasks + Cloud Scheduler (e.g., reservation expiry sweep, overdue B2B invoice reminders, webhook retries)
  - **Secrets & Observability:** Secret Manager, Cloud Logging, Cloud Monitoring
  - **Push Notifications:** Firebase Cloud Messaging (FCM)

---

## ADR-003: Identity via Firebase Authentication + Server-Side PostgreSQL RBAC
- **Status:** Approved
- **Decision:** Keep **Firebase Authentication** purely as the identity/authentication layer (Google Sign-In + Phone OTP), while moving all authorization and roles to **PostgreSQL (`users`, `roles`, `permissions`, `user_roles`)** verified by NestJS using **Firebase Admin SDK** (`verifyIdToken(token, checkRevoked=true)`).
- **Consequences:**
  - Removes hardcoded admin email checks (`ADMIN_EMAIL` in `CommerceRepository.kt` and `firestore.rules`).
  - Separates authentication (`firebase_uid`) from internal RBAC (`SUPER_ADMIN`, `OPERATIONS`, `INVENTORY`, `PURCHASING`, `FINANCE`, `DELIVERY`, `SUPPORT`) and customer segment (`RETAIL`, `WHOLESALE`, `BUSINESS`).

---

## ADR-004: Preserve Existing Android Compose UI & Design System
- **Status:** Approved
- **Decision:** Keep the existing Jetpack Compose screens (`AuthScreen`, `HomeScreen`, `CatalogScreen`, `CartCheckoutScreen`, customer tabs of `OrdersAccountScreen`), Material 3 `CleanSupplyTheme`, Cairo + IBM Plex Sans Arabic typography, RTL/LTR switching (`LanguagePreferences`), and adaptive `NavigationBar`/`NavigationRail` shell.
- **Modifications Allowed Only With Evidence:**
  - Remove the embedded `OperationsConsoleTab` (`OrdersAccountScreen.kt:675-774`) and `isAdmin` flags from the Customer App.
  - Add Phone OTP sign-in option in `AuthScreen`.
  - Add Room local database for structured offline catalog/cart caching (`libs.versions.toml` already includes Room 2.7.0).
  - Add missing UX states (loading/error/retry banners without silent `SeedData` masking, KYC file upload via Android Photo Picker, payment checkout flow, address edit/delete).

---

## ADR-005: Single Central Warehouse at Launch with Multi-Warehouse Schema
- **Status:** Approved
- **Decision:** Launch operations with **one central warehouse**, while modeling `warehouses`, `inventory_balances`, `inventory_batches`, `inventory_reservations`, and `inventory_ledger` in PostgreSQL with `warehouse_id` foreign keys from Day 1.
- **Consequences:** Zero schema migration pain when opening regional depots later; stock reservation locks use deterministic lock ordering `(warehouse_id, variant_id)` to prevent deadlocks.

---

## ADR-006: Configurable Neutral Branding (`CleanSupply / كلين سبلاي`)
- **Status:** Approved
- **Decision:** Keep `"CleanSupply"` / `"كلين سبلاي"` as a temporary, centralized identifier (`strings.xml`, `AppStrings.kt`, `metadata.json`, backend `system_configs`) so final rebranding requires zero structural refactoring.
- **Note:** Existing Android `applicationId` (`com.aistudio.cleansupply.kxmpzq`) remains unchanged during development per project toolchain rules until final store package assignment is decided by the owner.

---

## ADR-007: Separate Admin Web Portal (Next.js) & Future Driver Android App
- **Status:** Approved
- **Decision:**
  - Operations, Inventory, Purchasing, Finance, Delivery Dispatch, and Support move out of the Customer Android App into a dedicated **Next.js (TypeScript) Admin Portal** enforcing 7-role RBAC via the NestJS API.
  - Fleet delivery execution (driver auth, assigned stops, active-trip foreground GPS telemetry, Proof of Delivery, COD collection reconciliation) will be built as a **separate Android Driver App** in a dedicated phase, keeping the Customer App free of background location or driver permissions.

---

## ADR-008: Provider-Agnostic Payment & Carrier Adapters
- **Status:** Approved (Vendors Pending Owner Confirmation)
- **Decision:** Decouple `Orders` from `Payments` and `Shipments`, and implement `PaymentGatewayAdapter` (supporting COD, Online Card, Mobile Wallet, B2B Credit Invoice, optional Manual Bank/InstaPay transfer) and `CarrierAdapter` (supporting Internal Fleet vs. 3rd-party couriers) inside NestJS.
- **Rules:** Payment transitions require verified server webhook HMAC signatures or authorized Finance approval—never client flags.
