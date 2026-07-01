package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.chromaword.model.LetterColor
import com.enigma.wordnest.games.chromaword.ui.theme.letterColor

private val ROW1 = listOf('q','w','e','r','t','y','u','i','o','p')
private val ROW2 = listOf('a','s','d','f','g','h','j','k','l')
private val ROW3 = listOf('z','x','c','v','b','n','m')

@Composable
fun ChromaKeyboard(
    keyStates: Map<Char, LetterColor>,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gapDp      = 5.dp
        val availDp    = maxWidth - 8.dp
        val letterKeyW: Dp = minOf(
            (availDp - gapDp * 9)  / 10,
            (availDp - gapDp * 8)  / 10f
        ).coerceAtMost(36.dp)
        val actionKeyW = (letterKeyW * 1.5f)
        val keyHeight  = (letterKeyW * 1.55f).coerceAtMost(52.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gapDp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW1) LetterKey(ch, keyStates[ch] ?: LetterColor.EMPTY,
                    { onKey(ch) }, enabled, letterKeyW, keyHeight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW2) LetterKey(ch, keyStates[ch] ?: LetterColor.EMPTY,
                    { onKey(ch) }, enabled, letterKeyW, keyHeight)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(gapDp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionKey("⌫", onBackspace, enabled, actionKeyW, keyHeight)
                for (ch in ROW3) LetterKey(ch, keyStates[ch] ?: LetterColor.EMPTY,
                    { onKey(ch) }, enabled, letterKeyW, keyHeight)
                ActionKey("↵", onEnter, enabled, actionKeyW, keyHeight)
            }
        }
    }
}

@Composable
private fun LetterKey(
    ch: Char,
    color: LetterColor,
    onClick: () -> Unit,
    enabled: Boolean,
    width: Dp,
    height: Dp
) {
    val density = LocalDensity.current
    val fontSize = with(density) { (width.toPx() * 0.42f).toSp() }.let {
        if (it.value > 15f) 15.sp else it
    }

    val bg = when (color) {
        LetterColor.EMPTY -> Color(0xFF818384)
        else              -> letterColor(color)
    }
    Box(
        modifier = Modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(ch.uppercaseChar().toString(), color = Color.White,
            fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionKey(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    width: Dp,
    height: Dp
) {
    Box(
        modifier = Modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF818384))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
