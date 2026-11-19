package com.example.feature.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.components.CleanSupplyConfirmationDialog
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.SegmentVerificationBadge
import com.example.core.designsystem.AppThemeMode
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Address
import com.example.data.model.ApprovedTier
import com.example.data.model.CustomerSegment
import com.example.data.model.DeliveryZone
import com.example.data.model.LoyaltyLog
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.SupportTicket
import com.example.data.model.TicketCategory
import com.example.data.model.UserProfile
import com.example.feature.cart.AddAddressDialog
import com.example.feature.orders.OperationsConsoleTab
import com.example.feature.orders.OrdersListTab
import com.example.feature.orders.OrderTrackingDetailSheet
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    isAdmin: Boolean,
    profile: UserProfile?,
    addresses: List<Address>,
    zones: List<DeliveryZone>,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    ordersState: UiState<List<Order>>,
    selectedOrderId: String?,
    selectedOrderItems: List<OrderItem>,
    loyaltyLogs: List<LoyaltyLog>,
    supportTickets: List<SupportTicket>,
    adminUsers: List<UserProfile>,
    onSelectOrder: (String?) -> Unit,
    onCancelOrder: (String) -> Unit,
    onReorder: (String) -> Unit,
    onSaveAddress: (String, String, String, String, String, String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onRequestSegment: (CustomerSegment, String, String) -> Unit,
    onSubmitTicket: (String, TicketCategory, String, String) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSeedCatalog: () -> Unit,
    onApproveUserTier: (String, CustomerSegment, ApprovedTier, Double) -> Unit,
    onAdvanceOrder: (Order) -> Unit,
    onStepDriver: (Order) -> Unit,
    onToggleFulfillment: (Order) -> Unit,
    onResolveTicket: (String, String) -> Unit,
    onCopyTrackingToast: (String) -> Unit,
    onRetryOrders: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val type = CleanSupplyTheme.typography

    val tabs = remember(isAdmin, s) {
        buildList {
            add(s.ordersTab)
            add(s.loyaltySupportTab)
            add(s.profileTab)
            if (isAdmin) add(s.opsTab)
        }
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var ticketOrderPreset by remember { mutableStateOf<String?>(null) }

    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val inspectedOrder = orders.firstOrNull { it.id == selectedOrderId }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex),
            containerColor = colors.surface,
            contentColor = colors.primary
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    selectedContentColor = colors.primary,
                    unselectedContentColor = colors.textSecondary,
                    text = {
                        Text(
                            text = title,
                            style = type.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("account_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> OrdersListTab(
                ordersState = ordersState,
                onSelectOrder = onSelectOrder,
                onReorder = onReorder,
                onRetry = onRetryOrders
            )
            1 -> LoyaltySupportTab(
                profile = profile,
                loyaltyLogs = loyaltyLogs,
                supportTickets = supportTickets,
                presetOrderId = ticketOrderPreset,
                onClearPreset = { ticketOrderPreset = null },
                onSubmitTicket = onSubmitTicket
            )
            2 -> ProfileSettingsTab(
                profile = profile,
                addresses = addresses,
                zones = zones,
                currentLanguage = currentLanguage,
                currentThemeMode = currentThemeMode,
                onSaveAddress = onSaveAddress,
                onDeleteAddress = onDeleteAddress,
                onRequestSegment = onRequestSegment,
                onSelectLanguage = onSelectLanguage,
                onSelectThemeMode = onSelectThemeMode,
                onSignOut = onSignOut
            )
            3 -> if (isAdmin) {
                OperationsConsoleTab(
                    adminUsers = adminUsers,
                    orders = orders,
                    tickets = supportTickets,
                    onSeedCatalog = onSeedCatalog,
                    onApproveUserTier = onApproveUserTier,
                    onAdvanceOrder = onAdvanceOrder,
                    onStepDriver = onStepDriver,
                    onToggleFulfillment = onToggleFulfillment,
                    onResolveTicket = onResolveTicket
                )
            }
        }
    }

    if (inspectedOrder != null) {
        OrderTrackingDetailSheet(
            order = inspectedOrder,
            items = selectedOrderItems,
            onDismiss = { onSelectOrder(null) },
            onCancel = { onCancelOrder(inspectedOrder.id) },
            onReorder = {
                onSelectOrder(null)
                onReorder(inspectedOrder.id)
            },
            onReportIssue = {
                onSelectOrder(null)
                ticketOrderPreset = inspectedOrder.orderNumber
                selectedTab = 1
            },
            onCopyTracking = onCopyTrackingToast
        )
    }
}

