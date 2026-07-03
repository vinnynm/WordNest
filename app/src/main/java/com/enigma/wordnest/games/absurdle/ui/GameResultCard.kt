package com.enigma.wordnest.games.absurdle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.absurdle.model.GameOverReason
import com.enigma.wordnest.games.absurdle.ui.theme.ColorGreen
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurple
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurpleLight
import com.enigma.wordnest.games.common.ui.GameOverTemplate

@Composable
fun GameResultCard(
    reason: GameOverReason?,
    guessCount: Int,
    revealedWord: String?,
    onNewGame: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (reason) {
        GameOverReason.WON -> "😈 Beaten!"
        GameOverReason.FORCED_REVEAL -> "🤖 Trapped"
        GameOverReason.GAVE_UP -> "😵 Gave up"
        null -> ""
    }
    
    val titleColor = when (reason) {
        GameOverReason.WON -> ColorGreen
        GameOverReason.FORCED_REVEAL -> ColorPurpleLight
        GameOverReason.GAVE_UP -> MaterialTheme.colorScheme.error
        null -> Color.Unspecified
    }

    GameOverTemplate(
        title = title,
        titleColor = titleColor,
        onNewGame = onNewGame,
        onShare = onShare,
        accentColor = ColorPurple,
        modifier = modifier
    ) {
        when (reason) {
            GameOverReason.WON -> {
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
            }
            GameOverReason.FORCED_REVEAL -> {
                Text(
                    "The engine only had one word left, and you cornered it!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                revealedWord?.let { word ->
                    Text(word.uppercase(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        color = ColorPurpleLight)
                }
            }
            GameOverReason.GAVE_UP -> {
                revealedWord?.let { word ->
                    Text("The game could have used:", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Text(word.uppercase(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        color = ColorPurpleLight)
                }
            }
            null -> {}
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
