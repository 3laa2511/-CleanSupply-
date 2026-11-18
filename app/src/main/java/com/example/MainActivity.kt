package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.data.firestore.CommerceRepository
import com.example.data.model.PackMode
import com.example.data.model.Product
import com.example.domain.PricingEngine
import com.example.ui.components.ProductDetailSheet
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LanguagePreferences
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersAccountScreen
import com.example.ui.screens.signOut
import com.example.ui.theme.CleanSupplyTheme
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
const val ROUTE_CATALOG = "catalog"
const val ROUTE_CART = "cart"
const val ROUTE_ACCOUNT = "account"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) { LanguagePreferences(context.applicationContext) }
            val currentLang by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ARABIC)
            val scope = rememberCoroutineScope()

            CleanSupplyTheme(language = currentLang) {
                AppNavigation(
                    onToggleUnauthLanguage = {
                        val next = if (currentLang == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
                        scope.launch { prefs.setLanguage(next) }
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
    onToggleUnauthLanguage: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onToggleLanguage = onToggleUnauthLanguage,
            onAuthSuccess = {}
        )
    } else {
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
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

    val destinations = remember(s) {
        listOf(
            NavDestination(ROUTE_HOME, s.navHome, Icons.Filled.Home, Icons.Outlined.Home),
            NavDestination(ROUTE_CATALOG, s.navCatalog, Icons.Filled.Category, Icons.Outlined.Category),
            NavDestination(ROUTE_CART, s.navCart, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
            NavDestination(ROUTE_ACCOUNT, s.navAccount, Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val cartBadgeCount = cartItems.size

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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = s.appBrand,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.secondary
                    ),
                    actions = {
                        TextButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("top_language_toggle")
                        ) {
                            Icon(Icons.Default.Language, contentDescription = s.switchLangLabel)
                            Text(" ${s.switchLangLabel}")
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    if (dest.route == ROUTE_CART && cartBadgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                            Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
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
                if (useRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    if (dest.route == ROUTE_CART && cartBadgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                            Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label) }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 900.dp)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                profile = profile,
                                categories = categories,
                                products = allProducts,
                                recentOrders = (ordersState as? UiState.Success)?.data.orEmpty(),
                                onExploreBulkDeals = {
                                    if (!filters.bulkOnly) viewModel.toggleBulkOnly()
                                    navigateTo(ROUTE_CATALOG)
                                },
                                onSelectCategory = { catId ->
                                    viewModel.selectCategory(catId)
                                    navigateTo(ROUTE_CATALOG)
                                },
                                onOpenProductDetail = { inspectedProduct = it },
                                onQuickAdd = { prod ->
                                    val moq = PricingEngine.resolveMoqUnits(prod, profile)
                                    viewModel.addOrUpdateCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                                },
                                onReorder = { orderId ->
                                    viewModel.reorder(orderId, s.addedToCart) { navigateTo(ROUTE_CART) }
                                },
                                onOpenAllCatalog = {
                                    viewModel.selectCategory(null)
                                    navigateTo(ROUTE_CATALOG)
                                }
                            )
                        }

                        composable(ROUTE_CATALOG) {
                            CatalogScreen(
                                categories = categories,
                                products = filteredProducts,
                                filters = filters,
                                profile = profile,
                                onSearchChange = viewModel::updateSearch,
                                onSelectCategory = viewModel::selectCategory,
                                onToggleBulkOnly = viewModel::toggleBulkOnly,
                                onSelectSort = viewModel::setSortOption,
                                onOpenProductDetail = { inspectedProduct = it },
                                onQuickAdd = { prod ->
                                    val moq = PricingEngine.resolveMoqUnits(prod, profile)
                                    viewModel.addOrUpdateCart(prod.id, moq, PackMode.UNIT, s.addedToCart)
                                }
                            )
                        }

                        composable(ROUTE_CART) {
                            CartCheckoutScreen(
                                summary = checkoutSummary,
                                profile = profile,
                                addresses = addresses,
                                activeAddress = activeAddress,
                                zones = zones,
                                activeZone = activeZone,
                                coupons = coupons,
                                draft = draft,
                                isSubmitting = isSubmitting,
                                onBrowseCatalog = { navigateTo(ROUTE_CATALOG) },
                                onUpdateCartQty = { prodId, qty, mode ->
                                    viewModel.addOrUpdateCart(prodId, qty, mode)
                                },
                                onApplyCoupon = viewModel::applyCoupon,
                                onRemoveCoupon = viewModel::removeCoupon,
                                onToggleLoyalty = viewModel::toggleRedeemLoyalty,
                                onSelectAddress = viewModel::selectAddress,
                                onSaveNewAddress = viewModel::saveAddress,
                                onSelectSlot = viewModel::selectDeliverySlot,
                                onSelectPayment = viewModel::selectPaymentMethod,
                                onPaymentRefChange = viewModel::updatePaymentReference,
                                onPlaceOrder = {
                                    viewModel.placeOrder(s.orderPlacedSuccess) {
                                        navigateTo(ROUTE_ACCOUNT)
                                    }
                                }
                            )
                        }

                        composable(ROUTE_ACCOUNT) {
                            OrdersAccountScreen(
                                isAdmin = viewModel.isAdmin,
                                profile = profile,
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
                                onRequestSegment = { seg, comp, tax ->
                                    viewModel.requestSegmentSwitch(seg, comp, tax, s.saveBtn)
                                },
                                onSubmitTicket = { oid, cat, subj, msg ->
                                    viewModel.submitSupportTicket(oid, cat, subj, msg, s.submitTicket)
                                },
                                onSeedCatalog = viewModel::seedCatalog,
                                onApproveUserTier = viewModel::approveUserTier,
                                onAdvanceOrder = viewModel::advanceOrder,
                                onStepDriver = viewModel::stepDriver,
                                onToggleFulfillment = viewModel::toggleFulfillment,
                                onResolveTicket = viewModel::resolveTicket,
                                onCopyTrackingToast = { code ->
                                    viewModel.showMessage("${s.copyTracking}: $code")
                                },
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
        ProductDetailSheet(
            product = prod,
            profile = profile,
            onDismiss = { inspectedProduct = null },
            onAddToCart = { qty, mode ->
                viewModel.addOrUpdateCart(prod.id, qty, mode, s.addedToCart)
                inspectedProduct = null
            }
        )
    }
}
