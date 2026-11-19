package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.FulfillmentType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.i18n.LocalAppStrings

@Composable
fun OrderLifecycleTimeline(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    if (status == OrderStatus.CANCELLED) {
        Surface(
            color = colors.errorContainer,
            shape = shapes.card,
            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.25f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(CleanSupplyIcons.Rejected, contentDescription = null, tint = colors.error)
                Text(
                    text = if (s.isArabic) "تم إلغاء هذا الطلب" else "This order has been cancelled",
                    style = type.labelLarge,
                    color = colors.error
                )
            }
        }
        return
    }

    val steps = remember(s.isArabic) {
        listOf(
            OrderStatus.PLACED to (if (s.isArabic) "استلام" else "Placed"),
            OrderStatus.CONFIRMED to (if (s.isArabic) "تأكيد" else "Confirmed"),
            OrderStatus.PACKED to (if (s.isArabic) "تجهيز" else "Packed"),
            OrderStatus.OUT_FOR_DELIVERY to (if (s.isArabic) "التوصيل" else "On Way"),
            OrderStatus.DELIVERED to (if (s.isArabic) "تم" else "Delivered")
        )
    }
    val currentIdx = steps.indexOfFirst { it.first == status }

    Surface(
        color = colors.surfaceVariant,
        shape = shapes.card,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, (_, label) ->
                val active = idx <= currentIdx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (idx > 0) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 2.dp,
                                color = if (active) colors.primary else colors.outline
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (active) colors.primary else colors.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CleanSupplyIcons.Check,
                                contentDescription = label,
                                tint = if (active) colors.onPrimary else colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (idx < steps.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 2.dp,
                                color = if (idx < currentIdx) colors.primary else colors.outline
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = label,
                        style = type.labelSmall,
                        color = if (active) colors.primary else colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Displays strictly actual fulfillment/carrier/dispatch records present on the Order document.
 * Never fabricates map positions or driver details when absent.
 */
@Composable
fun OrderShipmentRecordCard(
    order: Order,
    onCopyTracking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val fulfillment = order.fulfillmentEnum()
    val createdLabel = Formatters.timestamp(order.createdAt, s.isArabic)

    Surface(
        color = colors.surfaceVariant,
        shape = shapes.card,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Icon(CleanSupplyIcons.Shipping, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Text(s.shipmentDetailsTitle, style = type.titleSmall, color = colors.textPrimary)
            }

            if (createdLabel.isNotBlank()) {
                Text(createdLabel, style = type.caption, color = colors.textSecondary)
            }

            Text(
                text = if (fulfillment == FulfillmentType.INTERNAL_FLEET) {
                    s.internalFleetLabel
                } else {
                    s.thirdPartyCarrierLabel
                },
                style = type.bodyMedium,
                color = colors.textPrimary
            )

            if (order.carrierName.isNotBlank()) {
                Text(
                    text = order.carrierName,
                    style = type.bodySmall,
                    color = colors.textSecondary
                )
            }

            if (order.deliveryAddress.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Icon(CleanSupplyIcons.Location, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text(order.deliveryAddress, style = type.bodySmall, color = colors.textSecondary)
                }
            }

            if (order.trackingNumber.isNotBlank()) {
                HorizontalDivider(color = colors.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.carrierTrackingTitle, style = type.caption, color = colors.textSecondary)
                        Text(order.trackingNumber, style = type.numericSmall, color = colors.textPrimary)
                    }
                    IconButton(onClick = { onCopyTracking(order.trackingNumber) }) {
                        Icon(CleanSupplyIcons.Copy, contentDescription = s.copyTracking, tint = colors.primary)
                    }
                }
            }

            if (order.driverName.isNotBlank()) {
                HorizontalDivider(color = colors.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(order.driverName, style = type.labelMedium, color = colors.textPrimary)
                    if (order.driverPhone.isNotBlank()) {
                        Text(order.driverPhone, style = type.numericSmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
