package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.Address
import com.example.data.model.ApprovedTier
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.CustomerSegment
import com.example.data.model.DeliveryZone
import com.example.data.model.FulfillmentType
import com.example.data.model.LoyaltyLog
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PackMode
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.SupportTicket
import com.example.data.model.TicketCategory
import com.example.data.model.TicketStatus
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.CheckoutSummary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToLong

class CommerceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val ADMIN_EMAIL = "Alaa.moagaza2016@gmail.com"
    }

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        require(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Firestore." }
        return uid
    }

    fun isBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    fun observeCategories(): Flow<List<Category>> =
        db.collection("categories")
            .snapshots()
            .map { snap ->
                snap.toObjects(Category::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }

    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .snapshots()
            .map { snap ->
                snap.toObjects(Product::class.java, ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
            }

    fun observeZones(): Flow<List<DeliveryZone>> =
        db.collection("zones")
            .snapshots()
            .map { snap ->
                snap.toObjects(DeliveryZone::class.java, ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
            }

    fun observeCoupons(): Flow<List<Coupon>> =
        db.collection("coupons")
            .snapshots()
            .map { snap ->
                snap.toObjects(Coupon::class.java, ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
            }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) else null
            }

    fun observeAddresses(userId: String): Flow<List<Address>> =
        db.collection("users").document(userId).collection("addresses")
            .snapshots()
            .map { snap ->
                snap.toObjects(Address::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.defaultAddress }
            }

    fun observeCart(userId: String): Flow<List<CartItem>> =
        db.collection("users").document(userId).collection("cart")
            .snapshots()
            .map { snap ->
                snap.toObjects(CartItem::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    fun observeLoyaltyLogs(userId: String): Flow<List<LoyaltyLog>> =
        db.collection("users").document(userId).collection("loyaltyLogs")
            .snapshots()
            .map { snap ->
                snap.toObjects(LoyaltyLog::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeOrders(userId: String, asAdmin: Boolean): Flow<List<Order>> {
        val query = if (asAdmin) {
            db.collection("orders")
        } else {
            db.collection("orders").whereEqualTo("userId", userId)
        }
        return query.snapshots().map { snap ->
            snap.toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    fun observeOrderItems(orderId: String, userId: String, asAdmin: Boolean): Flow<List<OrderItem>> {
        val col = db.collection("orders").document(orderId).collection("items")
        val query = if (asAdmin) col else col.whereEqualTo("userId", userId)
        return query.snapshots().map { snap ->
            snap.toObjects(OrderItem::class.java, ServerTimestampBehavior.ESTIMATE)
        }
    }

    fun observeSupportTickets(userId: String, asAdmin: Boolean): Flow<List<SupportTicket>> {
        val query = if (asAdmin) {
            db.collection("supportTickets")
        } else {
            db.collection("supportTickets").whereEqualTo("userId", userId)
        }
        return query.snapshots().map { snap ->
            snap.toObjects(SupportTicket::class.java, ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun ensureUserProfileAndSeedIfAdmin(
        userId: String,
        displayName: String,
        email: String,
        preferredLocale: String
    ): Result<Unit> {
        val userPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated UID mismatch." }
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "CleanSupply Customer" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.take(120),
                    "phone" to "01000000000",
                    "segment" to CustomerSegment.RETAIL.name,
                    "approvedTier" to ApprovedTier.STANDARD.name,
                    "verificationStatus" to VerificationStatus.VERIFIED.name,
                    "loyaltyPoints" to 0L,
                    "creditLimit" to 0.0,
                    "creditUsed" to 0.0,
                    "preferredLocale" to if (preferredLocale == "en") "en" else "ar",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            }

            // Create a starter default address if none exist
            val addressesSnap = userRef.collection("addresses").limit(1).get().await()
            if (addressesSnap.isEmpty) {
                val addrRef = userRef.collection("addresses").document("addr_primary")
                addrRef.set(
                    mapOf(
                        "label" to "Primary",
                        "recipientName" to displayName.trim().ifBlank { "CleanSupply Customer" }.take(80),
                        "phone" to "01012345678",
                        "zoneId" to "zone_cairo",
                        "streetDetails" to "15 Abbas El-Akkad St., Nasr City",
                        "buildingFloor" to "Floor 1",
                        "lat" to 30.0626,
                        "lng" to 31.3296,
                        "defaultAddress" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (isBootstrappedAdmin()) {
                val prodSnap = db.collection("products").limit(1).get().await()
                if (prodSnap.isEmpty) {
                    seedDefaultCatalog().getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultCatalog(): Result<Unit> {
        return try {
            requireUserId()
            val batch = db.batch()
            SeedData.defaultCategories.forEach { cat ->
                batch.set(
                    db.collection("categories").document(cat.id),
                    mapOf(
                        "nameAr" to cat.nameAr,
                        "nameEn" to cat.nameEn,
                        "iconKey" to cat.iconKey,
                        "sortOrder" to cat.sortOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            SeedData.defaultProducts.forEach { prod ->
                batch.set(
                    db.collection("products").document(prod.id),
                    mapOf(
                        "sku" to prod.sku,
                        "nameAr" to prod.nameAr,
                        "nameEn" to prod.nameEn,
                        "descriptionAr" to prod.descriptionAr,
                        "descriptionEn" to prod.descriptionEn,
                        "categoryId" to prod.categoryId,
                        "brand" to prod.brand,
                        "unitLabelAr" to prod.unitLabelAr,
                        "unitLabelEn" to prod.unitLabelEn,
                        "cartonSize" to prod.cartonSize,
                        "retailPrice" to prod.retailPrice,
                        "wholesalePrice" to prod.wholesalePrice,
                        "businessPrice" to prod.businessPrice,
                        "bulkMinQty" to prod.bulkMinQty,
                        "bulkDiscountPercent" to prod.bulkDiscountPercent,
                        "moqRetail" to prod.moqRetail,
                        "moqWholesale" to prod.moqWholesale,
                        "moqBusiness" to prod.moqBusiness,
                        "stockQty" to prod.stockQty,
                        "imageKey" to prod.imageKey,
                        "featured" to prod.featured,
                        "active" to prod.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            SeedData.defaultZones.forEach { z ->
                batch.set(
                    db.collection("zones").document(z.id),
                    mapOf(
                        "nameAr" to z.nameAr,
                        "nameEn" to z.nameEn,
                        "baseFee" to z.baseFee,
                        "freeShippingThreshold" to z.freeShippingThreshold,
                        "defaultFulfillmentType" to z.defaultFulfillmentType,
                        "estimatedHours" to z.estimatedHours,
                        "carrierDefaultName" to z.carrierDefaultName,
                        "active" to z.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            SeedData.defaultCoupons.forEach { c ->
                batch.set(
                    db.collection("coupons").document(c.id),
                    mapOf(
                        "code" to c.code,
                        "titleAr" to c.titleAr,
                        "titleEn" to c.titleEn,
                        "discountType" to c.discountType,
                        "discountValue" to c.discountValue,
                        "minOrderAmount" to c.minOrderAmount,
                        "allowedSegments" to c.allowedSegments,
                        "active" to c.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "catalog_seed")
            Result.failure(e)
        }
    }

    suspend fun updatePreferredLocale(userId: String, localeCode: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredLocale" to if (localeCode == "en") "en" else "ar",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestSegmentSwitch(
        userId: String,
        segment: CustomerSegment,
        companyName: String,
        taxId: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val newStatus = if (segment == CustomerSegment.RETAIL) {
                VerificationStatus.VERIFIED.name
            } else {
                VerificationStatus.PENDING_VERIFICATION.name
            }
            val updates = mutableMapOf<String, Any>(
                "segment" to segment.name,
                "verificationStatus" to newStatus,
                "approvedTier" to ApprovedTier.STANDARD.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (companyName.isNotBlank()) {
                updates["companyName"] = companyName.trim().take(120)
            }
            if (taxId.isNotBlank()) {
                updates["taxId"] = taxId.trim().take(50)
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAddress(
        userId: String,
        addressId: String?,
        label: String,
        recipientName: String,
        phone: String,
        zoneId: String,
        streetDetails: String,
        buildingFloor: String,
        defaultAddress: Boolean
    ): Result<Unit> {
        val docId = addressId?.takeIf { it.isNotBlank() } ?: "addr_${UUID.randomUUID().toString().take(8)}"
        val path = "users/$userId/addresses/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanLabel = label.trim().take(40).ifBlank { "Delivery" }
            val cleanRecipient = recipientName.trim().take(80).ifBlank { "Customer" }
            val cleanPhone = phone.trim().take(30).ifBlank { "01000000000" }
            val cleanStreet = streetDetails.trim().take(200).ifBlank { "Main Street" }
            val cleanBuilding = buildingFloor.trim().take(60)
            val latLng = when (zoneId) {
                "zone_giza_oct" -> 30.0131 to 31.2089
                "zone_alex_delta" -> 31.2001 to 29.9187
                "zone_upper_egypt" -> 27.1783 to 31.1859
                else -> 30.0626 to 31.3296
            }
            val ref = db.collection("users").document(uid).collection("addresses").document(docId)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "label" to cleanLabel,
                        "recipientName" to cleanRecipient,
                        "phone" to cleanPhone,
                        "zoneId" to zoneId,
                        "streetDetails" to cleanStreet,
                        "buildingFloor" to cleanBuilding,
                        "lat" to latLng.first,
                        "lng" to latLng.second,
                        "defaultAddress" to defaultAddress,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "label" to cleanLabel,
                        "recipientName" to cleanRecipient,
                        "phone" to cleanPhone,
                        "zoneId" to zoneId,
                        "streetDetails" to cleanStreet,
                        "buildingFloor" to cleanBuilding,
                        "lat" to latLng.first,
                        "lng" to latLng.second,
                        "defaultAddress" to defaultAddress,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> {
        val path = "users/$userId/addresses/$addressId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertCartItem(
        userId: String,
        productId: String,
        quantity: Long,
        packMode: PackMode
    ): Result<Unit> {
        val cartId = "${productId}_${packMode.name}"
        val path = "users/$userId/cart/$cartId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("cart").document(cartId)
            if (quantity <= 0L) {
                docRef.delete().await()
                return Result.success(Unit)
            }
            val safeQty = quantity.coerceIn(1L, 5000L)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "quantity" to safeQty,
                        "packMode" to packMode.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "productId" to productId,
                        "quantity" to safeQty,
                        "packMode" to packMode.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        userId: String,
        profile: UserProfile,
        address: Address,
        zone: DeliveryZone,
        deliverySlot: String,
        paymentMethod: PaymentMethod,
        paymentReference: String,
        couponCode: String,
        summary: CheckoutSummary
    ): Result<String> {
        val orderId = "ord_${UUID.randomUUID().toString().take(10)}"
        val orderPath = "orders/$orderId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(summary.lines.isNotEmpty()) { "Cart cannot be empty." }
            require(!summary.hasMoqViolation) { "Minimum order quantity not met." }

            if (paymentMethod == PaymentMethod.CREDIT_INVOICE) {
                require(
                    profile.segmentEnum() != CustomerSegment.RETAIL &&
                        profile.verificationEnum() == VerificationStatus.VERIFIED &&
                        summary.grandTotal <= profile.availableCredit
                ) {
                    "Credit invoice requires verified wholesale/business credit limit."
                }
            }

            val paymentStatus = when (paymentMethod) {
                PaymentMethod.COD -> PaymentStatus.PENDING.name
                PaymentMethod.CREDIT_INVOICE -> PaymentStatus.INVOICED.name
                PaymentMethod.CARD -> if (paymentReference.isNotBlank()) PaymentStatus.PAID.name else PaymentStatus.PENDING_VERIFICATION.name
                PaymentMethod.MOBILE_WALLET -> PaymentStatus.PENDING_VERIFICATION.name
            }

            val fulfillment = zone.fulfillmentEnum()
            val trackingNo = if (fulfillment == FulfillmentType.INTERNAL_FLEET) {
                "FLT-${(100000..999999).random()}"
            } else {
                "AWB-${(1000000..9999999).random()}"
            }
            val orderNum = "CS-${(10000..99999).random()}"

            // Pre-check which product documents exist in Firestore to safely decrement stock
            val existingProductStocks = mutableMapOf<String, Long>()
            for (line in summary.lines) {
                val pSnap = db.collection("products").document(line.product.id).get().await()
                if (pSnap.exists()) {
                    val currentStock = pSnap.getLong("stockQty") ?: line.product.stockQty
                    val remaining = (currentStock - line.effectiveUnits).coerceAtLeast(0L)
                    existingProductStocks[line.product.id] = remaining
                }
            }

            val batch = db.batch()
            val orderRef = db.collection("orders").document(orderId)

            val warehouseLat = 30.0444
            val warehouseLng = 31.2357

            batch.set(
                orderRef,
                mapOf(
                    "userId" to uid,
                    "orderNumber" to orderNum,
                    "customerName" to profile.displayName.take(80),
                    "customerSegment" to profile.segment,
                    "zoneId" to zone.id,
                    "zoneNameAr" to zone.nameAr,
                    "zoneNameEn" to zone.nameEn,
                    "deliveryAddress" to "${address.streetDetails}, ${address.buildingFloor}".trim(',', ' ').take(240),
                    "deliverySlot" to deliverySlot.take(80),
                    "fulfillmentType" to fulfillment.name,
                    "carrierName" to zone.carrierDefaultName,
                    "trackingNumber" to trackingNo,
                    "driverName" to if (fulfillment == FulfillmentType.INTERNAL_FLEET) "Karim Hassan" else "",
                    "driverPhone" to if (fulfillment == FulfillmentType.INTERNAL_FLEET) "01099887766" else "",
                    "driverLat" to warehouseLat,
                    "driverLng" to warehouseLng,
                    "destLat" to address.lat,
                    "destLng" to address.lng,
                    "paymentMethod" to paymentMethod.name,
                    "paymentStatus" to paymentStatus,
                    "paymentReference" to paymentReference.trim().take(80),
                    "status" to OrderStatus.PLACED.name,
                    "subtotal" to summary.retailSubtotal,
                    "tierDiscount" to summary.tierAndBulkSavings,
                    "couponCode" to couponCode.trim().take(30),
                    "couponDiscount" to summary.couponDiscount,
                    "loyaltyDiscount" to summary.loyaltyDiscount,
                    "loyaltyPointsUsed" to summary.loyaltyPointsUsed,
                    "loyaltyPointsEarned" to summary.pointsEarned,
                    "shippingFee" to summary.shippingFee,
                    "totalAmount" to summary.grandTotal,
                    "itemCount" to summary.lines.size.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            summary.lines.forEachIndexed { idx, line ->
                val itemRef = orderRef.collection("items").document("item_${idx + 1}_${line.product.id}")
                batch.set(
                    itemRef,
                    mapOf(
                        "userId" to uid,
                        "productId" to line.product.id,
                        "nameAr" to line.product.nameAr,
                        "nameEn" to line.product.nameEn,
                        "packMode" to line.cartItem.packMode,
                        "unitPrice" to line.effectivePackPrice,
                        "quantity" to line.cartItem.quantity,
                        "lineTotal" to line.lineEffectiveTotal,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.delete(db.collection("users").document(uid).collection("cart").document(line.cartItem.id))
            }

            existingProductStocks.forEach { (prodId, newStock) ->
                batch.update(
                    db.collection("products").document(prodId),
                    mapOf(
                        "stockQty" to newStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val updatedPoints = (profile.loyaltyPoints - summary.loyaltyPointsUsed + summary.pointsEarned)
                .coerceAtLeast(0L)
            val updatedCreditUsed = if (paymentMethod == PaymentMethod.CREDIT_INVOICE) {
                ((profile.creditUsed + summary.grandTotal) * 100.0).roundToLong() / 100.0
            } else {
                profile.creditUsed
            }

            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "loyaltyPoints" to updatedPoints,
                    "creditUsed" to updatedCreditUsed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (summary.pointsEarned > 0L) {
                val earnLogRef = db.collection("users").document(uid).collection("loyaltyLogs").document("log_earn_$orderId")
                batch.set(
                    earnLogRef,
                    mapOf(
                        "titleAr" to "نقاط مكتسبة عن الطلب $orderNum",
                        "titleEn" to "Earned from order $orderNum",
                        "pointsDelta" to summary.pointsEarned,
                        "orderId" to orderId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (summary.loyaltyPointsUsed > 0L) {
                val burnLogRef = db.collection("users").document(uid).collection("loyaltyLogs").document("log_burn_$orderId")
                batch.set(
                    burnLogRef,
                    mapOf(
                        "titleAr" to "استبدال نقاط في الطلب $orderNum",
                        "titleEn" to "Redeemed on order $orderNum",
                        "pointsDelta" to -summary.loyaltyPointsUsed,
                        "orderId" to orderId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reorderFromOrder(userId: String, orderId: String): Result<Int> {
        val path = "orders/$orderId/items"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val snap = db.collection("orders").document(orderId).collection("items")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val items = snap.toObjects(OrderItem::class.java, ServerTimestampBehavior.ESTIMATE)
            for (item in items) {
                val mode = if (item.packMode == PackMode.CARTON.name) PackMode.CARTON else PackMode.UNIT
                upsertCartItem(uid, item.productId, item.quantity, mode).getOrThrow()
            }
            Result.success(items.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createSupportTicket(
        userId: String,
        orderId: String,
        category: TicketCategory,
        subject: String,
        message: String
    ): Result<Unit> {
        val ticketId = "tkt_${UUID.randomUUID().toString().take(8)}"
        val path = "supportTickets/$ticketId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("supportTickets").document(ticketId).set(
                mapOf(
                    "userId" to uid,
                    "orderId" to orderId.trim().take(128),
                    "category" to category.name,
                    "subject" to subject.trim().take(120),
                    "message" to message.trim().take(1000),
                    "status" to TicketStatus.OPEN.name,
                    "adminReply" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Admin / Operations Console Actions ---

    suspend fun approveCustomerAccount(
        targetUserId: String,
        segment: CustomerSegment,
        tier: ApprovedTier,
        creditLimit: Double
    ): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "segment" to segment.name,
                    "approvedTier" to tier.name,
                    "verificationStatus" to VerificationStatus.VERIFIED.name,
                    "creditLimit" to creditLimit.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStatus(order: Order): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            val next = when (order.statusEnum()) {
                OrderStatus.PLACED -> OrderStatus.CONFIRMED
                OrderStatus.CONFIRMED -> OrderStatus.PACKED
                OrderStatus.PACKED -> OrderStatus.OUT_FOR_DELIVERY
                OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.DELIVERED
                else -> order.statusEnum()
            }
            val nextPayStatus = if (next == OrderStatus.DELIVERED && order.paymentMethodEnum() == PaymentMethod.COD) {
                PaymentStatus.PAID.name
            } else {
                order.paymentStatus
            }
            db.collection("orders").document(order.id).update(
                mapOf(
                    "status" to next.name,
                    "paymentStatus" to nextPayStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun stepDriverTowardsDestination(order: Order): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            val newLat = order.driverLat + (order.destLat - order.driverLat) * 0.45
            val newLng = order.driverLng + (order.destLng - order.driverLng) * 0.45
            db.collection("orders").document(order.id).update(
                mapOf(
                    "driverLat" to newLat,
                    "driverLng" to newLng,
                    "status" to if (order.statusEnum() == OrderStatus.PLACED || order.statusEnum() == OrderStatus.CONFIRMED || order.statusEnum() == OrderStatus.PACKED) {
                        OrderStatus.OUT_FOR_DELIVERY.name
                    } else {
                        order.status
                    },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFulfillmentProvider(order: Order): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            val toCarrier = order.fulfillmentEnum() == FulfillmentType.INTERNAL_FLEET
            db.collection("orders").document(order.id).update(
                mapOf(
                    "fulfillmentType" to if (toCarrier) FulfillmentType.THIRD_PARTY_CARRIER.name else FulfillmentType.INTERNAL_FLEET.name,
                    "carrierName" to if (toCarrier) "Aramex / Bosta Freight" else "CleanSupply Express Fleet",
                    "driverName" to if (toCarrier) "" else "Karim Hassan",
                    "driverPhone" to if (toCarrier) "" else "01099887766",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveSupportTicket(ticketId: String, reply: String): Result<Unit> {
        val path = "supportTickets/$ticketId"
        return try {
            requireUserId()
            db.collection("supportTickets").document(ticketId).update(
                mapOf(
                    "status" to TicketStatus.RESOLVED.name,
                    "adminReply" to reply.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
