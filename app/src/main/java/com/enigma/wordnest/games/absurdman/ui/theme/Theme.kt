package com.enigma.wordnest.games.absurdman.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Absurdman shares Absurdle's menacing-purple family, with a rust accent
// to distinguish it at a glance.
val ColorRust        = Color(0xFFC2542A)
val ColorRustLight    = Color(0xFFE87A4C)
val ColorBackground  = Color(0xFF0D0D18)
val ColorSurface     = Color(0xFF16162A)
val ColorBorder      = Color(0xFF3A3A5C)
val ColorText        = Color(0xFFE8E8F0)
val ColorSubtle      = Color(0xFF6B6B8A)
val ColorGood        = Color(0xFF538D4E)

private val DarkScheme = darkColorScheme(
    primary      = ColorRustLight,
    secondary    = ColorGood,
    background   = ColorBackground,
    surface      = ColorSurface,
    onBackground = ColorText,
    onSurface    = ColorText,
    outline      = ColorBorder,
    error        = Color(0xFFCF6679)
)

@Composable
fun AbsurdmanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
