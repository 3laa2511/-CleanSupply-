package com.example.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor

enum class AppThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): AppThemeMode = when (code) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

object CleanSupplyTheme {
    val colors: CleanSupplyColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCleanSupplyColors.current

    val typography: CleanSupplyTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCleanSupplyTypography.current

    val spacing: CleanSupplySpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCleanSupplySpacing.current

    val shapes: CleanSupplyShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCleanSupplyShapes.current

    val elevations: CleanSupplyElevationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCleanSupplyElevations.current
}

@Composable
fun CleanSupplyTheme(
    language: AppLanguage = AppLanguage.ARABIC,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkCleanSupplyColors else LightCleanSupplyColors
    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.onPrimaryContainer,
            secondary = tokens.secondary,
            onSecondary = tokens.onSecondary,
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.onSecondaryContainer,
            tertiary = tokens.warning,
            onTertiary = tokens.brandSlate,
            tertiaryContainer = tokens.warningContainer,
            onTertiaryContainer = tokens.warning,
            background = tokens.background,
            onBackground = tokens.textPrimary,
            surface = tokens.surface,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceVariant,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.outline,
            outlineVariant = tokens.outlineStrong,
            error = tokens.error,
            onError = tokens.onError,
            errorContainer = tokens.errorContainer,
            onErrorContainer = tokens.error
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.onPrimaryContainer,
            secondary = tokens.secondary,
            onSecondary = tokens.onSecondary,
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.onSecondaryContainer,
            tertiary = tokens.warning,
            onTertiary = tokens.onPrimary,
            tertiaryContainer = tokens.warningContainer,
            onTertiaryContainer = tokens.warning,
            background = tokens.background,
            onBackground = tokens.textPrimary,
            surface = tokens.surface,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceVariant,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.outline,
            outlineVariant = tokens.outlineStrong,
            error = tokens.error,
            onError = tokens.onError,
            errorContainer = tokens.errorContainer,
            onErrorContainer = tokens.error
        )
    }

    val layoutDirection = if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = stringsFor(language)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides strings,
        LocalCleanSupplyColors provides tokens,
        LocalCleanSupplyTypography provides CleanSupplyTypeScale,
        LocalCleanSupplySpacing provides CleanSupplySpacing,
        LocalCleanSupplyShapes provides CleanSupplyShapes,
        LocalCleanSupplyElevations provides CleanSupplyElevations
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = CleanSupplyTypeScale.toMaterialTypography(),
            shapes = CleanSupplyShapes.toMaterialShapes(),
            content = content
        )
    }
}
