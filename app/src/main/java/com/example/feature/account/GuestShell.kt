package com.example.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ROUTE_ACCOUNT
import com.example.ROUTE_CART
import com.example.ROUTE_CATEGORIES
import com.example.ROUTE_CHECKOUT
import com.example.ROUTE_HOME
import com.example.ROUTE_SEARCH
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.FastProductImage
import com.example.core.components.OfflineBanner
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

private data class GuestNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
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
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val connectivityObserver = remember(context) { ConnectivityObserver(context) }
    val isConnected by connectivityObserver.observeIsConnected().collectAsStateWithLifecycle(initialValue = true)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

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
        snackbarHostState.showSnackbar(msg)
        snackbarText = null
    }

    if (inspectedProduct != null) {
        BackHandler { inspectedProduct = null }
    } else if (currentRoute == ROUTE_HOME) {
        BackHandler { onExitGuestToAuth() }
    }

    fun openProduct(prod: Product) {
        recentlyViewedIds = (listOf(prod.id) + recentlyViewedIds.filterNot { it == prod.id }).take(8)
        inspectedProduct = prod
    }

    val recentlyViewedProducts = remember(allProducts, recentlyViewedIds) {
        val byId = allProducts.associateBy { it.id }
        recentlyViewedIds.mapNotNull { byId[it] }
    }

    val filteredProducts = remember(allProducts, filters) {
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

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(ROUTE_HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val destinations = remember(s) {
        listOf(
            GuestNavDestination(ROUTE_HOME, s.navHome, CleanSupplyIcons.NavHomeActive, CleanSupplyIcons.NavHomeInactive),
            GuestNavDestination(ROUTE_CATEGORIES, s.navCategories, CleanSupplyIcons.NavCategoriesActive, CleanSupplyIcons.NavCategoriesInactive),
            GuestNavDestination(ROUTE_SEARCH, s.navSearch, CleanSupplyIcons.NavSearchActive, CleanSupplyIcons.NavSearchInactive),
            GuestNavDestination(ROUTE_CART, s.navCart, CleanSupplyIcons.NavCartActive, CleanSupplyIcons.NavCartInactive),
            GuestNavDestination(ROUTE_ACCOUNT, s.navAccount, CleanSupplyIcons.NavAccountActive, CleanSupplyIcons.NavAccountInactive)
        )
    }

    val showBottomBar = currentRoute != ROUTE_CHECKOUT

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            if (currentRoute == ROUTE_CHECKOUT) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.size(spacing.minTouchTarget)
                                ) {
                                    Icon(CleanSupplyIcons.Back, contentDescription = s.backLabel, tint = colors.textPrimary)
                                }
                            }
                        },
                        title = {
                            if (currentRoute == ROUTE_CHECKOUT) {
                                Text(s.checkoutTitle, style = type.titleMedium, color = colors.textPrimary)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                                ) {
                                    FastProductImage(
                                        drawableRes = R.drawable.ic_cleansupply_logo,
                                        contentDescription = s.appBrand,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(shapes.smallComponent)
                                    )
                                    Text(
                                        text = s.appBrand,
                                        style = type.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        color = colors.primaryContainer,
                                        shape = shapes.pill,
                                        border = BorderStroke(0.8.dp, colors.primary.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = s.guestBadge,
                                            style = type.labelSmall,
                                            color = colors.onPrimaryContainer,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface,
                            titleContentColor = colors.textPrimary
                        ),
                        actions = {
                            OutlinedButton(
                                onClick = onToggleLanguage,
                                shape = shapes.pill,
                                border = BorderStroke(1.dp, colors.outline),
                                contentPadding = PaddingValues(horizontal = spacing.sm, vertical = 0.dp),
                                modifier = Modifier
                                    .padding(end = spacing.xs)
                                    .height(40.dp)
                                    .testTag("top_language_toggle")
                            ) {
                                Icon(CleanSupplyIcons.Language, contentDescription = s.switchLangLabel, tint = colors.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(s.switchLangLabel, style = type.labelMedium, color = colors.textPrimary)
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.8.dp, color = colors.outline)
                    OfflineBanner(isOffline = !isConnected)
                }
            },
            bottomBar = {
                if (!useRail && showBottomBar) {
                    Column {
                        HorizontalDivider(thickness = 0.8.dp, color = colors.outline)
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = colors.surface,
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.primary,
                                        selectedTextColor = colors.primary,
                                        indicatorColor = colors.primaryContainer,
                                        unselectedIconColor = colors.textSecondary,
                                        unselectedTextColor = colors.textSecondary
                                    ),
                                    icon = {
                                        if (dest.route == ROUTE_CART && guestCartItems.isNotEmpty()) {
                                            BadgedBox(badge = {
                                                Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                                                    Text(guestCartItems.size.toString())
                                                }
                                            }) {
                                                Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                            }
                                        } else {
                                            Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                        }
                                    },
                                    label = { Text(dest.label, style = type.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && showBottomBar) {
                    NavigationRail(containerColor = colors.surface) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    if (dest.route == ROUTE_CART && guestCartItems.isNotEmpty()) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                                                Text(guestCartItems.size.toString())
                                            }
                                        }) {
                                            Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label, style = type.labelSmall) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        enterTransition = { fadeIn(animationSpec = tween(150)) },
                        exitTransition = { fadeOut(animationSpec = tween(110)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                        popExitTransition = { fadeOut(animationSpec = tween(110)) },
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
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
                                onOpenSearch = { navigateTo(ROUTE_SEARCH) },
                                onOpenAllCategories = { navigateTo(ROUTE_CATEGORIES) },
                                onExploreBulkDeals = {
                                    filters = filters.copy(bulkOnly = true)
                                    navigateTo(ROUTE_SEARCH)
                                },
                                onSelectCategory = { catId ->
                                    filters = filters.copy(selectedCategoryId = catId)
                                    navigateTo(ROUTE_SEARCH)
                                },
                                onOpenProductDetail = ::openProduct,
                                onQuickAdd = { prod ->
                                    val moq = PricingEngine.resolveMoqUnits(prod, null)
                                    upsertGuestCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                                },
                                onReorder = {},
                                onOpenAllCatalog = {
                                    filters = filters.copy(selectedCategoryId = null)
                                    navigateTo(ROUTE_SEARCH)
                                }
                            )
                        }

                        composable(ROUTE_CATEGORIES) {
                            CategoriesScreen(
                                categories = categories,
                                products = allProducts,
                                selectedCategoryId = filters.selectedCategoryId,
                                onSelectCategory = { catId ->
                                    filters = filters.copy(selectedCategoryId = catId)
                                    navigateTo(ROUTE_SEARCH)
                                }
                            )
                        }

                        composable(ROUTE_SEARCH) {
                            SearchCatalogScreen(
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
                                }
                            )
                        }

                        composable(ROUTE_CART) {
                            CartScreen(
                                summary = checkoutSummary,
                                profile = null,
                                coupons = coupons,
                                draft = draft,
                                onBrowseCatalog = { navigateTo(ROUTE_SEARCH) },
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
                                onProceedToCheckout = { navController.navigate(ROUTE_CHECKOUT) }
                            )
                        }

                        composable(ROUTE_CHECKOUT) {
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
                                onBack = { navController.popBackStack() },
                                isGuest = true,
                                onSignInToCheckout = { triggerGoogleSignIn() }
                            )
                        }

                        composable(ROUTE_ACCOUNT) {
                            GuestAccountScreen(
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
private fun GuestAccountScreen(
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
            Column(
                modifier = Modifier.padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(colors.primaryContainer, shapes.largeSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CleanSupplyIcons.NavAccountInactive,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = s.guestSignInBannerTitle,
                    style = type.headlineSmall,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = s.guestAccountEmptyDesc,
                    style = type.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
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
