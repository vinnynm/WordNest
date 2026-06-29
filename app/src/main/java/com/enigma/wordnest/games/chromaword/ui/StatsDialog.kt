package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enigma.wordnest.games.chromaword.model.Stats
import com.enigma.wordnest.games.chromaword.ui.theme.ColorGreen
import kotlin.math.max

@Composable
fun StatsDialog(stats: Stats, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Statistics", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatNum(stats.gamesPlayed.toString(), "Played")
                    StatNum("${(stats.winRate * 100).toInt()}%", "Win %")
                    StatNum(stats.currentStreak.toString(), "Streak")
                    StatNum(stats.maxStreak.toString(), "Best")
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text("Guess Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                val maxVal = max(1, stats.guessDistribution.maxOrNull() ?: 1)
                stats.guessDistribution.forEachIndexed { i, count ->
                    if (i < 10) {
                        DistBar(label = "${i + 1}", count = count, fraction = count.toFloat() / maxVal)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatNum(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(label, fontSize = 11.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun DistBar(label: String, count: Int, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(20.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f)) {
            val w = maxWidth * fraction.coerceIn(0.05f, 1f)
            Box(
                Modifier.width(w).height(22.dp).clip(RoundedCornerShape(4.dp)).background(ColorGreen),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (count > 0) Text(" $count ", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = Color.White)
            }
        }
    }
}
