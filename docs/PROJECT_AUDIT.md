# CleanSupply — Full Project Audit (Phase Zero)

**Audit Date:** 2026-11-18  
**Project Name:** `CleanSupply` (`كلين سبلاي`)  
**Android Application ID:** `com.aistudio.cleansupply.kxmpzq` (`namespace = "com.example"`)  
**Build System:** Gradle Kotlin DSL (AGP `9.1.1`, Kotlin `2.4.21`, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`)

---

## 1. Repository Structure

```text
/
├── .env.example                          # Template for Secrets Gradle Plugin (GEMINI_API_KEY commented out)
├── .gitignore
├── build.gradle.kts                      # Root Gradle plugins (AGP, Compose, KSP, Secrets, Google Services)
├── settings.gradle.kts                   # rootProject.name = "CleanSupply", single module include(":app")
├── gradle.properties                     # JVM -Xmx4g, nonTransitiveRClass, config cache, in-process Kotlin
├── gradle/libs.versions.toml             # Version catalog (includes pre-configured Room, Retrofit, OkHttp, Moshi, Coil, Firebase)
├── metadata.json                         # AI Studio platform metadata
├── firebase.json                         # Firestore emulator (127.0.0.1:8085) & named database binding
├── firebase-blueprint.json               # 11-entity JSON schema for Firestore collections
├── firestore.rules                       # 487-line Firestore security & field-validation rules
├── firestore.test.js                     # 4 Node.js @firebase/rules-unit-testing suites
├── debug.keystore.base64                 # Environment debug keystore (do not modify)
└── app/
    ├── build.gradle.kts                  # :app dependencies, signingConfigs, secrets plugin
    ├── google-services.json              # Provisioned Firebase project config (mohamed-hussein--law-firm)
    ├── proguard-rules.pro                # Default ProGuard template (isMinifyEnabled = false)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml       # INTERNET + ACCESS_NETWORK_STATE, single MainActivity launcher
        │   ├── java/com/example/
        │   │   ├── MainActivity.kt                        # Entry point, Auth state gate, NavigationBar/Rail shell
        │   │   ├── data/model/Models.kt                   # 11 Firestore-annotated models + 11 enums
        │   │   ├── data/firestore/CommerceRepository.kt   # 906-line Firestore + Auth repository (customer & admin)
        │   │   ├── data/firestore/FirestoreErrorHandler.kt# Structured JSON logger for Firestore errors
        │   │   ├── data/firestore/SeedData.kt             # Hardcoded 5 categories, 10 products, 4 zones, 3 coupons
        │   │   ├── domain/PricingEngine.kt                # Client-side tier/bulk/carton/coupon/loyalty calculator
        │   │   ├── ui/i18n/AppStrings.kt                  # AR/EN strings + DataStore LanguagePreferences
        │   │   ├── ui/theme/{Color.kt,Theme.kt,Type.kt}   # Material 3 palette, RTL/LTR Theme, Cairo + IBM Plex fonts
        │   │   ├── ui/viewmodel/CommerceViewModel.kt      # 465-line single ViewModel driving all screens
        │   │   ├── ui/components/SharedComponents.kt      # ProductCard, ProductDetailSheet, LiveFleetMapCanvas, badges
        │   │   └── ui/screens/
        │   │       ├── AuthScreen.kt                      # Google Sign-In via CredentialManager
        │   │       ├── HomeScreen.kt                      # Storefront hero, quick reorder, categories, featured grid
        │   │       ├── CatalogScreen.kt                   # Search, category chips, bulk filter, sort, product grid
        │   │       ├── CartCheckoutScreen.kt              # Cart lines, coupons, loyalty, addresses, payment, checkout
        │   │       └── OrdersAccountScreen.kt             # Orders, tracking sheet, loyalty/support, profile, ops console
        │   └── res/
        │       ├── drawable/                              # ic_cleansupply_logo.png, hero & category JPGs, adaptive XMLs
        │       ├── font/                                  # cairo.ttf, ibm_plex_sans_arabic.ttf
        │       ├── mipmap-*/                              # Adaptive icon resources
        │       └── values/                                # strings.xml, themes.xml, colors.xml, firebase_applet_config.xml
        ├── test/java/com/example/
        │   ├── ExampleUnitTest.kt                         # 1 template test
        │   ├── ExampleRobolectricTest.kt                  # 1 Robolectric resource test ("CleanSupply")
        │   └── PricingEngineTest.kt                       # 3 pricing/MOQ/coupon unit tests
        └── androidTest/java/com/example/
            └── ExampleInstrumentedTest.kt                 # 1 template instrumented test (not runnable here)
```

---

## 2. Android Modules & Dependencies

- **Modules:** Single Android module `:app` defined in `settings.gradle.kts`. No backend (`/backend`), admin web (`/admin-web`), or separate driver module exists yet.
- **Active Dependencies (`app/build.gradle.kts`):**
  - Jetpack Compose BOM `2024.09.00`, Material 3, Material Icons Core & Extended
  - Navigation Compose `2.8.9`
  - Lifecycle Runtime / ViewModel Compose `2.8.7`
  - DataStore Preferences `1.1.7`
  - Firebase BOM `35.1.0` (`firebase-firestore`, `firebase-auth`)
  - Jetpack Credential Manager `1.5.0` (`androidx-credentials`, `androidx-credentials-play-services`) + `googleid` `1.1.1`
  - Kotlin Coroutines `1.10.2`
  - Unit testing: JUnit `4.13.2`, Robolectric `4.16.1`, `kotlinx-coroutines-test`, Compose UI test artifact
- **Pre-Declared Unused Dependencies (in `libs.versions.toml` & commented in `app/build.gradle.kts`):**
  - Networking: `retrofit` (`2.12.0`), `converter-moshi` (`2.12.0`), `okhttp` (`4.10.0`), `logging-interceptor` (`4.10.0`), `moshi-kotlin` & `moshi-kotlin-codegen` (`1.15.2`)
  - Local DB: `androidx-room-runtime`, `androidx-room-ktx`, `androidx-room-compiler` (`2.7.0`) + KSP plugin (`2.3.5`)
  - Media/Location/Permissions: `coil-compose` (`2.7.0`), `play-services-location` (`21.3.0`), `accompanist-permissions` (`0.37.3`), CameraX `1.5.0`
  - Unused Firebase: `firebase-ai`, `firebase-appcheck-recaptcha`, `firebase-appcheck-debug`

---

## 3. Screens, Navigation Routes, ViewModels, Repositories & Models

### Navigation Routes (`MainActivity.kt`)
1. **Unauthenticated Gate:** `AuthScreen` (`MainActivity.kt:137-141`) rendered when `FirebaseAuth.currentUser == null`.
2. **Authenticated Shell (`AuthenticatedMainShell`, `MainActivity.kt:173`):**
   - Responsive `NavigationBar` (`< 600.dp`) or `NavigationRail` (`>= 600.dp`) with `NavHost` (`maxWidth = 900.dp`):
   - `ROUTE_HOME = "home"` -> `HomeScreen`
   - `ROUTE_CATALOG = "catalog"` -> `CatalogScreen`
   - `ROUTE_CART = "cart"` -> `CartCheckoutScreen`
   - `ROUTE_ACCOUNT = "account"` -> `OrdersAccountScreen` (contains 3 customer tabs: Orders, Loyalty & Support, Profile + 1 conditional Ops Console tab `idx = 3` when `isAdmin == true`)
   - Modal overlays: `ProductDetailSheet` (`MainActivity.kt:448-458`), `OrderTrackingDetailSheet` (`OrdersAccountScreen.kt:186-203`), `AddAddressDialog` (`CartCheckoutScreen.kt:490-499`), Support Ticket `AlertDialog` (`OrdersAccountScreen.kt:522-564`).

### ViewModels & State
- Single ViewModel: `CommerceViewModel` (`ui/viewmodel/CommerceViewModel.kt`, 465 lines), constructed directly via `viewModelFactory` inside `MainActivity.kt:146-157`.
- Exposes `StateFlow` streams for `categories`, `products`, `zones`, `coupons`, `userProfileState`, `addresses`, `cartItems`, `loyaltyLogs`, `ordersState`, `selectedOrderItems`, `supportTickets`, `adminUsers`, `filteredProducts`, `checkoutSummary`, `checkoutDraft`, `language`, `snackbarMessage`, and `isSubmittingOrder`.

### Repositories, Services & Domain Logic
- `CommerceRepository` (`data/firestore/CommerceRepository.kt`, 906 lines): Single class handling all Firestore reads/writes, user provisioning, catalog seeding, cart CRUD, order creation batches, and admin operations.
- `FirestoreErrorHandler.kt`: Formats Firestore exceptions into structured JSON logs.
- `SeedData.kt`: Static catalog/zone/coupon fixtures used both for admin Firestore seeding and silent fallback when streams return empty or error (`CommerceViewModel.kt:122-140`).
- `PricingEngine` (`domain/PricingEngine.kt`, 195 lines): Pure Kotlin object computing segment prices, tier rebates, bulk discounts, 4% carton bonus, MOQ checks, coupon discounts, loyalty redemptions/earnings, and shipping fees.

### Data Models (`data/model/Models.kt`)
- Enums: `CustomerSegment`, `ApprovedTier`, `VerificationStatus`, `FulfillmentType`, `PaymentMethod`, `PaymentStatus`, `OrderStatus`, `PackMode`, `DiscountType`, `TicketCategory`, `TicketStatus`.
- Models: `Category`, `Product`, `DeliveryZone`, `Coupon`, `UserProfile`, `Address`, `CartItem`, `LoyaltyLog`, `Order`, `OrderItem`, `SupportTicket`.

---

## 4. Firebase Authentication & Firestore Usage

- **Firebase Configuration:**
  - `app/google-services.json`: Bound to GCP/Firebase project `mohamed-hussein--law-firm` (`project_number: 692170704872`), package `com.aistudio.cleansupply.kxmpzq`, debug SHA-1 `33e7b10d052379954f03e81215adc377a4f53111`.
  - `app/src/main/res/values/firebase_applet_config.xml`: Named Firestore database ID `ai-studio-android-cleansup-8e5230b4-40c4-4cec-b036-8043a42109bb`.
- **Firebase Auth:**
  - Google Sign-In only via Credential Manager + `GoogleAuthProvider.getCredential(token, null)`.
  - No Phone OTP, no custom claims, no token verification backend.
- **Firestore Usage:**
  - Named instance `FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))`.
  - 11 active paths matching `firebase-blueprint.json` and `firestore.rules`: `categories`, `products`, `zones`, `coupons`, `users/{uid}`, `users/{uid}/addresses`, `users/{uid}/cart`, `users/{uid}/loyaltyLogs`, `orders`, `orders/{oid}/items`, `supportTickets` (plus `/admins/{adminId}` rule lookup).
  - Note: Firebase App Check (`firebase-appcheck-*`) is commented out in `app/build.gradle.kts`.

---

## 5. Architectural Problems & Code Duplication

1. **Client-Authoritative Commerce & ERP State:** Prices, discounts, MOQ enforcement, stock decrements, B2B credit usage, loyalty points balances, order numbers, and payment status are all computed and persisted directly by the Android client (`PricingEngine` + `CommerceRepository.placeOrder`).
2. **God Repository & God ViewModel:** `CommerceRepository` (906 lines) and `CommerceViewModel` (465 lines) mix customer shopping, address management, checkout, support tickets, seeding, and admin operations.
3. **Silent Mock Fallbacks Masking Backend Failures:** `CommerceViewModel.kt:122-140` silently swaps empty or failed Firestore streams for `SeedData.default*`, hiding real network/permission errors from users and risking checkout against non-existent products.
4. **Hardcoded Placeholder Operational Data:**
   - Default address seeded automatically in `CommerceRepository.kt:202-217` (`"15 Abbas El-Akkad St., Nasr City"`, `"01012345678"`).
   - Hardcoded driver `"Karim Hassan"` (`"01099887766"`) and warehouse lat/lng `(30.0444, 31.2357)` in `CommerceRepository.kt:573-594, 872-873` and `SharedComponents.kt:541-542`.
   - Hardcoded zone coordinate lookup table in `CommerceRepository.kt:405-410`.
5. **Duplicated Pricing Logic in UI:** `ProductDetailSheet` (`SharedComponents.kt:323-328`) duplicates carton-bonus and pack-price math (`0.96` multiplier) instead of delegating completely to `PricingEngine`.
6. **Mixed Localization Pattern:** Most copy lives in `AppStrings.kt`, while some labels in `OrdersAccountScreen.kt` (e.g., `"Wholesale A"`, `"B2B Ent."`, `"Resolve"`, `"Order #"`, `"Resolved by Operations Team"`) and `CartCheckoutScreen.kt` (hardcoded slot strings and `"Remove"`/`"Increase"`/`"Decrease"` contentDescriptions) bypass `AppStrings.kt` and `strings.xml`.
7. **Unused/Dead Code:** `CommerceRepository.deleteAddress()` (`lines 454-467`) has no ViewModel or UI caller.
8. **Direct Dependency Instantiation:** `MainActivity.kt:150` instantiates `CommerceRepository(app)` inline, making repository mocking/testing difficult.
9. **Test Gaps:** Zero tests cover `CommerceRepository`, `CommerceViewModel`, `AuthScreen`, or Compose UI navigation/screens.

---

## 6. Verification Baseline (Executed 2026-11-18)

| Check | Command / Tool | Result | Details |
| :--- | :--- | :--- | :--- |
| **Android Build** | `compile_applet` | **PASS** | Compiled debug APK cleanly in ~16s |
| **Android JVM & Robolectric Tests** | `gradle :app:testDebugUnitTest --console=plain` | **PASS (5/5)** | `ExampleUnitTest`: 1/1; `ExampleRobolectricTest`: 1/1 (`"CleanSupply"`); `PricingEngineTest`: 3/3 |
| **Firestore Rules Tests (Cold Host)** | `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js` | **FAIL (0/4)** | Failed with `TypeError: fetch failed` because no emulator was running on `8085` |
| **Firestore Rules Tests (With Emulator)** | `firebase emulators:exec --only firestore --project demo-no-project "FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 node --test firestore.test.js"` | **PASS (4/4)** | All 4 test suites passed (`exit code 0`, duration `4587ms`) |
| **Instrumented Tests** | `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` | **NOT RUN** | No Android emulator/ADB in cloud build environment |
