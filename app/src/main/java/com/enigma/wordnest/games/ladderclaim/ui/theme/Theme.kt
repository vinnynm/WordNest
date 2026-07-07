package com.enigma.wordnest.games.ladderclaim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorP1 = Color(0xFF4FD8CC)   // teal — player 1
val ColorP2 = Color(0xFFCF9A3B)   // amber — player 2
val ColorNeutral = Color(0xFF3A3A45)
val ColorBackground = Color(0xFF101418)
val ColorSurface = Color(0xFF171C21)
val ColorBorder = Color(0xFF2E3B40)
val ColorText = Color(0xFFE8F0EE)

private val DarkScheme = darkColorScheme(
    primary = ColorP1,
    secondary = ColorP2,
    background = ColorBackground,
    surface = ColorSurface,
    onBackground = ColorText,
    onSurface = ColorText,
    outline = ColorBorder,
    error = Color(0xFFCF6679)
)

@Composable
fun LadderClaimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}

fun playerColor(ownerId: Int?): Color = when (ownerId) {
    0 -> ColorP1
    1 -> ColorP2
    else -> ColorNeutral
}