package com.enigma.wordnest.games.betweenle.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.betweenle.model.DistanceHint
import com.enigma.wordnest.games.betweenle.model.GuessFeedback
import com.enigma.wordnest.games.betweenle.model.GuessResult
import com.enigma.wordnest.games.betweenle.ui.theme.BurningOrange
import com.enigma.wordnest.games.betweenle.ui.theme.FarGray
import com.enigma.wordnest.games.betweenle.ui.theme.HotOrange
import com.enigma.wordnest.games.betweenle.ui.theme.WarmYellow
import com.enigma.wordnest.games.betweenle.ui.theme.feedbackColor


@Composable
fun GuessRow(
    result: GuessResult,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val color = feedbackColor(result.feedback)

    // Flip-in animation
    var flipped by remember { mutableStateOf(!animate) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 0f else 90f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "flip"
    )
    LaunchedEffect(Unit) { flipped = true }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .graphicsLayer { rotationX = rotation },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Feedback icon
        FeedbackIcon(result.feedback, color)

        // Word chip
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = result.word.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Distance hint badge
        DistanceHintBadge(result.distanceHint)
    }
}

@Composable
private fun FeedbackIcon(feedback: GuessFeedback, tint: Color) {
    val icon = when (feedback) {
        GuessFeedback.CORRECT           -> Icons.Filled.Check
        GuessFeedback.TOO_LOW           -> Icons.Filled.KeyboardArrowDown
        GuessFeedback.TOO_HIGH          -> Icons.Filled.KeyboardArrowUp
        GuessFeedback.IS_BOUNDARY       -> Icons.Filled.Block
        GuessFeedback.NOT_IN_DICTIONARY -> Icons.Filled.Close
    }
    Icon(
        imageVector = icon,
        contentDescription = feedback.name,
        tint = tint,
        modifier = Modifier.size(28.dp)
    )
}

@Composable
private fun DistanceHintBadge(hint: DistanceHint) {
    val (label, bg) = when (hint) {
        DistanceHint.BURNING -> "🔥🔥" to BurningOrange
        DistanceHint.HOT     -> "🔥"  to HotOrange
        DistanceHint.WARM    -> "♨️"  to WarmYellow
        DistanceHint.FAR     -> "❄️"  to FarGray
        DistanceHint.NONE    -> return  // no badge
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = 0.15f))
            .border(1.dp, bg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Empty placeholder row shown when a guess slot hasn't been used yet */
@Composable
fun EmptyGuessRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.size(28.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(44.dp))
    }
}
