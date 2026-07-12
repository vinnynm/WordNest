package com.enigma.wordnest.games.synthetix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.TileSetConfig

// ─────────────────────────────────────────────────────────────────────────────
//  Rack
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Rack(
    rack: List<Char>,
    selectedTile: Int?,
    activeTileSet: TileSetConfig,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scrollable(rememberScrollableState { 0f }, Orientation.Horizontal)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            rack.forEachIndexed { index, letter ->
                TilePiece(
                    letter   = letter,
                    points   = activeTileSet.pointsFor(letter),
                    isSelected = selectedTile == index,
                    onClick  = { onTileClick(index) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tile piece
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TilePiece(
    letter: Char,
    points: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .shadow(if (isSelected) 8.dp else 3.dp, RoundedCornerShape(6.dp))
            .background(Color(0xFFF2C97E), RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF00C9B1) else Color(0xFF8A6A30),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (letter == '?') "★" else letter.toString(),
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1A1208)
            )
            Text(
                if (letter == '?') "" else points.toString(),
                fontSize = 10.sp,
                color    = Color(0xFF1A1208).copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Player score card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerScoreCard(
    name: String,
    score: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                             else          MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (isActive) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                name,
                fontSize     = 11.sp,
                letterSpacing = 0.5.sp,
                color        = if (isActive) MaterialTheme.colorScheme.primary
                               else          MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                score.toString(),
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
