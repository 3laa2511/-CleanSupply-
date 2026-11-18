package com.example.domain

import com.example.data.model.ApprovedTier
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.CustomerSegment
import com.example.data.model.DeliveryZone
import com.example.data.model.DiscountType
import com.example.data.model.PackMode
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import kotlin.math.roundToLong

data class PricedCartLine(
    val cartItem: CartItem,
    val product: Product,
    val baseRetailUnitPrice: Double,
    val effectivePackPrice: Double,
    val lineRetailTotal: Double,
    val lineEffectiveTotal: Double,
    val lineSavings: Double,
    val effectiveUnits: Long,
    val requiredMoqUnits: Long,
    val meetsMoq: Boolean,
    val bulkDiscountApplied: Boolean
)

data class CheckoutSummary(
    val lines: List<PricedCartLine>,
    val retailSubtotal: Double,
    val effectiveSubtotal: Double,
    val tierAndBulkSavings: Double,
    val couponDiscount: Double,
    val loyaltyPointsUsed: Long,
    val loyaltyDiscount: Double,
    val shippingFee: Double,
    val freeShippingProgress: Float,
    val amountToFreeShipping: Double,
    val grandTotal: Double,
    val pointsEarned: Long,
    val hasMoqViolation: Boolean
)

object PricingEngine {
    const val POINTS_TO_CURRENCY_DIVISOR = 10.0 // 100 pts = 10 EGP

    fun resolveMoqUnits(product: Product, profile: UserProfile?): Long {
        val seg = profile?.segmentEnum() ?: CustomerSegment.RETAIL
        val verified = profile?.verificationEnum() == VerificationStatus.VERIFIED
        return when {
            seg == CustomerSegment.BUSINESS && verified -> product.moqBusiness.coerceAtLeast(1L)
            seg == CustomerSegment.WHOLESALE && verified -> product.moqWholesale.coerceAtLeast(1L)
            else -> product.moqRetail.coerceAtLeast(1L)
        }
    }

    fun resolveTierBaseUnitPrice(product: Product, profile: UserProfile?): Double {
        val seg = profile?.segmentEnum() ?: CustomerSegment.RETAIL
        val verified = profile?.verificationEnum() == VerificationStatus.VERIFIED
        val base = when {
            seg == CustomerSegment.BUSINESS && verified -> product.businessPrice
            seg == CustomerSegment.WHOLESALE && verified -> product.wholesalePrice
            else -> product.retailPrice
        }
        val tierRebate = when (profile?.tierEnum()) {
            ApprovedTier.TIER_A -> if (verified) 0.03 else 0.0
            ApprovedTier.TIER_B -> if (verified) 0.06 else 0.0
            ApprovedTier.ENTERPRISE -> if (verified) 0.10 else 0.0
            else -> 0.0
        }
        return round2(base * (1.0 - tierRebate))
    }

    fun resolveEffectiveUnitPrice(
        product: Product,
        profile: UserProfile?,
        effectiveUnits: Long
    ): Pair<Double, Boolean> {
        val tierUnit = resolveTierBaseUnitPrice(product, profile)
        val qualifiesBulk = product.bulkDiscountPercent > 0 && effectiveUnits >= product.bulkMinQty
        val finalUnit = if (qualifiesBulk) {
            round2(tierUnit * (1.0 - product.bulkDiscountPercent / 100.0))
        } else {
            tierUnit
        }
        return finalUnit to qualifiesBulk
    }

    fun priceCartLine(
        cartItem: CartItem,
        product: Product,
        profile: UserProfile?
    ): PricedCartLine {
        val packMode = cartItem.packModeEnum()
        val unitsPerPack = if (packMode == PackMode.CARTON) product.cartonSize.coerceAtLeast(1L) else 1L
        val effectiveUnits = cartItem.quantity * unitsPerPack
        val (unitPrice, bulkApplied) = resolveEffectiveUnitPrice(product, profile, effectiveUnits)
        val cartonBonus = if (packMode == PackMode.CARTON && product.cartonSize > 1L) 0.96 else 1.0
        val packPrice = round2(unitPrice * unitsPerPack * cartonBonus)
        val retailTotal = round2(product.retailPrice * effectiveUnits)
        val effectiveTotal = round2(packPrice * cartItem.quantity)
        val savings = (retailTotal - effectiveTotal).coerceAtLeast(0.0)
        val moq = resolveMoqUnits(product, profile)
        return PricedCartLine(
            cartItem = cartItem,
            product = product,
            baseRetailUnitPrice = product.retailPrice,
            effectivePackPrice = packPrice,
            lineRetailTotal = retailTotal,
            lineEffectiveTotal = effectiveTotal,
            lineSavings = savings,
            effectiveUnits = effectiveUnits,
            requiredMoqUnits = moq,
            meetsMoq = effectiveUnits >= moq,
            bulkDiscountApplied = bulkApplied
        )
    }

