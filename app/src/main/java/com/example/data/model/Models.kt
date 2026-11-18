package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

inline fun <reified T : Enum<T>> safeEnumValue(raw: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: default

enum class CustomerSegment { RETAIL, WHOLESALE, BUSINESS }
enum class ApprovedTier { STANDARD, TIER_A, TIER_B, ENTERPRISE }
enum class VerificationStatus { VERIFIED, PENDING_VERIFICATION, REJECTED }
enum class FulfillmentType { INTERNAL_FLEET, THIRD_PARTY_CARRIER }
enum class PaymentMethod { COD, CREDIT_INVOICE, CARD, MOBILE_WALLET }
enum class PaymentStatus { PENDING, PENDING_VERIFICATION, PAID, INVOICED }
enum class OrderStatus { PLACED, CONFIRMED, PACKED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }
enum class PackMode { UNIT, CARTON }
enum class DiscountType { PERCENT, FIXED }
enum class TicketCategory { DAMAGED, MISSING_ITEM, INVOICE, DELIVERY, GENERAL }
enum class TicketStatus { OPEN, IN_REVIEW, RESOLVED }

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val iconKey: String = "detergents",
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val sku: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val categoryId: String = "",
    val brand: String = "",
    val unitLabelAr: String = "",
    val unitLabelEn: String = "",
    val cartonSize: Long = 1L,
    val retailPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val businessPrice: Double = 0.0,
    val bulkMinQty: Long = 10L,
    val bulkDiscountPercent: Long = 0L,
    val moqRetail: Long = 1L,
    val moqWholesale: Long = 1L,
    val moqBusiness: Long = 1L,
    val stockQty: Long = 0L,
    val imageKey: String = "detergents",
    val featured: Boolean = false,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
    fun localizedDesc(isArabic: Boolean): String = if (isArabic) descriptionAr else descriptionEn
    fun localizedUnit(isArabic: Boolean): String = if (isArabic) unitLabelAr else unitLabelEn
}

@Keep
@IgnoreExtraProperties
data class DeliveryZone(
    @DocumentId val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val baseFee: Double = 0.0,
    val freeShippingThreshold: Double = 0.0,
    val defaultFulfillmentType: String = FulfillmentType.INTERNAL_FLEET.name,
    val estimatedHours: Long = 24L,
    val carrierDefaultName: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
    fun fulfillmentEnum(): FulfillmentType = safeEnumValue(defaultFulfillmentType, FulfillmentType.INTERNAL_FLEET)
}

@Keep
@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val discountType: String = DiscountType.PERCENT.name,
    val discountValue: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val allowedSegments: List<String> = listOf("RETAIL", "WHOLESALE", "BUSINESS"),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedTitle(isArabic: Boolean): String = if (isArabic) titleAr else titleEn
    fun typeEnum(): DiscountType = safeEnumValue(discountType, DiscountType.PERCENT)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val companyName: String? = null,
    val taxId: String? = null,
    val segment: String = CustomerSegment.RETAIL.name,
    val approvedTier: String = ApprovedTier.STANDARD.name,
    val verificationStatus: String = VerificationStatus.VERIFIED.name,
    val loyaltyPoints: Long = 0L,
    val creditLimit: Double = 0.0,
    val creditUsed: Double = 0.0,
    val preferredLocale: String = "ar",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun segmentEnum(): CustomerSegment = safeEnumValue(segment, CustomerSegment.RETAIL)
    fun tierEnum(): ApprovedTier = safeEnumValue(approvedTier, ApprovedTier.STANDARD)
    fun verificationEnum(): VerificationStatus = safeEnumValue(verificationStatus, VerificationStatus.VERIFIED)
    val availableCredit: Double get() = (creditLimit - creditUsed).coerceAtLeast(0.0)
}

@Keep
@IgnoreExtraProperties
data class Address(
    @DocumentId val id: String = "",
    val label: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val zoneId: String = "",
    val streetDetails: String = "",
    val buildingFloor: String = "",
    val lat: Double = 30.0444,
    val lng: Double = 31.2357,
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val quantity: Long = 1L,
    val packMode: String = PackMode.UNIT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun packModeEnum(): PackMode = safeEnumValue(packMode, PackMode.UNIT)
}

@Keep
@IgnoreExtraProperties
data class LoyaltyLog(
    @DocumentId val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val pointsDelta: Long = 0L,
    val orderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val customerSegment: String = CustomerSegment.RETAIL.name,
    val zoneId: String = "",
    val zoneNameAr: String = "",
    val zoneNameEn: String = "",
    val deliveryAddress: String = "",
    val deliverySlot: String = "",
    val fulfillmentType: String = FulfillmentType.INTERNAL_FLEET.name,
    val carrierName: String = "",
    val trackingNumber: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val driverLat: Double = 30.0444,
    val driverLng: Double = 31.2357,
    val destLat: Double = 30.0626,
    val destLng: Double = 31.3296,
    val paymentMethod: String = PaymentMethod.COD.name,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val paymentReference: String = "",
    val status: String = OrderStatus.PLACED.name,
    val subtotal: Double = 0.0,
    val tierDiscount: Double = 0.0,
    val couponCode: String = "",
    val couponDiscount: Double = 0.0,
    val loyaltyDiscount: Double = 0.0,
    val loyaltyPointsUsed: Long = 0L,
    val loyaltyPointsEarned: Long = 0L,
    val shippingFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val itemCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): OrderStatus = safeEnumValue(status, OrderStatus.PLACED)
    fun fulfillmentEnum(): FulfillmentType = safeEnumValue(fulfillmentType, FulfillmentType.INTERNAL_FLEET)
    fun paymentMethodEnum(): PaymentMethod = safeEnumValue(paymentMethod, PaymentMethod.COD)
    fun paymentStatusEnum(): PaymentStatus = safeEnumValue(paymentStatus, PaymentStatus.PENDING)
}

@Keep
@IgnoreExtraProperties
data class OrderItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val packMode: String = PackMode.UNIT.name,
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val lineTotal: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun localizedName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val category: String = TicketCategory.GENERAL.name,
    val subject: String = "",
    val message: String = "",
    val status: String = TicketStatus.OPEN.name,
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): TicketStatus = safeEnumValue(status, TicketStatus.OPEN)
    fun categoryEnum(): TicketCategory = safeEnumValue(category, TicketCategory.GENERAL)
}
