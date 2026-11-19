package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.components.FastProductImage
import com.example.core.components.OfflineBanner
import com.example.core.components.ProductDetailSheet
import com.example.core.designsystem.AppThemeMode
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.network.ConnectivityObserver
import com.example.data.firestore.CommerceRepository
import com.example.data.model.CartItem
import com.example.data.model.PackMode
import com.example.data.model.Product
import com.example.domain.PricingEngine
import com.example.feature.account.AccountScreen
import com.example.feature.account.GuestMainShell
import com.example.feature.auth.AuthScreen
import com.example.feature.auth.signOut
import com.example.feature.cart.CartScreen
import com.example.feature.cart.CheckoutScreen
import com.example.feature.catalog.CategoriesScreen
import com.example.feature.catalog.SearchCatalogScreen
import com.example.feature.home.HomeScreen
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LanguagePreferences
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CommerceViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_SEARCH = "search"
const val ROUTE_CART = "cart"
const val ROUTE_CHECKOUT = "checkout"
const val ROUTE_ACCOUNT = "account"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) { LanguagePreferences(context.applicationContext) }
            val currentLang by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ARABIC)
            val currentThemeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val recentSearches by prefs.recentSearchesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            val scope = rememberCoroutineScope()

            CleanSupplyTheme(language = currentLang, themeMode = currentThemeMode) {
                AppNavigation(
                    currentLanguage = currentLang,
                    currentThemeMode = currentThemeMode,
                    recentSearches = recentSearches,
                    onToggleUnauthLanguage = {
                        val next = if (currentLang == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
                        scope.launch { prefs.setLanguage(next) }
                    },
                    onSelectUnauthLanguage = { lang ->
                        scope.launch { prefs.setLanguage(lang) }
                    },
                    onSelectThemeMode = { mode ->
                        scope.launch { prefs.setThemeMode(mode) }
                    },
                    onCommitRecentSearch = { query ->
                        scope.launch { prefs.addRecentSearch(query) }
                    },
                    onClearRecentSearches = {
                        scope.launch { prefs.clearRecentSearches() }
                    }
                )
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    recentSearches: List<String>,
    onToggleUnauthLanguage: () -> Unit,
    onSelectUnauthLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onCommitRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var isGuestMode by rememberSaveable { mutableStateOf(false) }
    var guestCartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }

    if (user == null) {
        if (isGuestMode) {
            GuestMainShell(
                guestCartItems = guestCartItems,
                currentLanguage = currentLanguage,
                currentThemeMode = currentThemeMode,
                recentSearches = recentSearches,
                onUpdateGuestCart = { guestCartItems = it },
                onToggleLanguage = onToggleUnauthLanguage,
                onSelectLanguage = onSelectUnauthLanguage,
                onSelectThemeMode = onSelectThemeMode,
                onCommitRecentSearch = onCommitRecentSearch,
                onClearRecentSearches = onClearRecentSearches,
                onExitGuestToAuth = { isGuestMode = false }
            )
        } else {
            AuthScreen(
                onToggleLanguage = onToggleUnauthLanguage,
                onAuthSuccess = {},
                onContinueAsGuest = { isGuestMode = true }
            )
        }
    } else {
        LaunchedEffect(user.uid) {
            isGuestMode = false
        }
        key(user.uid) {
            val viewModel: CommerceViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        CommerceViewModel(
                            repository = CommerceRepository(app),
                            languagePreferences = LanguagePreferences(app),
                            currentUserId = user.uid,
                            userDisplayName = user.displayName ?: "CleanSupply Customer",
                            userEmail = user.email ?: ""
                        )
                    }
                }
            )
            LaunchedEffect(user.uid, guestCartItems) {
                if (guestCartItems.isNotEmpty()) {
                    viewModel.importGuestCart(guestCartItems)
                    guestCartItems = emptyList()
                }
            }
            AuthenticatedMainShell(viewModel = viewModel)
        }
    }
}

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainShell(viewModel: CommerceViewModel) {
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

    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewedProducts.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val checkoutSummary by viewModel.checkoutSummary.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val activeAddress by viewModel.activeAddress.collectAsStateWithLifecycle()
    val zones by viewModel.zones.collectAsStateWithLifecycle()
    val activeZone by viewModel.activeZone.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val draft by viewModel.checkoutDraft.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmittingOrder.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val selectedOrderId by viewModel.selectedOrderId.collectAsStateWithLifecycle()
    val selectedOrderItems by viewModel.selectedOrderItems.collectAsStateWithLifecycle()
    val loyaltyLogs by viewModel.loyaltyLogs.collectAsStateWithLifecycle()
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var inspectedProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    if (inspectedProduct != null) {
        BackHandler { inspectedProduct = null }
    }

    fun openProduct(prod: Product) {
        viewModel.recordProductViewed(prod.id)
        inspectedProduct = prod
    }

    val destinations = remember(s) {
        listOf(
            NavDestination(ROUTE_HOME, s.navHome, CleanSupplyIcons.NavHomeActive, CleanSupplyIcons.NavHomeInactive),
            NavDestination(ROUTE_CATEGORIES, s.navCategories, CleanSupplyIcons.NavCategoriesActive, CleanSupplyIcons.NavCategoriesInactive),
            NavDestination(ROUTE_SEARCH, s.navSearch, CleanSupplyIcons.NavSearchActive, CleanSupplyIcons.NavSearchInactive),
            NavDestination(ROUTE_CART, s.navCart, CleanSupplyIcons.NavCartActive, CleanSupplyIcons.NavCartInactive),
            NavDestination(ROUTE_ACCOUNT, s.navAccount, CleanSupplyIcons.NavAccountActive, CleanSupplyIcons.NavAccountInactive)
        )
    }

    val cartBadgeCount = cartItems.size
    val showBottomBar = currentRoute != ROUTE_CHECKOUT

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(ROUTE_HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface,
                            titleContentColor = colors.textPrimary
                        ),
                        actions = {
                            OutlinedButton(
                                onClick = { viewModel.toggleLanguage() },
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
                                        if (dest.route == ROUTE_CART && cartBadgeCount > 0) {
                                            BadgedBox(badge = {
                                                Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                                                    Text(cartBadgeCount.toString())
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
                                    if (dest.route == ROUTE_CART && cartBadgeCount > 0) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                                                Text(cartBadgeCount.toString())
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
                                profile = profile,
                                activeZone = activeZone,
                                categories = categories,
                                products = allProducts,
                                recentlyViewed = recentlyViewed,
                                recentOrders = (ordersState as? UiState.Success)?.data.orEmpty(),
                                isRefreshing = isRefreshing,
                                onRefresh = viewModel::triggerRefresh,
                                onOpenSearch = { navigateTo(ROUTE_SEARCH) },
                                onOpenAllCategories = { navigateTo(ROUTE_CATEGORIES) },
                                onExploreBulkDeals = {
                                    viewModel.setBulkOnly(true)
                                    navigateTo(ROUTE_SEARCH)
                                },
                                onSelectCategory = { catId ->
                                    viewModel.selectCategory(catId)
                                    navigateTo(ROUTE_SEARCH)
                                },
                                onOpenProductDetail = ::openProduct,
                                onQuickAdd = { prod ->
                                    val moq = PricingEngine.resolveMoqUnits(prod, profile)
                                    viewModel.addOrUpdateCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                                },
                                onReorder = { orderId ->
                                    viewModel.reorder(orderId, s.addedToCart) { navigateTo(ROUTE_CART) }
                                },
                                onOpenAllCatalog = {
                                    viewModel.selectCategory(null)
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
                                    viewModel.selectCategory(catId)
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
                                profile = profile,
                                isRefreshing = isRefreshing,
                                onRefresh = viewModel::triggerRefresh,
                                onSearchChange = viewModel::updateSearch,
                                onCommitSearch = viewModel::commitRecentSearch,
                                onClearRecentSearches = viewModel::clearRecentSearches,
                                onSelectCategory = viewModel::selectCategory,
                                onToggleBulkOnly = viewModel::toggleBulkOnly,
                                onToggleInStockOnly = viewModel::toggleInStockOnly,
                                onSelectSort = viewModel::setSortOption,
                                onOpenProductDetail = ::openProduct,
                                onQuickAdd = { prod ->
                                    val moq = PricingEngine.resolveMoqUnits(prod, profile)
                                    viewModel.addOrUpdateCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                                }
                            )
                        }

                        composable(ROUTE_CART) {
                            CartScreen(
                                summary = checkoutSummary,
                                profile = profile,
                                coupons = coupons,
                                draft = draft,
                                onBrowseCatalog = { navigateTo(ROUTE_SEARCH) },
                                onUpdateCartQty = { prodId, qty, mode ->
                                    viewModel.addOrUpdateCart(prodId, qty, mode)
                                },
                                onApplyCoupon = viewModel::applyCoupon,
                                onRemoveCoupon = viewModel::removeCoupon,
                                onToggleLoyalty = viewModel::toggleRedeemLoyalty,
                                onProceedToCheckout = { navController.navigate(ROUTE_CHECKOUT) }
                            )
                        }

                        composable(ROUTE_CHECKOUT) {
                            CheckoutScreen(
                                summary = checkoutSummary,
                                profile = profile,
                                addresses = addresses,
                                activeAddress = activeAddress,
                                zones = zones,
                                activeZone = activeZone,
                                draft = draft,
                                isSubmitting = isSubmitting,
                                onSelectAddress = viewModel::selectAddress,
                                onSaveNewAddress = viewModel::saveAddress,
                                onSelectSlot = viewModel::selectDeliverySlot,
                                onSelectPayment = viewModel::selectPaymentMethod,
                                onPaymentRefChange = viewModel::updatePaymentReference,
                                onPlaceOrder = {
                                    viewModel.placeOrder(s.orderPlacedSuccess) {
                                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                                        navigateTo(ROUTE_ACCOUNT)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ACCOUNT) {
                            AccountScreen(
                                isAdmin = viewModel.isAdmin,
                                profile = profile,
                                addresses = addresses,
                                zones = zones,
                                currentLanguage = currentLanguage,
                                currentThemeMode = currentThemeMode,
                                ordersState = ordersState,
                                selectedOrderId = selectedOrderId,
                                selectedOrderItems = selectedOrderItems,
                                loyaltyLogs = loyaltyLogs,
                                supportTickets = supportTickets,
                                adminUsers = adminUsers,
                                onSelectOrder = viewModel::selectOrder,
                                onCancelOrder = viewModel::cancelOrder,
                                onReorder = { orderId ->
                                    viewModel.reorder(orderId, s.addedToCart) { navigateTo(ROUTE_CART) }
                                },
                                onSaveAddress = viewModel::saveAddress,
                                onDeleteAddress = viewModel::deleteAddress,
                                onRequestSegment = { seg, comp, tax ->
                                    viewModel.requestSegmentSwitch(seg, comp, tax, s.saveBtn)
                                },
                                onSubmitTicket = { oid, cat, subj, msg ->
                                    viewModel.submitSupportTicket(oid, cat, subj, msg, s.submitTicket)
                                },
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onSeedCatalog = viewModel::seedCatalog,
                                onApproveUserTier = viewModel::approveUserTier,
                                onAdvanceOrder = viewModel::advanceOrder,
                                onStepDriver = viewModel::stepDriver,
                                onToggleFulfillment = viewModel::toggleFulfillment,
                                onResolveTicket = viewModel::resolveTicket,
                                onCopyTrackingToast = { code ->
                                    viewModel.showMessage("${s.copyTracking}: $code")
                                },
                                onRetryOrders = viewModel::triggerRefresh,
                                onSignOut = {
                                    signOut(credentialManager, scope)
                                }
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
            profile = profile,
            onDismiss = { inspectedProduct = null },
            onSelectRelated = ::openProduct,
            onAddToCart = { qty, mode ->
                viewModel.addOrUpdateCart(prod.id, qty, mode, s.addedToCart)
                inspectedProduct = null
            }
        )
    }
}
