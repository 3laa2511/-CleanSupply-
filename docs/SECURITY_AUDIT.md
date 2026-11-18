# CleanSupply — Security Audit (Phase Zero)

**Audit Date:** 2026-11-18  
**Severity Scale:** **CRITICAL** (Immediate financial/data-integrity/privilege exploit) | **HIGH** | **MEDIUM** | **LOW**

---

## 1. Executive Security Verdict

**The current client-direct Firestore MVP is NOT safe for production.**  
While `firestore.rules` correctly enforces basic authentication (`request.auth != null`), document ownership (`userId == request.auth.uid`), and prevents direct self-promotion of `approvedTier` / `verificationStatus`, **all core commerce invariants—prices, discounts, payment status, stock integrity, loyalty minting, and B2B credit—are trusted from the Android client.**

---

## 2. Critical & High Vulnerabilities Identified in Code

### SEC-01 [CRITICAL] Client-Forged Order Prices, Totals & Discounts
- **Evidence:**
  - `CommerceRepository.kt:576-631` writes `subtotal`, `tierDiscount`, `couponDiscount`, `loyaltyDiscount`, `shippingFee`, `totalAmount`, `unitPrice`, and `lineTotal` directly from the client's `CheckoutSummary`.
  - `firestore.rules:254-263, 282-284` only checks type and non-negativity (`data.subtotal >= 0`, `data.totalAmount >= 0`, `data.unitPrice > 0`).
- **Impact:** Any modified client or direct Firestore REST call using a valid Firebase ID token can place an order for **0.00 EGP** (`totalAmount: 0`) regardless of catalog prices or quantities.

### SEC-02 [CRITICAL] Self-Reported Online Card Payment (`PAID`) Bypass
- **Evidence:**
  - `CommerceRepository.kt:547`: `PaymentMethod.CARD -> if (paymentReference.isNotBlank()) PaymentStatus.PAID.name else PaymentStatus.PENDING_VERIFICATION.name`.
  - `firestore.rules:251` allows any authenticated user creating an order to set `paymentStatus in ['PENDING', 'PENDING_VERIFICATION', 'PAID', 'INVOICED']`.
- **Impact:** A customer can select `CARD`, type any string (e.g., `"test"`), or call Firestore directly with `paymentStatus: "PAID"` and obtain confirmed paid orders without paying a single EGP.

### SEC-03 [CRITICAL] Arbitrary Stock Manipulation on `products`
- **Evidence:**
  - `firestore.rules:328-331` allows **any signed-in user** to update `products/{productId}` as long as only `['stockQty', 'updatedAt']` change and `0 <= incoming.stockQty <= existing.stockQty`.
- **Impact:** Any authenticated customer can independently send an `update` setting `stockQty = 0` across all products (denial-of-service / catalog wipeout) without placing an order. Furthermore, `CommerceRepository.kt:559-568` reads `stockQty` *outside* a Firestore Transaction before calling `db.batch()`, creating a **TOCTOU race condition** allowing overselling under concurrent checkouts.

### SEC-04 [CRITICAL] Client-Minted Loyalty Points (+500 per write) & Unverified Cancel Exploitation
- **Evidence:**
  - `firestore.rules:389-396` allows the document owner to update `users/{userId}.loyaltyPoints` up to `existing().loyaltyPoints + 500` **on any arbitrary profile update**, independent of any order!
  - `firestore.rules:427-429` allows the owner to append `loyaltyLogs` with `pointsDelta <= 500`.
  - `CommerceRepository.kt:699-716` (`cancelOrder`) cancels orders without reversing `loyaltyPointsEarned` or restoring `creditUsed` / `stockQty`.
- **Impact:** A malicious user can mint +500 loyalty points repeatedly via direct Firestore updates, or place orders to earn points and immediately cancel.

### SEC-05 [CRITICAL] B2B Credit Limit Exhaustion / Race & Non-Reversal on Cancel
- **Evidence:**
  - `firestore.rules:389-396` allows any user to increase `creditUsed` up to `creditLimit` on their own document without linking to an order.
  - `CommerceRepository.kt:538, 647-651` checks `availableCredit` against cached `UserProfile` state rather than an atomic server transaction, and `cancelOrder()` never releases `creditUsed`.

### SEC-06 [HIGH] Hardcoded Admin Email in Client Binary & Firestore Rules
- **Evidence:**
  - `CommerceRepository.kt:52, 61-64`: `const val ADMIN_EMAIL = "Alaa.moagaza2016@gmail.com"`.
  - `firestore.rules:22`: `request.auth.token.email == 'Alaa.moagaza2016@gmail.com' && request.auth.token.email_verified == true`.
  - `OrdersAccountScreen.kt:170-182`: Admin Operations Console (`OperationsConsoleTab`) is compiled into the public Customer APK.
- **Impact:**
  - Admin identity is exposed in decompiled APK bytecode.
  - Single-admin email pattern cannot scale to 7 operational roles (`OPERATIONS`, `INVENTORY`, `PURCHASING`, `FINANCE`, `DELIVERY`, `SUPPORT`, `SUPER_ADMIN`), lacks key/role revocation auditability, and puts privileged UI inside the customer app.

