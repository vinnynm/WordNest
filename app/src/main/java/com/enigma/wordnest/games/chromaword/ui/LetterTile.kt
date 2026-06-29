package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.chromaword.model.LetterColor
import com.enigma.wordnest.games.chromaword.ui.theme.letterColor
import com.enigma.wordnest.games.chromaword.ui.theme.tileBorderColor

@Composable
fun LetterTile(
    letter: Char?,
    color: LetterColor,
    size: Dp = 52.dp,
    animate: Boolean = false,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    // Flip animation: tiles flip in after evaluation
    var flipped by remember(animate) { mutableStateOf(!animate) }
    val rotX by animateFloatAsState(
        targetValue = if (flipped) 0f else 90f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = animationDelay,
            easing = FastOutSlowInEasing
        ),
        label = "tileFlip"
    )
    LaunchedEffect(animate) { if (animate) flipped = true }

    // Bounce animation on keystroke
    var bounced by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (bounced) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bounce",
        finishedListener = { bounced = false }
    )
    LaunchedEffect(letter) { if (letter != null && !animate) bounced = true }

    val bg = if (color == LetterColor.EMPTY) Color.Transparent else letterColor(color)
    val border = tileBorderColor(color)
    val textColor = if (color == LetterColor.EMPTY && letter == null) Color.Transparent
                    else Color.White

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationX = rotX
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = if (color == LetterColor.EMPTY) 2.dp else 0.dp,
                color = border,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercaseChar().toString(),
                color = textColor,
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            )
        }
    }
}
