package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.chromaword.model.GameState
import com.enigma.wordnest.games.chromaword.ui.theme.ColorGreen
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRedAbsent
import com.enigma.wordnest.games.common.ui.GameOverTemplate

@Composable
fun GameOverBanner(
    state: GameState,
    onNewGame: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameOverTemplate(
        title = if (state.isWon) wonMessage(state.guessesUsed) else "Out of guesses",
        titleColor = if (state.isWon) ColorGreen else ColorRedAbsent,
        subtitle = if (state.isWon) "Solved in ${state.guessesUsed} guess${if (state.guessesUsed != 1) "es" else ""}" else "The word was  ${state.targetWord.uppercase()}",
        onNewGame = onNewGame,
        onShare = onShare,
        accentColor = ColorGreen,
        modifier = modifier
    ) {
        if (state.isWon) {
            Text("🎉", fontSize = 36.sp)
        } else {
            Text("😔", fontSize = 36.sp)
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
