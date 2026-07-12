package com.enigma.wordnest.games.synthetix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.model.PlacedTile
import com.enigma.wordnest.games.synthetix.model.SquareType
import com.enigma.wordnest.games.synthetix.model.Tile
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme
import com.enigma.wordnest.games.synthetix.ui.theme.drawBoardTexture
import com.enigma.wordnest.games.synthetix.ui.theme.drawTileTexture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

// ─────────────────────────────────────────────────────────────────────────────
//  Board
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Board(
    board: List<List<Tile?>>,
    boardConfig: BoardConfig,
    placedThisTurn: List<PlacedTile>,
    selectedTile: Int?,
    onCellClick: (Int, Int) -> Unit,
    cellSize: Dp,
    theme: BoardTheme
) {
    val size = boardConfig.size

    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
    ) {
        for (row in 0 until size) {
            Row {
                for (col in 0 until size) {
                    val placedTile  = placedThisTurn.find { it.row == row && it.col == col }
                    val displayTile = board[row][col] ?: placedTile?.let {
                        Tile(it.letter, it.points, it.isBlank)
                    }
                    BoardCell(
                        row              = row,
                        col              = col,
                        tile             = displayTile,
                        squareType       = boardConfig.squareAt(row, col),
                        isPlacedThisTurn = placedTile != null,
                        onCellClick      = onCellClick,
                        size             = cellSize,
                        theme            = theme
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Board Cell
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardCell(
    row: Int,
    col: Int,
    tile: Tile?,
    squareType: SquareType,
    isPlacedThisTurn: Boolean,
    onCellClick: (Int, Int) -> Unit,
    size: Dp,
    theme: BoardTheme
) {
    val px = size.value

    // Choose base background colour from theme
    val baseBg = when {
        tile != null && isPlacedThisTurn -> theme.tileNewColor
        tile != null                     -> theme.tileFaceColor
        squareType.isObstacle            -> theme.obsColor
        squareType.isNegative            -> theme.negColor
        squareType.isAnchor              -> theme.anchorColor
        else -> when (squareType) {
            SquareType.DL    -> theme.dlColor
            SquareType.TL    -> theme.tlColor
            SquareType.QL    -> theme.qlColor
            SquareType.DW    -> theme.dwColor
            SquareType.TW    -> theme.twColor
            SquareType.QW    -> theme.qwColor
            SquareType.VW    -> theme.vwColor
            else             -> theme.emptySquare
        }
    }

    val borderColor = when {
        isPlacedThisTurn -> theme.tileBorderColor
        squareType.isObstacle -> theme.obsColor.copy(alpha = 0.3f)
        else             -> theme.gridLineColor
    }
    val borderWidth = if (isPlacedThisTurn) 1.5.dp else 0.5.dp

    Box(
        modifier = Modifier
            .size(size)
            .border(borderWidth, borderColor, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .then(
                if (isPlacedThisTurn)
                    Modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(3.dp))
                else Modifier
            )
            .clickable(enabled = !squareType.isObstacle) { onCellClick(row, col) },
        contentAlignment = Alignment.Center
    ) {
        // ── Canvas: base fill + procedural texture ────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(baseBg)

            if (squareType.isObstacle && tile == null) {
                // Obstacle: draw a clear blocked pattern — hatching over dark base
                drawObstaclePattern(this.size.width, this.size.height)
            } else if (tile != null) {
                drawTileTexture(theme.tileTextureType, this.size.width, isPlacedThisTurn)
            } else if (squareType == SquareType.EMPTY) {
                clipRect { drawBoardTexture(theme.boardTextureType) }
            }
        }

        // ── Text overlay ──────────────────────────────────────────────────
        when {
            squareType.isObstacle && tile == null -> {
                // Obstacle shows an × symbol for clarity
                Text(
                    text = "×",
                    fontSize = (px * 0.38).sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.obsColor.copy(alpha = 0.0f) // invisible — pattern drawn in canvas
                )
            }
            tile != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text       = tile.letter.toString(),
                        fontSize   = (px * 0.42).sp,
                        fontWeight = FontWeight.Bold,
                        color      = theme.tileTextColor
                    )
                    if (!tile.isBlank) {
                        Text(
                            text     = tile.points.toString(),
                            fontSize = (px * 0.16).sp,
                            color    = theme.tilePointsColor
                        )
                    }
                }
            }
            squareType.isAnchor -> {
                Text("★", fontSize = (px * 0.42).sp, color = theme.premiumLabelLight,
                    fontWeight = FontWeight.Bold)
            }
            squareType != SquareType.EMPTY && !squareType.isObstacle && !squareType.isNegative -> {
                // Choose readable label color based on background luminance
                val labelColor = resolveLabelColor(squareType, theme)
                Text(
                    text       = squareType.code.uppercase(),
                    fontSize   = (px * 0.20).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = labelColor
                )
            }
            squareType.isNegative -> {
                Text(
                    text = "−",
                    fontSize = (px * 0.38).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6060)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Obstacle hatch pattern drawn in Canvas
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawObstaclePattern(w: Float, h: Float) {
    val lineColor = Color(0x40FFFFFF)
    val spacing = w * 0.28f
    var d = -h
    while (d < w + h) {
        drawLine(lineColor, Offset(d, 0f), Offset(d + h, h), strokeWidth = w * 0.08f)
        d += spacing
    }
    // Cross-hatch in opposite direction
    d = w + h
    while (d > -h) {
        drawLine(Color(0x28FFFFFF), Offset(d, 0f), Offset(d - h, h), strokeWidth = w * 0.05f)
        d -= spacing * 1.4f
    }
    // Dark overlay to keep it clearly blocked
    drawRect(Color(0x22000000), size = Size(w, h))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Smart label color — picks contrasting white or dark based on square
// ─────────────────────────────────────────────────────────────────────────────

private fun resolveLabelColor(squareType: SquareType, theme: BoardTheme): Color {
    // Get the background color for this square type
    val bg = when (squareType) {
        SquareType.DL -> theme.dlColor
        SquareType.TL -> theme.tlColor
        SquareType.QL -> theme.qlColor
        SquareType.DW -> theme.dwColor
        SquareType.TW -> theme.twColor
        SquareType.QW -> theme.qwColor
        SquareType.VW -> theme.vwColor
        else          -> theme.emptySquare
    }
    // Approximate luminance
    val r = bg.red; val g = bg.green; val b = bg.blue
    val luminance = 0.299f * r + 0.587f * g + 0.114f * b

    return if (luminance > 0.55f) {
        // Light background — use dark label
        theme.premiumLabelDark
    } else {
        // Dark background — use light label
        theme.premiumLabelLight
    }
}