@Composable
private fun LoyaltySupportTab(
    profile: UserProfile?,
    loyaltyLogs: List<LoyaltyLog>,
    supportTickets: List<SupportTicket>,
    presetOrderId: String?,
    onClearPreset: () -> Unit,
    onSubmitTicket: (String, TicketCategory, String, String) -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    var showTicketDialog by remember(presetOrderId) { mutableStateOf(presetOrderId != null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item(key = "loyalty_wallet_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.largeSurface)
                    .background(Brush.linearGradient(listOf(colors.brandSlate, colors.brandSlateSurface)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                        Text(s.loyaltyPointsTitle, style = type.labelMedium, color = colors.primaryContainer)
                        Text(
                            text = "${profile?.loyaltyPoints ?: 0L} pts",
                            style = type.displaySmall,
                            color = colors.onPrimary
                        )
                        Surface(
                            color = colors.primary.copy(alpha = 0.22f),
                            shape = shapes.pill
                        ) {
                            Text(
                                text = Formatters.money((profile?.loyaltyPoints ?: 0L) / 10.0, s.currency),
                                style = type.numericSmall,
                                color = colors.onPrimary,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(colors.warning.copy(alpha = 0.20f), shapes.card),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CleanSupplyIcons.Loyalty,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        if (loyaltyLogs.isNotEmpty()) {
            items(loyaltyLogs.take(5), key = { it.id }) { log ->
                Surface(
                    color = colors.surface,
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (s.isArabic) log.titleAr else log.titleEn,
                            style = type.bodyMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (log.pointsDelta >= 0) "+${log.pointsDelta}" else "${log.pointsDelta}",
                            style = type.numericSmall,
                            color = if (log.pointsDelta >= 0) colors.success else colors.warning
                        )
                    }
                }
            }
        }

        item(key = "tickets_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.supportTicketsTitle, style = type.titleMedium, color = colors.textPrimary)
                CleanSupplyPrimaryButton(
                    text = s.newSupportTicket,
                    icon = CleanSupplyIcons.Support,
                    onClick = { showTicketDialog = true }
                )
            }
        }

        items(supportTickets, key = { it.id }) { ticket ->
            Card(
                shape = shapes.card,
                border = BorderStroke(1.dp, colors.outline),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ticket.subject, style = type.titleSmall, color = colors.textPrimary)
                        Surface(color = colors.primaryContainer, shape = shapes.pill) {
                            Text(
                                text = ticket.status,
                                style = type.labelSmall,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = spacing.xs, vertical = 3.dp)
                            )
                        }
                    }
                    Text(ticket.message, style = type.bodySmall, color = colors.textSecondary)
                    if (ticket.adminReply.isNotBlank()) {
                        Surface(
                            color = colors.successContainer,
                            shape = shapes.smallComponent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ticket.adminReply,
                                style = type.bodySmall,
                                color = colors.success,
                                modifier = Modifier.padding(spacing.sm)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTicketDialog) {
        var orderRef by remember(presetOrderId) { mutableStateOf(presetOrderId.orEmpty()) }
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf(TicketCategory.GENERAL) }

        AlertDialog(
            onDismissRequest = {
                showTicketDialog = false
                onClearPreset()
            },
            shape = shapes.dialog,
            containerColor = colors.surface,
            title = { Text(s.newSupportTicket, style = type.titleLarge, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                        items(TicketCategory.entries) { c ->
                            FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c.name, style = type.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = orderRef, onValueChange = { orderRef = it }, label = { Text("Order #") }, singleLine = true, shape = shapes.input)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(if (s.isArabic) "العنوان" else "Subject") }, singleLine = true, shape = shapes.input)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text(if (s.isArabic) "التفاصيل" else "Details") }, shape = shapes.input)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && message.isNotBlank()) {
                            onSubmitTicket(orderRef, cat, subject, message)
                            showTicketDialog = false
                            onClearPreset()
                        }
                    },
                    shape = shapes.button
                ) { Text(s.submitTicket) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTicketDialog = false
                    onClearPreset()
                }) { Text(s.closeBtn) }
            }
        )
    }
}

