@file:OptIn(ExperimentalTextApi::class)

package com.linedraw.game.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.linedraw.game.R

// Sora (display) + Inter (UI/body), bundled as variable fonts.
val Sora = FontFamily(
    Font(R.font.sora, FontWeight.Bold, variationSettings = FontVariation.Settings(FontWeight.Bold, FontStyle.Normal)),
    Font(R.font.sora, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)),
    Font(R.font.sora, FontWeight.Medium, variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)),
)

val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal)),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)),
)

val LineDrawTypography = Typography(
    // App title / splash wordmark — Sora 700, 30–34sp
    displayMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.32.sp,
    ),
    // Level number / section headers — Sora 700, 20sp
    titleLarge = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
    // Button labels — Inter 600, 15sp
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    // In-game HUD — Inter 600, 13sp
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    ),
    // Body / settings text — Inter 400, 13sp
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
    ),
)