### SEC-07 [HIGH] Unverified Wholesale/Business Accounts Can Apply B2B Coupons
- **Evidence:**
  - `PricingEngine.kt:120-125` checks `coupon.allowedSegments.contains(profile?.segment)` without verifying `profile.verificationEnum() == VerificationStatus.VERIFIED`.
  - Confirmed by `PricingEngineTest.kt:55-64` (`pendingWholesale` passes `isCouponEligible(b2bPro, pendingWholesale, 2000.0)`).
- **Impact:** Any newly signed-up retail user can submit a `WHOLESALE` switch request (`PENDING_VERIFICATION`) and immediately redeem wholesale-exclusive coupons (`B2BPRO`) at checkout.

### SEC-08 [MEDIUM] Unenforced Coupon Eligibility, Usage Limits & Single-Redemption Rules
- **Evidence:**
  - `firestore.rules` has no `couponRedemptions` collection and does not cross-check `order.couponCode` / `order.couponDiscount` against `/coupons/{id}` or user redemption history.
- **Impact:** Single-use or expired coupons can be abused indefinitely on the client or via direct Firestore writes.

### SEC-09 [MEDIUM] Hardcoded Synthetic PII / Placeholder Coordinates & Phone Numbers
- **Evidence:**
  - `CommerceRepository.kt:185, 207, 591-592, 872-873`: Defaults customer phone to `"01000000000"`, address to `"15 Abbas El-Akkad St., Nasr City"`, and driver to `"Karim Hassan"` (`"01099887766"`).
- **Impact:** Invalid contact telemetry on real orders and misleading driver identity on customer screens.

### SEC-10 [MEDIUM] Verbose Auth/Path Logging & Firestore Emulator Rule Evaluation Warnings
- **Evidence:**
  - `FirestoreErrorHandler.kt:38-54` logs full `userId`, `email`, `emailVerified`, and `providerInfo` via `Log.e("FirestoreError", ...)`, which remains active in release builds (`isMinifyEnabled = false`).
  - `firestore.test.js` run against Firestore Emulator logs `evaluation error at L323:24` / `L367:24` when un-email-populated mock tokens evaluate `isAdmin()`.

### SEC-11 [LOW / CLEAN] Secrets & Play Policy Checks
- **Findings from `grep` scan:**
  - **Clean:** Zero `signInAnonymously()` calls in `app/src/main/`.
  - **Clean:** Zero `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` / `ACCESS_BACKGROUND_LOCATION` permissions in `AndroidManifest.xml` (only `INTERNET` and `ACCESS_NETWORK_STATE`).
  - **Note:** `app/google-services.json` contains Firebase Web/Android API key (`AIzaSy...`) which is standard public Firebase client config (not a private secret), while `.env.example` keeps `GEMINI_API_KEY` commented out.
  - **Note:** `app/build.gradle.kts:46` has `isMinifyEnabled = false` for `release` builds; production release builds should enable R8 (`isMinifyEnabled = true` + ProGuard rules) before Play Store distribution.
  - **Note:** `android:allowBackup="true"` in `AndroidManifest.xml:9` with sample `backup_rules.xml` / `data_extraction_rules.xml` (`<!-- TODO -->`) should be hardened before production so local auth/cache tokens are excluded from cloud backups.

---

## 3. Target Security Model for Rebuild

All vulnerabilities `SEC-01` through `SEC-09` are eliminated by design in the Phase 1–9 rebuild:
1. **Firebase Admin SDK Token Guard (`Authorization: Bearer <idToken>`):** Verifies token signature, expiration, and revocation (`checkRevoked = true`), mapping `firebase_uid` to PostgreSQL `users.id` and checking `users.status == 'ACTIVE'`.
2. **PostgreSQL RBAC (`roles`, `permissions`, `user_roles`):** Replaces `ADMIN_EMAIL` and `isAdmin()` with granular server-side permission guards on every Admin/Driver endpoint.
3. **Server-Authoritative Pricing & Quote Engine:** Client sends only `{ variantId, packMode, quantity, couponCode, redeemPoints, addressId, deliverySlot, paymentMethod }`. NestJS computes and persists authoritative prices, VAT, discounts, and totals inside DB transactions.
4. **Atomic Inventory Reservations & Ledger:** Uses PostgreSQL `SELECT ... FOR UPDATE` with deterministic `(warehouse_id, variant_id)` lock ordering + `idempotency_keys` table to prevent overselling and duplicate orders.
5. **Decoupled Verified Payments:** Online payments transition to `PAID` **only** via HMAC-verified provider webhooks (`payment_webhooks_log`) or server-to-server status verification; manual transfers require authorized `FINANCE` approval; COD requires confirmed delivery/collection reconciliation.
6. **Lock Down Firestore:** Once Android cutover completes, set `firestore.rules` to `allow read, write: if false;` (or remove Firestore dependency entirely from `:app`).
