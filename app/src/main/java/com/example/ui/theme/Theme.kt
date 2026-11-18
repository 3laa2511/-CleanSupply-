package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor

private val LightColors = lightColorScheme(
    primary = AquaTeal,
    onPrimary = Color.White,
    primaryContainer = AquaContainer,
    onPrimaryContainer = AquaContainerText,
    secondary = SlateNavy,
    onSecondary = Color.White,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = SlateNavy,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = MistBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBorder,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = AquaTealDark,
    onPrimary = SlateNavy,
    primaryContainer = AquaContainerText,
    onPrimaryContainer = AquaContainer,
    secondary = SlateNavySoft,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = MistBackground,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = AmberOnContainer,
    onTertiaryContainer = AmberContainer,
    background = DarkBackground,
    onBackground = MistBackground,
    surface = DarkSurface,
    onSurface = MistBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SlateContainer,
    outline = DarkOutline,
    error = ErrorRed,
    onError = Color.White
)

private val CleanSupplyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CleanSupplyTheme(
    language: AppLanguage = AppLanguage.ARABIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val layoutDirection = if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
    val strings = stringsFor(language)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CleanSupplyTypography,
            shapes = CleanSupplyShapes,
            content = content
        )
    }
}