@Composable
private fun ProfileSettingsTab(
    profile: UserProfile?,
    addresses: List<Address>,
    zones: List<DeliveryZone>,
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    onSaveAddress: (String, String, String, String, String, String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onRequestSegment: (CustomerSegment, String, String) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    var selectedSegment by remember(profile?.segment) {
        mutableStateOf(profile?.segmentEnum() ?: CustomerSegment.RETAIL)
    }
    var companyName by remember(profile?.companyName) { mutableStateOf(profile?.companyName.orEmpty()) }
    var taxId by remember(profile?.taxId) { mutableStateOf(profile?.taxId.orEmpty()) }

    var showAddAddressDialog by remember { mutableStateOf(false) }
    var addressPendingDelete by remember { mutableStateOf<Address?>(null) }
    var confirmSignOutOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        // Identity & Credit Card
        item(key = "profile_header") {
            Card(
                shape = shapes.largeSurface,
                border = BorderStroke(1.dp, colors.outline),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(colors.primaryContainer, shapes.button),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(CleanSupplyIcons.NavAccountActive, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(profile?.displayName.orEmpty(), style = type.titleMedium, color = colors.textPrimary)
                                Text(profile?.email.orEmpty(), style = type.bodySmall, color = colors.textSecondary)
                            }
                        }
                        SegmentVerificationBadge(profile)
                    }

                    if ((profile?.creditLimit ?: 0.0) > 0.0) {
                        Surface(
                            color = colors.primaryContainer.copy(alpha = 0.65f),
                            shape = shapes.button,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (s.isArabic) "الحد الائتماني المتاح" else "Available B2B Credit"}: ${Formatters.money(profile?.availableCredit ?: 0.0, s.currency)}",
                                style = type.numericSmall,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(spacing.sm)
                            )
                        }
                    }
                }
            }
        }

        // Saved Addresses Management Card
        item(key = "saved_addresses_card") {
            Card(
                shape = shapes.card,
                border = BorderStroke(1.dp, colors.outline),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.savedAddressesTitle, style = type.titleMedium, color = colors.textPrimary)
                        TextButton(
                            onClick = { showAddAddressDialog = true },
                            modifier = Modifier.height(spacing.minTouchTarget)
                        ) {
                            Icon(CleanSupplyIcons.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.addAddress, style = type.labelLarge, color = colors.primary)
                        }
                    }

                    if (addresses.isEmpty()) {
                        Text(
                            text = if (s.isArabic) "لا توجد عناوين محفوظة بعد." else "No saved addresses yet.",
                            style = type.bodySmall,
                            color = colors.textSecondary
                        )
                    } else {
                        addresses.forEach { addr ->
                            val zName = zones.firstOrNull { it.id == addr.zoneId }?.localizedName(s.isArabic) ?: addr.zoneId
                            Surface(
                                color = colors.surfaceVariant,
                                shape = shapes.button,
                                border = BorderStroke(1.dp, colors.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                                ) {
                                    Icon(CleanSupplyIcons.Location, contentDescription = null, tint = colors.primary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${addr.label} • $zName", style = type.labelLarge, color = colors.textPrimary)
                                        Text("${addr.streetDetails} (${addr.phone})", style = type.bodySmall, color = colors.textSecondary)
                                    }
                                    IconButton(
                                        onClick = { addressPendingDelete = addr },
                                        modifier = Modifier.size(spacing.minTouchTarget)
                                    ) {
                                        Icon(CleanSupplyIcons.Delete, contentDescription = s.deleteAddress, tint = colors.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Segment Upgrade Card
        item(key = "profile_segment") {
            Card(
                shape = shapes.card,
                border = BorderStroke(1.dp, colors.outline),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Text(s.accountSegmentTitle, style = type.titleMedium, color = colors.textPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        items(CustomerSegment.entries) { seg ->
                            val label = when (seg) {
                                CustomerSegment.RETAIL -> s.retailLabel
                                CustomerSegment.WHOLESALE -> s.wholesaleLabel
                                CustomerSegment.BUSINESS -> s.businessLabel
                            }
                            FilterChip(
                                selected = selectedSegment == seg,
                                onClick = { selectedSegment = seg },
                                shape = shapes.pill,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = colors.surfaceVariant,
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer
                                ),
                                label = { Text(label, style = type.labelMedium) }
                            )
                        }
                    }

                    if (selectedSegment != CustomerSegment.RETAIL) {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text(s.companyNameHint) },
                            singleLine = true,
                            shape = shapes.input,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text(s.taxIdHint) },
                            singleLine = true,
                            shape = shapes.input,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    CleanSupplyPrimaryButton(
                        text = s.requestUpgrade,
                        onClick = { onRequestSegment(selectedSegment, companyName, taxId) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Appearance & Language Settings Card
        item(key = "appearance_language") {
            AppearanceLanguageCard(
                currentLanguage = currentLanguage,
                currentThemeMode = currentThemeMode,
                onSelectLanguage = onSelectLanguage,
                onSelectThemeMode = onSelectThemeMode
            )
        }

        // Sign Out
        item(key = "sign_out") {
            CleanSupplySecondaryButton(
                text = s.signOut,
                icon = CleanSupplyIcons.Logout,
                contentColor = colors.error,
                borderColor = colors.error.copy(alpha = 0.45f),
                onClick = { confirmSignOutOpen = true },
                testTag = "sign_out_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAddAddressDialog) {
        AddAddressDialog(
            zones = zones,
            onDismiss = { showAddAddressDialog = false },
            onSave = { label, rec, phone, zoneId, street, floor ->
                onSaveAddress(label, rec, phone, zoneId, street, floor)
                showAddAddressDialog = false
            }
        )
    }

    addressPendingDelete?.let { addr ->
        CleanSupplyConfirmationDialog(
            title = s.deleteAddress,
            body = "${addr.label} • ${addr.streetDetails}",
            confirmText = s.confirmAction,
            dismissText = s.closeBtn,
            isDestructive = true,
            onConfirm = {
                onDeleteAddress(addr.id)
                addressPendingDelete = null
            },
            onDismiss = { addressPendingDelete = null }
        )
    }

    if (confirmSignOutOpen) {
        CleanSupplyConfirmationDialog(
            title = s.signOut,
            body = if (s.isArabic) "هل تريد تسجيل الخروج من حسابك؟" else "Sign out of your CleanSupply account?",
            confirmText = s.signOut,
            dismissText = s.closeBtn,
            isDestructive = true,
            onConfirm = {
                confirmSignOutOpen = false
                onSignOut()
            },
            onDismiss = { confirmSignOutOpen = false }
        )
    }
}

@Composable
fun AppearanceLanguageCard(
    currentLanguage: AppLanguage,
    currentThemeMode: AppThemeMode,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = colors.surfaceVariant,
        labelColor = colors.textPrimary,
        iconColor = colors.textSecondary,
        selectedContainerColor = colors.primaryContainer,
        selectedLabelColor = colors.onPrimaryContainer,
        selectedLeadingIconColor = colors.primary
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.card,
        border = BorderStroke(1.dp, colors.outline),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(s.appearanceLanguageTitle, style = type.titleMedium, color = colors.textPrimary)

            Text(s.languageSettingLabel, style = type.labelMedium, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                FilterChip(
                    selected = currentLanguage == AppLanguage.ARABIC,
                    onClick = { onSelectLanguage(AppLanguage.ARABIC) },
                    shape = shapes.pill,
                    colors = chipColors,
                    leadingIcon = { Icon(CleanSupplyIcons.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("العربية", style = type.labelMedium) },
                    modifier = Modifier.testTag("lang_chip_ar")
                )
                FilterChip(
                    selected = currentLanguage == AppLanguage.ENGLISH,
                    onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
                    shape = shapes.pill,
                    colors = chipColors,
                    leadingIcon = { Icon(CleanSupplyIcons.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("English", style = type.labelMedium) },
                    modifier = Modifier.testTag("lang_chip_en")
                )
            }

            HorizontalDivider(color = colors.outline)

            Text(s.themeSettingLabel, style = type.labelMedium, color = colors.textSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                item {
                    FilterChip(
                        selected = currentThemeMode == AppThemeMode.SYSTEM,
                        onClick = { onSelectThemeMode(AppThemeMode.SYSTEM) },
                        shape = shapes.pill,
                        colors = chipColors,
                        leadingIcon = { Icon(CleanSupplyIcons.ThemeAuto, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(s.themeSystem, style = type.labelMedium) },
                        modifier = Modifier.testTag("theme_chip_system")
                    )
                }
                item {
                    FilterChip(
                        selected = currentThemeMode == AppThemeMode.LIGHT,
                        onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
                        shape = shapes.pill,
                        colors = chipColors,
                        leadingIcon = { Icon(CleanSupplyIcons.ThemeLight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(s.themeLight, style = type.labelMedium) },
                        modifier = Modifier.testTag("theme_chip_light")
                    )
                }
                item {
                    FilterChip(
                        selected = currentThemeMode == AppThemeMode.DARK,
                        onClick = { onSelectThemeMode(AppThemeMode.DARK) },
                        shape = shapes.pill,
                        colors = chipColors,
                        leadingIcon = { Icon(CleanSupplyIcons.ThemeDark, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(s.themeDark, style = type.labelMedium) },
                        modifier = Modifier.testTag("theme_chip_dark")
                    )
                }
            }
        }
    }
}
