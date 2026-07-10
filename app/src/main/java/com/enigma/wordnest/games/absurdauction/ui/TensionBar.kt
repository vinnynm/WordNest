package com.enigma.wordnest.games.absurdauction.ui

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
import com.enigma.wordnest.games.absurdauction.model.DrawRole
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorGreen
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorPurple
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorSubtle
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorTensionHigh
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorTensionLow

/**
 * Clinical Readout style tension bar (design doc §2.6), extended per playtest
 * feedback: the original version only showed a one-off "pity override" string
 * and a raw rejected-alternatives count, which didn't answer "is the Banker
 * helping or hurting right now, and who." This version shows:
 *  - the role of the MOST RECENT draw (STARVED / FAVORED / MEDIAN / etc.), and
 *    which player it applied to, every single turn — not just on pity events.
 *  - a running per-player cumulative-impact readout, so "was it actually
 *    helping" is answerable at a glance instead of requiring mental math.
 */
@Composable
fun DrawPoolTensionBar(
    stance: BankerStance,
    lastDecision: BankerDecision?,
    lastDecisionPlayerName: String?,
    cumulativeImpact: Map<Int, Int>,
    playerNames: List<String>,
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

        // ── Per-turn readout: what just happened, to whom ──────────────────────
        if (lastDecision != null) {
            val (label, color) = roleLabelAndColor(lastDecision.role)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$label${lastDecisionPlayerName?.let { " — $it" } ?: ""}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (lastDecision.role == DrawRole.STARVED || lastDecision.role == DrawRole.FAVORED) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(potential ${if (lastDecision.potentialDelta >= 0) "+" else ""}${lastDecision.potentialDelta})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ColorSubtle
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

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
            text = "REJECTED ALTERNATIVES: $rejected" +
                if (lastDecision?.pityOverrideApplied == true) "  ·  PITY OVERRIDE" else "",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = ColorSubtle
        )

        // ── Running cumulative impact per player — the "was it actually helping" answer ──
        if (cumulativeImpact.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                playerNames.forEachIndexed { idx, name ->
                    val impact = cumulativeImpact[idx] ?: 0
                    val sign = if (impact >= 0) "+" else ""
                    val color = if (impact >= 0) ColorGreen else ColorTensionHigh
                    Text(
                        text = "$name: $sign$impact",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

private fun roleLabelAndColor(role: DrawRole) = when (role) {
    DrawRole.STARVED -> "🩸 STARVED THIS DRAW" to ColorTensionHigh
    DrawRole.FAVORED -> "🍀 FAVORED THIS DRAW" to ColorGreen
    DrawRole.MEDIAN -> "⚖️ EVEN DRAW" to ColorSubtle
    DrawRole.PITY_OVERRIDE -> "🕊️ PITY OVERRIDE" to ColorGreen
    DrawRole.UNIFORM_RANDOM -> "🎲 RANDOM (Banker off)" to ColorSubtle
    DrawRole.OPENING -> "🃏 OPENING RACK" to ColorSubtle
}
