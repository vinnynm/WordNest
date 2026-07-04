package com.enigma.wordnest.games.wordladder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorTeal      = Color(0xFF2AA198)
val ColorTealLight = Color(0xFF4FD8CC)
val ColorAmber     = Color(0xFFCF9A3B)
val ColorBackground = Color(0xFF101418)
val ColorSurface    = Color(0xFF171C21)
val ColorBorder     = Color(0xFF2E3B40)
val ColorText       = Color(0xFFE8F0EE)
val ColorSubtle     = Color(0xFF6B8A85)

private val DarkScheme = darkColorScheme(
    primary      = ColorTealLight,
    secondary    = ColorAmber,
    background   = ColorBackground,
    surface      = ColorSurface,
    onBackground = ColorText,
    onSurface    = ColorText,
    outline      = ColorBorder,
    error        = Color(0xFFCF6679)
)

@Composable
fun WordLadderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
