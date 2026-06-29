package com.enigma.wordnest.games.betweenle.ui

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
import com.enigma.wordnest.games.betweenle.model.Stats
import com.enigma.wordnest.games.betweenle.ui.theme.PrimaryGreen
import kotlin.math.max

@Composable
fun StatsDialog(stats: Stats, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Statistics", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatNumber(stats.gamesPlayed.toString(), "Played")
                    StatNumber("${(stats.winRate * 100).toInt()}%", "Win %")
                    StatNumber(stats.currentStreak.toString(), "Streak")
                    StatNumber(stats.maxStreak.toString(), "Max Streak")
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    "Guess Distribution",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                val maxVal = max(1, stats.guessDistribution.maxOrNull() ?: 1)
                stats.guessDistribution.forEachIndexed { index, count ->
                    DistributionBar(
                        guessNumber = index + 1,
                        count = count,
                        fraction = count.toFloat() / maxVal
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatNumber(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DistributionBar(guessNumber: Int, count: Int, fraction: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$guessNumber",
            modifier = Modifier.width(20.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val barWidth = maxWidth * fraction.coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryGreen),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (count > 0) {
                    Text(
                        text = " $count ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
