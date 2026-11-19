package com.example.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Refined, restrained shape & subtle elevation tokens (preferring surface contrast over heavy shadows).
 */
@Immutable
data class CleanSupplyShapeTokens(
    val smallComponent: CornerBasedShape = RoundedCornerShape(8.dp),
    val chip: CornerBasedShape = RoundedCornerShape(10.dp),
    val button: CornerBasedShape = RoundedCornerShape(12.dp),
    val input: CornerBasedShape = RoundedCornerShape(12.dp),
    val card: CornerBasedShape = RoundedCornerShape(14.dp),
    val largeSurface: CornerBasedShape = RoundedCornerShape(18.dp),
    val dialog: CornerBasedShape = RoundedCornerShape(20.dp),
    val bottomSheet: CornerBasedShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    val pill: CornerBasedShape = RoundedCornerShape(50)
)

@Immutable
data class CleanSupplyElevationTokens(
    val none: Dp = 0.dp,
    val subtle: Dp = 1.dp,
    val card: Dp = 2.dp,
    val floating: Dp = 4.dp
)

val CleanSupplyShapes = CleanSupplyShapeTokens()
val CleanSupplyElevations = CleanSupplyElevationTokens()

fun CleanSupplyShapeTokens.toMaterialShapes(): Shapes = Shapes(
    extraSmall = smallComponent,
    small = chip,
    medium = card,
    large = largeSurface,
    extraLarge = dialog
)

val LocalCleanSupplyShapes = staticCompositionLocalOf { CleanSupplyShapes }
val LocalCleanSupplyElevations = staticCompositionLocalOf { CleanSupplyElevations }
