package com.enigma.wordnest.games.betweenle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.enigma.wordnest.games.betweenle.model.GuessFeedback

// ── Palette ───────────────────────────────────────────────────────────────────

val PrimaryGreen   = Color(0xFF538D4E)
val LightGreen     = Color(0xFF6AAF65)
val PrimaryYellow  = Color(0xFFB59F3B)
val LightYellow    = Color(0xFFCFB84A)
val PrimaryRed     = Color(0xFFBF3B3B)
val LightRed       = Color(0xFFD95555)
val BurningOrange  = Color(0xFFE05C00)
val HotOrange      = Color(0xFFE08200)
val WarmYellow     = Color(0xFFD4A017)
val FarGray        = Color(0xFF888888)

val DarkBackground = Color(0xFF121213)
val DarkSurface    = Color(0xFF1A1A1B)
val DarkTile       = Color(0xFF121213)
val DarkBorder     = Color(0xFF3A3A3C)
val LightBorder    = Color(0xFFD3D6DA)
val DarkText       = Color(0xFFFFFFFF)
val LightText      = Color(0xFF1A1A1B)
val SubtleText     = Color(0xFF818384)

private val DarkColors = darkColorScheme(
    primary          = PrimaryGreen,
    onPrimary        = Color.White,
    secondary        = PrimaryYellow,
    background       = DarkBackground,
    surface          = DarkSurface,
    onBackground     = DarkText,
    onSurface        = DarkText,
    outline          = DarkBorder,
    error            = PrimaryRed,
)

private val LightColors = lightColorScheme(
    primary          = PrimaryGreen,
    onPrimary        = Color.White,
    secondary        = PrimaryYellow,
    background       = Color(0xFFF9F9F9),
    surface          = Color.White,
    onBackground     = LightText,
    onSurface        = LightText,
    outline          = LightBorder,
    error            = PrimaryRed,
)

@Composable
fun BetweenleTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

// ── Guess tile colours ────────────────────────────────────────────────────────

@Composable
fun feedbackColor(feedback: GuessFeedback): Color {
    return when (feedback) {
        GuessFeedback.CORRECT           -> PrimaryGreen
        GuessFeedback.TOO_LOW           -> PrimaryRed
        GuessFeedback.TOO_HIGH          -> PrimaryYellow
        GuessFeedback.IS_BOUNDARY       -> Color(0xFF5C5C5C)
        GuessFeedback.NOT_IN_DICTIONARY -> Color(0xFF3A3A3C)
    }
}
