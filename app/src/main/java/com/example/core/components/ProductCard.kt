package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.core.formatting.Formatters
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.domain.PricingEngine
import com.example.ui.i18n.LocalAppStrings

@Composable
fun ProductGridCard(
    product: Product,
    profile: UserProfile?,
    onCardClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val priceInfo = remember(
        product.id,
        product.retailPrice,
        product.wholesalePrice,
        product.businessPrice,
        profile?.segment,
        profile?.verificationStatus,
        profile?.approvedTier
    ) {
        val unitPrice = PricingEngine.resolveTierBaseUnitPrice(product, profile)
        val moq = PricingEngine.resolveMoqUnits(product, profile)
        Triple(unitPrice, moq, unitPrice < product.retailPrice)
    }
    val (effectiveUnitPrice, moq, hasTierDiscount) = priceInfo
    val inStock = product.stockQty >= moq
    val drawableRes = remember(product.imageKey) { Formatters.productDrawable(product.imageKey) }

    Card(
        onClick = onCardClick,
        shape = shapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CleanSupplyTheme.elevations.none),
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .padding(spacing.xs)
                    .clip(shapes.smallComponent)
            ) {
                FastProductImage(
                    drawableRes = drawableRes,
                    contentDescription = product.localizedName(s.isArabic),
                    modifier = Modifier.fillMaxSize()
                )
                if (product.bulkDiscountPercent > 0L) {
                    Surface(
                        color = colors.discountContainer,
                        shape = shapes.smallComponent,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(spacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.xs, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = CleanSupplyIcons.BulkDeal,
                                contentDescription = s.bulkSavingsTag,
                                tint = colors.discount,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "-${product.bulkDiscountPercent}%",
                                style = type.numericSmall,
                                color = colors.discount,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                if (product.cartonSize > 1L) {
                    Surface(
                        color = colors.brandSlate.copy(alpha = 0.88f),
                        shape = shapes.smallComponent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(spacing.xs)
                    ) {
                        Text(
                            text = "${product.cartonSize}x",
                            style = type.numericSmall,
                            color = colors.onPrimary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = spacing.sm,
                    end = spacing.sm,
                    top = spacing.xxs,
                    bottom = spacing.sm
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.brand,
                        style = type.labelSmall,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(spacing.xxs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (inStock) colors.success else colors.outOfStock, CircleShape)
                        )
                        if (!inStock) {
                            Text(
                                text = s.outOfStock,
                                style = type.caption,
                                color = colors.outOfStock,
                                maxLines = 1
                            )
                        }
                    }
                }

                Text(
                    text = product.localizedName(s.isArabic),
                    style = type.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${product.localizedUnit(s.isArabic)} • ${s.moqLabel}: $moq",
                    style = type.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontFamily = type.numericMedium.fontFamily,
                                    fontWeight = type.numericMedium.fontWeight,
                                    fontSize = type.numericMedium.fontSize,
                                    color = colors.textPrimary
                                )
                            ) {
                                append(Formatters.number(effectiveUnitPrice))
                            }
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = type.caption.fontFamily,
                                    fontSize = type.caption.fontSize,
                                    color = colors.textSecondary
                                )
                            ) {
                                append(s.currency)
                            }
                        },
                        maxLines = 1
                    )
                    if (hasTierDiscount) {
                        Text(
                            text = Formatters.number(product.retailPrice),
                            style = type.numericSmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = onQuickAdd,
                    enabled = inStock,
                    shape = shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        disabledContainerColor = colors.disabledContainer,
                        disabledContentColor = colors.disabled
                    ),
                    contentPadding = PaddingValues(horizontal = spacing.sm, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.minTouchTarget)
                        .testTag("quick_add_${product.id}")
                ) {
                    Icon(
                        imageVector = CleanSupplyIcons.AddToCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(spacing.xs))
                    Text(
                        text = if (inStock) s.addToCart else s.outOfStock,
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
fun ProductListRowCard(
    product: Product,
    profile: UserProfile?,
    onCardClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val priceInfo = remember(
        product.id,
        product.retailPrice,
        product.wholesalePrice,
        product.businessPrice,
        profile?.segment,
        profile?.verificationStatus,
        profile?.approvedTier
    ) {
        val unitPrice = PricingEngine.resolveTierBaseUnitPrice(product, profile)
        val moq = PricingEngine.resolveMoqUnits(product, profile)
        Triple(unitPrice, moq, unitPrice < product.retailPrice)
    }
    val (effectiveUnitPrice, moq, hasTierDiscount) = priceInfo
    val inStock = product.stockQty >= moq
    val drawableRes = remember(product.imageKey) { Formatters.productDrawable(product.imageKey) }

    Card(
        onClick = onCardClick,
        shape = shapes.card,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(shapes.smallComponent)
            ) {
                FastProductImage(
                    drawableRes = drawableRes,
                    contentDescription = product.localizedName(s.isArabic),
                    modifier = Modifier.fillMaxSize()
                )
                if (product.bulkDiscountPercent > 0L) {
                    Surface(
                        color = colors.discountContainer,
                        shape = shapes.smallComponent,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(spacing.xxs)
                    ) {
                        Text(
                            text = "-${product.bulkDiscountPercent}%",
                            style = type.caption,
                            color = colors.discount,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = product.brand,
                    style = type.labelSmall,
                    color = colors.primary,
                    maxLines = 1
                )
                Text(
                    text = product.localizedName(s.isArabic),
                    style = type.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.localizedUnit(s.isArabic)} • ${s.moqLabel}: $moq",
                    style = type.caption,
                    color = colors.textSecondary,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Text(
                        text = Formatters.money(effectiveUnitPrice, s.currency),
                        style = type.numericMedium,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    if (hasTierDiscount) {
                        Text(
                            text = Formatters.number(product.retailPrice),
                            style = type.numericSmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = colors.textMuted
                        )
                    }
                }
            }

            Button(
                onClick = onQuickAdd,
                enabled = inStock,
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.disabledContainer,
                    disabledContentColor = colors.disabled
                ),
                contentPadding = PaddingValues(horizontal = spacing.sm),
                modifier = Modifier
                    .height(spacing.minTouchTarget)
                    .testTag("quick_add_${product.id}")
            ) {
                Icon(
                    imageVector = CleanSupplyIcons.AddToCart,
                    contentDescription = if (inStock) s.addToCart else s.outOfStock,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
