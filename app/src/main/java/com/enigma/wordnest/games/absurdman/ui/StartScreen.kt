package com.enigma.wordnest.games.absurdman.ui

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
import com.enigma.wordnest.games.absurdman.model.AbsurdmanMode
import com.enigma.wordnest.games.absurdman.model.WORD_LENGTH_OPTIONS
import com.enigma.wordnest.games.absurdman.ui.theme.ColorRustLight
import com.enigma.wordnest.games.absurdman.ui.theme.ColorSubtle

@Composable
fun AbsurdmanStartScreen(onStartGame: (Int, AbsurdmanMode) -> Unit) {
    var wordLength by remember { mutableIntStateOf(6) }
    var mode by remember { mutableStateOf(AbsurdmanMode.CLASSIC) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ABSURDMAN", fontSize = 44.sp, fontWeight = FontWeight.Black,
            color = ColorRustLight, letterSpacing = 3.sp)
        Text(
            if (mode == AbsurdmanMode.HELL) "The hangman that never gives you a break"
            else "Classic hangman, with a few honest clues",
            fontSize = 15.sp,
            color = ColorSubtle,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            listOf(AbsurdmanMode.CLASSIC to "Classic", AbsurdmanMode.HELL to "Hell Mode 😈").forEach { (m, label) ->
                val selected = mode == m
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ColorRustLight else Color(0xFF2A2A2C))
                        .clickable { mode = m }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(label, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("WORD LENGTH", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 40.dp)) {
            WORD_LENGTH_OPTIONS.forEach { length ->
                val selected = wordLength == length
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ColorRustLight else Color(0xFF2A2A2C))
                        .clickable { wordLength = length },
                    contentAlignment = Alignment.Center
                ) {
                    Text(length.toString(), color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Button(
            onClick = { onStartGame(wordLength, mode) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorRustLight),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("START GAME", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
fun AbsurdmanHowToPlayDialog(onDismiss: () -> Unit) {
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
                    "Classic mode is standard hangman: a real word is picked at the start and " +
                            "never changes. Hell Mode is the adversarial version — there is no secret " +
                            "word at all. The game keeps every possible word alive and, on each letter " +
                            "you guess, picks whichever answer (present or absent) keeps the most words " +
                            "in play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Text(
                    "Both modes give you a few honest clues up front (syllable count, a letter " +
                            "it contains, etc.) — whatever word you're up against is guaranteed to match them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "You have 6 wrong guesses before the gallows claims you. Guess all the " +
                            "letters in the word before then to win.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}