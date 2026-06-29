package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enigma.wordnest.games.chromaword.model.Difficulty
import com.enigma.wordnest.games.chromaword.ui.theme.ColorGreen

@Composable
fun DifficultyDialog(
    current: Difficulty,
    onSelect: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Difficulty", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }

                Text(
                    "Changing difficulty starts a new game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Difficulty.entries.forEach { diff ->
                    DifficultyOption(
                        difficulty = diff,
                        selected   = diff == current,
                        onClick    = { onSelect(diff); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyOption(
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ColorGreen else MaterialTheme.colorScheme.outline
    val bg = if (selected) ColorGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(difficulty.emoji, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(difficulty.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val guessText = difficulty.maxGuesses?.let { "$it guesses" } ?: "Unlimited guesses"
            val histText  = when {
                difficulty.historyVisible == null  -> "Full history"
                difficulty.historyVisible == 0     -> "No history"
                else -> "Last ${difficulty.historyVisible} rows shown"
            }
            Text(
                "$guessText · $histText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (selected) {
            Text("✓", color = ColorGreen, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}
