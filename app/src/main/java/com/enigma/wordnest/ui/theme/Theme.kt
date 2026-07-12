package com.enigma.wordnest.ui.theme

import com.enigma.wordnest.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

val NestPink    = Color(0xFFFF7AA2)
val NestPurple  = Color(0xFFB388FF)
val NestTeal    = Color(0xFF4ECDC4)
val NestBackground = Color(0xFF14121A)
val NestSurface    = Color(0xFF1E1B26)
val NestText       = Color(0xFFF4F1FA)
val NestSubtle     = Color(0xFF8A84A0)

val NestGradient = listOf(NestPink, NestPurple, NestTeal)

val NestGradientDark = listOf(NestSurface, NestSubtle, NestBackground)
val ArchitectDaughter = Font(R.font.architects_daughter)



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
    MaterialTheme(
        colorScheme = NestDarkScheme,
        content = content,
        typography = Typography().copy(
            bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily(ArchitectDaughter)),
            bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily(ArchitectDaughter)),
            bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily(ArchitectDaughter)),
            titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily(ArchitectDaughter)),
            titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily(ArchitectDaughter)),
            titleSmall = Typography().titleSmall.copy(fontFamily = FontFamily(ArchitectDaughter)),
            headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily(ArchitectDaughter)),
            headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily(ArchitectDaughter)),
            headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily(ArchitectDaughter)),
            displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily(ArchitectDaughter)),
            displayMedium = Typography().displayMedium.copy(fontFamily = FontFamily(ArchitectDaughter)),
            displaySmall = Typography().displaySmall.copy(fontFamily = FontFamily(ArchitectDaughter)),
            labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily(ArchitectDaughter)),
            labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily(ArchitectDaughter)),
            labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily(ArchitectDaughter)),
        )
    )
}
