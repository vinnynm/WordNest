package com.enigma.wordnest.games.absurdle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.enigma.wordnest.games.absurdle.model.TileColor

// Absurdle uses a menacing purple-dark palette
val ColorGreen     = Color(0xFF538D4E)
val ColorYellow    = Color(0xFFB59F3B)
val ColorGray      = Color(0xFF3A3A3C)
val ColorPurple    = Color(0xFF6B21A8)   // brand accent
val ColorPurpleLight = Color(0xFFA855F7)
val ColorBackground = Color(0xFF0D0D18)  // very dark blue-black
val ColorSurface    = Color(0xFF16162A)
val ColorBorder     = Color(0xFF3A3A5C)
val ColorText       = Color(0xFFE8E8F0)
val ColorSubtle     = Color(0xFF6B6B8A)

private val DarkScheme = darkColorScheme(
    primary      = ColorPurpleLight,
    secondary    = ColorYellow,
    background   = ColorBackground,
    surface      = ColorSurface,
    onBackground = ColorText,
    onSurface    = ColorText,
    outline      = ColorBorder,
    error        = Color(0xFFCF6679)
)

@Composable
fun AbsurdleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}

fun tileBackground(color: TileColor): Color = when (color) {
    TileColor.GREEN  -> ColorGreen
    TileColor.YELLOW -> ColorYellow
    TileColor.GRAY   -> ColorGray
    TileColor.EMPTY  -> Color.Transparent
}

fun tileBorder(color: TileColor): Color = when (color) {
    TileColor.EMPTY -> ColorBorder
    else            -> tileBackground(color)
}

fun keyBackground(color: TileColor): Color = when (color) {
    TileColor.GREEN  -> ColorGreen
    TileColor.YELLOW -> ColorYellow
    TileColor.GRAY   -> ColorGray
    TileColor.EMPTY  -> Color(0xFF565670)
}
