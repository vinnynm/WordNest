package com.enigma.wordnest.games.wordladder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.wordladder.model.LadderStep
import com.enigma.wordnest.games.wordladder.ui.theme.ColorAmber
import com.enigma.wordnest.games.wordladder.ui.theme.ColorTealLight

/** Vertical chain of ladder steps, highlighting the letter changed at each rung. */
@Composable
fun LadderChain(
    path: List<LadderStep>,
    targetWord: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        path.forEachIndexed { idx, step ->
            val isLast = idx == path.lastIndex
            val reachedTarget = step.word == targetWord
            RungRow(step = step, highlighted = isLast, isTarget = reachedTarget)
        }
        if (path.lastOrNull()?.word != targetWord) {
            TargetGhostRow(targetWord)
        }
    }
}

@Composable
private fun RungRow(step: LadderStep, highlighted: Boolean, isTarget: Boolean) {
    val bg = when {
        isTarget    -> ColorTealLight.copy(alpha = 0.25f)
        highlighted -> ColorTealLight.copy(alpha = 0.12f)
        else        -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                step.word.forEachIndexed { i, c ->
                    if (i == step.changedIndex) {
                        withStyle(SpanStyle(color = ColorAmber, fontWeight = FontWeight.Black)) {
                            append(c.uppercaseChar())
                        }
                    } else {
                        append(c.uppercaseChar())
                    }
                }
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun TargetGhostRow(targetWord: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(
            text = targetWord.uppercase(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}
