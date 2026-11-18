package com.example

import com.example.data.firestore.SeedData
import com.example.data.model.ApprovedTier
import com.example.data.model.CartItem
import com.example.data.model.CustomerSegment
import com.example.data.model.PackMode
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.PricingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingEngineTest {

    private val gelProduct = SeedData.defaultProducts.first { it.id == "sku_gel_5l" }
    private val cairoZone = SeedData.defaultZones.first { it.id == "zone_cairo" }
    private val clean10 = SeedData.defaultCoupons.first { it.code == "CLEAN10" }
    private val b2bPro = SeedData.defaultCoupons.first { it.code == "B2BPRO" }

    @Test
    fun `verified wholesale user receives wholesale and bulk discounts with MOQ enforcement`() {
        val wholesaleProfile = UserProfile(
            userId = "u1",
            segment = CustomerSegment.WHOLESALE.name,
            approvedTier = ApprovedTier.TIER_A.name,
            verificationStatus = VerificationStatus.VERIFIED.name,
            loyaltyPoints = 200L
        )
        // Wholesale base = 178.0, TIER_A (-3%) = 172.66; 2 cartons = 8 units >= bulkMinQty(8) -> -10% = 155.39
        // Carton bonus (-4%) -> 155.39 * 4 * 0.96 = 596.70 per carton
        val cart = listOf(
            CartItem(id = "c1", productId = gelProduct.id, quantity = 2L, packMode = PackMode.CARTON.name)
        )
        val summary = PricingEngine.calculateCheckoutSummary(
            cartItems = cart,
            productsById = mapOf(gelProduct.id to gelProduct),
            profile = wholesaleProfile,
            zone = cairoZone,
            coupon = clean10,
            redeemLoyalty = true
        )

        assertFalse(summary.hasMoqViolation)
        assertTrue(summary.lines.first().bulkDiscountApplied)
        assertTrue(summary.tierAndBulkSavings > 0.0)
        assertTrue(summary.couponDiscount > 0.0)
        assertEquals(20.0, summary.loyaltyDiscount, 0.01)
        assertEquals(200L, summary.loyaltyPointsUsed)
    }

    @Test
    fun `unverified wholesale user falls back to retail pricing and retail coupon eligibility`() {
        val pendingWholesale = UserProfile(
            userId = "u2",
            segment = CustomerSegment.WHOLESALE.name,
            approvedTier = ApprovedTier.STANDARD.name,
            verificationStatus = VerificationStatus.PENDING_VERIFICATION.name
        )
        val unitPrice = PricingEngine.resolveTierBaseUnitPrice(gelProduct, pendingWholesale)
        assertEquals(gelProduct.retailPrice, unitPrice, 0.01)
        assertTrue(PricingEngine.isCouponEligible(b2bPro, pendingWholesale, 2000.0))
        val retailUser = pendingWholesale.copy(segment = CustomerSegment.RETAIL.name)
        assertFalse(PricingEngine.isCouponEligible(b2bPro, retailUser, 2000.0))
    }

    @Test
    fun `moq violation flagged when wholesale orders below wholesale MOQ`() {
        val verifiedWholesale = UserProfile(
            userId = "u3",
            segment = CustomerSegment.WHOLESALE.name,
            verificationStatus = VerificationStatus.VERIFIED.name
        )
        val cart = listOf(
            CartItem(id = "c1", productId = gelProduct.id, quantity = 1L, packMode = PackMode.UNIT.name)
        )
        val summary = PricingEngine.calculateCheckoutSummary(
            cartItems = cart,
            productsById = mapOf(gelProduct.id to gelProduct),
            profile = verifiedWholesale,
            zone = cairoZone,
            coupon = null,
            redeemLoyalty = false
        )
        assertTrue(summary.hasMoqViolation)
    }
}
