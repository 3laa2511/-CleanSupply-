package com.example.feature.cart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.components.CleanSupplyPrimaryButton
import com.example.core.components.CleanSupplySecondaryButton
import com.example.core.components.EmptyStateView
import com.example.core.components.FastProductImage
import com.example.core.components.QuantityStepper
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Coupon
import com.example.data.model.PackMode
import com.example.data.model.UserProfile
import com.example.domain.CheckoutSummary
import com.example.domain.PricedCartLine
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.CheckoutDraft

@Composable
fun CartScreen(
    summary: CheckoutSummary,
    profile: UserProfile?,
    coupons: List<Coupon>,
    draft: CheckoutDraft,
    onBrowseCatalog: () -> Unit,
    onUpdateCartQty: (String, Long, PackMode) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onToggleLoyalty: (Boolean) -> Unit,
    onProceedToCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    var couponInput by remember(draft.appliedCouponCode) { mutableStateOf(draft.appliedCouponCode) }
    val shippingProgress by animateFloatAsState(
        targetValue = summary.freeShippingProgress,
        label = "cartShippingProgress"
    )

    if (summary.lines.isEmpty()) {
        EmptyStateView(
            icon = CleanSupplyIcons.NavCartInactive,
            title = s.emptyCartTitle,
            subtitle = s.emptyCartDesc,
            actionLabel = s.browseCatalog,
            onAction = onBrowseCatalog,
            actionTestTag = "empty_cart_browse_btn",
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            item(key = "cart_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.cartTitle, style = type.headlineSmall, color = colors.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        if (summary.tierAndBulkSavings > 0.0) {
                            Surface(color = colors.successContainer, shape = shapes.pill) {
                                Text(
                                    text = "-${Formatters.money(summary.tierAndBulkSavings, s.currency)}",
                                    style = type.numericSmall,
                                    color = colors.success,
                                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp)
                                )
                            }
                        }
                        Surface(color = colors.primaryContainer, shape = shapes.pill) {
                            Text(
                                text = "${summary.lines.size} ${s.resultsCountLabel}",
                                style = type.numericSmall,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (summary.hasMoqViolation) {
                item(key = "moq_warning") {
                    Surface(
                        color = colors.errorContainer,
                        shape = shapes.card,
                        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(CleanSupplyIcons.Warning, contentDescription = null, tint = colors.error)
                            Text(s.moqWarning, style = type.bodySmall, color = colors.error)
                        }
                    }
                }
            }

            item(key = "free_shipping") {
                Card(
                    shape = shapes.card,
                    border = BorderStroke(1.dp, if (summary.shippingFee == 0.0) colors.success.copy(alpha = 0.30f) else colors.outline),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        val shipMsg = if (summary.shippingFee == 0.0) {
                            if (s.isArabic) "مبروك! حصلت على شحن مجاني لهذه المنطقة" else "Free delivery unlocked for this zone"
                        } else {
                            if (s.isArabic) "أضف ${Formatters.money(summary.amountToFreeShipping, s.currency)} للحصول على شحن مجاني"
                            else "Add ${Formatters.money(summary.amountToFreeShipping, s.currency)} more for free shipping"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            Icon(
                                imageVector = if (summary.shippingFee == 0.0) CleanSupplyIcons.Check else CleanSupplyIcons.Shipping,
                                contentDescription = null,
                                tint = if (summary.shippingFee == 0.0) colors.success else colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = shipMsg,
                                style = type.labelLarge,
                                color = if (summary.shippingFee == 0.0) colors.success else colors.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { shippingProgress },
                            color = if (summary.shippingFee == 0.0) colors.success else colors.primary,
                            trackColor = colors.primaryContainer,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(shapes.pill)
                        )
                    }
                }
            }

            items(summary.lines, key = { it.cartItem.id }, contentType = { "cart_line" }) { line ->
                CartLineCard(
                    line = line,
                    onUpdateQty = { newQty ->
                        onUpdateCartQty(line.product.id, newQty, line.cartItem.packModeEnum())
                    }
                )
            }

            item(key = "coupon_loyalty") {
                Card(
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it },
                                placeholder = { Text(s.couponHint, style = type.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = { Icon(CleanSupplyIcons.Offer, contentDescription = null, tint = colors.primary) },
                                singleLine = true,
                                shape = shapes.input,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.outline,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coupon_input")
                            )
                            if (draft.appliedCouponCode.isNotEmpty()) {
                                CleanSupplySecondaryButton(
                                    text = s.removeCoupon,
                                    onClick = {
                                        couponInput = ""
                                        onRemoveCoupon()
                                    },
                                    modifier = Modifier.height(52.dp)
                                )
                            } else {
                                CleanSupplyPrimaryButton(
                                    text = s.applyCoupon,
                                    onClick = { onApplyCoupon(couponInput) },
                                    testTag = "apply_coupon_btn",
                                    modifier = Modifier.height(52.dp)
                                )
                            }
                        }

                        if (draft.couponError) {
                            Text(s.invalidCoupon, style = type.bodySmall, color = colors.error)
                        }

                        if (coupons.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                items(coupons, key = { it.id }) { c ->
                                    FilterChip(
                                        selected = draft.appliedCouponCode.equals(c.code, ignoreCase = true),
                                        onClick = {
                                            couponInput = c.code
                                            onApplyCoupon(c.code)
                                        },
                                        shape = shapes.pill,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = colors.surfaceVariant,
                                            selectedContainerColor = colors.primaryContainer,
                                            selectedLabelColor = colors.onPrimaryContainer
                                        ),
                                        label = { Text("${c.code} • ${c.localizedTitle(s.isArabic)}", style = type.labelSmall) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colors.outline)

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
                                        .size(38.dp)
                                        .background(colors.warningContainer, shapes.button),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(CleanSupplyIcons.Loyalty, contentDescription = null, tint = colors.warning, modifier = Modifier.size(19.dp))
                                }
                                Column {
                                    Text(s.loyaltyRedeemLabel, style = type.titleSmall, color = colors.textPrimary)
                                    Text(
                                        text = "${profile?.loyaltyPoints ?: 0L} ${s.pointsUnit} • ${Formatters.money((profile?.loyaltyPoints ?: 0L) / 10.0, s.currency)}",
                                        style = type.numericSmall,
                                        color = colors.warning
                                    )
                                }
                            }
                            Switch(
                                checked = draft.redeemLoyalty,
                                enabled = (profile?.loyaltyPoints ?: 0L) >= 10L,
                                onCheckedChange = onToggleLoyalty,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onPrimary,
                                    checkedTrackColor = colors.primary
                                ),
                                modifier = Modifier.testTag("loyalty_redeem_switch")
                            )
                        }
                    }
                }
            }

            item(key = "cart_summary_cta") {
                Card(
                    shape = shapes.largeSurface,
                    border = BorderStroke(1.dp, colors.outline),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        CartSummaryLine(s.subtotalLabel, Formatters.money(summary.retailSubtotal, s.currency))
                        if (summary.tierAndBulkSavings > 0.0) {
                            CartSummaryLine(s.tierSavingsLabel, "-${Formatters.money(summary.tierAndBulkSavings, s.currency)}", colors.success)
                        }
                        if (summary.couponDiscount > 0.0) {
                            CartSummaryLine(s.couponDiscountLabel, "-${Formatters.money(summary.couponDiscount, s.currency)}", colors.success)
                        }
                        if (summary.loyaltyDiscount > 0.0) {
                            CartSummaryLine(s.loyaltyDiscountLabel, "-${Formatters.money(summary.loyaltyDiscount, s.currency)}", colors.warning)
                        }
                        CartSummaryLine(
                            s.shippingFeeLabel,
                            if (summary.shippingFee == 0.0) s.freeShippingLabel else Formatters.money(summary.shippingFee, s.currency),
                            if (summary.shippingFee == 0.0) colors.success else colors.textPrimary
                        )
                        HorizontalDivider(color = colors.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.totalLabel, style = type.titleMedium, color = colors.textPrimary)
                            Text(
                                text = Formatters.money(summary.grandTotal, s.currency),
                                style = type.numericLarge,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }

        // Sticky Checkout Dock
        Surface(
            color = colors.surface,
            tonalElevation = 0.dp
        ) {
            Column {
                HorizontalDivider(color = colors.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(s.totalLabel, style = type.caption, color = colors.textSecondary)
                        Text(
                            text = Formatters.money(summary.grandTotal, s.currency),
                            style = type.numericMedium,
                            color = colors.primary
                        )
                    }
                    CleanSupplyPrimaryButton(
                        text = s.proceedToCheckout,
                        icon = CleanSupplyIcons.Forward,
                        enabled = !summary.hasMoqViolation,
                        onClick = onProceedToCheckout,
                        testTag = "proceed_to_checkout_btn",
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: PricedCartLine,
    onUpdateQty: (Long) -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Card(
        shape = shapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (!line.meetsMoq) colors.error else colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                FastProductImage(
                    drawableRes = Formatters.productDrawable(line.product.imageKey),
                    contentDescription = line.product.localizedName(s.isArabic),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(shapes.smallComponent)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = line.product.localizedName(s.isArabic),
                        style = type.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (line.cartItem.packModeEnum() == PackMode.CARTON) {
                            "${s.cartonMode} (${line.product.cartonSize}x) • ${Formatters.money(line.effectivePackPrice, s.currency)}"
                        } else {
                            "${s.unitMode} • ${Formatters.money(line.effectivePackPrice, s.currency)}"
                        },
                        style = type.bodySmall,
                        color = colors.textSecondary
                    )
                    if (!line.meetsMoq) {
                        Text(
                            text = "${s.moqLabel}: ${line.requiredMoqUnits}",
                            style = type.labelSmall,
                            color = colors.error
                        )
                    } else if (line.bulkDiscountApplied) {
                        Surface(color = colors.discountContainer, shape = shapes.pill) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(CleanSupplyIcons.BulkDeal, contentDescription = null, tint = colors.discount, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "-${line.product.bulkDiscountPercent}% ${s.bulkSavingsTag}",
                                    style = type.labelSmall,
                                    color = colors.discount
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { onUpdateQty(0L) },
                    modifier = Modifier
                        .size(spacing.minTouchTarget)
                        .testTag("cart_remove_${line.product.id}")
                ) {
                    Icon(CleanSupplyIcons.Delete, contentDescription = "Remove item", tint = colors.error)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuantityStepper(
                    quantity = line.cartItem.quantity,
                    canDecrement = line.cartItem.quantity > 0L,
                    canIncrement = line.cartItem.quantity < 5000L,
                    onDecrement = { onUpdateQty(line.cartItem.quantity - 1L) },
                    onIncrement = { onUpdateQty(line.cartItem.quantity + 1L) },
                    minusTestTag = "cart_qty_minus_${line.product.id}",
                    plusTestTag = "cart_qty_plus_${line.product.id}"
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.money(line.lineEffectiveTotal, s.currency),
                        style = type.numericMedium,
                        color = colors.textPrimary
                    )
                    if (line.lineSavings > 0.0) {
                        Text(
                            text = "-${Formatters.money(line.lineSavings, s.currency)}",
                            style = type.caption,
                            color = colors.success
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CartSummaryLine(
    label: String,
    value: String,
    valueColor: Color = CleanSupplyTheme.colors.textPrimary
) {
    val colors = CleanSupplyTheme.colors
    val type = CleanSupplyTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = type.bodyMedium, color = colors.textSecondary)
        Text(value, style = type.numericSmall, color = valueColor)
    }
}
