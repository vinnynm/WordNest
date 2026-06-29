package com.enigma.wordnest.games.absurdle.ui

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
import com.enigma.wordnest.games.absurdle.model.TileColor
import com.enigma.wordnest.games.absurdle.ui.theme.tileBackground
import com.enigma.wordnest.games.absurdle.ui.theme.tileBorder

@Composable
fun LetterTile(
    letter: Char?,
    color: TileColor,
    size: Dp = 54.dp,
    animateFlip: Boolean = false,
    flipDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var flipped by remember(animateFlip) { mutableStateOf(!animateFlip) }
    val rotX by animateFloatAsState(
        targetValue = if (flipped) 0f else 90f,
        animationSpec = tween(durationMillis = 300, delayMillis = flipDelay, easing = FastOutSlowInEasing),
        label = "flip"
    )
    LaunchedEffect(animateFlip) { if (animateFlip) flipped = true }

    val bg     = tileBackground(color)
    val border = tileBorder(color)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationX = rotX }
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = if (color == TileColor.EMPTY) 2.dp else 0.dp,
                color = border,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercaseChar().toString(),
                color = Color.White,
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

fun adaptiveTileSize(wordLength: Int): Dp = when {
    wordLength <= 4 -> 60.dp
    wordLength <= 5 -> 54.dp
    wordLength <= 6 -> 48.dp
    else            -> 42.dp
}
