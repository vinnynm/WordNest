package com.enigma.wordnest.games.chromaword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.enigma.wordnest.games.chromaword.model.LetterColor

// ── Game tile colours ─────────────────────────────────────────────────────────
val ColorGreen      = Color(0xFF538D4E)   // correct position
val ColorYellow     = Color(0xFFB59F3B)   // in word, wrong position (single)
val ColorLightBlue  = Color(0xFF5BA4CF)   // multi-occurrence, partially correct
val ColorRoyalBlue  = Color(0xFF2255CC)   // multi-occurrence, all wrong
val ColorRedExtra   = Color(0xFFBF3B3B)   // surplus correct-letter instances
val ColorRedAbsent  = Color(0xFF8B1A1A)   // letter not in word
val ColorEmpty      = Color(0xFF3A3A3C)   // unfilled tile border
val ColorFilled     = Color(0xFF565758)   // typed but not submitted

// ── UI chrome ─────────────────────────────────────────────────────────────────
val BackgroundDark  = Color(0xFF121213)
val SurfaceDark     = Color(0xFF1A1A1B)
val OnSurface       = Color(0xFFFFFFFF)
val SubtleGray      = Color(0xFF818384)

private val DarkScheme = darkColorScheme(
    primary      = ColorGreen,
    secondary    = ColorYellow,
    background   = BackgroundDark,
    surface      = SurfaceDark,
    onBackground = OnSurface,
    onSurface    = OnSurface,
    outline      = ColorEmpty,
    error        = ColorRedAbsent
)

@Composable
fun ChromaWordTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}

// ── Mapping function used by tile composables ─────────────────────────────────
fun letterColor(lc: LetterColor): Color = when (lc) {
    LetterColor.GREEN      -> ColorGreen
    LetterColor.YELLOW     -> ColorYellow
    LetterColor.LIGHT_BLUE -> ColorLightBlue
    LetterColor.ROYAL_BLUE -> ColorRoyalBlue
    LetterColor.RED_EXTRA  -> ColorRedExtra
    LetterColor.RED_ABSENT -> ColorRedAbsent
    LetterColor.EMPTY      -> Color.Transparent
}

fun tileBorderColor(lc: LetterColor): Color = when (lc) {
    LetterColor.EMPTY -> ColorEmpty
    else              -> letterColor(lc)
}
