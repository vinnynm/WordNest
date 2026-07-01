package com.enigma.wordnest.games.absurdle.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.absurdle.model.ScoredGuess
import com.enigma.wordnest.games.absurdle.model.TileColor
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurple
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurpleLight

@Composable
fun GuessGrid(
    wordLength: Int,
    submittedGuesses: List<ScoredGuess>,
    candidateHistory: List<Int>,
    currentInput: String,
    isActive: Boolean,
    showCandidateCount: Boolean,
    isShaking: Boolean,
    modifier: Modifier = Modifier
) {
    val tileSize = adaptiveTileSize(wordLength)
    val gap      = 5.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)      // tighter — badge fills the gap
    ) {
        submittedGuesses.forEachIndexed { idx, guess ->
            val isLatest = idx == submittedGuesses.lastIndex
            val count    = candidateHistory.getOrNull(idx)

            // ── Tile row ──────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                guess.letters.forEachIndexed { i, letter ->
                    LetterTile(
                        letter = letter.char,
                        color = letter.color,
                        size = tileSize,
                        animateFlip = isLatest,
                        flipDelay = i * 80
                    )
                }
            }

            // ── Candidate count badge — own row so it never steals horizontal space ──
            if (showCandidateCount && count != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    CandidateBadge(count = count, isLatest = isLatest)
                }
            }
        }

        // Current input row (shake if error)
        if (isActive) {
            ShakeRow(shaking = isShaking) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    for (i in 0 until wordLength) {
                        val ch = currentInput.getOrNull(i)
                        LetterTile(
                            letter = ch,
                            color = TileColor.EMPTY,
                            size = tileSize,
                            animateFlip = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateBadge(count: Int, isLatest: Boolean) {
    val bg = if (isLatest) ColorPurple else ColorPurple.copy(alpha = 0.35f)
    val textColor = if (isLatest) Color.White else ColorPurpleLight.copy(alpha = 0.7f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (count == 1) "1 word left!" else "$count words",
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ShakeRow(shaking: Boolean, content: @Composable () -> Unit) {
    val offsetX by animateFloatAsState(
        targetValue = 0f,
        animationSpec = if (shaking) keyframes {
            durationMillis = 400
            0f at 0; (-12f) at 50; 12f at 100
            (-10f) at 150; 10f at 200; (-6f) at 250; 0f at 400
        } else spring(),
        label = "shake"
    )
    Box(modifier = Modifier.graphicsLayer { translationX = offsetX }) { content() }
}
