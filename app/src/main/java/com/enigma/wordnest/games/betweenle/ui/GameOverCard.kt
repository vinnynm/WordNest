package com.enigma.wordnest.games.betweenle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.betweenle.model.GameState
import com.enigma.wordnest.games.betweenle.ui.theme.PrimaryGreen
import com.enigma.wordnest.games.common.ui.GameOverTemplate

@Composable
fun GameOverCard(
    state: GameState,
    onNewGame: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameOverTemplate(
        title = if (state.isWon) "🎉 Correct!" else "😔 Out of guesses",
        titleColor = if (state.isWon) PrimaryGreen else MaterialTheme.colorScheme.error,
        subtitle = if (state.isWon) "Found in ${state.guesses.size} guesses" else "Any of these would have worked:",
        onNewGame = onNewGame,
        onShare = onShare,
        accentColor = PrimaryGreen,
        modifier = modifier
    ) {
        if (!state.isWon) {
            SolutionScroll(state.solutionWords.take(20))
        }
    }
}

@Composable
private fun SolutionScroll(words: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(words) { word ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )
            }
        }
    }
}
