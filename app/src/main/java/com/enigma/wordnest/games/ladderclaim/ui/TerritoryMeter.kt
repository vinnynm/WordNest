package com.enigma.wordnest.games.ladderclaim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.ladderclaim.model.LadderPlayer
import com.enigma.wordnest.games.ladderclaim.ui.theme.playerColor

@Composable
fun TerritoryMeter(
    players: List<LadderPlayer>,
    tally: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    val claimed = tally.values.sum()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            if (claimed > 0) {
                players.indices.forEach { pid ->
                    val count = tally[pid] ?: 0
                    if (count > 0) {
                        Box(modifier = Modifier.weight(count.toFloat()).fillMaxHeight().background(playerColor(pid)))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            players.forEachIndexed { pid, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(playerColor(pid)))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${p.name}: ${tally[pid] ?: 0}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}