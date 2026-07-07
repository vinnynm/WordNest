package com.enigma.wordnest.games.ladderclaim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.ladderclaim.model.LadderTile
import com.enigma.wordnest.games.ladderclaim.model.PlacedLadderTile
import com.enigma.wordnest.games.ladderclaim.ui.theme.playerColor

@Composable
fun LadderBoard(
    board: Array<Array<LadderTile?>>,
    placedThisTurn: List<PlacedLadderTile>,
    onCellClick: (Int, Int) -> Unit,
    cellSize: Dp
) {
    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        for (row in 0 until 15) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (col in 0 until 15) {
                    val placed = placedThisTurn.find { it.row == row && it.col == col }
                    val settled = board[row][col]
                    LadderCell(
                        letter = placed?.letter ?: settled?.letter,
                        ownerId = if (placed != null) null else settled?.ownerId,
                        isPlacedThisTurn = placed != null,
                        isCenter = row == 7 && col == 7,
                        onClick = { onCellClick(row, col) },
                        size = cellSize
                    )
                }
            }
        }
    }
}

@Composable
private fun LadderCell(
    letter: Char?, ownerId: Int?, isPlacedThisTurn: Boolean, isCenter: Boolean,
    onClick: () -> Unit, size: Dp
) {
    val bg = when {
        isPlacedThisTurn -> Color(0xFFFFE066)
        letter != null   -> playerColor(ownerId).copy(alpha = if (ownerId == null) 0.5f else 0.85f)
        isCenter         -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else             -> MaterialTheme.colorScheme.surface
    }
    Box(
        modifier = Modifier
            .size(size)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            .background(bg, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                letter.toString(), fontSize = (size.value * 0.42).sp, fontWeight = FontWeight.Bold,
                color = if (isPlacedThisTurn) Color(0xFF1A1208) else Color.White
            )
        } else if (isCenter) {
            Text("★", fontSize = (size.value * 0.4).sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}