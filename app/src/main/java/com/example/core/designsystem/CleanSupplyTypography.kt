package com.example.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.i18n.AppLanguage

val ArabicSupplyFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal)
)

val EnglishInterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal)
)

@Immutable
data class CleanSupplyTypographyTokens(
    val displayLarge: TextStyle,
    val displaySmall: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val caption: TextStyle,
    val numericLarge: TextStyle,
    val numericMedium: TextStyle,
    val numericSmall: TextStyle
)

fun buildCleanSupplyTypography(language: AppLanguage): CleanSupplyTypographyTokens {
    val primaryFamily = if (language == AppLanguage.ARABIC) ArabicSupplyFontFamily else EnglishInterFontFamily
    val isArabic = language == AppLanguage.ARABIC

    return CleanSupplyTypographyTokens(
        displayLarge = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isArabic) 28.sp else 27.sp,
            lineHeight = if (isArabic) 38.sp else 34.sp,
            letterSpacing = if (isArabic) 0.sp else (-0.4).sp
        ),
        displaySmall = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isArabic) 24.sp else 23.sp,
            lineHeight = if (isArabic) 33.sp else 30.sp,
            letterSpacing = if (isArabic) 0.sp else (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = if (isArabic) 30.sp else 28.sp,
            letterSpacing = if (isArabic) 0.sp else (-0.2).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = if (isArabic) 26.sp else 24.sp,
            letterSpacing = if (isArabic) 0.sp else (-0.1).sp
        ),
        titleLarge = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = if (isArabic) 25.sp else 23.sp
        ),
        titleMedium = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.5.sp,
            lineHeight = if (isArabic) 23.sp else 21.sp
        ),
        titleSmall = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = if (isArabic) 21.sp else 19.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.5.sp,
            lineHeight = if (isArabic) 25.sp else 23.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = if (isArabic) 22.sp else 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = if (isArabic) 20.sp else 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp
        ),
        labelMedium = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        ),
        labelSmall = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        caption = TextStyle(
            fontFamily = primaryFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        numericLarge = TextStyle(
            fontFamily = EnglishInterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontFeatureSettings = "tnum"
        ),
        numericMedium = TextStyle(
            fontFamily = EnglishInterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontFeatureSettings = "tnum"
        ),
        numericSmall = TextStyle(
            fontFamily = EnglishInterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            fontFeatureSettings = "tnum"
        )
    )
}

val ArabicCleanSupplyTypography = buildCleanSupplyTypography(AppLanguage.ARABIC)
val EnglishCleanSupplyTypography = buildCleanSupplyTypography(AppLanguage.ENGLISH)

fun CleanSupplyTypographyTokens.toMaterialTypography(): Typography = Typography(
    displayLarge = displayLarge,
    displaySmall = displaySmall,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall
)

val LocalCleanSupplyTypography = staticCompositionLocalOf { ArabicCleanSupplyTypography }
