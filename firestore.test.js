const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(ms = 5000) {
  return new Date(Date.now() - ms);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write catalog, users, or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      email: "a@b.com",
      phone: "01000000000",
      segment: "RETAIL",
      approvedTier: "STANDARD",
      verificationStatus: "VERIFIED",
      loyaltyPoints: 0,
      creditLimit: 0,
      creditUsed: 0,
      preferredLocale: "ar",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Users can create retail or pending wholesale profile, but cannot self-approve tier or mint points", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate(10000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Retail",
      email: "alice@example.com",
      phone: "01012345678",
      segment: "RETAIL",
      approvedTier: "STANDARD",
      verificationStatus: "VERIFIED",
      loyaltyPoints: 0,
      creditLimit: 0,
      creditUsed: 0,
      preferredLocale: "ar",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject self-verification for WHOLESALE
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      segment: "WHOLESALE",
      verificationStatus: "VERIFIED",
      approvedTier: "TIER_A",
      updatedAt: pastDate(1000),
    })
  );

  // Allow valid request to switch to PENDING_VERIFICATION
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      segment: "WHOLESALE",
      companyName: "Al-Noor Market",
      taxId: "TX-99012",
      verificationStatus: "PENDING_VERIFICATION",
      approvedTier: "STANDARD",
      updatedAt: pastDate(1000),
    })
  );

  // Reject unverified admin email spoof
  const spoofDb = testEnv.authenticatedContext("spoof_1", {
    email: "Alaa.moagaza2016@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());
});

test("Bootstrapped verified admin can seed catalog and approve wholesale accounts", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "Alaa.moagaza2016@gmail.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate(10000);
  await assertSucceeds(
    adminDb.collection("categories").doc("cat_detergents").set({
      nameAr: "منظفات",
      nameEn: "Detergents",
      iconKey: "detergents",
      sortOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("sku_gel_5l").set({
      sku: "CS-GEL-5L",
      nameAr: "جل غسيل مركز 5 لتر",
      nameEn: "Concentrated Laundry Gel 5L",
      descriptionAr: "تركيبة صناعية ومنزلية عالية التركيز",
      descriptionEn: "High-efficiency household & commercial detergent",
      categoryId: "cat_detergents",
      brand: "CleanPro",
      unitLabelAr: "جركن 5 لتر",
      unitLabelEn: "5L Jerrycan",
      cartonSize: 4,
      retailPrice: 185.0,
      wholesalePrice: 152.0,
      businessPrice: 140.0,
      bulkMinQty: 8,
      bulkDiscountPercent: 10,
      moqRetail: 1,
      moqWholesale: 4,
      moqBusiness: 8,
      stockQty: 120,
      imageKey: "detergents",
      featured: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin cannot create products
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("products").doc("fake_sku").set({
      sku: "FAKE",
      nameAr: "x",
      nameEn: "x",
      descriptionAr: "",
      descriptionEn: "",
      categoryId: "cat_detergents",
      brand: "x",
      unitLabelAr: "u",
      unitLabelEn: "u",
      cartonSize: 1,
      retailPrice: 1,
      wholesalePrice: 1,
      businessPrice: 1,
      bulkMinQty: 1,
      bulkDiscountPercent: 0,
      moqRetail: 1,
      moqWholesale: 1,
      moqBusiness: 1,
      stockQty: 10,
      imageKey: "detergents",
      featured: false,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cart, addresses, orders, order items, and cross-user isolation match Kotlin queries", async () => {
  const ts = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("products").doc("sku_gel_5l").set({
      sku: "CS-GEL-5L",
      nameAr: "جل غسيل",
      nameEn: "Laundry Gel",
      descriptionAr: "وصف",
      descriptionEn: "Desc",
      categoryId: "cat_detergents",
      brand: "CleanPro",
      unitLabelAr: "5 لتر",
      unitLabelEn: "5L",
      cartonSize: 4,
      retailPrice: 185.0,
      wholesalePrice: 152.0,
      businessPrice: 140.0,
      bulkMinQty: 8,
      bulkDiscountPercent: 10,
      moqRetail: 1,
      moqWholesale: 4,
      moqBusiness: 8,
      stockQty: 50,
      imageKey: "detergents",
      featured: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Owner cart & address
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("sku_gel_5l_UNIT").set({
      productId: "sku_gel_5l",
      quantity: 2,
      packMode: "UNIT",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );

  // Atomic batch order + order items + stock decrement
  const orderRef = aliceDb.collection("orders").doc("ord_1001");
  const itemRef = orderRef.collection("items").doc("item_1");
  const prodRef = aliceDb.collection("products").doc("sku_gel_5l");
  const batch = aliceDb.batch();
  batch.set(orderRef, {
    userId: ALICE_UID,
    orderNumber: "CS-1001",
    customerName: "Alice",
    customerSegment: "RETAIL",
    zoneId: "zone_cairo",
    zoneNameAr: "القاهرة الكبرى",
    zoneNameEn: "Greater Cairo",
    deliveryAddress: "12 Nasr City",
    deliverySlot: "Today 4PM - 8PM",
    fulfillmentType: "INTERNAL_FLEET",
    carrierName: "CleanSupply Fleet",
    trackingNumber: "FLT-1001",
    driverName: "Karim Hassan",
    driverPhone: "01099887766",
    driverLat: 30.0444,
    driverLng: 31.2357,
    destLat: 30.0626,
    destLng: 31.3296,
    paymentMethod: "COD",
    paymentStatus: "PENDING",
    paymentReference: "",
    status: "PLACED",
    subtotal: 370.0,
    tierDiscount: 0.0,
    couponCode: "",
    couponDiscount: 0.0,
    loyaltyDiscount: 0.0,
    loyaltyPointsUsed: 0,
    loyaltyPointsEarned: 37,
    shippingFee: 35.0,
    totalAmount: 405.0,
    itemCount: 1,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(itemRef, {
    userId: ALICE_UID,
    productId: "sku_gel_5l",
    nameAr: "جل غسيل",
    nameEn: "Laundry Gel",
    packMode: "UNIT",
    unitPrice: 185.0,
    quantity: 2,
    lineTotal: 370.0,
    createdAt: ts,
  });
  batch.update(prodRef, {
    stockQty: 48,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Filtered query succeeds, unfiltered fails for non-admin
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("ord_1001").get());

  // Owner can cancel PLACED order
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1001").update({
      status: "CANCELLED",
      updatedAt: pastDate(500),
    })
  );
});
