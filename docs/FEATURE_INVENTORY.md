# CleanSupply — Feature Inventory (Phase Zero Audit)

**Audit Date:** 2026-11-18  
**Scope:** Actual codebase inspection of `:app` (`com.example` / `applicationId = com.aistudio.cleansupply.kxmpzq`) and root Firebase artifacts.

---

## 1. Summary Classification

Every feature in the current repository is categorized as:
- **Implemented (MVP Client-Direct):** Working end-to-end against Firebase Auth + Firestore on the client, but lacking server authority / production controls.
- **Partially Implemented / Simulated:** UI or model exists, but relies on hardcoded stubs, manual text input, or synthetic client state.
- **Missing (Required for Production):** Not present in the repository yet.

---

## 2. Implemented Features (Backed by Actual Code)

### 2.1 Authentication & Session Shell
- **Google Sign-In via Jetpack Credential Manager:**
  - `AuthScreen.kt` (`attemptAutoSignIn`, `onGoogleSignInClicked`, `signOut`) uses `GetGoogleIdOption` and `GetSignInWithGoogleOption` with `R.string.default_web_client_id`, exchanging the Google ID token via `Firebase.auth.signInWithCredential(...)`.
  - `MainActivity.kt` (`AppNavigation`) observes `FirebaseAuth.AuthStateListener` via `callbackFlow` and scopes `CommerceViewModel` per `user.uid` (`key(user.uid)`).
- **Bilingual RTL/LTR Switching:**
  - `AppStrings.kt` (`ArabicStrings`, `EnglishStrings`, `LanguagePreferences` backed by Jetpack DataStore `"cleansupply_prefs"`).
  - `Theme.kt` (`CleanSupplyTheme`) dynamically sets `LocalLayoutDirection` (`LayoutDirection.Rtl` for Arabic, `LayoutDirection.Ltr` for English).
  - Switching updates DataStore and persists `preferredLocale` on `users/{uid}` in Firestore.

### 2.2 Product Discovery & Catalog
- **Home Storefront (`HomeScreen.kt`):**
  - Segment & verification status strip + loyalty points pill (`SegmentVerificationBadge`).
  - Hero banner with bulk deals CTA (`home_explore_bulk_cta`).
  - Quick Reorder banner when `recentOrders.isNotEmpty()`, triggering `reorderFromOrder` and navigating to Cart.
  - Horizontal category carousel (`LazyRow`).
  - Promo banner highlighting `CLEAN10` and `BULK50`.
  - 2-column featured product grid with `ProductCard` and quick-add at resolved MOQ.
- **Catalog Screen (`CatalogScreen.kt`):**
  - Live search bar matching `nameAr`, `nameEn`, `sku`, `brand` (`CommerceViewModel.filteredProducts`).
  - Category `FilterChip` bar, bulk-discount-only filter (`bulkOnlyFilter`), and sort menu (`FEATURED`, `PRICE_LOW`, `PRICE_HIGH`).
  - Adaptive grid (`GridCells.Adaptive(minSize = 166.dp)`).
- **Product Detail Bottom Sheet (`SharedComponents.kt` — `ProductDetailSheet`):**
  - Unit vs. Carton pack-mode toggle (`PackMode.UNIT` vs. `PackMode.CARTON`, with automatic 4% carton bonus when `cartonSize > 1`).
  - Segment & volume tier pricing table (Retail, Wholesale, Business, plus bulk threshold discount indicator).
  - Stock-capped quantity stepper enforcing segment MOQ.
- **Offline/Empty Fallback Catalog (`SeedData.kt` + `CommerceViewModel.kt`):**
  - Falls back to 5 hardcoded categories, 10 SKUs, 4 delivery zones, and 3 promo coupons if Firestore streams are empty or fail.

### 2.3 Multi-Tier Pricing & Cart / Checkout
- **Client Pricing Engine (`PricingEngine.kt`):**
  - Resolves `moqRetail`, `moqWholesale`, `moqBusiness` (only verified Wholesale/Business accounts get B2B MOQs & B2B base prices; unverified fall back to retail).
  - Applies `ApprovedTier` rebates (`TIER_A` 3%, `TIER_B` 6%, `ENTERPRISE` 10%), bulk quantity discount (`bulkDiscountPercent` when `effectiveUnits >= bulkMinQty`), and 4% carton discount.
  - Coupon evaluation (`PERCENT` / `FIXED`, `minOrderAmount`, `allowedSegments`), loyalty point redemption (`100 pts = 10 EGP`, capped at 50% of post-coupon subtotal), and zone-based free-shipping progress bar.
