package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApprovedTier
import com.example.data.model.CustomerSegment
import com.example.data.model.FulfillmentType
import com.example.data.model.LoyaltyLog
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.SupportTicket
import com.example.data.model.TicketCategory
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.ui.components.LiveFleetMapCanvas
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.SegmentVerificationBadge
import com.example.ui.components.formatMoney
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AquaContainer
import com.example.ui.theme.AquaTeal
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersAccountScreen(
    isAdmin: Boolean,
    profile: UserProfile?,
    ordersState: UiState<List<Order>>,
    selectedOrderId: String?,
    selectedOrderItems: List<OrderItem>,
    loyaltyLogs: List<LoyaltyLog>,
    supportTickets: List<SupportTicket>,
    adminUsers: List<UserProfile>,
    onSelectOrder: (String?) -> Unit,
    onCancelOrder: (String) -> Unit,
    onReorder: (String) -> Unit,
    onRequestSegment: (CustomerSegment, String, String) -> Unit,
    onSubmitTicket: (String, TicketCategory, String, String) -> Unit,
    onSeedCatalog: () -> Unit,
    onApproveUserTier: (String, CustomerSegment, ApprovedTier, Double) -> Unit,
    onAdvanceOrder: (Order) -> Unit,
    onStepDriver: (Order) -> Unit,
    onToggleFulfillment: (Order) -> Unit,
    onResolveTicket: (String, String) -> Unit,
    onCopyTrackingToast: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val tabs = remember(isAdmin, s) {
        buildList {
            add(s.ordersTab)
            add(s.loyaltySupportTab)
            add(s.profileTab)
            if (isAdmin) add(s.opsTab)
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var ticketOrderPreset by remember { mutableStateOf<String?>(null) }

    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val inspectedOrder = orders.firstOrNull { it.id == selectedOrderId }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("account_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> OrdersListTab(
                ordersState = ordersState,
                onSelectOrder = onSelectOrder,
                onReorder = onReorder
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
                onRequestSegment = onRequestSegment,
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
private fun OrdersListTab(
    ordersState: UiState<List<Order>>,
    onSelectOrder: (String) -> Unit,
    onReorder: (String) -> Unit
) {
    val s = LocalAppStrings.current
    when (ordersState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(ordersState.message, color = ErrorRed)
        }
        is UiState.Success -> {
            val orders = ordersState.data
            if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = AquaTeal, modifier = Modifier.size(56.dp))
                        Text(s.emptyOrdersDesc, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        Card(
                            onClick = { onSelectOrder(order.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    OrderStatusChip(order.statusEnum())
                                }
                                Text(
                                    text = "${if (s.isArabic) order.zoneNameAr else order.zoneNameEn} • ${order.carrierName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatMoney(order.totalAmount, s.currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AquaTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(
                                        onClick = { onReorder(order.id) },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(s.reorderNow)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderTrackingDetailSheet(
    order: Order,
    items: List<OrderItem>,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onReorder: () -> Unit,
    onReportIssue: () -> Unit,
    onCopyTracking: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val status = order.statusEnum()
    val canCancel = status == OrderStatus.PLACED || status == OrderStatus.CONFIRMED || status == OrderStatus.PACKED

    val steps = listOf(
        OrderStatus.PLACED to (if (s.isArabic) "استلام" else "Placed"),
        OrderStatus.CONFIRMED to (if (s.isArabic) "تأكيد" else "Confirmed"),
        OrderStatus.PACKED to (if (s.isArabic) "تجهيز" else "Packed"),
        OrderStatus.OUT_FOR_DELIVERY to (if (s.isArabic) "التوصيل" else "On Way"),
        OrderStatus.DELIVERED to (if (s.isArabic) "تم" else "Delivered")
    )
    val currentIdx = steps.indexOfFirst { it.first == status }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.headlineSmall)
                    Text(order.deliverySlot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OrderStatusChip(status)
            }

            // Timeline Stepper
            if (status != OrderStatus.CANCELLED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    steps.forEachIndexed { idx, (_, label) ->
                        val active = idx <= currentIdx
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(if (active) AquaTeal else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Dual Fulfillment: Live Fleet Map vs Third-Party Carrier Card
            if (order.fulfillmentEnum() == FulfillmentType.INTERNAL_FLEET) {
                LiveFleetMapCanvas(order = order)
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.carrierTrackingTitle, style = MaterialTheme.typography.labelMedium)
                            Text("${order.carrierName} • ${order.trackingNumber}", style = MaterialTheme.typography.titleSmall)
                        }
                        IconButton(onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Tracking", order.trackingNumber))
                            onCopyTracking(order.trackingNumber)
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = s.copyTracking)
                        }
                    }
                }
            }

            // Order Line Items
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.localizedName(s.isArabic)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(formatMoney(item.lineTotal, s.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.totalLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(formatMoney(order.totalAmount, s.currency), style = MaterialTheme.typography.titleMedium, color = AquaTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReportIssue,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(s.reportIssue)
                }
                if (canCancel) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(s.cancelOrder)
                    }
                } else {
                    Button(
                        onClick = onReorder,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(s.reorderNow)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
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
    var showTicketDialog by remember(presetOrderId) { mutableStateOf(presetOrderId != null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SlateNavy)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.loyaltyPointsTitle, style = MaterialTheme.typography.labelMedium, color = AquaContainer)
                        Text("${profile?.loyaltyPoints ?: 0L} pts", style = MaterialTheme.typography.displaySmall, color = Color.White)
                        Text(
                            text = formatMoney((profile?.loyaltyPoints ?: 0L) / 10.0, s.currency),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberContainer
                        )
                    }
                    Icon(Icons.Default.Stars, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(48.dp))
                }
            }
        }

        if (loyaltyLogs.isNotEmpty()) {
            items(loyaltyLogs.take(5), key = { it.id }) { log ->
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (s.isArabic) log.titleAr else log.titleEn, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (log.pointsDelta >= 0) "+${log.pointsDelta}" else "${log.pointsDelta}",
                            color = if (log.pointsDelta >= 0) SuccessGreen else AmberAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.supportTicketsTitle, style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showTicketDialog = true }, modifier = Modifier.height(48.dp)) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.newSupportTicket)
                }
            }
        }

        items(supportTickets, key = { it.id }) { ticket ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleSmall)
                        Surface(color = AquaContainer, shape = RoundedCornerShape(50)) {
                            Text(ticket.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                    if (ticket.adminReply.isNotBlank()) {
                        Surface(color = SuccessContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(ticket.adminReply, style = MaterialTheme.typography.bodySmall, color = SuccessGreen, modifier = Modifier.padding(8.dp))
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
            title = { Text(s.newSupportTicket) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TicketCategory.entries) { c ->
                            FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c.name) })
                        }
                    }
                    OutlinedTextField(value = orderRef, onValueChange = { orderRef = it }, label = { Text("Order #") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(if (s.isArabic) "العنوان" else "Subject") }, singleLine = true)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text(if (s.isArabic) "التفاصيل" else "Details") })
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
                    }
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
    onRequestSegment: (CustomerSegment, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val s = LocalAppStrings.current
    var selectedSegment by remember(profile?.segment) {
        mutableStateOf(profile?.segmentEnum() ?: CustomerSegment.RETAIL)
    }
    var companyName by remember(profile?.companyName) { mutableStateOf(profile?.companyName.orEmpty()) }
    var taxId by remember(profile?.taxId) { mutableStateOf(profile?.taxId.orEmpty()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(profile?.displayName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SegmentVerificationBadge(profile)
                    }
                    if ((profile?.creditLimit ?: 0.0) > 0.0) {
                        HorizontalDivider()
                        Text(
                            text = "${if (s.isArabic) "الحد الائتماني المتاح" else "Available B2B Credit"}: ${formatMoney(profile?.availableCredit ?: 0.0, s.currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AquaTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.accountSegmentTitle, style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CustomerSegment.entries) { seg ->
                            val label = when (seg) {
                                CustomerSegment.RETAIL -> s.retailLabel
                                CustomerSegment.WHOLESALE -> s.wholesaleLabel
                                CustomerSegment.BUSINESS -> s.businessLabel
                            }
                            FilterChip(
                                selected = selectedSegment == seg,
                                onClick = { selectedSegment = seg },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (selectedSegment != CustomerSegment.RETAIL) {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text(s.companyNameHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = taxId,
                            onValueChange = { taxId = it },
                            label = { Text(s.taxIdHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { onRequestSegment(selectedSegment, companyName, taxId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(s.requestUpgrade)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(s.signOut)
            }
        }
    }
}

@Composable
private fun OperationsConsoleTab(
    adminUsers: List<UserProfile>,
    orders: List<Order>,
    tickets: List<SupportTicket>,
    onSeedCatalog: () -> Unit,
    onApproveUserTier: (String, CustomerSegment, ApprovedTier, Double) -> Unit,
    onAdvanceOrder: (Order) -> Unit,
    onStepDriver: (Order) -> Unit,
    onToggleFulfillment: (Order) -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    val s = LocalAppStrings.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SlateNavy)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AquaContainer)
                        Text(s.opsConsoleTitle, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Button(
                        onClick = onSeedCatalog,
                        colors = ButtonDefaults.buttonColors(containerColor = AquaTeal),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(s.seedCatalogBtn)
                    }
                }
            }
        }

        items(adminUsers, key = { it.userId }) { u ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${u.displayName} (${u.segment})", style = MaterialTheme.typography.titleSmall)
                        Text(u.verificationStatus, style = MaterialTheme.typography.labelSmall, color = AquaTeal)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onApproveUserTier(u.userId, CustomerSegment.WHOLESALE, ApprovedTier.TIER_A, 25000.0) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Wholesale A") }
                        Button(
                            onClick = { onApproveUserTier(u.userId, CustomerSegment.BUSINESS, ApprovedTier.ENTERPRISE, 75000.0) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("B2B Ent.") }
                    }
                }
            }
        }

        items(orders.take(10), key = { it.id }) { order ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${order.orderNumber} • ${order.customerName}", style = MaterialTheme.typography.titleSmall)
                        OrderStatusChip(order.statusEnum())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAdvanceOrder(order) }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text(s.advanceOrderBtn, maxLines = 1)
                        }
                        if (order.fulfillmentEnum() == FulfillmentType.INTERNAL_FLEET) {
                            Button(onClick = { onStepDriver(order) }, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.moveDriverBtn, maxLines = 1)
                            }
                        }
                    }
                    TextButton(onClick = { onToggleFulfillment(order) }) {
                        Text("${s.switchCarrierBtn}: ${order.fulfillmentType}")
                    }
                }
            }
        }

        items(tickets.filter { it.status != "RESOLVED" }, key = { it.id }) { t ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.subject, style = MaterialTheme.typography.titleSmall)
                        Text(t.message, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onResolveTicket(t.id, "Resolved by Operations Team") }) {
                        Text("Resolve")
                    }
                }
            }
        }
    }
}
