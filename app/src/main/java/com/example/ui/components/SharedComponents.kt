package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CustomerSegment
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PackMode
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.domain.PricingEngine
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AquaContainer
import com.example.ui.theme.AquaContainerText
import com.example.ui.theme.AquaTeal
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import java.util.Locale

@DrawableRes
fun productDrawableForKey(key: String): Int = when (key) {
    "paper" -> R.drawable.img_cat_paper
    "tools" -> R.drawable.img_cat_tools
    else -> R.drawable.img_cat_detergents
}

fun formatMoney(amount: Double, currency: String): String {
    val formatted = if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", amount)
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
    return "$formatted $currency"
}

@Composable
fun SegmentVerificationBadge(profile: UserProfile?, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val seg = profile?.segmentEnum() ?: CustomerSegment.RETAIL
    val status = profile?.verificationEnum() ?: VerificationStatus.VERIFIED
    val segText = when (seg) {
        CustomerSegment.RETAIL -> s.retailLabel
        CustomerSegment.WHOLESALE -> s.wholesaleLabel
        CustomerSegment.BUSINESS -> s.businessLabel
    }
    val statusColor = when (status) {
        VerificationStatus.VERIFIED -> SuccessGreen
        VerificationStatus.PENDING_VERIFICATION -> AmberAccent
        VerificationStatus.REJECTED -> ErrorRed
    }
    val statusBg = when (status) {
        VerificationStatus.VERIFIED -> SuccessContainer
        VerificationStatus.PENDING_VERIFICATION -> AmberContainer
        VerificationStatus.REJECTED -> ErrorContainer
    }
    Surface(
        modifier = modifier,
        color = statusBg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = segText,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = segText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val s = LocalAppStrings.current
    val (label, bg, fg) = when (status) {
        OrderStatus.PLACED -> Triple(if (s.isArabic) "تم الاستلام" else "Placed", AquaContainer, AquaContainerText)
        OrderStatus.CONFIRMED -> Triple(if (s.isArabic) "مؤكد" else "Confirmed", AquaContainer, AquaContainerText)
        OrderStatus.PACKED -> Triple(if (s.isArabic) "جاري التجهيز" else "Packed", AmberContainer, AmberOnContainer)
        OrderStatus.OUT_FOR_DELIVERY -> Triple(if (s.isArabic) "في الطريق" else "Out for Delivery", AquaTeal, Color.White)
        OrderStatus.DELIVERED -> Triple(if (s.isArabic) "تم التوصيل" else "Delivered", SuccessContainer, SuccessGreen)
        OrderStatus.CANCELLED -> Triple(if (s.isArabic) "ملغي" else "Cancelled", ErrorContainer, ErrorRed)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    profile: UserProfile?,
    onCardClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val effectiveUnitPrice = PricingEngine.resolveTierBaseUnitPrice(product, profile)
    val moq = PricingEngine.resolveMoqUnits(product, profile)
    val hasDiscount = effectiveUnitPrice < product.retailPrice

    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                Image(
                    painter = painterResource(id = productDrawableForKey(product.imageKey)),
                    contentDescription = product.localizedName(s.isArabic),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (product.bulkDiscountPercent > 0L) {
                    Surface(
                        color = AmberAccent,
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${product.bulkDiscountPercent}% ${s.bulkSavingsTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.localizedName(s.isArabic),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.localizedUnit(s.isArabic)} • ${s.moqLabel}: $moq",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatMoney(effectiveUnitPrice, s.currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasDiscount) {
                        Text(
                            text = formatMoney(product.retailPrice, s.currency),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onQuickAdd,
                    enabled = product.stockQty >= moq,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quick_add_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (product.stockQty >= moq) s.addToCart else s.outOfStock,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onAddToCart: (Long, PackMode) -> Unit
) {
    val s = LocalAppStrings.current
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
    val cartonBonus = if (packMode == PackMode.CARTON && product.cartonSize > 1L) 0.96 else 1.0
    val packPrice = PricingEngine.round2(effectiveUnit * unitsPerPack * cartonBonus)
    val totalPrice = PricingEngine.round2(packPrice * quantity)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = productDrawableForKey(product.imageKey)),
                contentDescription = product.localizedName(s.isArabic),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${product.brand} • ${product.sku}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = if (product.stockQty > 0L) SuccessContainer else ErrorContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (product.stockQty > 0L) "${s.stockAvailable}: ${product.stockQty}" else s.outOfStock,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stockQty > 0L) SuccessGreen else ErrorRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = product.localizedName(s.isArabic),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = product.localizedDesc(s.isArabic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Pack Mode Selector
            if (product.cartonSize > 1L) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = packMode == PackMode.UNIT,
                        onClick = { packMode = PackMode.UNIT },
                        label = { Text("${s.unitMode} (${product.localizedUnit(s.isArabic)})") },
                        modifier = Modifier.testTag("pack_mode_unit")
                    )
                    FilterChip(
                        selected = packMode == PackMode.CARTON,
                        onClick = { packMode = PackMode.CARTON },
                        label = { Text("${s.cartonMode} (${product.cartonSize}x -4%)") },
                        modifier = Modifier.testTag("pack_mode_carton")
                    )
                }
            }

            // Segment & Volume Tier Pricing Table
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = s.tierPricingTable,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    TierPriceRow(s.retailLabel, formatMoney(product.retailPrice, s.currency), profile?.segmentEnum() == CustomerSegment.RETAIL)
                    TierPriceRow(s.wholesaleLabel, "${formatMoney(product.wholesalePrice, s.currency)} (${s.moqLabel} ${product.moqWholesale})", profile?.segmentEnum() == CustomerSegment.WHOLESALE)
                    TierPriceRow(s.businessLabel, "${formatMoney(product.businessPrice, s.currency)} (${s.moqLabel} ${product.moqBusiness})", profile?.segmentEnum() == CustomerSegment.BUSINESS)
                    if (product.bulkDiscountPercent > 0L) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${s.bulkSavingsTag} (${product.bulkMinQty}+)",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberOnContainer
                            )
                            Text(
                                text = if (bulkApplied) "✓ -${product.bulkDiscountPercent}%" else "-${product.bulkDiscountPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (bulkApplied) SuccessGreen else AmberOnContainer
                            )
                        }
                    }
                }
            }

            // Quantity & Add CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (quantity > minPacks) quantity-- },
                        enabled = quantity > minPacks,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .testTag("detail_qty_minus")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$effectiveUnits ${if (s.isArabic) "وحدة" else "units"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { if (effectiveUnits + unitsPerPack <= product.stockQty) quantity++ },
                        enabled = effectiveUnits + unitsPerPack <= product.stockQty,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .testTag("detail_qty_plus")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Button(
                    onClick = { onAddToCart(quantity, packMode) },
                    enabled = product.stockQty >= effectiveUnits,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("detail_add_to_cart")
                ) {
                    Text("${s.addToCart} • ${formatMoney(totalPrice, s.currency)}")
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TierPriceRow(label: String, value: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isActive) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AquaTeal, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) AquaTeal else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LiveFleetMapCanvas(order: Order, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val pulseTransition = rememberInfiniteTransition(label = "driverPulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 16f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulseRadius"
    )

    val warehouseLat = 30.0444
    val warehouseLng = 31.2357
    val progress = remember(order.driverLat, order.driverLng, order.destLat, order.destLng) {
        val totalD = kotlin.math.hypot(order.destLat - warehouseLat, order.destLng - warehouseLng).coerceAtLeast(0.0001)
        val coveredD = kotlin.math.hypot(order.driverLat - warehouseLat, order.driverLng - warehouseLng)
        (coveredD / totalD).toFloat().coerceIn(0.08f, 0.96f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateNavy),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AquaTeal)
                    Text(
                        text = s.liveFleetMapTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = AquaContainer
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0B1324))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Subtle city grid
                    val gridColor = Color(0xFF1E293B)
                    for (i in 1..5) {
                        val y = h * i / 6f
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                    }
                    for (j in 1..7) {
                        val x = w * j / 8f
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.2f)
                    }

                    val start = Offset(w * 0.14f, h * 0.72f)
                    val end = Offset(w * 0.86f, h * 0.26f)
                    val driver = Offset(
                        x = start.x + (end.x - start.x) * progress,
                        y = start.y + (end.y - start.y) * progress
                    )

                    // Planned dashed route
                    drawLine(
                        color = Color(0xFF475569),
                        start = start,
                        end = end,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                    // Completed route
                    drawLine(
                        color = Color(0xFF14B8A6),
                        start = start,
                        end = driver,
                        strokeWidth = 7f,
                        cap = StrokeCap.Round
                    )

                    // Warehouse pin
                    drawCircle(color = Color(0xFF94A3B8), radius = 11f, center = start)
                    // Customer destination pin
                    drawCircle(color = Color(0xFFF59E0B), radius = 13f, center = end)
                    // Pulsing live driver pin
                    drawCircle(color = Color(0x5514B8A6), radius = pulseRadius, center = driver)
                    drawCircle(color = Color.White, radius = 12f, center = driver)
                    drawCircle(color = Color(0xFF0D9488), radius = 8f, center = driver)
                }

                // Telemetry overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color(0xCC0F172A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = AquaContainer, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${order.driverName.ifBlank { "Fleet Driver" }} • ${String.format(Locale.US, "%.4f, %.4f", order.driverLat, order.driverLng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
