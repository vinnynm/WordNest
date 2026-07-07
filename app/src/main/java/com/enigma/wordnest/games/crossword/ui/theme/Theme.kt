package com.enigma.wordnest.games.crossword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorInk        = Color(0xFF2B4C6F)
val ColorInkLight   = Color(0xFF5B8CB8)
val ColorAccent     = Color(0xFFD98E4A)
val ColorBackground = Color(0xFF10151C)
val ColorSurface    = Color(0xFF171F29)
val ColorGridLine   = Color(0xFF2A3648)
val ColorText       = Color(0xFFE9EEF5)
val ColorSubtle     = Color(0xFF7A889C)
val ColorWrong      = Color(0xFFCF6679)
val ColorRevealed   = Color(0xFF6FA88A)

private val DarkScheme = darkColorScheme(
    primary      = ColorInkLight,
    secondary    = ColorAccent,
    background   = ColorBackground,
    surface      = ColorSurface,
    onBackground = ColorText,
    onSurface    = ColorText,
    outline      = ColorGridLine,
    error        = ColorWrong
)

@Composable
fun CrosswordTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
