package com.enigma.wordnest.games.absurdle.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("How To Play", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }

                Text(
                    "Absurdle is the adversarial version of Wordle.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Section("The twist") {
                    "There is no secret word at the start. Instead, the game maintains a pool " +
                    "of ALL possible words and actively tries to avoid giving you useful information."
                }

                Section("How it works") {
                    "After each guess, the game groups every remaining candidate word by the " +
                    "colour pattern it would produce against your guess. It then picks the " +
                    "pattern shared by the MOST words — keeping as many possibilities alive as possible. " +
                    "Only when the pool collapses to a single word is the game committed to it."
                }

                Section("Colours") {
                    "🟩 Green — right letter, right position\n" +
                    "🟨 Yellow — right letter, wrong position\n" +
                    "⬛ Gray — letter not in word"
                }

                Section("Candidate count") {
                    "The purple badge next to each row shows how many words remained after that guess. " +
                    "Watch it shrink — the closer it gets to 1, the closer you are to winning."
                }

                Section("Hard mode") {
                    "The engine avoids giving you an all-green response unless it has no other choice, " +
                    "making the game even harder."
                }

                Section("Goal") {
                    "Force the game into a corner until only one word survives, then guess it. " +
                    "Your score is the number of guesses — lower is better!"
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Text(
                    "Good luck. You'll need it. 😈",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, body: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            body(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}
