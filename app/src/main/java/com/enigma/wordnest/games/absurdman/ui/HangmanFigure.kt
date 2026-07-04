package com.enigma.wordnest.games.absurdman.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.enigma.wordnest.games.absurdman.model.MAX_WRONG_GUESSES

/**
 * Draws the classic gallows figure, one piece per wrong guess.
 * Stages (0..6): gallows only -> head -> body -> left arm -> right arm -> left leg -> right leg
 */
@Composable
fun HangmanFigure(
    wrongGuesses: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val stage = wrongGuesses.coerceIn(0, MAX_WRONG_GUESSES)
    Canvas(modifier = modifier.size(160.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 6f)

        // Gallows (always drawn)
        drawLine(color, Offset(w * 0.1f, h * 0.95f), Offset(w * 0.7f, h * 0.95f), 6f)
        drawLine(color, Offset(w * 0.25f, h * 0.95f), Offset(w * 0.25f, h * 0.08f), 6f)
        drawLine(color, Offset(w * 0.2f, h * 0.08f), Offset(w * 0.6f, h * 0.08f), 6f)
        drawLine(color, Offset(w * 0.6f, h * 0.08f), Offset(w * 0.6f, h * 0.22f), 6f)

        if (stage >= 1) { // head
            drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.6f, h * 0.30f), style = stroke)
        }
        if (stage >= 2) { // body
            drawLine(color, Offset(w * 0.6f, h * 0.39f), Offset(w * 0.6f, h * 0.62f), 6f)
        }
        if (stage >= 3) { // left arm
            drawLine(color, Offset(w * 0.6f, h * 0.46f), Offset(w * 0.48f, h * 0.56f), 6f)
        }
        if (stage >= 4) { // right arm
            drawLine(color, Offset(w * 0.6f, h * 0.46f), Offset(w * 0.72f, h * 0.56f), 6f)
        }
        if (stage >= 5) { // left leg
            drawLine(color, Offset(w * 0.6f, h * 0.62f), Offset(w * 0.5f, h * 0.78f), 6f)
        }
        if (stage >= 6) { // right leg
            drawLine(color, Offset(w * 0.6f, h * 0.62f), Offset(w * 0.7f, h * 0.78f), 6f)
        }
    }
}
