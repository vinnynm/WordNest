package com.enigma.wordnest.games.synthetix.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.SquareType
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme
import com.enigma.wordnest.games.synthetix.ui.theme.ThemeManager
import com.enigma.wordnest.games.synthetix.ui.theme.drawBoardTexture
import com.enigma.wordnest.games.synthetix.ui.theme.drawTileTexture

// ─────────────────────────────────────────────────────────────────────────────
//  Theme Picker Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerScreen(
    themeManager: ThemeManager,
    onBack: () -> Unit
) {
    val activeTheme by themeManager.activeTheme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = activeTheme.appBarBackground,
                    titleContentColor = activeTheme.appBarContent,
                    navigationIconContentColor = activeTheme.appBarContent
                )
            )
        },
        containerColor = activeTheme.surfaceBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "SELECT BOARD THEME",
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = activeTheme.onSurfaceMuted,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))

            // ── Theme cards grid ──────────────────────────────────────────────
            BoardTheme.all.forEach { theme ->
                ThemeCard(
                    theme    = theme,
                    isActive = theme.id == activeTheme.id,
                    onClick  = { themeManager.setTheme(theme) }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Individual theme card with mini board preview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemeCard(theme: BoardTheme, isActive: Boolean, onClick: () -> Unit) {
    val elevation = if (isActive) 12.dp else 3.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isActive) 2.dp else 0.5.dp,
                color = if (isActive) theme.primaryAccent else theme.gridLineColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        color = theme.surfaceCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Mini board preview ────────────────────────────────────────
            MiniBoardPreview(
                theme    = theme,
                modifier = Modifier.size(110.dp)
            )

            // ── Theme info ────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        theme.emoji,
                        fontSize = 22.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        theme.displayName,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = theme.onSurface
                    )
                }
                Spacer(Modifier.height(6.dp))

                // Square type colour strip
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(theme.twColor, theme.dwColor, theme.tlColor, theme.dlColor, theme.anchorColor)
                        .forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(3.dp))
                            ) {
                                Canvas(Modifier.fillMaxSize()) { drawRect(color) }
                            }
                        }
                }

                Spacer(Modifier.height(6.dp))

                // Tile preview strip
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    "ABCD".forEach { letter ->
                        MiniTilePreview(letter = letter, theme = theme)
                    }
                }
            }

            // ── Active check ──────────────────────────────────────────────
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.5.dp, theme.primaryAccent, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) { drawRect(theme.primaryAccent.copy(alpha = 0.2f)) }
                    Icon(Icons.Default.Check, null, tint = theme.primaryAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mini board preview — a 5×5 sample of the premium layout
// ─────────────────────────────────────────────────────────────────────────────

// Sample 5×5 grid codes for a visually interesting preview
private val PREVIEW_GRID = arrayOf(
    arrayOf("tw",  null,  "dl",  null,  "qw"),
    arrayOf(null,  "dw",  null,  "tl",  null),
    arrayOf("tl",  null,  "anchor", null, "tl"),
    arrayOf(null,  "tl",  null,  "dw",  null),
    arrayOf("qw",  null,  "dl",  null,  "tw")
)

// Sample placed tiles to show in preview
private val PREVIEW_TILES = mapOf(
    (1 to 2) to 'W',
    (2 to 3) to 'O',
    (3 to 2) to 'R',
    (3 to 3) to 'D',
)

@Composable
fun MiniBoardPreview(theme: BoardTheme, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, theme.primaryAccent.copy(0.4f), RoundedCornerShape(8.dp))
    ) {
        val gridSize  = 5
        val cellSize  = size.width / gridSize
        val cellSizeH = size.height / gridSize

        // Board background
        drawRect(theme.boardBackground)
        drawBoardTexture(theme.boardTextureType)

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val left = c * cellSize
                val top  = r * cellSizeH
                val code = PREVIEW_GRID[r][c]
                val sq   = SquareType.fromCode(code)
                val placedLetter = PREVIEW_TILES[r to c]

                val bg = if (placedLetter != null) theme.tileFaceColor
                else when {
                    sq.isAnchor -> theme.anchorColor
                    else -> when (sq) {
                        SquareType.TW    -> theme.twColor
                        SquareType.DW    -> theme.dwColor
                        SquareType.TL    -> theme.tlColor
                        SquareType.DL    -> theme.dlColor
                        SquareType.QW    -> theme.qwColor
                        else             -> theme.emptySquare
                    }
                }

                val inset = 1.5f
                drawRoundRect(
                    color        = bg,
                    topLeft      = Offset(left + inset, top + inset),
                    size         = Size(cellSize - inset*2, cellSizeH - inset*2),
                    cornerRadius = CornerRadius(3f)
                )

                // Texture on tiles
                if (placedLetter != null) {
                    // Mini tile texture
                    drawTileTexture(theme.tileTextureType, cellSize - inset*2, isNew = false)
                }

                // Grid lines
                drawRoundRect(
                    color        = theme.gridLineColor,
                    topLeft      = Offset(left + inset, top + inset),
                    size         = Size(cellSize - inset*2, cellSizeH - inset*2),
                    cornerRadius = CornerRadius(3f),
                    style        = Stroke(width = 0.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mini tile preview for the colour strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniTilePreview(letter: Char, theme: BoardTheme) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(0.5.dp, theme.tileBorderColor, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(theme.tileFaceColor)
            drawTileTexture(theme.tileTextureType, size.width, isNew = false)
        }
        Text(
            letter.toString(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = theme.tileTextColor,
            textAlign  = TextAlign.Center
        )
    }
}

// Need SquareType visible here
private fun SquareType.Companion.fromCode(code: String?): SquareType {
    return SquareType.paletteOrder.find { it.code == (code ?: "") } ?: SquareType.EMPTY
}