- **Persistent Cart & Checkout (`CartCheckoutScreen.kt`, `CommerceRepository.kt`):**
  - Firestore cart stored at `users/{uid}/cart/{productId}_{packMode}`.
  - Address selector and `AddAddressDialog` saving to `users/{uid}/addresses/{id}` with zone linkage.
  - Delivery slot selection (3 static local strings).
  - Payment method selection: `COD`, `CARD`, `MOBILE_WALLET`, `CREDIT_INVOICE` (gated in UI and repository by verified B2B/Wholesale status and `availableCredit >= grandTotal`).
  - Atomic Firestore `WriteBatch` in `placeOrder()` creating `orders/{orderId}`, `orders/{orderId}/items/{itemId}`, deleting cart lines, decrementing `products/{id}.stockQty`, updating `users/{uid}.loyaltyPoints` & `creditUsed`, and appending `loyaltyLogs`.

### 2.4 Orders, Loyalty, Support & Embedded Ops Console
- **Orders History & Tracking Detail Sheet (`OrdersAccountScreen.kt`):**
  - 5-step order timeline (`PLACED` -> `CONFIRMED` -> `PACKED` -> `OUT_FOR_DELIVERY` -> `DELIVERED`, or `CANCELLED`).
  - Client cancellation for `PLACED`, `CONFIRMED`, or `PACKED` orders (`cancelOrder`).
  - One-tap Reorder (`reorderFromOrder`).
  - Dual fulfillment view: `LiveFleetMapCanvas` for `INTERNAL_FLEET` vs. tracking card + clipboard copy for `THIRD_PARTY_CARRIER`.
- **Loyalty & Support Tab (`OrdersAccountScreen.kt`):**
  - Loyalty points wallet card and top 5 `loyaltyLogs`.
  - Support ticket creation (`supportTickets/{ticketId}`) linked optionally to an `orderNumber`, with categories (`DAMAGED`, `MISSING_ITEM`, `INVOICE`, `DELIVERY`, `GENERAL`).
- **Profile & Segment Upgrade Request (`OrdersAccountScreen.kt`):**
  - Displays current segment, verification badge, and available B2B credit.
  - Allows user to submit `WHOLESALE` or `BUSINESS` switch request with `companyName` and `taxId` (sets `verificationStatus = PENDING_VERIFICATION`, `approvedTier = STANDARD`).
- **Embedded Operations Tab (`OrdersAccountScreen.kt` — `OperationsConsoleTab`):**
  - Visible only when `CommerceRepository.isBootstrappedAdmin()` returns `true` (hardcoded email check).
  - Allows seeding catalog, approving user tiers (`Wholesale A` / `B2B Ent.` with hardcoded credit limits `25000.0` / `75000.0`), advancing order status, stepping driver coordinates (`+45%` interpolation toward destination), toggling `INTERNAL_FLEET` / `THIRD_PARTY_CARRIER`, and resolving tickets.

---

## 3. Incomplete, Disconnected, or Simulated Features

