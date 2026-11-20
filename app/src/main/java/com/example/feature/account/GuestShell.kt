package com.example.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ROUTE_ACCOUNT
import com.example.ROUTE_CART
import com.example.ROUTE_CATEGORIES
import com.example.ROUTE_HOME
import com.example.ROUTE_SEARCH
import com.example.core.components.AppShellDestination
import com.example.core.components.AppShellScaffold
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.ProductDetailSheet
import com.example.core.designsystem.AppThemeMode
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.network.ConnectivityObserver
import com.example.data.firestore.SeedData
import com.example.data.model.CartItem
import com.example.data.model.PackMode
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.domain.PricingEngine
import com.example.feature.auth.onGoogleSignInClicked
import com.example.feature.cart.CartScreen
import com.example.feature.cart.CheckoutScreen
import com.example.feature.catalog.CategoriesScreen
import com.example.feature.catalog.SearchCatalogScreen
import com.example.feature.home.HomeScreen
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CatalogFilters
import com.example.ui.viewmodel.CheckoutDraft
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GuestMainShell(
    guestCartItems: List<CartItem>,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    recentSearches: List<String>,
    onUpdateGuestCart: (List<CartItem>) -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onCommitRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onExitGuestToAuth: () -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val connectivityObserver = remember(context) { ConnectivityObserver(context) }
    val isConnected by connectivityObserver.observeIsConnected().collectAsStateWithLifecycle(initialValue = true)

    var currentTabRoute by rememberSaveable { mutableStateOf(ROUTE_HOME) }
    var showCheckout by rememberSaveable { mutableStateOf(false) }
    val tabStateHolder = rememberSaveableStateHolder()

    val categories = SeedData.defaultCategories
    val allProducts = SeedData.defaultProducts
    val zones = SeedData.defaultZones
    val coupons = SeedData.defaultCoupons

    var filters by remember { mutableStateOf(CatalogFilters()) }
    var draft by remember { mutableStateOf(CheckoutDraft()) }
    var recentlyViewedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var inspectedProduct by remember { mutableStateOf<Product?>(null) }
    var snackbarText by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarText) {
        val msg = snackbarText ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(msg)
        snackbarText = null
    }

    LaunchedEffect(showCheckout, currentTabRoute) {
        if (showCheckout || currentTabRoute == ROUTE_CART) {
            snackbarText = null
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    BackHandler(enabled = inspectedProduct != null || showCheckout || currentTabRoute != ROUTE_HOME) {
        when {
            inspectedProduct != null -> inspectedProduct = null
            showCheckout -> showCheckout = false
            currentTabRoute != ROUTE_HOME -> currentTabRoute = ROUTE_HOME
        }
    }
    BackHandler(enabled = inspectedProduct == null && !showCheckout && currentTabRoute == ROUTE_HOME) {
        onExitGuestToAuth()
    }

    fun openProduct(prod: Product) {
        recentlyViewedIds = (listOf(prod.id) + recentlyViewedIds.filterNot { it == prod.id }).take(8)
        inspectedProduct = prod
    }

    val recentlyViewedProducts by remember(allProducts) {
        derivedStateOf {
            val byId = allProducts.associateBy { it.id }
            recentlyViewedIds.mapNotNull { byId[it] }
        }
    }

    val filteredProducts by remember(allProducts) {
        derivedStateOf {
            val q = filters.searchQuery.trim().lowercase()
            val filtered = allProducts.filter { p ->
                val matchesCat = filters.selectedCategoryId == null || p.categoryId == filters.selectedCategoryId
                val matchesBulk = !filters.bulkOnly || p.bulkDiscountPercent > 0L
                val matchesStock = !filters.inStockOnly || p.stockQty > 0L
                val matchesQuery = q.isBlank() ||
                    p.nameAr.lowercase().contains(q) ||
                    p.nameEn.lowercase().contains(q) ||
                    p.sku.lowercase().contains(q) ||
                    p.brand.lowercase().contains(q)
                matchesCat && matchesBulk && matchesStock && matchesQuery
            }
            when (filters.sortOption) {
                SortOption.FEATURED -> filtered.sortedByDescending { it.featured }
                SortOption.PRICE_LOW -> filtered.sortedBy { PricingEngine.resolveTierBaseUnitPrice(it, null) }
                SortOption.PRICE_HIGH -> filtered.sortedByDescending { PricingEngine.resolveTierBaseUnitPrice(it, null) }
            }
        }
    }

    val checkoutSummary = remember(guestCartItems, draft, zones, coupons) {
        val prodMap = allProducts.associateBy { it.id }
        val coupon = coupons.firstOrNull { it.code.equals(draft.appliedCouponCode.trim(), ignoreCase = true) }
        PricingEngine.calculateCheckoutSummary(
            cartItems = guestCartItems,
            productsById = prodMap,
            profile = null,
            zone = zones.firstOrNull(),
            coupon = coupon,
            redeemLoyalty = false
        )
    }

    fun upsertGuestCart(productId: String, quantity: Long, packMode: PackMode, confirmationMsg: String? = null) {
        val itemId = "${productId}_${packMode.name}"
        val updated = if (quantity <= 0L) {
            guestCartItems.filterNot { it.id == itemId }
        } else {
            val safeQty = quantity.coerceIn(1L, 5000L)
            val existing = guestCartItems.any { it.id == itemId }
            if (existing) {
                guestCartItems.map { if (it.id == itemId) it.copy(quantity = safeQty) else it }
            } else {
                guestCartItems + CartItem(id = itemId, productId = productId, quantity = safeQty, packMode = packMode.name)
            }
        }
        onUpdateGuestCart(updated)
        if (confirmationMsg != null) snackbarText = confirmationMsg
    }

    fun triggerGoogleSignIn() {
        if (isSigningIn) return
        isSigningIn = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { isSigningIn = false },
            onAuthError = { msg ->
                isSigningIn = false
                snackbarText = msg
            },
            scope = scope,
            onAuthCancelled = { isSigningIn = false }
        )
    }

    fun navigateToTab(route: String) {
        showCheckout = false
        currentTabRoute = route
    }

    val destinations = remember(s) {
        listOf(
            AppShellDestination(ROUTE_HOME, s.navHome, CleanSupplyIcons.NavHomeActive, CleanSupplyIcons.NavHomeInactive),
            AppShellDestination(ROUTE_CATEGORIES, s.navCategories, CleanSupplyIcons.NavCategoriesActive, CleanSupplyIcons.NavCategoriesInactive),
            AppShellDestination(ROUTE_SEARCH, s.navSearch, CleanSupplyIcons.NavSearchActive, CleanSupplyIcons.NavSearchInactive),
            AppShellDestination(ROUTE_CART, s.navCart, CleanSupplyIcons.NavCartActive, CleanSupplyIcons.NavCartInactive),
            AppShellDestination(ROUTE_ACCOUNT, s.navAccount, CleanSupplyIcons.NavAccountActive, CleanSupplyIcons.NavAccountInactive)
        )
    }

    AppShellScaffold(
        destinations = destinations,
        currentRoute = currentTabRoute,
        cartBadgeCount = guestCartItems.size,
        isCheckoutRoute = showCheckout,
        isConnected = isConnected,
        snackbarHostState = snackbarHostState,
        onNavigate = ::navigateToTab,
        onBackClick = { showCheckout = false },
        onToggleLanguage = onToggleLanguage,
        badgeLabel = s.guestBadge
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showCheckout) {
                CheckoutScreen(
                    summary = checkoutSummary,
                    profile = null,
                    addresses = emptyList(),
                    activeAddress = null,
                    zones = zones,
                    activeZone = zones.firstOrNull(),
                    draft = draft,
                    isSubmitting = isSigningIn,
                    onSelectAddress = {},
                    onSaveNewAddress = { _, _, _, _, _, _ -> },
                    onSelectSlot = { draft = draft.copy(selectedSlot = it) },
                    onSelectPayment = { method: PaymentMethod -> draft = draft.copy(paymentMethod = method) },
                    onPaymentRefChange = { draft = draft.copy(paymentReference = it) },
                    onPlaceOrder = {},
                    onBack = { showCheckout = false },
                    isGuest = true,
                    onSignInToCheckout = { triggerGoogleSignIn() }
                )
            } else {
                tabStateHolder.SaveableStateProvider(key = currentTabRoute) {
                    when (currentTabRoute) {
                        ROUTE_HOME -> HomeScreen(
                            profile = null,
                            activeZone = zones.firstOrNull(),
                            categories = categories,
                            products = allProducts,
                            recentlyViewed = recentlyViewedProducts,
                            recentOrders = emptyList(),
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                scope.launch {
                                    isRefreshing = true
                                    delay(350L)
                                    isRefreshing = false
                                }
                            },
                            onOpenSearch = { navigateToTab(ROUTE_SEARCH) },
                            onOpenAllCategories = { navigateToTab(ROUTE_CATEGORIES) },
                            onExploreBulkDeals = {
                                filters = filters.copy(bulkOnly = true)
                                navigateToTab(ROUTE_SEARCH)
                            },
                            onSelectCategory = { catId ->
                                filters = filters.copy(selectedCategoryId = catId)
                                navigateToTab(ROUTE_SEARCH)
                            },
                            onOpenProductDetail = ::openProduct,
                            onQuickAdd = { prod ->
                                val moq = PricingEngine.resolveMoqUnits(prod, null)
                                upsertGuestCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                            },
                            onReorder = {},
                            onOpenAllCatalog = {
                                filters = filters.copy(selectedCategoryId = null)
                                navigateToTab(ROUTE_SEARCH)
                            },
                            onOpenOrders = { navigateToTab(ROUTE_ACCOUNT) }
                        )

                        ROUTE_CATEGORIES -> CategoriesScreen(
                            categories = categories,
                            products = allProducts,
                            selectedCategoryId = filters.selectedCategoryId,
                            onSelectCategory = { catId ->
                                filters = filters.copy(selectedCategoryId = catId)
                                navigateToTab(ROUTE_SEARCH)
                            }
                        )

                        ROUTE_SEARCH -> SearchCatalogScreen(
                            categories = categories,
                            products = filteredProducts,
                            filters = filters,
                            recentSearches = recentSearches,
                            profile = null,
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                scope.launch {
                                    isRefreshing = true
                                    delay(350L)
                                    isRefreshing = false
                                }
                            },
                            onSearchChange = { filters = filters.copy(searchQuery = it) },
                            onCommitSearch = onCommitRecentSearch,
                            onClearRecentSearches = onClearRecentSearches,
                            onSelectCategory = { filters = filters.copy(selectedCategoryId = it) },
                            onToggleBulkOnly = { filters = filters.copy(bulkOnly = !filters.bulkOnly) },
                            onToggleInStockOnly = { filters = filters.copy(inStockOnly = !filters.inStockOnly) },
                            onSelectSort = { filters = filters.copy(sortOption = it) },
                            onOpenProductDetail = ::openProduct,
                            onQuickAdd = { prod ->
                                val moq = PricingEngine.resolveMoqUnits(prod, null)
                                upsertGuestCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                            },
                            onResetFilters = { filters = CatalogFilters(sortOption = filters.sortOption) }
                        )

                        ROUTE_CART -> CartScreen(
                            summary = checkoutSummary,
                            profile = null,
                            coupons = coupons,
                            draft = draft,
                            onBrowseCatalog = { navigateToTab(ROUTE_SEARCH) },
                            onUpdateCartQty = { prodId, qty, mode ->
                                upsertGuestCart(prodId, qty, mode)
                            },
                            onApplyCoupon = { code ->
                                val clean = code.trim().uppercase()
                                if (clean.isEmpty()) {
                                    draft = draft.copy(appliedCouponCode = "", couponError = false)
                                } else {
                                    val match = coupons.firstOrNull { it.code.equals(clean, ignoreCase = true) }
                                    val valid = match != null && PricingEngine.isCouponEligible(match, null, checkoutSummary.effectiveSubtotal)
                                    draft = if (valid) {
                                        draft.copy(appliedCouponCode = clean, couponError = false)
                                    } else {
                                        draft.copy(couponError = true)
                                    }
                                }
                            },
                            onRemoveCoupon = { draft = draft.copy(appliedCouponCode = "", couponError = false) },
                            onToggleLoyalty = {},
                            onProceedToCheckout = { showCheckout = true }
                        )

                        ROUTE_ACCOUNT -> GuestAccountScreen(
                            isSigningIn = isSigningIn,
                            currentLanguage = currentLanguage,
                            currentThemeMode = currentThemeMode,
                            onSelectLanguage = onSelectLanguage,
                            onSelectThemeMode = onSelectThemeMode,
                            onSignInWithGoogle = { triggerGoogleSignIn() },
                            onBackToWelcome = onExitGuestToAuth
                        )
                    }
                }
            }
        }
    }

    inspectedProduct?.let { prod ->
        val related = remember(prod.id, allProducts) {
            allProducts.filter { it.categoryId == prod.categoryId && it.id != prod.id }.take(6)
        }
        ProductDetailSheet(
            product = prod,
            relatedProducts = related,
            profile = null,
            onDismiss = { inspectedProduct = null },
            onSelectRelated = ::openProduct,
            onAddToCart = { qty, mode ->
                upsertGuestCart(prod.id, qty, mode, s.addedToCart)
                inspectedProduct = null
            }
        )
    }
}

