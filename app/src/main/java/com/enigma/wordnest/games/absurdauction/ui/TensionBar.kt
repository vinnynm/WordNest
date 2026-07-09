package com.enigma.wordnest.games.absurdauction.ui

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.absurdauction.model.BankerDecision
import com.enigma.wordnest.games.absurdauction.model.BankerStance
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorPurple
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorSubtle
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorTensionHigh
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorTensionLow

/**
 * Clinical Readout style tension bar (design doc §2.6). Always-on, transparent
 * default — shows the Banker's stance, the most recent draw's rejected-alternative
 * count, and a fraction bar of how skewed that draw was, without any taunting copy
 * (that's a separate optional overlay, not built in this pass).
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DrawPoolTensionBar(
    stance: BankerStance,
    lastDecision: BankerDecision?,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ColorPurple.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM STANCE: ${stance.name}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorSubtle
            )
            if (isThinking) {
                Text(
                    "BANKER IS THINKING…",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ColorTensionHigh
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val rejected = lastDecision?.rejectedAlternativesCount ?: 0
        val fraction = (rejected / 20f).coerceIn(0f, 1f)
        val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(500), label = "tension")

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
                    .background(if (fraction < 0.5f) ColorTensionLow else ColorTensionHigh)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (lastDecision != null) "REJECTED ALTERNATIVES: $rejected" +
                    if (lastDecision.pityOverrideApplied) "  ·  PITY OVERRIDE" else ""
            else "REJECTED ALTERNATIVES: —",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = ColorSubtle
        )
    }
}