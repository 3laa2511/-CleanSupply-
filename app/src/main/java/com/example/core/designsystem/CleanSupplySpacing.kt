package com.example.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Documented 4dp-increment spacing system for CleanSupply.
 */
@Immutable
data class CleanSupplySpacingTokens(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val screenHorizontal: Dp = 16.dp,
    val sectionGap: Dp = 20.dp,
    val cardPadding: Dp = 16.dp,
    val compactCardPadding: Dp = 12.dp,
    val listGap: Dp = 12.dp,
    val gridGap: Dp = 12.dp,
    val dialogPadding: Dp = 24.dp,
    val minTouchTarget: Dp = 48.dp
)

val CleanSupplySpacing = CleanSupplySpacingTokens()

val LocalCleanSupplySpacing = staticCompositionLocalOf { CleanSupplySpacing }