    fun isCouponEligible(coupon: Coupon, profile: UserProfile?, subtotal: Double): Boolean {
        if (!coupon.active) return false
        val segment = profile?.segment ?: CustomerSegment.RETAIL.name
        if (!coupon.allowedSegments.contains(segment)) return false
        return subtotal >= coupon.minOrderAmount
    }

    fun calculateCheckoutSummary(
        cartItems: List<CartItem>,
        productsById: Map<String, Product>,
        profile: UserProfile?,
        zone: DeliveryZone?,
        coupon: Coupon?,
        redeemLoyalty: Boolean
    ): CheckoutSummary {
        val pricedLines = cartItems.mapNotNull { item ->
            val prod = productsById[item.productId] ?: return@mapNotNull null
            priceCartLine(item, prod, profile)
        }
        val retailSubtotal = round2(pricedLines.sumOf { it.lineRetailTotal })
        val effectiveSubtotal = round2(pricedLines.sumOf { it.lineEffectiveTotal })
        val tierAndBulkSavings = (retailSubtotal - effectiveSubtotal).coerceAtLeast(0.0)

        val couponDiscount = if (coupon != null && isCouponEligible(coupon, profile, effectiveSubtotal)) {
            when (coupon.typeEnum()) {
                DiscountType.PERCENT -> round2((effectiveSubtotal * (coupon.discountValue / 100.0)).coerceAtMost(effectiveSubtotal))
                DiscountType.FIXED -> round2(coupon.discountValue.coerceAtMost(effectiveSubtotal))
            }
        } else {
            0.0
        }

        val afterCoupon = (effectiveSubtotal - couponDiscount).coerceAtLeast(0.0)
        val availablePoints = profile?.loyaltyPoints ?: 0L
        val maxRedeemDiscount = round2(afterCoupon * 0.5)
        val desiredPointsDiscount = if (redeemLoyalty && availablePoints >= 10L) {
            round2(availablePoints / POINTS_TO_CURRENCY_DIVISOR)
        } else {
            0.0
        }
        val loyaltyDiscount = desiredPointsDiscount.coerceAtMost(maxRedeemDiscount)
        val pointsUsed = (loyaltyDiscount * POINTS_TO_CURRENCY_DIVISOR).roundToLong().coerceAtMost(availablePoints)

        val threshold = zone?.freeShippingThreshold ?: 1000.0
        val baseFee = zone?.baseFee ?: 35.0
        val shippingFee = if (effectiveSubtotal <= 0.0 || (threshold > 0.0 && effectiveSubtotal >= threshold)) {
            0.0
        } else {
            baseFee
        }
        val progress = if (threshold > 0.0) (effectiveSubtotal / threshold).toFloat().coerceIn(0f, 1f) else 1f
        val toFree = if (threshold > 0.0) (threshold - effectiveSubtotal).coerceAtLeast(0.0) else 0.0

        val grandTotal = round2((afterCoupon - loyaltyDiscount).coerceAtLeast(0.0) + shippingFee)
        val earned = ((afterCoupon - loyaltyDiscount).coerceAtLeast(0.0) / 10.0).roundToLong().coerceIn(0L, 500L)

        return CheckoutSummary(
            lines = pricedLines,
            retailSubtotal = retailSubtotal,
            effectiveSubtotal = effectiveSubtotal,
            tierAndBulkSavings = round2(tierAndBulkSavings),
            couponDiscount = couponDiscount,
            loyaltyPointsUsed = pointsUsed,
            loyaltyDiscount = loyaltyDiscount,
            shippingFee = shippingFee,
            freeShippingProgress = progress,
            amountToFreeShipping = round2(toFree),
            grandTotal = grandTotal,
            pointsEarned = earned,
            hasMoqViolation = pricedLines.any { !it.meetsMoq }
        )
    }

    fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0
}
