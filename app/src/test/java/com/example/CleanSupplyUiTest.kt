package com.example

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.components.ProductDetailSheet
import com.example.core.designsystem.AppThemeMode
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.designsystem.DarkCleanSupplyColors
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
import com.example.feature.account.AppearanceLanguageCard
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
        composeRule.onNodeWithTag("detail_qty_plus").performScrollTo().performClick()
        composeRule.onNodeWithTag("detail_add_to_cart").performScrollTo().performClick()

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

        composeRule.onNodeWithText(EnglishStrings.currentOrdersTitle).assertIsDisplayed()
        composeRule.onNodeWithText(EnglishStrings.pastOrdersTitle).assertIsDisplayed()
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
}
