package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.chromaword.model.GameState
import com.enigma.wordnest.games.chromaword.ui.theme.ColorGreen
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRedAbsent

@Composable
fun GameOverBanner(
    state: GameState,
    onNewGame: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.isWon) {
                Text("🎉", fontSize = 36.sp)
                Text(
                    wonMessage(state.guessesUsed),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorGreen
                )
                Text(
                    "Solved in ${state.guessesUsed} guess${if (state.guessesUsed != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Text("😔", fontSize = 36.sp)
                Text("Out of guesses", fontSize = 20.sp, fontWeight = FontWeight.Black,
                    color = ColorRedAbsent)
                Text(
                    "The word was  ${state.targetWord.uppercase()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
                Button(
                    onClick = onNewGame,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorGreen)
                ) {
                    Icon(Icons.Filled.Replay, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New game")
                }
            }
        }
    }
}

private fun wonMessage(guesses: Int) = when (guesses) {
    1    -> "Genius! 🧠"
    2    -> "Magnificent!"
    3    -> "Impressive!"
    4    -> "Splendid!"
    5    -> "Great!"
    6    -> "Phew!"
    else -> "Got it!"
}
