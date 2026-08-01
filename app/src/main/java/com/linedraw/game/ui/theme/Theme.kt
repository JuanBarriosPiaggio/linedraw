package com.linedraw.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Vivid Void palette ───────────────────────────────────────────
val VoidBackground = Color(0xFF0C0E12) // default surface everywhere
val VoidSurface = Color(0xFF15181D)    // elevated surface — cards, tiles, sheets
val AccentCyan = Color(0xFF33E8FF)     // the line; primary CTAs, active states
val StarGold = Color(0xFFFFC24B)       // stars, success highlights only
val TextPrimary = Color(0xFFF4F6F8)
val TextSecondary = Color(0xFF8A9099)  // secondary text, unvisited dots, disabled
val DotIdle = Color(0xFF5A6270)        // unvisited dot rings
val BorderSubtle = Color(0xFF3A3E45)   // secondary button borders, empty stars

private val VividVoidColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = VoidBackground,
    secondary = StarGold,
    onSecondary = VoidBackground,
    background = VoidBackground,
    onBackground = TextPrimary,
    surface = VoidSurface,
    onSurface = TextPrimary,
    surfaceVariant = VoidSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
)

@Composable
fun LineDrawTheme(content: @Composable () -> Unit) {
    // Single dark theme by design (Vivid Void) — no light variant.
    MaterialTheme(
        colorScheme = VividVoidColorScheme,
        typography = LineDrawTypography,
        content = content,
    )
}
