package com.enigma.wordnest.games.synthetix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.SquareType
import com.google.accompanist.systemuicontroller.rememberSystemUiController


private object SynthetixColors {
    val ElectricTeal  = Color(0xFF00C9B1)
    val DeepTeal      = Color(0xFF007A6E)
    val WarmAmber     = Color(0xFFF2A93B)
    val AmberDark     = Color(0xFF8A5E15)
    val Ink900        = Color(0xFF0A0E14)
    val Ink800        = Color(0xFF111722)
    val Ink700        = Color(0xFF1A2233)
    val Ink600        = Color(0xFF232E42)
    val Ink500        = Color(0xFF2E3D57)
    val InkText       = Color(0xFFD4E0F0)
    val InkSubtext    = Color(0xFF5E7A9E)
    val Cloud50       = Color(0xFFF4F7FB)
    val Cloud100      = Color(0xFFE6EDF8)
    val Cloud200      = Color(0xFFCFDCF0)
    val CloudText     = Color(0xFF0D1826)
    val CloudSubtext  = Color(0xFF4A6080)
    val Success       = Color(0xFF2ECC71)
    val Error         = Color(0xFFE05C5C)
}

private val DarkColorScheme = darkColorScheme(
    primary             = SynthetixColors.ElectricTeal,
    primaryContainer    = SynthetixColors.Ink700,
    onPrimary           = SynthetixColors.Ink900,
    onPrimaryContainer  = SynthetixColors.ElectricTeal,
    secondary           = SynthetixColors.WarmAmber,
    secondaryContainer  = SynthetixColors.Ink600,
    onSecondary         = SynthetixColors.Ink900,
    onSecondaryContainer= SynthetixColors.WarmAmber,
    background          = SynthetixColors.Ink900,
    surface             = SynthetixColors.Ink800,
    surfaceVariant      = SynthetixColors.Ink700,
    onBackground        = SynthetixColors.InkText,
    onSurface           = SynthetixColors.InkText,
    onSurfaceVariant    = SynthetixColors.InkSubtext,
    error               = SynthetixColors.Error,
    outline             = SynthetixColors.Ink500
)

private val LightColorScheme = lightColorScheme(
    primary             = SynthetixColors.DeepTeal,
    primaryContainer    = SynthetixColors.Cloud100,
    onPrimary           = Color.White,
    onPrimaryContainer  = SynthetixColors.DeepTeal,
    secondary           = SynthetixColors.AmberDark,
    secondaryContainer  = SynthetixColors.Cloud200,
    onSecondary         = Color.White,
    onSecondaryContainer= SynthetixColors.AmberDark,
    background          = SynthetixColors.Cloud50,
    surface             = Color.White,
    surfaceVariant      = SynthetixColors.Cloud100,
    onBackground        = SynthetixColors.CloudText,
    onSurface           = SynthetixColors.CloudText,
    onSurfaceVariant    = SynthetixColors.CloudSubtext,
    error               = SynthetixColors.Error,
    outline             = SynthetixColors.Cloud200
)

val SynthetixTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Black,  fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Black,  fontSize = 45.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.15.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

enum class TileTextureType {
    WOOD_GRAIN, ICE_CRYSTAL, LAVA_ROCK, CIRCUIT_TRACE, MARBLE_VEIN, NEON_GLOW, BAMBOO
}

enum class BoardTextureType {
    DARK_WOOD, FROST, MAGMA, PCB_GRID, STONE_TILE, NOIR_GRID, LEAF_PATTERN
}

// ─────────────────────────────────────────────────────────────────────────────
//  BoardTheme
// ─────────────────────────────────────────────────────────────────────────────

