package com.enigma.wordnest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NestPink    = Color(0xFFFF7AA2)
val NestPurple  = Color(0xFFB388FF)
val NestTeal    = Color(0xFF4ECDC4)
val NestBackground = Color(0xFF14121A)
val NestSurface    = Color(0xFF1E1B26)
val NestText       = Color(0xFFF4F1FA)
val NestSubtle     = Color(0xFF8A84A0)

private val NestDarkScheme = darkColorScheme(
    primary      = NestPink,
    secondary    = NestPurple,
    tertiary     = NestTeal,
    background   = NestBackground,
    surface      = NestSurface,
    onBackground = NestText,
    onSurface    = NestText,
    outline      = Color(0xFF3A3548)
)

@Composable
fun WordNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NestDarkScheme, content = content)
}
