package com.enigma.wordnest.games.wordladder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enigma.wordnest.games.wordladder.model.WORD_LENGTH_OPTIONS
import com.enigma.wordnest.games.wordladder.ui.theme.ColorSubtle
import com.enigma.wordnest.games.wordladder.ui.theme.ColorTealLight

@Composable
fun WordLadderStartScreen(onStartGame: (Int) -> Unit, isGenerating: Boolean) {
    var wordLength by remember { mutableIntStateOf(4) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("WORD LADDER", fontSize = 40.sp, fontWeight = FontWeight.Black,
            color = ColorTealLight, letterSpacing = 2.sp)
        Text("Climb from one word to another, one letter at a time", fontSize = 15.sp,
            color = ColorSubtle, modifier = Modifier.padding(bottom = 48.dp))

        Text("WORD LENGTH", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 40.dp)) {
            WORD_LENGTH_OPTIONS.forEach { length ->
                val selected = wordLength == length
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ColorTealLight else Color(0xFF232B2E))
                        .clickable { wordLength = length },
                    contentAlignment = Alignment.Center
                ) {
                    Text(length.toString(), color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Button(
            onClick = { onStartGame(wordLength) },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorTealLight),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("START GAME", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun WordLadderHowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("How To Play", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                Text(
                    "You're given a start word and a target word of the same length. " +
                    "Change exactly one letter at a time, and every word along the way " +
                    "must be a real word — reach the target to win.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Text(
                    "Example: COLD \u2192 CORD \u2192 CARD \u2192 WARD \u2192 WARM",
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                )
                Text(
                    "The \"par\" shown is the shortest possible chain — try to match or beat it! " +
                    "Stuck? Use a hint to reveal the next correct step.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
