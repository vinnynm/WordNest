package com.enigma.wordnest.games.ladderclaim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LadderRack(
    rack: List<Char>,
    selectedTile: Int?,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rack.forEachIndexed { index, letter ->
                val selected = selectedTile == index
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFCF9A3B), RoundedCornerShape(6.dp))
                        .border(
                            if (selected) 3.dp else 1.dp,
                            if (selected) Color.White else Color(0xFF8A6A30),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onTileClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter.uppercaseChar().toString(), fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF1A1208)
                    )
                }
            }
        }
    }
}