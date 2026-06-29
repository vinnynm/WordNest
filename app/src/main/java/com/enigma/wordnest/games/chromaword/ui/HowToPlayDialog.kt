package com.enigma.wordnest.games.chromaword.ui

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
import com.enigma.wordnest.games.chromaword.ui.theme.ColorGreen
import com.enigma.wordnest.games.chromaword.ui.theme.ColorLightBlue
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRedAbsent
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRedExtra
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRoyalBlue
import com.enigma.wordnest.games.chromaword.ui.theme.ColorYellow

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    "Guess the hidden word. Each guess must be a valid word of the same length.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text("Tile colours", fontWeight = FontWeight.Bold)

                ColorRule(ColorGreen,     "🟩 GREEN",      "Right letter, right position")
                ColorRule(ColorYellow,    "🟨 YELLOW",     "Right letter, wrong position (appears once in word)")
                ColorRule(ColorLightBlue, "🔵 LIGHT BLUE", "Letter appears 2+ times; this position is wrong but another position IS correct")
                ColorRule(ColorRoyalBlue, "🟦 ROYAL BLUE", "Letter appears 2+ times; ALL your positions for this letter are wrong")
                ColorRule(ColorRedExtra,  "🟥 RED (surplus)", "All correct positions already green, but you used an extra instance of the letter")
                ColorRule(ColorRedAbsent, "🟥 RED (absent)",  "Letter is not in the word at all")

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text("Difficulty", fontWeight = FontWeight.Bold)
                Text(
                    "🍼 Baby — unlimited guesses, full history\n" +
                    "😊 Easy — 8 guesses, all rows shown\n" +
                    "😐 Normal — 6 guesses, last 6 rows shown\n" +
                    "💀 Hard — 4 guesses, last 4 rows shown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text(
                    "The word length changes every round (4–8 letters). Good luck!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ColorRule(swatch: Color, label: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(swatch)
        )
        Column {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}
