package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.CommerceRepository
import com.example.data.firestore.SeedData
import com.example.data.model.Address
import com.example.data.model.ApprovedTier
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.CustomerSegment
import com.example.data.model.DeliveryZone
import com.example.data.model.LoyaltyLog
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PackMode
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SupportTicket
import com.example.data.model.TicketCategory
import com.example.data.model.UserProfile
import com.example.domain.CheckoutSummary
import com.example.domain.PricingEngine
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LanguagePreferences
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOption { FEATURED, PRICE_LOW, PRICE_HIGH }

data class CheckoutDraft(
    val selectedAddressId: String? = null,
    val selectedSlot: String = "Today 16:00 - 20:00",
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val paymentReference: String = "",
    val appliedCouponCode: String = "",
    val redeemLoyalty: Boolean = false,
    val couponError: Boolean = false
)

data class CatalogFilters(
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val bulkOnly: Boolean = false,
    val sortOption: SortOption = SortOption.FEATURED
)

class CommerceViewModel(
    private val repository: CommerceRepository,
    private val languagePreferences: LanguagePreferences,
    private val currentUserId: String,
    userDisplayName: String,
    userEmail: String
) : ViewModel() {

    val isAdmin: Boolean = repository.isBootstrappedAdmin()

    val language: StateFlow<AppLanguage> = languagePreferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.ARABIC)

    private val _filters = MutableStateFlow(CatalogFilters())
    val filters: StateFlow<CatalogFilters> = _filters.asStateFlow()

    private val _checkoutDraft = MutableStateFlow(CheckoutDraft())
    val checkoutDraft: StateFlow<CheckoutDraft> = _checkoutDraft.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfileAndSeedIfAdmin(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                preferredLocale = language.value.code
            )
        }
    }

    private fun <T> Flow<T>.toSafeUiState(tag: String, fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tag stream error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .map { it.ifEmpty { SeedData.defaultCategories } }
        .catch { emit(SeedData.defaultCategories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedData.defaultCategories)

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .map { it.ifEmpty { SeedData.defaultProducts } }
        .catch { emit(SeedData.defaultProducts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedData.defaultProducts)

    val zones: StateFlow<List<DeliveryZone>> = repository.observeZones()
        .map { it.ifEmpty { SeedData.defaultZones } }
        .catch { emit(SeedData.defaultZones) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedData.defaultZones)

    val coupons: StateFlow<List<Coupon>> = repository.observeCoupons()
        .map { it.ifEmpty { SeedData.defaultCoupons } }
        .catch { emit(SeedData.defaultCoupons) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedData.defaultCoupons)

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toSafeUiState("userProfile", "Failed to load profile")

    val addresses: StateFlow<List<Address>> =
        repository.observeAddresses(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cartItems: StateFlow<List<CartItem>> =
        repository.observeCart(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val loyaltyLogs: StateFlow<List<LoyaltyLog>> =
        repository.observeLoyaltyLogs(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val ordersState: StateFlow<UiState<List<Order>>> =
        repository.observeOrders(currentUserId, isAdmin)
            .toSafeUiState("orders", "Failed to load orders")

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedOrderItems: StateFlow<List<OrderItem>> =
        _selectedOrderId.flatMapLatest { orderId ->
            if (orderId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeOrderItems(orderId, currentUserId, isAdmin)
                .catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val supportTickets: StateFlow<List<SupportTicket>> =
        repository.observeSupportTickets(currentUserId, isAdmin)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminUsers: StateFlow<List<UserProfile>> =
        if (isAdmin) {
            repository.observeAllUsersForAdmin()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    val filteredProducts: StateFlow<List<Product>> =
        combine(products, _filters, userProfileState) { list, f, profState ->
            val profile = (profState as? UiState.Success)?.data
            val q = f.searchQuery.trim().lowercase()
            val filtered = list.filter { p ->
                val matchesCat = f.selectedCategoryId == null || p.categoryId == f.selectedCategoryId
                val matchesBulk = !f.bulkOnly || p.bulkDiscountPercent > 0L
                val matchesQuery = q.isBlank() ||
                    p.nameAr.lowercase().contains(q) ||
                    p.nameEn.lowercase().contains(q) ||
                    p.sku.lowercase().contains(q) ||
                    p.brand.lowercase().contains(q)
                matchesCat && matchesBulk && matchesQuery
            }
            when (f.sortOption) {
                SortOption.FEATURED -> filtered.sortedByDescending { it.featured }
                SortOption.PRICE_LOW -> filtered.sortedBy { PricingEngine.resolveTierBaseUnitPrice(it, profile) }
                SortOption.PRICE_HIGH -> filtered.sortedByDescending { PricingEngine.resolveTierBaseUnitPrice(it, profile) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedData.defaultProducts)

    val activeAddress: StateFlow<Address?> =
        combine(addresses, _checkoutDraft) { list, draft ->
            list.firstOrNull { it.id == draft.selectedAddressId } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeZone: StateFlow<DeliveryZone?> =
        combine(zones, activeAddress) { zList, addr ->
            zList.firstOrNull { it.id == addr?.zoneId } ?: zList.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeedData.defaultZones.first())

    val checkoutSummary: StateFlow<CheckoutSummary> =
        combine(
            cartItems,
            products,
            userProfileState,
            activeZone,
            combine(coupons, _checkoutDraft) { c, d -> c to d }
        ) { cart, prods, profState, zone, (couponList, draft) ->
            val profile = (profState as? UiState.Success)?.data
            val prodMap = prods.associateBy { it.id }
            val coupon = couponList.firstOrNull { it.code.equals(draft.appliedCouponCode.trim(), ignoreCase = true) }
            PricingEngine.calculateCheckoutSummary(
                cartItems = cart,
                productsById = prodMap,
                profile = profile,
                zone = zone,
                coupon = coupon,
                redeemLoyalty = draft.redeemLoyalty
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            PricingEngine.calculateCheckoutSummary(emptyList(), emptyMap(), null, SeedData.defaultZones.first(), null, false)
        )

    fun toggleLanguage() {
        val next = if (language.value == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
        viewModelScope.launch {
            languagePreferences.setLanguage(next)
            repository.updatePreferredLocale(currentUserId, next.code)
        }
    }

    fun updateSearch(query: String) {
        _filters.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(categoryId: String?) {
        _filters.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun toggleBulkOnly() {
        _filters.update { it.copy(bulkOnly = !it.bulkOnly) }
    }

    fun setSortOption(sort: SortOption) {
        _filters.update { it.copy(sortOption = sort) }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun addOrUpdateCart(productId: String, quantity: Long, packMode: PackMode, confirmationMsg: String? = null) {
        viewModelScope.launch {
            repository.upsertCartItem(currentUserId, productId, quantity, packMode)
                .onSuccess { if (confirmationMsg != null) _snackbarMessage.value = confirmationMsg }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update cart" }
        }
    }

    fun selectAddress(addressId: String) {
        _checkoutDraft.update { it.copy(selectedAddressId = addressId) }
    }

    fun selectDeliverySlot(slot: String) {
        _checkoutDraft.update { it.copy(selectedSlot = slot) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _checkoutDraft.update { it.copy(paymentMethod = method) }
    }

    fun updatePaymentReference(ref: String) {
        _checkoutDraft.update { it.copy(paymentReference = ref) }
    }

    fun toggleRedeemLoyalty(enabled: Boolean) {
        _checkoutDraft.update { it.copy(redeemLoyalty = enabled) }
    }

    fun applyCoupon(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) {
            _checkoutDraft.update { it.copy(appliedCouponCode = "", couponError = false) }
            return
        }
        val profile = (userProfileState.value as? UiState.Success)?.data
        val subtotal = checkoutSummary.value.effectiveSubtotal
        val match = coupons.value.firstOrNull { it.code.equals(clean, ignoreCase = true) }
        val valid = match != null && PricingEngine.isCouponEligible(match, profile, subtotal)
        if (valid) {
            _checkoutDraft.update { it.copy(appliedCouponCode = clean, couponError = false) }
        } else {
            _checkoutDraft.update { it.copy(couponError = true) }
        }
    }

    fun removeCoupon() {
        _checkoutDraft.update { it.copy(appliedCouponCode = "", couponError = false) }
    }

    fun saveAddress(
        label: String,
        recipientName: String,
        phone: String,
        zoneId: String,
        streetDetails: String,
        buildingFloor: String
    ) {
        viewModelScope.launch {
            repository.saveAddress(
                userId = currentUserId,
                addressId = null,
                label = label,
                recipientName = recipientName,
                phone = phone,
                zoneId = zoneId,
                streetDetails = streetDetails,
                buildingFloor = buildingFloor,
                defaultAddress = true
            ).onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun placeOrder(successMessage: String, onOrderCreated: (String) -> Unit) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val address = activeAddress.value ?: return
        val zone = activeZone.value ?: return
        val summary = checkoutSummary.value
        val draft = _checkoutDraft.value
        if (summary.lines.isEmpty() || summary.hasMoqViolation || _isSubmittingOrder.value) return

        viewModelScope.launch {
            _isSubmittingOrder.value = true
            repository.placeOrder(
                userId = currentUserId,
                profile = profile,
                address = address,
                zone = zone,
                deliverySlot = draft.selectedSlot,
                paymentMethod = draft.paymentMethod,
                paymentReference = draft.paymentReference,
                couponCode = draft.appliedCouponCode,
                summary = summary
            ).onSuccess { orderId ->
                _checkoutDraft.update { it.copy(appliedCouponCode = "", redeemLoyalty = false, paymentReference = "") }
                _selectedOrderId.value = orderId
                _snackbarMessage.value = successMessage
                onOrderCreated(orderId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to place order"
            }
            _isSubmittingOrder.value = false
        }
    }

    fun selectOrder(orderId: String?) {
        _selectedOrderId.value = orderId
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelOrder(orderId)
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun reorder(orderId: String, successMsg: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.reorderFromOrder(currentUserId, orderId)
                .onSuccess {
                    _snackbarMessage.value = successMsg
                    onDone()
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun requestSegmentSwitch(segment: CustomerSegment, companyName: String, taxId: String, msg: String) {
        viewModelScope.launch {
            repository.requestSegmentSwitch(currentUserId, segment, companyName, taxId)
                .onSuccess { _snackbarMessage.value = msg }
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun submitSupportTicket(orderId: String, category: TicketCategory, subject: String, message: String, okMsg: String) {
        viewModelScope.launch {
            repository.createSupportTicket(currentUserId, orderId, category, subject, message)
                .onSuccess { _snackbarMessage.value = okMsg }
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    // --- Admin Actions ---
    fun seedCatalog() {
        viewModelScope.launch {
            repository.seedDefaultCatalog()
                .onSuccess { _snackbarMessage.value = "Catalog synchronized" }
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun approveUserTier(targetUserId: String, segment: CustomerSegment, tier: ApprovedTier, creditLimit: Double) {
        viewModelScope.launch {
            repository.approveCustomerAccount(targetUserId, segment, tier, creditLimit)
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun advanceOrder(order: Order) {
        viewModelScope.launch {
            repository.advanceOrderStatus(order)
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun stepDriver(order: Order) {
        viewModelScope.launch {
            repository.stepDriverTowardsDestination(order)
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun toggleFulfillment(order: Order) {
        viewModelScope.launch {
            repository.toggleFulfillmentProvider(order)
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    fun resolveTicket(ticketId: String, reply: String) {
        viewModelScope.launch {
            repository.resolveSupportTicket(ticketId, reply)
                .onFailure { _snackbarMessage.value = it.localizedMessage }
        }
    }

    companion object {
        private const val TAG = "CommerceVM"
    }
}
