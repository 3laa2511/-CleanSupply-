package com.example.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.CleanSupplyIcons
import com.example.core.designsystem.CleanSupplyTheme
import com.example.ui.i18n.LocalAppStrings

@Composable
fun rememberShimmerBrush(): Brush {
    val colors = CleanSupplyTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerShift"
    )
    return Brush.linearGradient(
        colors = listOf(colors.shimmerBase, colors.shimmerHighlight, colors.shimmerBase),
        start = Offset(shift - 350f, shift - 350f),
        end = Offset(shift, shift)
    )
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val brush = rememberShimmerBrush()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .padding(spacing.xs)
                    .clip(shapes.smallComponent)
                    .background(brush)
            )
            Column(
                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(14.dp)
                        .clip(shapes.smallComponent)
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(shapes.smallComponent)
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(20.dp)
                        .clip(shapes.smallComponent)
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(shapes.button)
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun ListRowSkeleton(modifier: Modifier = Modifier) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val spacing = CleanSupplyTheme.spacing
    val brush = rememberShimmerBrush()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.width(100.dp).height(18.dp).clip(shapes.smallComponent).background(brush))
                Box(modifier = Modifier.width(72.dp).height(20.dp).clip(shapes.pill).background(brush))
            }
            Box(modifier = Modifier.fillMaxWidth(0.72f).height(14.dp).clip(shapes.smallComponent).background(brush))
            Box(modifier = Modifier.width(110.dp).height(18.dp).clip(shapes.smallComponent).background(brush))
        }
    }
}

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val spacing = CleanSupplyTheme.spacing
    val type = CleanSupplyTheme.typography

    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = colors.warningContainer,
            border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Icon(
                    imageVector = CleanSupplyIcons.Offline,
                    contentDescription = s.offlineTitle,
                    tint = colors.warning,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.offlineTitle, style = type.labelMedium, color = colors.warning)
                    Text(s.offlineSubtitle, style = type.caption, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionTestTag: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = CleanSupplyTheme.colors
    val spacing = CleanSupplyTheme.spacing
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                color = colors.primaryContainer.copy(alpha = 0.72f),
                shape = shapes.largeSurface,
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.18f)),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Text(
                text = title,
                style = type.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = type.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(spacing.xxs))
                CleanSupplyPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    testTag = actionTestTag
                )
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String? = null
) {
    val s = LocalAppStrings.current
    val colors = CleanSupplyTheme.colors
    val spacing = CleanSupplyTheme.spacing
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                color = colors.errorContainer,
                shape = shapes.largeSurface,
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.25f)),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CleanSupplyIcons.Error,
                        contentDescription = message,
                        tint = colors.error,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Text(
                text = message,
                style = type.bodyLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onRetry,
                shape = shapes.button,
                border = BorderStroke(1.dp, colors.outlineStrong),
                modifier = Modifier
                    .height(spacing.minTouchTarget)
                    .let { if (retryTestTag != null) it.testTag(retryTestTag) else it }
            ) {
                Text(s.retryBtn, style = type.labelLarge, color = colors.primary)
            }
        }
    }
}

@Composable
fun CleanSupplyConfirmationDialog(
    title: String,
    body: String,
    confirmText: String,
    dismissText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CleanSupplyTheme.colors
    val shapes = CleanSupplyTheme.shapes
    val type = CleanSupplyTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.dialog,
        containerColor = colors.surface,
        title = { Text(title, style = type.titleLarge, color = colors.textPrimary) },
        text = { Text(body, style = type.bodyMedium, color = colors.textSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) colors.error else colors.primary,
                    contentColor = if (isDestructive) colors.onError else colors.onPrimary
                )
            ) {
                Text(confirmText, style = type.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, style = type.labelLarge, color = colors.textSecondary)
            }
        }
    )
}
