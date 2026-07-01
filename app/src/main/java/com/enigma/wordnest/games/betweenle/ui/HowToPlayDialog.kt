package com.enigma.wordnest.games.betweenle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enigma.wordnest.games.betweenle.ui.theme.*

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("How To Play", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }

                Text(
                    "Guess a word that falls alphabetically between the two boundary words.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                RuleItem("10 guesses", "You have 10 attempts to find the secret target word.")
                RuleItem("Narrow the range", "Each guess between the boundaries will narrow the range. The goal is to find the exact target word!")
                RuleItem("Feedback colours", "Tiles show you where your guess landed:")

                // Colour legend
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendRow(PrimaryGreen, "✅ CORRECT — you found the target word!")
                    LegendRow(PrimaryRed, "⬇️ TOO LOW — your word comes before the target")
                    LegendRow(PrimaryYellow, "⬆️ TOO HIGH — your word comes after the target")
                    LegendRow(Color(0xFF5C5C5C), "🚧 BOUNDARY — you guessed one of the boundary words itself")
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Text("Heat hints", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendRow(BurningOrange, "🔥🔥 BURNING — just 1 word away!")
                    LegendRow(HotOrange,    "🔥 HOT — within 10 words")
                    LegendRow(WarmYellow,   "♨️ WARM — within 50 words")
                    LegendRow(FarGray,      "❄️ FAR — more than 50 words away")
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Text(
                    "A new game is available whenever you want one — tap New Game after each round.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun RuleItem(title: String, body: String) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
    }
}
