package com.example.feature.orders

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.components.CleanSupplyConfirmationDialog
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.EmptyStateView
import com.example.core.components.ErrorStateView
import com.example.core.components.ListRowSkeleton
import com.example.core.components.OrderLifecycleTimeline
import com.example.core.components.OrderShipmentRecordCard
import com.example.core.components.OrderStatusChip
import com.example.core.components.SectionHeader
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.UiState

@Composable
fun OrdersListTab(
    ordersState: UiState<List<Order>>,
    onSelectOrder: (String) -> Unit,
    onReorder: (String) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    when (ordersState) {
        is UiState.Loading -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                items(4) { ListRowSkeleton() }
            }
        }
        is UiState.Error -> {
            ErrorStateView(
                message = ordersState.message,
                onRetry = onRetry,
                retryTestTag = "orders_retry_btn",
                modifier = modifier
            )
        }
        is UiState.Success -> {
            val orders = ordersState.data
            if (orders.isEmpty()) {
                EmptyStateView(
                    icon = CleanSupplyIcons.orderStatusIcon(OrderStatus.PLACED),
                    title = s.ordersTab,
                    subtitle = s.emptyOrdersDesc,
                    modifier = modifier
                )
            } else {
                val activeOrders = remember(orders) {
                    orders.filter {
                        val st = it.statusEnum()
                        st != OrderStatus.DELIVERED && st != OrderStatus.CANCELLED
                    }
                }
                val pastOrders = remember(orders) {
                    orders.filter {
                        val st = it.statusEnum()
                        st == OrderStatus.DELIVERED || st == OrderStatus.CANCELLED
                    }
                }

                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    item(key = "orders_kpi_strip") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Surface(
                                color = colors.primaryContainer.copy(alpha = 0.65f),
                                shape = shapes.card,
                                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.20f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(s.currentOrdersTitle, style = type.labelMedium, color = colors.onPrimaryContainer)
                                    Text(activeOrders.size.toString(), style = type.numericMedium, color = colors.primary)
                                }
                            }
                            Surface(
                                color = colors.surface,
                                shape = shapes.card,
                                border = BorderStroke(1.dp, colors.outline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(s.pastOrdersTitle, style = type.labelMedium, color = colors.textSecondary)
                                    Text(pastOrders.size.toString(), style = type.numericMedium, color = colors.textPrimary)
                                }
                            }
                        }
                    }

                    if (activeOrders.isNotEmpty()) {
                        item(key = "active_orders_header") {
                            SectionHeader(title = s.currentOrdersTitle)
                        }
                        items(activeOrders, key = { it.id }, contentType = { "order_card" }) { order ->
                            OrderSummaryCard(
                                order = order,
                                onOpen = { onSelectOrder(order.id) },
                                onReorder = { onReorder(order.id) }
                            )
                        }
                    }

                    if (pastOrders.isNotEmpty()) {
                        item(key = "past_orders_header") {
                            SectionHeader(title = s.pastOrdersTitle)
                        }
                        items(pastOrders, key = { it.id }, contentType = { "order_card" }) { order ->
                            OrderSummaryCard(
                                order = order,
                                onOpen = { onSelectOrder(order.id) },
                                onReorder = { onReorder(order.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    order: Order,
    onOpen: () -> Unit,
    onReorder: () -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography
    val createdLabel = Formatters.timestamp(order.createdAt, s.isArabic)

    Card(
        onClick = onOpen,
        shape = shapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CleanSupplyTheme.elevations.subtle),
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
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
                Column {
                    Text(order.orderNumber, style = type.numericMedium, color = colors.textPrimary)
                    if (createdLabel.isNotBlank()) {
                        Text(createdLabel, style = type.caption, color = colors.textMuted)
                    }
                }
                OrderStatusChip(order.statusEnum())
            }

            Surface(
                color = colors.surfaceVariant,
                shape = shapes.smallComponent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Icon(CleanSupplyIcons.Shipping, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${if (s.isArabic) order.zoneNameAr else order.zoneNameEn} • ${order.carrierName}",
                        style = type.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = colors.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.money(order.totalAmount, s.currency),
                        style = type.numericMedium,
                        color = colors.primary
                    )
                    Text(
                        text = "${order.itemCount} ${s.resultsCountLabel}",
                        style = type.caption,
                        color = colors.textSecondary
                    )
                }
                CleanSupplySecondaryButton(
                    text = s.reorderNow,
                    icon = CleanSupplyIcons.Reorder,
                    onClick = onReorder
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingDetailSheet(
    order: Order,
    items: List<OrderItem>,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onReorder: () -> Unit,
    onReportIssue: () -> Unit,
    onCopyTracking: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography
    val context = LocalContext.current

    val status = order.statusEnum()
    val canCancel = status == OrderStatus.PLACED || status == OrderStatus.CONFIRMED || status == OrderStatus.PACKED
    var confirmCancelOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = shapes.bottomSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.primaryContainer, shapes.button),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(CleanSupplyIcons.Receipt, contentDescription = null, tint = colors.primary)
                    }
                    Column {
                        Text(order.orderNumber, style = type.numericLarge, color = colors.textPrimary)
                        Text(order.deliverySlot, style = type.caption, color = colors.textSecondary)
                    }
                }
                OrderStatusChip(status)
            }

            OrderLifecycleTimeline(status = status)

            OrderShipmentRecordCard(
                order = order,
                onCopyTracking = { tracking ->
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Tracking", tracking))
                    onCopyTracking(tracking)
                }
            )

            Surface(
                color = colors.surfaceVariant,
                shape = shapes.card,
                border = BorderStroke(1.dp, colors.outline)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(s.orderItemsTitle, style = type.titleSmall, color = colors.textPrimary)
                    items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${item.quantity}x ${item.localizedName(s.isArabic)}",
                                style = type.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.money(item.lineTotal, s.currency),
                                style = type.numericSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                    HorizontalDivider(color = colors.outline)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.totalLabel, style = type.titleSmall, color = colors.textPrimary)
                        Text(
                            text = Formatters.money(order.totalAmount, s.currency),
                            style = type.numericMedium,
                            color = colors.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                CleanSupplySecondaryButton(
                    text = s.reportIssue,
                    icon = CleanSupplyIcons.Support,
                    onClick = onReportIssue,
                    modifier = Modifier.weight(1f)
                )
                if (canCancel) {
                    CleanSupplyPrimaryButton(
                        text = s.cancelOrder,
                        containerColor = colors.error,
                        contentColor = colors.onError,
                        onClick = { confirmCancelOpen = true },
                        testTag = "cancel_order_button",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    CleanSupplyPrimaryButton(
                        text = s.reorderNow,
                        icon = CleanSupplyIcons.Reorder,
                        onClick = onReorder,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(spacing.sm))
        }
    }

    if (confirmCancelOpen) {
        CleanSupplyConfirmationDialog(
            title = s.confirmCancelTitle,
            body = s.confirmCancelBody,
            confirmText = s.confirmAction,
            dismissText = s.closeBtn,
            isDestructive = true,
            onConfirm = {
                confirmCancelOpen = false
                onCancel()
            },
            onDismiss = { confirmCancelOpen = false }
        )
    }
}