sealed class BoardTheme(
    val id: String,
    val displayName: String,
    val emoji: String,
    val boardBackground: Color,
    val emptySquare: Color,
    val dlColor: Color,
    val tlColor: Color,
    val qlColor: Color,
    val dwColor: Color,
    val twColor: Color,
    val qwColor: Color,
    val vwColor: Color,
    val negColor: Color,
    val obsColor: Color,
    val anchorColor: Color,
    val premiumLabelLight: Color,
    val premiumLabelDark: Color,
    val tileFaceColor: Color,
    val tileNewColor: Color,
    val tileBorderColor: Color,
    val tileSelectedBorder: Color,
    val tileTextColor: Color,
    val tilePointsColor: Color,
    val rackBackground: Color,
    val rackBorder: Color,
    val gridLineColor: Color,
    val appBarBackground: Color,
    val appBarContent: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val surfaceBackground: Color,
    val surfaceCard: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val tileTextureType: TileTextureType,
    val boardTextureType: BoardTextureType,
    val activePlayerCard: Color,
    val inactivePlayerCard: Color
) {
    /**
     * Single source of truth for mapping a [SquareType] to its background
     * color in this theme. Replaces three duplicate implementations that
     * previously existed across BoardBuilderScreen, ThemeManager, and
     * ThemePickerScreen.
     */
    fun colorForSquare(squareType: SquareType): Color = when {
        squareType.isObstacle -> obsColor
        squareType.isNegative -> negColor
        squareType.isAnchor   -> anchorColor
        else -> when (squareType) {
            SquareType.DL -> dlColor
            SquareType.TL -> tlColor
            SquareType.QL -> qlColor
            SquareType.DW -> dwColor
            SquareType.TW -> twColor
            SquareType.QW -> qwColor
            SquareType.VW -> vwColor
            else          -> emptySquare
        }
    }

    /**
     * Pick a readable label color for a premium square given this theme's
     * background luminance.
     */
    fun labelColorForSquare(squareType: SquareType): Color {
        val bg = colorForSquare(squareType)
        val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
        return if (luminance > 0.55f) premiumLabelDark else premiumLabelLight
    }

    // ── 1. CLASSIC WOOD ──────────────────────────────────────────────────────
    object Wood : BoardTheme(
        id = "wood", displayName = "Classic Wood", emoji = "🪵",
        boardBackground   = Color(0xFF3D2B1F),
        emptySquare       = Color(0xFF8DB596),
        dlColor           = Color(0xFFA8DADC),
        tlColor           = Color(0xFF457B9D),
        qlColor           = Color(0xFF1A3A5C),
        dwColor           = Color(0xFFFFB3B3),
        twColor           = Color(0xFFE63946),
        qwColor           = Color(0xFF9B2335),
        vwColor           = Color(0xFF5C1A5C),
        negColor          = Color(0xFF2D2D2D),
        obsColor          = Color(0xFF1A1A2E),
        anchorColor       = Color(0xFFE63946),
        premiumLabelLight = Color.White,
        premiumLabelDark  = Color(0xFF8B0000),
        tileFaceColor     = Color(0xFFF2C97E),
        tileNewColor      = Color(0xFFFFE066),
        tileBorderColor   = Color(0xFF8A6A30),
        tileSelectedBorder= Color(0xFFFFFFFF),
        tileTextColor     = Color(0xFF1A1208),
        tilePointsColor   = Color(0xFF5A3A10),
        rackBackground    = Color(0xFF4A3020),
        rackBorder        = Color(0xFF6A4A30),
        gridLineColor     = Color(0x33000000),
        appBarBackground  = Color(0xFF2A1A10),
        appBarContent     = Color(0xFFF2C97E),
        primaryAccent     = Color(0xFFF2C97E),
        onPrimaryAccent   = Color(0xFF1A0800),
        surfaceBackground = Color(0xFF1C1208),
        surfaceCard       = Color(0xFF2A1A10),
        onSurface         = Color(0xFFE8D8C0),
        onSurfaceMuted    = Color(0xFF9A7A60),
        tileTextureType   = TileTextureType.WOOD_GRAIN,
        boardTextureType  = BoardTextureType.DARK_WOOD,
        activePlayerCard  = Color(0xFF4A3020),
        inactivePlayerCard= Color(0xFF2A1A10)
    )

    // ── 2. ARCTIC ICE ────────────────────────────────────────────────────────
    object Ice : BoardTheme(
        id = "ice", displayName = "Arctic Ice", emoji = "🧊",
        boardBackground   = Color(0xFF0D2137),
        emptySquare       = Color(0x8CCDE8F5),
        dlColor           = Color(0xFF7FC8E8),
        tlColor           = Color(0xFF3FA0D0),
        qlColor           = Color(0xFF1B6A9A),
        dwColor           = Color(0xFFB0E8FF),
        twColor           = Color(0xFF0077B6),
        qwColor           = Color(0xFF023E8A),
        vwColor           = Color(0xFF03045E),
        negColor          = Color(0xFF1A2A3A),
        obsColor          = Color(0xFF0A1520),
        anchorColor       = Color(0xFF00B4D8),
        premiumLabelLight = Color.White,
        premiumLabelDark  = Color(0xFF023E8A),
        tileFaceColor     = Color(0xFFE0F4FF),
        tileNewColor      = Color(0xFFB3ECFF),
        tileBorderColor   = Color(0xFF90D4F0),
        tileSelectedBorder= Color(0xFF00D4FF),
        tileTextColor     = Color(0xFF0A2540),
        tilePointsColor   = Color(0xFF1A5070),
        rackBackground    = Color(0xFF0D2137),
        rackBorder        = Color(0xFF1A4060),
        gridLineColor     = Color(0x4490D4F0),
        appBarBackground  = Color(0xFF0A1A2E),
        appBarContent     = Color(0xFF90D8FF),
        primaryAccent     = Color(0xFF00B4D8),
        onPrimaryAccent   = Color(0xFF001A2E),
        surfaceBackground = Color(0xFF081420),
        surfaceCard       = Color(0xFF0D2030),
        onSurface         = Color(0xFFD0EEFF),
        onSurfaceMuted    = Color(0xFF4A8AAA),
        tileTextureType   = TileTextureType.ICE_CRYSTAL,
        boardTextureType  = BoardTextureType.FROST,
        activePlayerCard  = Color(0xFF0D3050),
        inactivePlayerCard= Color(0xFF0A1E30)
    )

    // ── 3. VOLCANO ───────────────────────────────────────────────────────────
    object Volcano : BoardTheme(
        id = "volcano", displayName = "Volcano", emoji = "🌋",
        boardBackground   = Color(0xFF1A0800),
        emptySquare       = Color(0xFF2D1A0A),
        dlColor           = Color(0xFFFF6B35),
        tlColor           = Color(0xFFFF4500),
        qlColor           = Color(0xFFCC2000),
        dwColor           = Color(0xFFFF8C42),
        twColor           = Color(0xFFFF2200),
        qwColor           = Color(0xFFAA0000),
        vwColor           = Color(0xFF7A0000),
        negColor          = Color(0xFF0A0500),
        obsColor          = Color(0xFF151010),
        anchorColor       = Color(0xFFFF4500),
        premiumLabelLight = Color(0xFFFFE0C0),
        premiumLabelDark  = Color(0xFF800000),
        tileFaceColor     = Color(0xFF3A1A08),
        tileNewColor      = Color(0xFF7A3010),
        tileBorderColor   = Color(0xFFFF4500),
        tileSelectedBorder= Color(0xFFFFAA00),
        tileTextColor     = Color(0xFFFFCCA0),
        tilePointsColor   = Color(0xFFFF8050),
        rackBackground    = Color(0xFF200800),
        rackBorder        = Color(0xFF5A2000),
        gridLineColor     = Color(0x66FF4500),
        appBarBackground  = Color(0xFF100400),
        appBarContent     = Color(0xFFFF8042),
        primaryAccent     = Color(0xFFFF4500),
        onPrimaryAccent   = Color(0xFF200800),
        surfaceBackground = Color(0xFF0D0400),
        surfaceCard       = Color(0xFF1A0800),
        onSurface         = Color(0xFFFFD0A0),
        onSurfaceMuted    = Color(0xFF9A5030),
        tileTextureType   = TileTextureType.LAVA_ROCK,
        boardTextureType  = BoardTextureType.MAGMA,
        activePlayerCard  = Color(0xFF3A1200),
        inactivePlayerCard= Color(0xFF1A0800)
    )

    // ── 4. CIRCUIT ───────────────────────────────────────────────────────────
    object Circuit : BoardTheme(
        id = "circuit", displayName = "Circuit", emoji = "⚡",
        boardBackground   = Color(0xFF040D08),
        emptySquare       = Color(0xFF071A0E),
        dlColor           = Color(0x4000FF88),
        tlColor           = Color(0x7300FF88),
        qlColor           = Color(0xA600FF88),
        dwColor           = Color(0x4D00AAFF),
        twColor           = Color(0x8C00AAFF),
        qwColor           = Color(0xBF00AAFF),
        vwColor           = Color(0xCCAA00FF),
        negColor          = Color(0xFF220000),
        obsColor          = Color(0xFF080808),
        anchorColor       = Color(0xB300FF88),
        premiumLabelLight = Color(0xFF00FF88),
        premiumLabelDark  = Color(0xFF00AAFF),
        tileFaceColor     = Color(0xFF0A1F12),
        tileNewColor      = Color(0xFF002A15),
        tileBorderColor   = Color(0xFF00FF88),
        tileSelectedBorder= Color(0xFF00FFFF),
        tileTextColor     = Color(0xFF00FF88),
        tilePointsColor   = Color(0xFF00AA55),
        rackBackground    = Color(0xFF040D08),
        rackBorder        = Color(0x6600FF88),
        gridLineColor     = Color(0x5500FF88),
        appBarBackground  = Color(0xFF020906),
        appBarContent     = Color(0xFF00FF88),
        primaryAccent     = Color(0xFF00FF88),
        onPrimaryAccent   = Color(0xFF000800),
        surfaceBackground = Color(0xFF020906),
        surfaceCard       = Color(0xFF040D08),
        onSurface         = Color(0xFF80FFBB),
        onSurfaceMuted    = Color(0xFF208840),
        tileTextureType   = TileTextureType.CIRCUIT_TRACE,
        boardTextureType  = BoardTextureType.PCB_GRID,
        activePlayerCard  = Color(0xFF061A0D),
        inactivePlayerCard= Color(0xFF040A07)
    )

    // ── 5. MARBLE ────────────────────────────────────────────────────────────
    object Marble : BoardTheme(
        id = "marble", displayName = "Marble", emoji = "🏛️",
        boardBackground   = Color(0xFF2A2A2A),
        emptySquare       = Color(0xFFE8E0D8),
        dlColor           = Color(0xFFB8D4CC),
        tlColor           = Color(0xFF7AADA8),
        qlColor           = Color(0xFF4A8080),
        dwColor           = Color(0xFFD4B8B8),
        twColor           = Color(0xFFA06060),
        qwColor           = Color(0xFF704040),
        vwColor           = Color(0xFF501A1A),
        negColor          = Color(0xFF181818),
        obsColor          = Color(0xFF0A0A0A),
        anchorColor       = Color(0xFFA06060),
        premiumLabelLight = Color.White,
        premiumLabelDark  = Color(0xFF504040),
        tileFaceColor     = Color(0xFFF8F4EE),
        tileNewColor      = Color(0xFFFFF0D0),
        tileBorderColor   = Color(0xFFC0A880),
        tileSelectedBorder= Color(0xFF8060A0),
        tileTextColor     = Color(0xFF1A1208),
        tilePointsColor   = Color(0xFF6A5040),
        rackBackground    = Color(0xFF3A3028),
        rackBorder        = Color(0xFF6A5040),
        gridLineColor     = Color(0x44808080),
        appBarBackground  = Color(0xFF1A1A18),
        appBarContent     = Color(0xFFF0E8D8),
        primaryAccent     = Color(0xFFA06060),
        onPrimaryAccent   = Color.White,
        surfaceBackground = Color(0xFF141412),
        surfaceCard       = Color(0xFF222018),
        onSurface         = Color(0xFFF0E8D8),
        onSurfaceMuted    = Color(0xFF907860),
        tileTextureType   = TileTextureType.MARBLE_VEIN,
        boardTextureType  = BoardTextureType.STONE_TILE,
        activePlayerCard  = Color(0xFF3A2828),
        inactivePlayerCard= Color(0xFF222018)
    )

    // ── 6. NEON NOIR ─────────────────────────────────────────────────────────
    object NeonNoir : BoardTheme(
        id = "neon_noir", displayName = "Neon Noir", emoji = "🌃",
        boardBackground   = Color(0xFF08060E),
        emptySquare       = Color(0xFF100C1A),
        dlColor           = Color(0x599B00FF),
        tlColor           = Color(0x999B00FF),
        qlColor           = Color(0xD99B00FF),
        dwColor           = Color(0x59FF006E),
        twColor           = Color(0xA6FF006E),
        qwColor           = Color(0xE6FF006E),
        vwColor           = Color(0xE6FFBE00),
        negColor          = Color(0xFF120010),
        obsColor          = Color(0xFF060408),
        anchorColor       = Color(0xCCFF006E),
        premiumLabelLight = Color(0xFFFF80CF),
        premiumLabelDark  = Color(0xFFAA00FF),
        tileFaceColor     = Color(0xFF16101F),
        tileNewColor      = Color(0xFF2A1A3A),
        tileBorderColor   = Color(0xFF9B00FF),
        tileSelectedBorder= Color(0xFFFF006E),
        tileTextColor     = Color(0xFFFF80CF),
        tilePointsColor   = Color(0xFFAA60CC),
        rackBackground    = Color(0xFF100C18),
        rackBorder        = Color(0x669B00FF),
        gridLineColor     = Color(0x669B00FF),
        appBarBackground  = Color(0xFF06040C),
        appBarContent     = Color(0xFFFF80CF),
        primaryAccent     = Color(0xFFFF006E),
        onPrimaryAccent   = Color.White,
        surfaceBackground = Color(0xFF06040C),
        surfaceCard       = Color(0xFF0E0A18),
        onSurface         = Color(0xFFEED0FF),
        onSurfaceMuted    = Color(0xFF7050AA),
        tileTextureType   = TileTextureType.NEON_GLOW,
        boardTextureType  = BoardTextureType.NOIR_GRID,
        activePlayerCard  = Color(0xFF1A0A28),
        inactivePlayerCard= Color(0xFF100A1A)
    )

    // ── 7. JUNGLE ────────────────────────────────────────────────────────────
    object Jungle : BoardTheme(
        id = "jungle", displayName = "Jungle", emoji = "🌿",
        boardBackground   = Color(0xFF0D1A0A),
        emptySquare       = Color(0xFF1E3A1A),
        dlColor           = Color(0xFF6DBF67),
        tlColor           = Color(0xFF3A9A30),
        qlColor           = Color(0xFF1A7010),
        dwColor           = Color(0xFFA8D878),
        twColor           = Color(0xFF2D7A20),
        qwColor           = Color(0xFF155A10),
        vwColor           = Color(0xFF084008),
        negColor          = Color(0xFF1A0808),
        obsColor          = Color(0xFF0A1208),
        anchorColor       = Color(0xFFFFCC44),
        premiumLabelLight = Color(0xFFE0FFD0),
        premiumLabelDark  = Color(0xFF1A4010),
        tileFaceColor     = Color(0xFF5A4020),
        tileNewColor      = Color(0xFF7A6030),
        tileBorderColor   = Color(0xFF3A6820),
        tileSelectedBorder= Color(0xFFAAFF44),
        tileTextColor     = Color(0xFFF0E8C0),
        tilePointsColor   = Color(0xFFC0AA60),
        rackBackground    = Color(0xFF0D1A0A),
        rackBorder        = Color(0xFF2A4A20),
        gridLineColor     = Color(0x663A6820),
        appBarBackground  = Color(0xFF081208),
        appBarContent     = Color(0xFFAAEE66),
        primaryAccent     = Color(0xFF66CC22),
        onPrimaryAccent   = Color(0xFF0A1A05),
        surfaceBackground = Color(0xFF060E05),
        surfaceCard       = Color(0xFF0D1A0A),
        onSurface         = Color(0xFFD0F0A0),
        onSurfaceMuted    = Color(0xFF5A8A40),
        tileTextureType   = TileTextureType.BAMBOO,
        boardTextureType  = BoardTextureType.LEAF_PATTERN,
        activePlayerCard  = Color(0xFF162A10),
        inactivePlayerCard= Color(0xFF0D1A0A)
    )

    companion object {
        val all: List<BoardTheme> by lazy { listOf(Wood, Ice, Volcano, Circuit, Marble, NeonNoir, Jungle) }
        fun fromId(id: String): BoardTheme = all.find { it.id == id } ?: Wood
    }
}

@Composable
fun SynthetixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = !darkTheme)
    }

    MaterialTheme(colorScheme = colorScheme, typography = SynthetixTypography, content = content)
}
