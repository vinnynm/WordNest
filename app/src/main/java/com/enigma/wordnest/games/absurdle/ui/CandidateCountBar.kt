package com.enigma.wordnest.games.absurdle.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurple
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurpleLight
import com.enigma.wordnest.games.absurdle.ui.theme.ColorSubtle

@Composable
fun CandidateCountBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (total == 0) 0f else current.toFloat() / total
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(600),
        label = "candidateBar"
    )

    val barColor = when {
        fraction > 0.5f -> ColorPurple.copy(alpha = 0.7f)
        fraction > 0.2f -> Color(0xFF7C3AED)
        fraction > 0.05f -> Color(0xFF9333EA)
        else            -> Color(0xFFA855F7)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Candidate pool",
                fontSize = 11.sp,
                color = ColorSubtle
            )
            Text(
                "$current / $total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPurpleLight
            )
        }
        Spacer(Modifier.height(4.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ColorPurple.copy(alpha = 0.15f))
        ) {
            val barWidth = maxWidth * animatedFraction
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}
