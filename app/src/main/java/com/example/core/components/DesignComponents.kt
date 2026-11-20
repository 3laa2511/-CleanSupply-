package com.example.core.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.data.model.CustomerSegment
import com.example.data.model.OrderStatus
import com.example.data.model.UserProfile
import com.example.data.model.VerificationStatus
import com.example.ui.i18n.LocalAppStrings

@Composable
fun FastProductImage(
    @DrawableRes drawableRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val colors = CleanSupplyTheme.colors
    val context = LocalContext.current
    val request = remember(drawableRes) {
        ImageRequest.Builder(context)
            .data(drawableRes)
            .crossfade(160)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.background(colors.surfaceVariant)
    )
}

@Composable
fun CleanSupplyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = CleanSupplyTheme.colors.primary,
    contentColor: Color = CleanSupplyTheme.colors.onPrimary,
    testTag: String? = null
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !isLoading) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnPressScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabled
        ),
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = 0.dp),
        modifier = modifier
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(spacing.xs))
            }
            Text(
                text = text,
                style = type.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CleanSupplySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = CleanSupplyTheme.colors.primary,
    borderColor: Color = CleanSupplyTheme.colors.outlineStrong,
    testTag: String? = null
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shapes.button,
        border = BorderStroke(1.dp, if (enabled) borderColor else colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = contentColor,
            disabledContentColor = colors.disabled
        ),
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = 0.dp),
        modifier = modifier
            .height(48.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(spacing.xs))
        }
        Text(
            text = text,
            style = type.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CleanSupplySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {},
    testTag: String? = null
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                style = type.bodyMedium,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(CleanSupplyIcons.Search, contentDescription = s.navSearch, tint = colors.primary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(CleanSupplyIcons.Close, contentDescription = s.closeBtn, tint = colors.textSecondary)
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        shape = shapes.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineStrong,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it }
    )
}

@Composable
fun QuantityStepper(
    quantity: Long,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    minusTestTag: String? = null,
    plusTestTag: String? = null
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.xxs, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = canDecrement,
                modifier = Modifier
                    .size(spacing.minTouchTarget)
                    .let { if (minusTestTag != null) it.testTag(minusTestTag) else it }
            ) {
                Icon(
                    imageVector = CleanSupplyIcons.Remove,
                    contentDescription = "Decrease quantity",
                    tint = if (canDecrement) colors.textPrimary else colors.disabled
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = spacing.xs)
            ) {
                Text(
                    text = quantity.toString(),
                    style = type.numericMedium,
                    color = colors.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = type.caption,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
            IconButton(
                onClick = onIncrement,
                enabled = canIncrement,
                modifier = Modifier
                    .size(spacing.minTouchTarget)
                    .let { if (plusTestTag != null) it.testTag(plusTestTag) else it }
            ) {
                Icon(
                    imageVector = CleanSupplyIcons.Add,
                    contentDescription = "Increase quantity",
                    tint = if (canIncrement) colors.primary else colors.disabled
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography
    val spacing = CleanSupplyTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (subtitle.isNullOrBlank()) 20.dp else 32.dp)
                    .background(colors.primary, shapes.pill)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = type.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = type.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.height(spacing.minTouchTarget)
            ) {
                Text(actionLabel, style = type.labelLarge, color = colors.primary)
                Spacer(Modifier.width(spacing.xxs))
                Icon(
                    imageVector = CleanSupplyIcons.Forward,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SegmentVerificationBadge(profile: UserProfile?, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography
    val spacing = CleanSupplyTheme.spacing

    val seg = profile?.segmentEnum() ?: CustomerSegment.RETAIL
    val status = profile?.verificationEnum() ?: VerificationStatus.VERIFIED
    val segText = when (seg) {
        CustomerSegment.RETAIL -> s.retailLabel
        CustomerSegment.WHOLESALE -> s.wholesaleLabel
        CustomerSegment.BUSINESS -> s.businessLabel
    }
    val (fg, bg, icon) = when (status) {
        VerificationStatus.VERIFIED -> Triple(colors.success, colors.successContainer, CleanSupplyIcons.Verified)
        VerificationStatus.PENDING_VERIFICATION -> Triple(colors.pending, colors.pendingContainer, CleanSupplyIcons.Pending)
        VerificationStatus.REJECTED -> Triple(colors.error, colors.errorContainer, CleanSupplyIcons.Rejected)
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = shapes.pill,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Icon(icon, contentDescription = segText, tint = fg, modifier = Modifier.size(14.dp))
            Text(
                text = segText,
                style = type.labelSmall,
                color = fg,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography
    val spacing = CleanSupplyTheme.spacing

    val (label, bg, fg) = when (status) {
        OrderStatus.PLACED -> Triple(if (s.isArabic) "تم الاستلام" else "Placed", colors.infoContainer, colors.info)
        OrderStatus.CONFIRMED -> Triple(if (s.isArabic) "مؤكد" else "Confirmed", colors.primaryContainer, colors.onPrimaryContainer)
        OrderStatus.PACKED -> Triple(if (s.isArabic) "جاري التجهيز" else "Packed", colors.pendingContainer, colors.pending)
        OrderStatus.OUT_FOR_DELIVERY -> Triple(if (s.isArabic) "في الطريق" else "Out for Delivery", colors.primary, colors.onPrimary)
        OrderStatus.DELIVERED -> Triple(if (s.isArabic) "تم التوصيل" else "Delivered", colors.successContainer, colors.success)
        OrderStatus.CANCELLED -> Triple(if (s.isArabic) "ملغي" else "Cancelled", colors.errorContainer, colors.error)
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = shapes.pill,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            Icon(
                imageVector = CleanSupplyIcons.orderStatusIcon(status),
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = type.labelSmall,
                color = fg,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
