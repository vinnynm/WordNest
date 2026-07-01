package com.enigma.wordnest.games.lexicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.lexicon.model.PlacedTile
import com.enigma.wordnest.games.lexicon.model.Tile


// ── Premium square colours (matching real Scrabble) ──────────────────────────
private val ColorTW      = Color(0xFFE63946) // Triple Word  – bold red
private val ColorDW      = Color(0xFFFFB3B3) // Double Word  – soft pink/rose
private val ColorTL      = Color(0xFF457B9D) // Triple Letter – steel blue
private val ColorDL      = Color(0xFFA8DADC) // Double Letter – light teal
private val ColorCenter  = Color(0xFFE63946) // Centre star   – same as TW
private val ColorEmpty   = Color(0xFF8DB596) // Plain square  – muted green-grey (board feel)

// ── Tile colours ──────────────────────────────────────────────────────────────
private val ColorTileWood      = Color(0xFFF2C97E) // Settled tile – warm cream/wood
private val ColorTileThisTurn  = Color(0xFFFFE066) // Newly placed  – bright yellow (stands out)
private val ColorTileBorder    = Color(0xFF8A6A30)
private val ColorTileText      = Color(0xFF1A1208)


@Composable
fun Board(
    board: Array<Array<Tile?>>,
    placedThisTurn: List<PlacedTile>,
    selectedTile: Int?,
    onCellClick: (Int, Int) -> Unit,
    cellSize: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        for (row in 0 until 15) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 0 until 15) {
                    val placedTile = placedThisTurn.find { it.row == row && it.col == col }
                    val displayTile = board[row][col] ?: placedTile?.let { 
                        Tile(it.letter, it.points, it.isBlank) 
                    }
                    BoardCell(
                        row = row,
                        col = col,
                        tile = displayTile,
                        isPlacedThisTurn = placedTile != null,
                        onCellClick = onCellClick,
                        size = cellSize
                    )
                }
            }
        }
    }
}

@Composable
fun BoardCell(
    row: Int,
    col: Int,
    tile: Tile?,
    isPlacedThisTurn: Boolean,
    onCellClick: (Int, Int) -> Unit,
    size: Dp
) {
    val isCenter = row == 7 && col == 7

    val key = "$row,$col"

    // Resolve premium type for this cell
    val premiumType: String? = when (key) {
        "0,0","0,7","0,14","7,0","7,14","14,0","14,7","14,14" -> "TW"
        "1,1","2,2","3,3","4,4","10,10","11,11","12,12","13,13",
        "1,13","2,12","3,11","4,10","10,4","11,3","12,2","13,1" -> "DW"
        "1,5","1,9","5,1","5,5","5,9","5,13",
        "9,1","9,5","9,9","9,13","13,5","13,9" -> "TL"
        "0,3","0,11","2,6","2,8","3,0","3,7","3,14",
        "6,2","6,6","6,8","6,12","7,3","7,11",
        "8,2","8,6","8,8","8,12","11,0","11,7","11,14",
        "12,6","12,8","14,3","14,11" -> "DL"
        else -> null
    }

    // Background: tiles override square colour; otherwise use premium/empty colours
    val bgColor = when {
        tile != null && isPlacedThisTurn -> ColorTileThisTurn
        tile != null                     -> ColorTileWood
        isCenter                         -> ColorCenter
        else -> when (premiumType) {
            "TW" -> ColorTW
            "DW" -> ColorDW
            "TL" -> ColorTL
            "DL" -> ColorDL
            else -> ColorEmpty
        }
    }

    // Premium label colours so text is readable on each background
    val premiumTextColor = when (premiumType) {
        "TW" -> Color.White
        "DW" -> Color(0xFF8B0000)
        "TL" -> Color.White
        "DL" -> Color(0xFF003049)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = if (isPlacedThisTurn) 1.5.dp else 0.5.dp,
                color = if (isPlacedThisTurn) Color(0xFFB8860B)
                        else Color.Black.copy(alpha = 0.18f),
                shape = RoundedCornerShape(3.dp)
            )
            .background(bgColor, shape = RoundedCornerShape(3.dp))
            .then(
                // Subtle lift shadow on freshly placed tiles
                if (isPlacedThisTurn)
                    Modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(3.dp))
                else Modifier
            )
            .clickable(enabled = tile == null || isPlacedThisTurn) { onCellClick(row, col) },
        contentAlignment = Alignment.Center
    ) {
        if (tile != null) {
            // ── Tile on the board ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tile.letter.toString(),
                    fontSize = (size.value * 0.42).sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTileText
                )
                if (!tile.isBlank) {
                    Text(
                        text = tile.points.toString(),
                        fontSize = (size.value * 0.16).sp,
                        color = ColorTileText.copy(alpha = 0.65f)
                    )
                }
            }
        }
        else if (isCenter) {
            // ── Centre star ────────────────────────────────────────────────
            Text("★", fontSize = (size.value * 0.4).sp, color = Color.White)
        } else if (premiumType != null) {
            // ── Premium label ──────────────────────────────────────────────
            Text(
                text = premiumType,
                fontSize = (size.value * 0.22).sp,
                fontWeight = FontWeight.Bold,
                color = premiumTextColor
            )
        }
        // Plain squares show nothing
    }
}
