package com.example.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.OrderStatusChip
import com.example.core.components.SectionHeader
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.data.model.ApprovedTier
import com.example.data.model.CustomerSegment
import com.example.data.model.FulfillmentType
import com.example.data.model.Order
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.ui.i18n.LocalAppStrings

@Composable
fun OperationsConsoleTab(
    adminUsers: List<UserProfile>,
    orders: List<Order>,
    tickets: List<SupportTicket>,
    onSeedCatalog: () -> Unit,
    onApproveUserTier: (String, CustomerSegment, ApprovedTier, Double) -> Unit,
    onAdvanceOrder: (Order) -> Unit,
    onStepDriver: (Order) -> Unit,
    onToggleFulfillment: (Order) -> Unit,
    onResolveTicket: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography
    val openTickets = tickets.filter { it.status != "RESOLVED" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item(key = "ops_header") {
            Card(
                shape = shapes.largeSurface,
                colors = CardDefaults.cardColors(containerColor = colors.brandBanner)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Icon(CleanSupplyIcons.Admin, contentDescription = null, tint = colors.heroAccent)
                        Text(s.opsConsoleTitle, style = type.titleMedium, color = colors.onBrandBanner)
                    }
                    CleanSupplyPrimaryButton(
                        text = s.seedCatalogBtn,
                        onClick = onSeedCatalog,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (adminUsers.isNotEmpty()) {
            item(key = "ops_users_header") {
                SectionHeader(title = s.opsUsersTitle)
            }
            items(adminUsers, key = { it.userId }) { u ->
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
                            Text("${u.displayName} (${u.segment})", style = type.titleSmall, color = colors.textPrimary)
                            Surface(color = colors.primaryContainer, shape = shapes.pill) {
                                Text(
                                    text = u.verificationStatus,
                                    style = type.labelSmall,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = spacing.xs, vertical = 2.dp)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            CleanSupplyPrimaryButton(
                                text = s.approveWholesaleABtn,
                                onClick = { onApproveUserTier(u.userId, CustomerSegment.WHOLESALE, ApprovedTier.TIER_A, 25000.0) },
                                modifier = Modifier.weight(1f)
                            )
                            CleanSupplyPrimaryButton(
                                text = s.approveBusinessEntBtn,
                                onClick = { onApproveUserTier(u.userId, CustomerSegment.BUSINESS, ApprovedTier.ENTERPRISE, 75000.0) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (orders.isNotEmpty()) {
            item(key = "ops_orders_header") {
                SectionHeader(title = s.opsOrdersTitle)
            }
            items(orders.take(10), key = { it.id }) { order ->
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
                            Text("${order.orderNumber} • ${order.customerName}", style = type.titleSmall, color = colors.textPrimary)
                            OrderStatusChip(order.statusEnum())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            CleanSupplySecondaryButton(
                                text = s.advanceOrderBtn,
                                onClick = { onAdvanceOrder(order) },
                                modifier = Modifier.weight(1f)
                            )
                            if (order.fulfillmentEnum() == FulfillmentType.INTERNAL_FLEET) {
                                CleanSupplyPrimaryButton(
                                    text = s.moveDriverBtn,
                                    icon = CleanSupplyIcons.Shipping,
                                    onClick = { onStepDriver(order) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        TextButton(onClick = { onToggleFulfillment(order) }) {
                            Text("${s.switchCarrierBtn}: ${order.fulfillmentType}", style = type.labelSmall, color = colors.primary)
                        }
                    }
                }
            }
        }

        if (openTickets.isNotEmpty()) {
            item(key = "ops_tickets_header") {
                SectionHeader(title = s.opsTicketsTitle)
            }
            items(openTickets, key = { it.id }) { t ->
                Card(
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.subject, style = type.titleSmall, color = colors.textPrimary)
                            Text(t.message, style = type.bodySmall, color = colors.textSecondary)
                        }
                        CleanSupplyPrimaryButton(
                            text = s.resolveTicketBtn,
                            onClick = { onResolveTicket(t.id, s.resolvedReplyDefault) }
                        )
                    }
                }
            }
        }
    }
}
