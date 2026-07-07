package com.enigma.wordnest.games.ladderclaim.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun LadderClaimHowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("How To Play", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                Text(
                    "Play any valid dictionary word that connects to the board — just like Scrabble. " +
                            "There's no letter scoring; instead, you're claiming territory.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Before you submit, tap an existing board word to set it as your target. " +
                            "The closer your new word is to a one-letter ladder move away from that target, " +
                            "the more of your newly placed tiles turn your colour:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("• FULL — same letters except one (or one added/removed): every new tile claimed", fontSize = 13.sp)
                Text("• PARTIAL — some letters line up: only those tiles claimed", fontSize = 13.sp)
                Text("• NEUTRAL — no target, or nothing lines up: tiles stay unclaimed", fontSize = 13.sp)
                Text(
                    "If your target word was still unclaimed (grey), even a single matching letter " +
                            "flips its ENTIRE word to your colour — watch for juicy neutral words to strike!",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Whoever controls the most tiles when the bag runs out (or the turn limit is hit) wins.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}