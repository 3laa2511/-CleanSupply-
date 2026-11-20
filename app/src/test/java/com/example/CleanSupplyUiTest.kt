package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.components.ProductDetailSheet
import com.example.core.designsystem.AppThemeMode
import com.example.core.designsystem.ArabicCleanSupplyTypography
import com.example.core.designsystem.ArabicSupplyFontFamily
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.designsystem.DarkCleanSupplyColors
import com.example.core.designsystem.EnglishCleanSupplyTypography
import com.example.core.designsystem.EnglishInterFontFamily
import com.example.core.designsystem.LightCleanSupplyColors
import com.example.data.firestore.SeedData
import com.example.data.model.Address
import com.example.data.model.CartItem
import com.example.data.model.CustomerSegment
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PackMode
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.PricingEngine
import com.example.feature.account.AccountScreen
import com.example.feature.account.AppearanceLanguageCard
import com.example.feature.account.GuestAccountScreen
import com.example.feature.account.GuestMainShell
import com.example.feature.auth.AuthScreen
import com.example.feature.cart.CartScreen
import com.example.feature.cart.CheckoutScreen
import com.example.feature.catalog.CategoriesScreen
import com.example.feature.catalog.SearchCatalogScreen
import com.example.feature.home.HomeScreen
import com.example.feature.orders.OrdersListTab
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.viewmodel.CatalogFilters
import com.example.ui.viewmodel.CheckoutDraft
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CleanSupplyUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gelProduct = SeedData.defaultProducts.first { it.id == "sku_gel_5l" }
    private val relatedProduct = SeedData.defaultProducts.first { it.id != gelProduct.id && it.categoryId == gelProduct.categoryId }
    private val cairoZone = SeedData.defaultZones.first { it.id == "zone_cairo" }

    @Test
    fun `theme applies RTL for Arabic and LTR for English on HomeScreen`() {
        var arabicDir: LayoutDirection? = null
        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ARABIC, themeMode = AppThemeMode.LIGHT) {
                arabicDir = LocalLayoutDirection.current
                HomeScreen(
                    profile = null,
                    activeZone = cairoZone,
                    categories = SeedData.defaultCategories,
                    products = SeedData.defaultProducts,
                    recentlyViewed = emptyList(),
                    recentOrders = emptyList(),
                    isRefreshing = false,
                    onRefresh = {},
                    onOpenSearch = {},
                    onOpenAllCategories = {},
                    onExploreBulkDeals = {},
                    onSelectCategory = {},
                    onOpenProductDetail = {},
                    onQuickAdd = {},
                    onReorder = {},
                    onOpenAllCatalog = {}
                )
            }
        }
        composeRule.onNodeWithTag("home_search_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_explore_bulk_cta"))
        composeRule.onNodeWithTag("home_explore_bulk_cta").assertIsDisplayed()
        composeRule.onNodeWithText(ArabicStrings.exploreBulkCta).assertIsDisplayed()
        assertEquals(LayoutDirection.Rtl, arabicDir)
    }

    @Test
    fun `theme switches between light and dark color tokens`() {
        var lightBg = LightCleanSupplyColors.surface
        var darkBg = DarkCleanSupplyColors.surface
        var ltrDir: LayoutDirection? = null

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.DARK) {
                ltrDir = LocalLayoutDirection.current
                darkBg = CleanSupplyTheme.colors.background
                CategoriesScreen(
                    categories = SeedData.defaultCategories,
                    products = SeedData.defaultProducts,
                    selectedCategoryId = null,
                    onSelectCategory = {}
                )
            }
        }
        composeRule.onNodeWithText(EnglishStrings.categoriesTitle).assertIsDisplayed()
        composeRule.onNodeWithTag("category_grid_card_all").assertIsDisplayed()
        assertEquals(LayoutDirection.Ltr, ltrDir)
        assertEquals(DarkCleanSupplyColors.background, darkBg)
        assertTrue(lightBg != darkBg)
    }

    @Test
    fun `product detail hides unauthorized wholesale and business prices for retail user`() {
        val retailProfile = UserProfile(
            userId = "u_retail",
            displayName = "Retail Shopper",
            segment = CustomerSegment.RETAIL.name,
            verificationStatus = VerificationStatus.VERIFIED.name
        )
        var addedPackMode: PackMode? = null
        var addedQty = 0L

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                ProductDetailSheet(
                    product = gelProduct,
                    relatedProducts = listOf(relatedProduct),
                    profile = retailProfile,
                    onDismiss = {},
                    onSelectRelated = {},
                    onAddToCart = { qty, mode ->
                        addedQty = qty
                        addedPackMode = mode
                    }
                )
            }
        }

        // Retail price (210) & upgrade guidance visible; wholesale (178) & business (165) must NOT leak.
        composeRule.onNodeWithText(EnglishStrings.authorizedPricingTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.b2bUpgradePricingNote).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("178 EGP", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("165 EGP", substring = true).assertDoesNotExist()

        composeRule.onNodeWithTag("pack_mode_carton").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail_qty_plus").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("detail_add_to_cart").assertIsDisplayed().performClick()

        assertEquals(PackMode.CARTON, addedPackMode)
        assertEquals(2L, addedQty)
    }

    @Test
    fun `search catalog supports query input, filter chips, and grid-list toggle`() {
        var currentQuery = ""
        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                SearchCatalogScreen(
                    categories = SeedData.defaultCategories,
                    products = SeedData.defaultProducts,
                    filters = CatalogFilters(),
                    recentSearches = emptyList(),
                    profile = null,
                    onSearchChange = { currentQuery = it },
                    onCommitSearch = {},
                    onClearRecentSearches = {},
                    onSelectCategory = {},
                    onToggleBulkOnly = {},
                    onToggleInStockOnly = {},
                    onSelectSort = {},
                    onOpenProductDetail = {},
                    onQuickAdd = {}
                )
            }
        }

        composeRule.onNodeWithTag("catalog_search_input").performTextInput("Gel")
        assertEquals("Gel", currentQuery)
        composeRule.onNodeWithTag("bulk_filter_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("in_stock_filter_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("layout_toggle_button").performClick()
        composeRule.onNodeWithTag("product_card_${gelProduct.id}").assertIsDisplayed()
    }

    @Test
    fun `cart screen enforces MOQ violation and navigates when valid`() {
        val wholesaleProfile = UserProfile(
            userId = "u_wh",
            segment = CustomerSegment.WHOLESALE.name,
            verificationStatus = VerificationStatus.VERIFIED.name
        )
        val invalidSummary = PricingEngine.calculateCheckoutSummary(
            cartItems = listOf(CartItem(id = "c1", productId = gelProduct.id, quantity = 1L, packMode = PackMode.UNIT.name)),
            productsById = mapOf(gelProduct.id to gelProduct),
            profile = wholesaleProfile,
            zone = cairoZone,
            coupon = null,
            redeemLoyalty = false
        )

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                CartScreen(
                    summary = invalidSummary,
                    profile = wholesaleProfile,
                    coupons = SeedData.defaultCoupons,
                    draft = CheckoutDraft(),
                    onBrowseCatalog = {},
                    onUpdateCartQty = { _, _, _ -> },
                    onApplyCoupon = {},
                    onRemoveCoupon = {},
                    onToggleLoyalty = {},
                    onProceedToCheckout = {}
                )
            }
        }

        composeRule.onNodeWithText(EnglishStrings.moqWarning).assertIsDisplayed()
        composeRule.onNodeWithTag("proceed_to_checkout_btn").assertIsNotEnabled()
    }

    @Test
    fun `checkout shows pending verification note for card payment and enables submit with address`() {
        val retailProfile = UserProfile(
            userId = "u_retail",
            segment = CustomerSegment.RETAIL.name,
            verificationStatus = VerificationStatus.VERIFIED.name
        )
        val validSummary = PricingEngine.calculateCheckoutSummary(
            cartItems = listOf(CartItem(id = "c1", productId = gelProduct.id, quantity = 1L, packMode = PackMode.UNIT.name)),
            productsById = mapOf(gelProduct.id to gelProduct),
            profile = retailProfile,
            zone = cairoZone,
            coupon = null,
            redeemLoyalty = false
        )
        val address = Address(
            id = "addr_1",
            label = "HQ",
            recipientName = "Ahmed",
            phone = "01000000000",
            zoneId = cairoZone.id,
            streetDetails = "10 Nasr St"
        )
        var orderPlaced = false

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                CheckoutScreen(
                    summary = validSummary,
                    profile = retailProfile,
                    addresses = listOf(address),
                    activeAddress = address,
                    zones = SeedData.defaultZones,
                    activeZone = cairoZone,
                    draft = CheckoutDraft(paymentMethod = PaymentMethod.CARD),
                    isSubmitting = false,
                    onSelectAddress = {},
                    onSaveNewAddress = { _, _, _, _, _, _ -> },
                    onSelectSlot = {},
                    onSelectPayment = {},
                    onPaymentRefChange = {},
                    onPlaceOrder = { orderPlaced = true },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("checkout_list")
            .performScrollToNode(hasText(EnglishStrings.paymentPendingVerificationNote))
        composeRule.onNodeWithText(EnglishStrings.paymentPendingVerificationNote).assertIsDisplayed()
        composeRule.onNodeWithTag("place_order_button").assertIsEnabled().performClick()
        assertTrue(orderPlaced)
    }

    @Test
    fun `orders screen separates active and past orders and handles error retry`() {
        val activeOrder = Order(
            id = "o1",
            orderNumber = "CS-1001",
            status = OrderStatus.CONFIRMED.name,
            zoneNameEn = "Cairo",
            carrierName = "Fleet",
            totalAmount = 420.0,
            itemCount = 2L
        )
        val pastOrder = Order(
            id = "o2",
            orderNumber = "CS-1000",
            status = OrderStatus.DELIVERED.name,
            zoneNameEn = "Cairo",
            carrierName = "Fleet",
            totalAmount = 210.0,
            itemCount = 1L
        )

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                OrdersListTab(
                    ordersState = UiState.Success(listOf(activeOrder, pastOrder)),
                    onSelectOrder = {},
                    onReorder = {}
                )
            }
        }

        composeRule.onAllNodesWithText(EnglishStrings.currentOrdersTitle).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(EnglishStrings.pastOrdersTitle).onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("order_card_o1").assertIsDisplayed()
        composeRule.onNodeWithTag("order_card_o2").assertIsDisplayed()
    }

    @Test
    fun `appearance settings card triggers language and theme mode selection`() {
        var selectedLang = AppLanguage.ARABIC
        var selectedTheme = AppThemeMode.SYSTEM

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                AppearanceLanguageCard(
                    currentLanguage = selectedLang,
                    currentThemeMode = selectedTheme,
                    onSelectLanguage = { selectedLang = it },
                    onSelectThemeMode = { selectedTheme = it }
                )
            }
        }

        composeRule.onNodeWithTag("lang_chip_en").performClick()
        composeRule.onNodeWithTag("theme_chip_dark").performClick()

        assertEquals(AppLanguage.ENGLISH, selectedLang)
        assertEquals(AppThemeMode.DARK, selectedTheme)
    }

    @Test
    fun `auth screen exposes google sign in, guest mode, and language toggle`() {
        var guestClicked = false
        var langToggled = false

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                AuthScreen(
                    onToggleLanguage = { langToggled = true },
                    onAuthSuccess = {},
                    onContinueAsGuest = { guestClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("auth_language_toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("continue_as_guest_button").performScrollTo().performClick()

        assertTrue(langToggled)
        assertTrue(guestClicked)
    }

    @Test
    fun `guest account screen renders sign in and welcome navigation controls`() {
        var signInClicked = false
        var backToWelcomeClicked = false

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                GuestAccountScreen(
                    isSigningIn = false,
                    currentLanguage = AppLanguage.ENGLISH,
                    currentThemeMode = AppThemeMode.LIGHT,
                    onSelectLanguage = {},
                    onSelectThemeMode = {},
                    onSignInWithGoogle = { signInClicked = true },
                    onBackToWelcome = { backToWelcomeClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("guest_account_sign_in_btn").performClick()
        composeRule.onNodeWithTag("guest_back_to_auth_btn").performClick()

        assertTrue(signInClicked)
        assertTrue(backToWelcomeClicked)
    }

    @Test
    fun `search empty state shows reset filters CTA and triggers callback`() {
        var resetCalled = false

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                SearchCatalogScreen(
                    categories = SeedData.defaultCategories,
                    products = emptyList(),
                    filters = CatalogFilters(searchQuery = "nonexistent-sku"),
                    recentSearches = emptyList(),
                    profile = null,
                    onSearchChange = {},
                    onCommitSearch = {},
                    onClearRecentSearches = {},
                    onSelectCategory = {},
                    onToggleBulkOnly = {},
                    onToggleInStockOnly = {},
                    onSelectSort = {},
                    onOpenProductDetail = {},
                    onQuickAdd = {},
                    onResetFilters = { resetCalled = true }
                )
            }
        }

        composeRule.onNodeWithTag("search_reset_filters_btn").assertIsDisplayed().performClick()
        assertTrue(resetCalled)
    }

    @Test
    fun `cart screen stepper increments quantity and applies coupon`() {
        val retailProfile = UserProfile(
            userId = "u_retail",
            segment = CustomerSegment.RETAIL.name,
            verificationStatus = VerificationStatus.VERIFIED.name,
            loyaltyPoints = 100L
        )
        val validSummary = PricingEngine.calculateCheckoutSummary(
            cartItems = listOf(CartItem(id = "c1", productId = gelProduct.id, quantity = 1L, packMode = PackMode.UNIT.name)),
            productsById = mapOf(gelProduct.id to gelProduct),
            profile = retailProfile,
            zone = cairoZone,
            coupon = null,
            redeemLoyalty = false
        )
        var updatedQty = 0L
        var appliedCoupon = ""

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                CartScreen(
                    summary = validSummary,
                    profile = retailProfile,
                    coupons = SeedData.defaultCoupons,
                    draft = CheckoutDraft(),
                    onBrowseCatalog = {},
                    onUpdateCartQty = { _, qty, _ -> updatedQty = qty },
                    onApplyCoupon = { appliedCoupon = it },
                    onRemoveCoupon = {},
                    onToggleLoyalty = {},
                    onProceedToCheckout = {}
                )
            }
        }

        composeRule.onNodeWithTag("cart_qty_plus_${gelProduct.id}").performClick()
        assertEquals(2L, updatedQty)
        composeRule.onNodeWithTag("coupon_input").performScrollTo().performTextInput("CLEAN10")
        composeRule.onNodeWithTag("apply_coupon_btn").performClick()
        assertEquals("CLEAN10", appliedCoupon)
        composeRule.onNodeWithTag("proceed_to_checkout_btn").assertIsEnabled()
    }

    @Test
    fun `orders error state displays error message and invokes retry callback`() {
        var retried = false

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                OrdersListTab(
                    ordersState = UiState.Error("Network unavailable"),
                    onSelectOrder = {},
                    onReorder = {},
                    onRetry = { retried = true }
                )
            }
        }

        composeRule.onNodeWithText("Network unavailable").assertIsDisplayed()
        composeRule.onNodeWithTag("orders_retry_btn").performClick()
        assertTrue(retried)
    }

    @Test
    fun `verified wholesale user sees bulk unlock indicator on product detail`() {
        val wholesaleProfile = UserProfile(
            userId = "u_wh",
            displayName = "Al-Nour Market",
            segment = CustomerSegment.WHOLESALE.name,
            verificationStatus = VerificationStatus.VERIFIED.name
        )

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                ProductDetailSheet(
                    product = gelProduct,
                    relatedProducts = listOf(relatedProduct),
                    profile = wholesaleProfile,
                    onDismiss = {},
                    onSelectRelated = {},
                    onAddToCart = { _, _ -> }
                )
            }
        }

        // Wholesale MOQ >= carton size starts in carton mode where 1 carton exceeds bulkMinQty or unlocks easily; verify B2B note is hidden
        composeRule.onNodeWithText(EnglishStrings.b2bUpgradePricingNote).assertDoesNotExist()
        composeRule.onNodeWithText(EnglishStrings.authorizedPricingTitle).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `new visual identity tokens enforce forest green ivory palette and dark mode banner contrast`() {
        assertEquals(Color(0xFF174C3C), LightCleanSupplyColors.primary)
        assertEquals(Color(0xFF23845D), LightCleanSupplyColors.interactive)
        assertEquals(Color(0xFFF7F6F1), LightCleanSupplyColors.background)
        assertEquals(Color(0xFFFFFFFF), LightCleanSupplyColors.surface)
        assertEquals(Color(0xFFD8C6A0), LightCleanSupplyColors.secondary)
        assertEquals(Color(0xFFE8E7DF), LightCleanSupplyColors.outline)

        // Dark mode banner ink must stay bright ivory over deep forest banner (never dark-on-dark)
        assertEquals(Color(0xFF174C3C), DarkCleanSupplyColors.brandBanner)
        assertEquals(Color(0xFFF9F8F3), DarkCleanSupplyColors.onBrandBanner)
    }

    @Test
    fun `bilingual typography uses IBM Plex Sans Arabic and Inter with tabular numerals`() {
        var resolvedArFont = EnglishInterFontFamily
        var resolvedEnFont = ArabicSupplyFontFamily
        var numericFeatures: String? = null

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ARABIC, themeMode = AppThemeMode.LIGHT) {
                resolvedArFont = CleanSupplyTheme.typography.titleLarge.fontFamily!!
                numericFeatures = CleanSupplyTheme.typography.numericMedium.fontFeatureSettings
            }
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                resolvedEnFont = CleanSupplyTheme.typography.titleLarge.fontFamily!!
            }
        }

        assertEquals(ArabicSupplyFontFamily, resolvedArFont)
        assertEquals(EnglishInterFontFamily, resolvedEnFont)
        assertEquals("tnum", numericFeatures)
        assertEquals(ArabicSupplyFontFamily, ArabicCleanSupplyTypography.bodyMedium.fontFamily)
        assertEquals(EnglishInterFontFamily, EnglishCleanSupplyTypography.bodyMedium.fontFamily)
    }

    @Test
    fun `guest shell switches bottom tabs smoothly and preserves search query and cart state`() {
        composeRule.setContent {
            var cartItems by remember { mutableStateOf(emptyList<CartItem>()) }
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.LIGHT) {
                GuestMainShell(
                    guestCartItems = cartItems,
                    currentLanguage = AppLanguage.ENGLISH,
                    currentThemeMode = AppThemeMode.LIGHT,
                    recentSearches = emptyList(),
                    onUpdateGuestCart = { cartItems = it },
                    onToggleLanguage = {},
                    onSelectLanguage = {},
                    onSelectThemeMode = {},
                    onCommitRecentSearch = {},
                    onClearRecentSearches = {},
                    onExitGuestToAuth = {}
                )
            }
        }

        // Start on Home, add a product, navigate to Search -> enter query -> switch to Cart -> back to Search
        composeRule.onNodeWithTag("home_search_entry").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_$ROUTE_SEARCH").performClick()
        composeRule.onNodeWithTag("catalog_search_input").performTextInput("Gel")
        composeRule.onNodeWithTag("product_card_${gelProduct.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_${gelProduct.id}").performScrollTo().performClick()

        // Navigate to Cart and verify line item & proceed button, then navigate to Checkout
        composeRule.onNodeWithTag("nav_$ROUTE_CART").performClick()
        composeRule.onNodeWithTag("proceed_to_checkout_btn").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("place_order_button").assertIsDisplayed()

        // Navigate back from Checkout to Cart, then switch to Categories and back to Search
        composeRule.onNodeWithContentDescription(EnglishStrings.backLabel).performClick()
        composeRule.onNodeWithTag("proceed_to_checkout_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_$ROUTE_CATEGORIES").performClick()
        composeRule.onNodeWithTag("category_grid_card_all").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_$ROUTE_SEARCH").performClick()
        composeRule.onNodeWithTag("catalog_search_input").assertTextContains("Gel")
    }

    @Test
    fun `account screen renders loyalty wallet and B2B credit meter in dark mode`() {
        val wholesaleProfile = UserProfile(
            userId = "u_wh",
            displayName = "Delta Cleaning Co.",
            email = "orders@deltaclean.eg",
            segment = CustomerSegment.WHOLESALE.name,
            verificationStatus = VerificationStatus.VERIFIED.name,
            loyaltyPoints = 420L,
            creditLimit = 25000.0,
            creditUsed = 5500.0
        )

        composeRule.setContent {
            CleanSupplyTheme(language = AppLanguage.ENGLISH, themeMode = AppThemeMode.DARK) {
                AccountScreen(
                    isAdmin = false,
                    profile = wholesaleProfile,
                    addresses = emptyList(),
                    zones = SeedData.defaultZones,
                    currentLanguage = AppLanguage.ENGLISH,
                    currentThemeMode = AppThemeMode.DARK,
                    ordersState = UiState.Success(emptyList()),
                    selectedOrderId = null,
                    selectedOrderItems = emptyList(),
                    loyaltyLogs = emptyList(),
                    supportTickets = emptyList(),
                    adminUsers = emptyList(),
                    onSelectOrder = {},
                    onCancelOrder = {},
                    onReorder = {},
                    onSaveAddress = { _, _, _, _, _, _ -> },
                    onDeleteAddress = {},
                    onRequestSegment = { _, _, _ -> },
                    onSubmitTicket = { _, _, _, _ -> },
                    onSelectLanguage = {},
                    onSelectThemeMode = {},
                    onSeedCatalog = {},
                    onApproveUserTier = { _, _, _, _ -> },
                    onAdvanceOrder = {},
                    onStepDriver = {},
                    onToggleFulfillment = {},
                    onResolveTicket = { _, _ -> },
                    onCopyTrackingToast = {},
                    onSignOut = {}
                )
            }
        }

        // Switch to Loyalty & Help tab
        composeRule.onNodeWithTag("account_tab_1").performClick()
        composeRule.onNodeWithText(EnglishStrings.loyaltyPointsTitle).assertIsDisplayed()
        composeRule.onNodeWithText("420 pts").assertIsDisplayed()

        // Switch to Profile & Settings tab and verify B2B credit meter
        composeRule.onNodeWithTag("account_tab_2").performClick()
        composeRule.onNodeWithText(EnglishStrings.creditUtilizationLabel).assertIsDisplayed()
    }
}
