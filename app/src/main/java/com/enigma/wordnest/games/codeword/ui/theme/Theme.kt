package com.enigma.wordnest.games.codeword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorViolet      = Color(0xFF7C5CD9)
val ColorVioletLight = Color(0xFFA48CE8)
val ColorBackground  = Color(0xFF120E1A)
val ColorSurface     = Color(0xFF1C1628)
val ColorGridLine    = Color(0xFF332A45)
val ColorText        = Color(0xFFEDE9F5)
val ColorSubtle      = Color(0xFF8A7FA0)
val ColorCorrect     = Color(0xFF6FA88A)
val ColorStarter     = Color(0xFFD9A34A)

private val DarkScheme = darkColorScheme(
    primary      = ColorVioletLight,
    secondary    = ColorStarter,
    background   = ColorBackground,
    surface      = ColorSurface,
    onBackground = ColorText,
    onSurface    = ColorText,
    outline      = ColorGridLine,
    error        = Color(0xFFCF6679)
)

@Composable
fun CodewordTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
