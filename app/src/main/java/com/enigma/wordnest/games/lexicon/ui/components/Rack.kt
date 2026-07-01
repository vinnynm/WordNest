package com.enigma.wordnest.games.lexicon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.lexicon.getLetterPoints


@Composable
fun Rack(
    rack: List<Char>,
    selectedTile: Int?,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        var scrollState = rememberScrollableState {
            0f
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scrollable(
                    state = scrollState,
                    orientation = Orientation.Horizontal
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            rack.forEachIndexed { index, letter ->
                TilePiece(
                    letter = letter,
                    points = getLetterPoints(letter),
                    isSelected = selectedTile == index,
                    onClick = { onTileClick(index) }
                )
            }
        }
    }
}

@Composable
fun TilePiece(
    letter: Char,
    points: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = Color(0xFFC8A96E),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0xFF8A6A30),
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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1208)
            )
            Text(
                if (letter == '?') "" else points.toString(),
                fontSize = 12.sp,
                color = Color(0xFF1A1208).copy(alpha = 0.7f)
            )
        }
    }
}
