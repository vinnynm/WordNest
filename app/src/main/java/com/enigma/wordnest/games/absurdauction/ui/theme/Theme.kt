package com.enigma.wordnest.games.absurdauction.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorGreen        = Color(0xFF538D4E)
val ColorPurple       = Color(0xFF6B21A8)
val ColorPurpleLight  = Color(0xFFA855F7)
val ColorBackground   = Color(0xFF0D0D18)
val ColorSurface      = Color(0xFF16162A)
val ColorBorder       = Color(0xFF3A3A5C)
val ColorText         = Color(0xFFE8E8F0)
val ColorSubtle       = Color(0xFF6B6B8A)
val ColorTensionLow   = Color(0xFF4ADE80)
val ColorTensionHigh  = Color(0xFFA855F7)

private val DarkScheme = darkColorScheme(
    primary      = ColorPurpleLight,
    secondary    = ColorGreen,
    background   = ColorBackground,
    surface      = ColorSurface,
    onBackground = ColorText,
    onSurface    = ColorText,
    outline      = ColorBorder,
    error        = Color(0xFFCF6679)
)

@Composable
fun AbsurdAuctionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}