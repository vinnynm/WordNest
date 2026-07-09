package com.enigma.wordnest.games.fragment.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorAmber      = Color(0xFFD9A34A)
val ColorAmberLight = Color(0xFFEBC078)
val ColorGood       = Color(0xFF6FA88A)
val ColorBackground = Color(0xFF14100C)
val ColorSurface    = Color(0xFF201A14)
val ColorBorder     = Color(0xFF3A2F22)
val ColorText       = Color(0xFFF0E8DC)
val ColorSubtle     = Color(0xFF8A7B68)

private val DarkScheme = darkColorScheme(
    primary = ColorAmberLight, secondary = ColorGood,
    background = ColorBackground, surface = ColorSurface,
    onBackground = ColorText, onSurface = ColorText,
    outline = ColorBorder, error = Color(0xFFCF6679)
)

@Composable
fun FragmentTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}