package com.enigma.wordnest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.ui.theme.NestPink
import com.enigma.wordnest.ui.theme.NestPurple
import com.enigma.wordnest.ui.theme.NestSubtle
import com.enigma.wordnest.ui.theme.NestTeal


data class GameEntry(
    val title: String,
    val emoji: String,
    val tagline: String,
    val accent: Color,
    val route: String
)

private val games = listOf(
    GameEntry("Betweenle", "🔤", "Find the word hiding between two boundaries", NestPink, "betweenle"),
    GameEntry("Absurdle", "😈", "The Wordle that fights back", NestPurple, "absurdle"),
    GameEntry("ChromaWord", "🌈", "Wordle, but every letter tells a richer story", NestTeal, "chromaword"),
    GameEntry("Lexicon", "📜", "Two-player word strategy, vs a real AI", NestPurple, "lexicon"),
    GameEntry("WordLadder", "🪜", "Connect two words by changing one letter at a time", NestPink, "WordLadder"),
    GameEntry(
        title = "HangMan",
        emoji = "🪂",
        tagline = "Guess the letters to ensure a safe landing",
        accent = NestTeal,
        route = "Hangman"
    ),
    GameEntry("Fragment", "🧩", "Solve clues, collect letters, crack the word", NestTeal, "fragment"),
    GameEntry("Absurd Auction", "🏦", "Scrabble, but the Banker picks your tiles", NestPurple, "absurd_auction"),
    GameEntry("Ladder Claim", "🏗️", "Claim territory by playing near ladder-legal words", NestPink, "ladder_claim"),
    GameEntry("Crossword", "📰", "A fresh generated puzzle every time", NestTeal, "crossword"),
    GameEntry("Codeword", "🔢", "Decode the grid, one number at a time", NestPurple, "codeword")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "WordNest",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "three little word games, made just for you 💌",
                        fontSize = 14.sp,
                        color = NestSubtle,
                        modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
                    )
                }

            }

            items(games.toList()) { game ->
                GameCard(game = game, onClick = { onNavigate(game.route) })
                Spacer(Modifier.height(16.dp))
            }

        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(game.accent.copy(alpha = 0.22f), game.accent.copy(alpha = 0.06f))
                )
            )
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(game.accent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(game.emoji, fontSize = 26.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                game.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                game.tagline,
                fontSize = 13.sp,
                color = NestSubtle
            )
        }
        Text("›", fontSize = 26.sp, color = game.accent, fontWeight = FontWeight.Bold)
    }
}
