package com.example.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CleanSupplyColorTokens(
    // Brand & Interactive Greens
    val primary: Color,
    val primaryDark: Color,
    val interactive: Color,
    val onInteractive: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Warm Sand Accent
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    // Surfaces & Warm Ivory Canvas
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineStrong: Color,

    // Typography Ink
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // Editorial Forest Banners (always light ink over deep forest canvas in both Light & Dark)
    val brandBanner: Color,
    val brandBannerSurface: Color,
    val onBrandBanner: Color,
    val heroAccent: Color,
    val scrim: Color,

    // Semantic States
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val discount: Color,
    val discountContainer: Color,
    val outOfStock: Color,
    val outOfStockContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val disabled: Color,
    val disabledContainer: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color
)

val LightCleanSupplyColors = CleanSupplyColorTokens(
    primary = Color(0xFF174C3C),
    primaryDark = Color(0xFF0F352A),
    interactive = Color(0xFF23845D),
    onInteractive = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4F0EA),
    onPrimaryContainer = Color(0xFF123D30),

    secondary = Color(0xFFD8C6A0),
    onSecondary = Color(0xFF202722),
    secondaryContainer = Color(0xFFF3EDE0),
    onSecondaryContainer = Color(0xFF3A3222),

    background = Color(0xFFF7F6F1),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0EEE7),
    surfaceContainerHigh = Color(0xFFE7E4DA),
    outline = Color(0xFFE8E7DF),
    outlineStrong = Color(0xFFD2D0C5),

    textPrimary = Color(0xFF202722),
    textSecondary = Color(0xFF5D655F),
    textMuted = Color(0xFF737B75),

    brandBanner = Color(0xFF174C3C),
    brandBannerSurface = Color(0xFF10382C),
    onBrandBanner = Color(0xFFF9F8F3),
    heroAccent = Color(0xFFD8C6A0),
    scrim = Color(0xBF141D18),

    success = Color(0xFF1B7A4B),
    successContainer = Color(0xFFE2F3E9),
    warning = Color(0xFF9A5B13),
    warningContainer = Color(0xFFFAF0DC),
    error = Color(0xFFB82E24),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDEBE9),
    info = Color(0xFF1E5E6D),
    infoContainer = Color(0xFFE5F1F3),
    discount = Color(0xFFA65211),
    discountContainer = Color(0xFFFBF0E1),
    outOfStock = Color(0xFFB82E24),
    outOfStockContainer = Color(0xFFFDEBE9),
    pending = Color(0xFF9A5B13),
    pendingContainer = Color(0xFFFAF0DC),
    disabled = Color(0xFF9EA49F),
    disabledContainer = Color(0xFFECEBE4),
    shimmerBase = Color(0xFFEAE8DF),
    shimmerHighlight = Color(0xFFF6F5F0)
)

val DarkCleanSupplyColors = CleanSupplyColorTokens(
    primary = Color(0xFF47B385),
    primaryDark = Color(0xFF23845D),
    interactive = Color(0xFF2B956A),
    onInteractive = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF0A2118),
    primaryContainer = Color(0xFF183B2E),
    onPrimaryContainer = Color(0xFFD6EFE4),

    secondary = Color(0xFFD8C6A0),
    onSecondary = Color(0xFF1D1B14),
    secondaryContainer = Color(0xFF2B271D),
    onSecondaryContainer = Color(0xFFEDE2C9),

    background = Color(0xFF111613),
    surface = Color(0xFF181F1B),
    surfaceElevated = Color(0xFF1F2823),
    surfaceVariant = Color(0xFF232D27),
    surfaceContainerHigh = Color(0xFF2B3630),
    outline = Color(0xFF2D3731),
    outlineStrong = Color(0xFF3E4C44),

    textPrimary = Color(0xFFF2F1EC),
    textSecondary = Color(0xFFC2C8C3),
    textMuted = Color(0xFF8E9790),

    brandBanner = Color(0xFF174C3C),
    brandBannerSurface = Color(0xFF10372B),
    onBrandBanner = Color(0xFFF9F8F3),
    heroAccent = Color(0xFFD8C6A0),
    scrim = Color(0xD90C120F),

    success = Color(0xFF4ADE80),
    successContainer = Color(0xFF123A27),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF3D2A0D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF4B1818),
    info = Color(0xFF5EEAD4),
    infoContainer = Color(0xFF133A3E),
    discount = Color(0xFFFBBF24),
    discountContainer = Color(0xFF3D2A0D),
    outOfStock = Color(0xFFF87171),
    outOfStockContainer = Color(0xFF4B1818),
    pending = Color(0xFFFBBF24),
    pendingContainer = Color(0xFF3D2A0D),
    disabled = Color(0xFF6B746E),
    disabledContainer = Color(0xFF202823),
    shimmerBase = Color(0xFF1F2823),
    shimmerHighlight = Color(0xFF2B3630)
)

val LocalCleanSupplyColors = staticCompositionLocalOf { LightCleanSupplyColors }
