package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.enigma.wordnest.games.chromaword.model.EvaluatedGuess
import com.enigma.wordnest.games.chromaword.model.LetterColor

@Composable
fun GuessGrid(
    wordLength: Int,
    submittedGuesses: List<EvaluatedGuess>,
    currentInput: String,
    emptyRows: Int,
    isShaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tileSize = tileSize(wordLength)
    val spacing  = 5.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Submitted rows
        submittedGuesses.forEachIndexed { rowIndex, guess ->
            val isLatest = rowIndex == submittedGuesses.lastIndex
            EvaluatedRow(
                guess = guess,
                tileSize = tileSize,
                spacing = spacing,
                animate = isLatest
            )
        }

        // Current input row (only when game active)
        if (currentInput.isNotEmpty() || emptyRows > 0) {
            ShakeRow(shaking = isShaking) {
                CurrentInputRow(
                    input = currentInput,
                    wordLength = wordLength,
                    tileSize = tileSize,
                    spacing = spacing
                )
            }

            // Remaining empty rows (minus the current one)
            repeat((emptyRows - 1).coerceAtLeast(0)) {
                EmptyRow(wordLength = wordLength, tileSize = tileSize, spacing = spacing)
            }
        }
    }
}

@Composable
private fun EvaluatedRow(
    guess: EvaluatedGuess,
    tileSize: Dp,
    spacing: Dp,
    animate: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        guess.letters.forEachIndexed { i, letter ->
            LetterTile(
                letter = letter.char,
                color = letter.color,
                size = tileSize,
                animate = animate,
                animationDelay = i * 80
            )
        }
    }
}

@Composable
private fun CurrentInputRow(
    input: String,
    wordLength: Int,
    tileSize: Dp,
    spacing: Dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        for (i in 0 until wordLength) {
            val ch = input.getOrNull(i)
            LetterTile(
                letter = ch,
                color = if (ch != null) LetterColor.EMPTY else LetterColor.EMPTY,
                size = tileSize,
                animate = false
            )
        }
    }
}

@Composable
private fun EmptyRow(wordLength: Int, tileSize: Dp, spacing: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(wordLength) {
            LetterTile(letter = null, color = LetterColor.EMPTY, size = tileSize)
        }
    }
}

@Composable
private fun ShakeRow(shaking: Boolean, content: @Composable () -> Unit) {
    val offsetX by animateFloatAsState(
        targetValue = 0f,
        animationSpec = if (shaking) {
            keyframes {
                durationMillis = 400
                0f at 0
                (-12f) at 50
                12f at 100
                (-10f) at 150
                10f at 200
                (-6f) at 250
                6f at 300
                0f at 400
            }
        } else spring(),
        label = "shake"
    )
    Box(modifier = Modifier.graphicsLayer { translationX = offsetX }) {
        content()
    }
}

/** Adaptive tile size based on word length */
fun tileSize(wordLength: Int): Dp = when {
    wordLength <= 4 -> 62.dp
    wordLength <= 5 -> 56.dp
    wordLength <= 6 -> 50.dp
    wordLength <= 7 -> 44.dp
    else            -> 40.dp
}
