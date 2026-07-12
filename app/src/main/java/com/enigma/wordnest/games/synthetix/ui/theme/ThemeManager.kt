package com.enigma.wordnest.games.synthetix.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import com.enigma.wordnest.games.synthetix.model.SquareType

/**
 * Application-scoped theme manager.
 *
 * Provides a [StateFlow] of [BoardTheme] that composables can collect.
 * The active theme is persisted to SharedPreferences so it survives
 * process death.
 *
 * Usage:
 *   // In ViewModel:
 *   val themeManager = ThemeManager(context)
 *
 *   // In Composable:
 *   val theme by themeManager.activeTheme.collectAsStateWithLifecycle()
 */
class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("synthetix_theme", Context.MODE_PRIVATE)

    private val _activeTheme = MutableStateFlow(loadSavedTheme())
    val activeTheme: StateFlow<BoardTheme> = _activeTheme.asStateFlow()

    fun setTheme(theme: BoardTheme) {
        _activeTheme.value = theme
        prefs.edit { putString("theme_id", theme.id) }
    }

    private fun loadSavedTheme(): BoardTheme {
        val id = prefs.getString("theme_id", BoardTheme.Wood.id) ?: BoardTheme.Wood.id
        return BoardTheme.fromId(id)
    }

    // Convenience accessor for the current value without Flow collection
    val current: BoardTheme get() = _activeTheme.value

    // Helper for builder UI colors (defaulting to Wood theme palette)
    object SquareColors {

        fun forSquare(squareType: SquareType): Color = when {
            squareType.isObstacle -> Color(0xFF1A1A2E)
            squareType.isNegative -> Color(0xFF2D2D2D)
            squareType.isAnchor -> Color(0xFFE63946)
            else -> when (squareType) {
                SquareType.DL -> Color(0xFFA8DADC)
                SquareType.TL -> Color(0xFF457B9D)
                SquareType.QL -> Color(0xFF1A3A5C)
                SquareType.DW -> Color(0xFFFFB3B3)
                SquareType.TW -> Color(0xFFE63946)
                SquareType.QW -> Color(0xFF9B2335)
                SquareType.VW -> Color(0xFF5C1A5C)
                else -> Color(0xFF8DB596) // Empty
            }
        }

        fun labelColorFor(squareType: SquareType): Color = when (squareType) {
            SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
            else -> Color.White
        }
        fun labelColorFromThemesFor(squareType: SquareType, boardTheme: BoardTheme, isDark: Boolean): Color = when (boardTheme) {
            BoardTheme.Circuit -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                } else {
                   when (squareType) {
                       SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                       else -> Color.White
                   }
                }
            }
            BoardTheme.Ice -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                    } else {
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                }
            }
            BoardTheme.Jungle -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF1A0B0B)
                        else -> Color.Black
                    }
                    } else {
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF2D1919)
                        else -> Color.Black
                    }
                }
            }
            BoardTheme.Marble -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFFB9E5B9)
                        else -> Color.Black
                    }
                    } else {
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF022605)
                        else -> Color.White
                    }
                }
            }
            BoardTheme.NeonNoir -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                    } else {
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                }
            }
            BoardTheme.Volcano -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                    } else {
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                }
            }
            BoardTheme.Wood -> {
                if (isDark){
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                    } else {
                    when (squareType) {
                        SquareType.DW, SquareType.DL -> Color(0xFF8B0000)
                        else -> Color.White
                    }
                }
            }
        }

        private fun boardColorsFromThemes(
            boardTheme: BoardTheme,
            isDark: Boolean,
            squareType: SquareType
        ): Color {
            when (boardTheme) {
                BoardTheme.Wood -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFFD99C45) else Color(0xFFC5542F)
                            }
                        }
                    }

                }

                BoardTheme.Circuit -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFF0AE512) else Color(0xFFCCFFBE)
                            }
                        }
                    }
                }
                BoardTheme.Ice -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFF5FC6E0) else Color(0xFF03254D)
                            }
                        }
                    }
                }
                BoardTheme.Jungle -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFF74EE90) else Color(0xFF3AFA05)
                            }
                        }
                    }
                }
                BoardTheme.Marble -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFF2D2D60) else Color(0xFF5C1A5C)
                            }
                        }
                    }
                }
                BoardTheme.NeonNoir -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFFCCF6D6) else Color(0xFF312831)
                            }
                        }
                    }
                }
                BoardTheme.Volcano -> {
                    return when{
                        squareType.isObstacle -> Color(0xFF1A1A2E)
                        squareType.isNegative -> Color(0xFF2D2D2D)
                        squareType.isAnchor -> Color(0xFFE63946)
                        else -> when (squareType) {
                            SquareType.DL -> Color(0xFFA8DADC)
                            SquareType.TL -> Color(0xFF457B9D)
                            SquareType.QL -> Color(0xFF1A3A5C)
                            else -> {
                                if (isDark) Color(0xFFF3399A) else Color(0xFF500303)
                            }
                        }
                    }
                }
            }
        }
    }

}