@Composable
fun GuestAccountScreen(
    isSigningIn: Boolean,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onBackToWelcome: () -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outline),
            shape = shapes.largeSurface,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(colors.brandBanner, colors.brandBannerSurface)))
                        .padding(spacing.lg)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = colors.heroAccent.copy(alpha = 0.22f),
                            shape = shapes.largeSurface,
                            border = BorderStroke(1.dp, colors.heroAccent.copy(alpha = 0.50f)),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CleanSupplyIcons.NavAccountActive,
                                    contentDescription = null,
                                    tint = colors.heroAccent,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = s.guestSignInBannerTitle,
                            style = type.headlineSmall,
                            color = colors.onBrandBanner,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = s.guestAccountEmptyDesc,
                            style = type.bodySmall,
                            color = colors.onBrandBanner.copy(alpha = 0.84f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    CleanSupplyPrimaryButton(
                        text = s.signInWithGoogle,
                        icon = CleanSupplyIcons.Login,
                        isLoading = isSigningIn,
                        onClick = onSignInWithGoogle,
                        testTag = "guest_account_sign_in_btn",
                        modifier = Modifier.fillMaxWidth()
                    )
                    CleanSupplySecondaryButton(
                        text = s.signInTitle,
                        onClick = onBackToWelcome,
                        testTag = "guest_back_to_auth_btn",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        AppearanceLanguageCard(
            currentLanguage = currentLanguage,
            currentThemeMode = currentThemeMode,
            onSelectLanguage = onSelectLanguage,
            onSelectThemeMode = onSelectThemeMode,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        )
    }
}
