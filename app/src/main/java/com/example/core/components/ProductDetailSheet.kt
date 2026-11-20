package com.example.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.CustomerSegment
import com.example.data.model.PackMode
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.PricingEngine
import com.example.ui.i18n.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    relatedProducts: List<Product>,
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onSelectRelated: (Product) -> Unit,
    onAddToCart: (Long, PackMode) -> Unit
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val moq = remember(product.id, profile?.segment, profile?.verificationStatus) {
        PricingEngine.resolveMoqUnits(product, profile)
    }
    var packMode by remember(product.id) {
        mutableStateOf(if (moq >= product.cartonSize && product.cartonSize > 1L) PackMode.CARTON else PackMode.UNIT)
    }
    val minPacks = remember(packMode, moq, product.cartonSize) {
        if (packMode == PackMode.CARTON && product.cartonSize > 1L) {
            ((moq + product.cartonSize - 1L) / product.cartonSize).coerceAtLeast(1L)
        } else {
            moq
        }
    }
    var quantity by remember(product.id, packMode) { mutableLongStateOf(minPacks) }

    val unitsPerPack = if (packMode == PackMode.CARTON) product.cartonSize.coerceAtLeast(1L) else 1L
    val effectiveUnits = quantity * unitsPerPack
    val (effectiveUnit, bulkApplied) = PricingEngine.resolveEffectiveUnitPrice(product, profile, effectiveUnits)
    val authorizedBaseUnit = PricingEngine.resolveTierBaseUnitPrice(product, profile)
    val cartonBonus = if (packMode == PackMode.CARTON && product.cartonSize > 1L) 0.96 else 1.0
    val packPrice = PricingEngine.round2(effectiveUnit * unitsPerPack * cartonBonus)
    val totalPrice = PricingEngine.round2(packPrice * quantity)

    val unitPackPreviewPrice = authorizedBaseUnit
    val cartonPackPreviewPrice = PricingEngine.round2(authorizedBaseUnit * product.cartonSize.coerceAtLeast(1L) * 0.96)

    val isAuthorizedB2B = profile != null &&
        profile.segmentEnum() != CustomerSegment.RETAIL &&
        profile.verificationEnum() == VerificationStatus.VERIFIED

    val bulkProgressTarget = if (product.bulkMinQty > 0L) {
        (effectiveUnits.toFloat() / product.bulkMinQty.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val bulkProgress by animateFloatAsState(targetValue = bulkProgressTarget, label = "bulkProgress")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = shapes.bottomSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                // Clean Hero Photo without dark scrim obscuring details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(shapes.largeSurface)
                        .background(colors.surfaceVariant)
                ) {
                    FastProductImage(
                        drawableRes = Formatters.productDrawable(product.imageKey),
                        contentDescription = product.localizedName(s.isArabic),
                        targetSizePx = 800,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = if (product.stockQty > 0L) colors.successContainer else colors.outOfStockContainer,
                        shape = shapes.pill,
                        border = BorderStroke(
                            0.8.dp,
                            (if (product.stockQty > 0L) colors.success else colors.outOfStock).copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(spacing.sm)
                    ) {
                        Text(
                            text = if (product.stockQty > 0L) "${s.stockAvailable}: ${product.stockQty}" else s.outOfStock,
                            style = type.labelSmall,
                            color = if (product.stockQty > 0L) colors.success else colors.outOfStock,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 5.dp)
                        )
                    }
                }

                // Eyebrow + Product Title & Description
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = shapes.pill,
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
                        Text(
                            text = "${product.brand} • ${product.sku}",
                            style = type.labelSmall,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = product.localizedName(s.isArabic),
                        style = type.headlineSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = product.localizedDesc(s.isArabic),
                        style = type.bodyMedium,
                        color = colors.textSecondary
                    )
                }

                // 3-Pill Commercial Specs Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    SpecMetricChip(
                        label = s.specsUnitLabel,
                        value = product.localizedUnit(s.isArabic),
                        modifier = Modifier.weight(1f)
                    )
                    SpecMetricChip(
                        label = s.specsCartonLabel,
                        value = "${product.cartonSize} ${s.unitsWord}",
                        modifier = Modifier.weight(1f)
                    )
                    SpecMetricChip(
                        label = s.specsMoqLabel,
                        value = "$moq ${s.unitsWord}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Interactive Unit vs Carton Selector
                if (product.cartonSize > 1L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        PackOptionCard(
                            title = s.unitMode,
                            subtitle = Formatters.money(unitPackPreviewPrice, s.currency),
                            badge = null,
                            selected = packMode == PackMode.UNIT,
                            onClick = { packMode = PackMode.UNIT },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pack_mode_unit")
                        )
                        PackOptionCard(
                            title = s.cartonMode,
                            subtitle = Formatters.money(cartonPackPreviewPrice, s.currency),
                            badge = s.cartonBonusTag,
                            selected = packMode == PackMode.CARTON,
                            onClick = { packMode = PackMode.CARTON },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pack_mode_carton")
                        )
                    }
                }

                // Authorized Segment Pricing ONLY (zero leakage of unauthorized B2B rates)
                Surface(
                    color = colors.surfaceVariant,
                    shape = shapes.card,
                    border = BorderStroke(1.dp, colors.outline),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = s.authorizedPricingTitle,
                                style = type.titleSmall,
                                color = colors.textPrimary
                            )
                            SegmentVerificationBadge(profile)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "${s.moqLabel}: $moq • ${product.localizedUnit(s.isArabic)}",
                                    style = type.caption,
                                    color = colors.textSecondary
                                )
                                if (authorizedBaseUnit < product.retailPrice) {
                                    Text(
                                        text = Formatters.money(product.retailPrice, s.currency),
                                        style = type.numericSmall,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = colors.textMuted
                                    )
                                }
                            }
                            Text(
                                text = Formatters.money(effectiveUnit, s.currency),
                                style = type.numericLarge,
                                color = colors.primary
                            )
                        }

                        if (product.bulkDiscountPercent > 0L) {
                            HorizontalDivider(color = colors.outline)
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = CleanSupplyIcons.BulkDeal,
                                            contentDescription = null,
                                            tint = if (bulkApplied) colors.success else colors.discount,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (bulkApplied) {
                                                s.bulkUnlockedLabel
                                            } else {
                                                val missing = (product.bulkMinQty - effectiveUnits).coerceAtLeast(1L)
                                                s.unlockBulkHint.replace("%d", missing.toString())
                                            },
                                            style = type.labelMedium,
                                            color = if (bulkApplied) colors.success else colors.textPrimary
                                        )
                                    }
                                    Surface(
                                        color = if (bulkApplied) colors.successContainer else colors.discountContainer,
                                        shape = shapes.pill
                                    ) {
                                        Text(
                                            text = "-${product.bulkDiscountPercent}%",
                                            style = type.numericSmall,
                                            color = if (bulkApplied) colors.success else colors.discount,
                                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = 3.dp)
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { bulkProgress },
                                    color = if (bulkApplied) colors.success else colors.discount,
                                    trackColor = colors.outline,
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(shapes.pill)
                                )
                            }
                        }

                        if (!isAuthorizedB2B) {
                            HorizontalDivider(color = colors.outline)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                            ) {
                                Icon(CleanSupplyIcons.Info, contentDescription = null, tint = colors.info, modifier = Modifier.size(16.dp))
                                Text(
                                    text = s.b2bUpgradePricingNote,
                                    style = type.caption,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Related Products Shelf
                if (relatedProducts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = s.relatedProductsTitle,
                            style = type.titleMedium,
                            color = colors.textPrimary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            items(relatedProducts, key = { it.id }) { rel ->
                                Surface(
                                    onClick = { onSelectRelated(rel) },
                                    shape = shapes.card,
                                    color = colors.surfaceVariant,
                                    border = BorderStroke(1.dp, colors.outline),
                                    modifier = Modifier.width(156.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(spacing.xs),
                                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                                    ) {
                                        FastProductImage(
                                            drawableRes = Formatters.productDrawable(rel.imageKey),
                                            contentDescription = rel.localizedName(s.isArabic),
                                            targetSizePx = 320,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(74.dp)
                                                .clip(shapes.smallComponent)
                                        )
                                        Text(
                                            text = rel.localizedName(s.isArabic),
                                            style = type.labelMedium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.money(
                                                PricingEngine.resolveTierBaseUnitPrice(rel, profile),
                                                s.currency
                                            ),
                                            style = type.numericSmall,
                                            color = colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Bottom Add-To-Cart Dock
            HorizontalDivider(color = colors.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                QuantityStepper(
                    quantity = quantity,
                    canDecrement = quantity > minPacks,
                    canIncrement = effectiveUnits + unitsPerPack <= product.stockQty,
                    onDecrement = { if (quantity > minPacks) quantity-- },
                    onIncrement = { if (effectiveUnits + unitsPerPack <= product.stockQty) quantity++ },
                    subtitle = "$effectiveUnits ${s.unitsWord}",
                    minusTestTag = "detail_qty_minus",
                    plusTestTag = "detail_qty_plus"
                )

                Button(
                    onClick = { onAddToCart(quantity, packMode) },
                    enabled = product.stockQty >= effectiveUnits,
                    shape = shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.interactive,
                        contentColor = colors.onInteractive,
                        disabledContainerColor = colors.disabledContainer,
                        disabledContentColor = colors.disabled
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("detail_add_to_cart")
                ) {
                    Text(
                        text = "${s.addToCart} • ${Formatters.money(totalPrice, s.currency)}",
                        style = type.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Surface(
        color = colors.surfaceVariant,
        shape = shapes.button,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = type.caption, color = colors.textSecondary, maxLines = 1)
            Text(value, style = type.labelMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PackOptionCard(
    title: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = shapes.button,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.interactive else colors.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = title,
                        style = type.labelLarge,
                        color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                        maxLines = 1
                    )
                    if (!badge.isNullOrBlank()) {
                        Surface(
                            color = colors.successContainer,
                            shape = shapes.pill
                        ) {
                            Text(
                                text = badge,
                                style = type.caption,
                                color = colors.success,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = type.numericSmall,
                    color = if (selected) colors.primary else colors.textSecondary
                )
            }
            if (selected) {
                Icon(CleanSupplyIcons.Check, contentDescription = null, tint = colors.interactive, modifier = Modifier.size(18.dp))
            }
        }
    }
}