| Feature | Location | Actual State in Repo | Gap / Disconnect |
| :--- | :--- | :--- | :--- |
| **Live Fleet Map Tracking** | `SharedComponents.kt:531-651`, `CommerceRepository.kt:591-595, 836-861` | Custom Jetpack Compose `Canvas` drawing a diagonal line on a static dark grid. Driver is hardcoded as `"Karim Hassan"` (`"01099887766"`). Warehouse coordinates hardcoded as `(30.0444, 31.2357)`. | No real Maps SDK, no real Driver GPS ingestion, no `Drivers` collection/table. Movement only happens when an admin clicks "Step Driver Location". |
| **Online Card & Mobile Wallet Payments** | `CartCheckoutScreen.kt:422-430`, `CommerceRepository.kt:544-549` | `CARD` / `MOBILE_WALLET` only collects a free-text `paymentReference` field (`OutlinedTextField`). | Entering **any** non-blank string for `CARD` immediately sets `paymentStatus = "PAID"` on the client (`CommerceRepository.kt:547`). No payment gateway SDK, server session, or webhook verification exists. |
| **Third-Party Carrier Integration** | `CommerceRepository.kt:555, 871` | Generates random `"AWB-xxxxxxx"` string and sets `"Aramex / Bosta Freight"`. | No real carrier API call, label generation, webhook tracking sync, or AWB status polling. |
| **Address Geocoding & Management** | `CommerceRepository.kt:405-410, 454-467` | Lat/lng are hardcoded per `zoneId` (`when (zoneId) ...`). `deleteAddress()` exists in `CommerceRepository.kt` but is **never called** in `CommerceViewModel` or UI. | Customers cannot edit or delete saved addresses from the UI, nor pick coordinates on a map. |
| **Order Cancellation Restocking & Credit Reversal** | `CommerceRepository.kt:699-716` | `cancelOrder()` only sets `status = "CANCELLED"` and `updatedAt`. | Does **not** restore decremented `products.stockQty`, does **not** refund `creditUsed` on `CREDIT_INVOICE` orders, and does **not** reverse `loyaltyPointsUsed`/`loyaltyPointsEarned`. |
| **Coupon Eligibility Check Flaw** | `PricingEngine.kt:120-125` | `isCouponEligible()` checks `coupon.allowedSegments.contains(profile?.segment)` without checking `verificationStatus == VERIFIED`. | A pending/unverified `WHOLESALE` user can apply B2B-only coupons (`B2BPRO`) immediately after requesting upgrade (`PricingEngineTest.kt:64` explicitly confirms this behavior). |
| **Wholesale/B2B KYC Verification** | `OrdersAccountScreen.kt:630-645` | Collects only `companyName` and `taxId` text strings. | No document upload (commercial register, tax card, VAT certificate), no rejection reason feedback, no custom credit limit UI in Admin (`25000`/`75000` hardcoded). |
| **Product Catalog CRUD & Multi-Image** | `Product.kt`, `SeedData.kt` | Products use local drawable key `imageKey` (`detergents`, `paper`, `tools`) mapped in `productDrawableForKey()`. `coil-compose` is commented out in `app/build.gradle.kts`. | No admin UI to create/edit/deactivate products, upload product images, manage variants, or update stock. |
| **Phone OTP Authentication** | `AuthScreen.kt` | Google Sign-In only. | Retail/wholesale merchants in Arabic-first markets without active Google accounts on device cannot sign in via Phone OTP. |
| **Admin RBAC & Dedicated Console** | `CommerceRepository.kt:52, 61-64`, `OrdersAccountScreen.kt:675-774` | Hardcoded single email `Alaa.moagaza2016@gmail.com` triggers an in-app tab in the customer app. | No Next.js Admin Portal, no multi-role RBAC (`OPERATIONS`, `INVENTORY`, `PURCHASING`, `FINANCE`, `DELIVERY`, `SUPPORT`), no audit logs. |
| **Local Offline Cache (Room)** | `app/build.gradle.kts:106-107, 138` | Room and KSP dependencies are commented out; relies solely on Firestore SDK default cache and `SeedData` fallback. | No Room entities/DAOs for offline catalog, draft orders, or structured sync. |

---

## 4. Missing Production Modules (Confirmed Absent)

The following backend/ERP and commerce modules are completely absent from the codebase:
1. **NestJS REST API & OpenAPI/Swagger Layer** (`backend/` does not exist).
2. **PostgreSQL + Prisma Schema & Migrations** (no relational database or financial ledgers).
3. **Multi-Warehouse, Inventory Ledger, Batches/FEFO & Atomic Reservations** (current app only decrements a flat `stockQty` integer on `products`).
4. **Suppliers, Purchase Orders (PO), and Goods Receipt Notes (GRN)**.
5. **Sequential Tax/VAT Invoices (`sales_invoices`), Tax Configuration (`tax_configs`), Credit Ledger & Collections**.
6. **Returns / RMA & Refund Processing (`returns`, `refunds`)**.
7. **Push Notifications (FCM) & Background Transactional Emails/SMS**.
8. **Immutable Audit Logs (`audit_logs`)**.
9. **Next.js Admin Dashboard** (separate web app).
10. **Separate Driver Android App & Live Location / POD / COD Collection Workflow**.
