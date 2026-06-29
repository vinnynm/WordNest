package com.enigma.wordnest.games.absurdle.ui

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
import com.enigma.wordnest.games.absurdle.ui.theme.ColorGreen
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurple
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurpleLight

@Composable
fun GameResultCard(
    isWon: Boolean,
    guessCount: Int,
    revealedWord: String?,
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
            if (isWon) {
                Text("😈 Beaten!", fontSize = 26.sp, fontWeight = FontWeight.Black,
                    color = ColorGreen)
                Text(
                    "You forced the game in $guessCount guess${if (guessCount != 1) "es" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                revealedWord?.let { word ->
                    Text(
                        "The word: ${word.uppercase()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPurpleLight
                    )
                }
                Text(
                    scoreMessage(guessCount),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                Text("😵 Gave up", fontSize = 24.sp, fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error)
                revealedWord?.let { word ->
                    Text("The game could have used:", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Text(word.uppercase(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        color = ColorPurpleLight)
                }
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
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPurple)
                ) {
                    Icon(Icons.Filled.Replay, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New game")
                }
            }
        }
    }
}

private fun scoreMessage(guesses: Int) = when {
    guesses <= 4  -> "Incredible. The game barely had a chance. 🏆"
    guesses <= 6  -> "Very impressive! You know how to corner it."
    guesses <= 9  -> "Solid. You got there eventually."
    guesses <= 12 -> "You made it, but the game made you work for it."
    else          -> "A hard-fought victory. Every guess counts!"
}
