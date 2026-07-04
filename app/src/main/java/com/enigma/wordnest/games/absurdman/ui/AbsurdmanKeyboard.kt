package com.enigma.wordnest.games.absurdman.ui

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
import com.enigma.wordnest.games.absurdman.ui.theme.ColorGood
import com.enigma.wordnest.games.absurdman.ui.theme.ColorRust

private val ROW1 = "qwertyuiop".toList()
private val ROW2 = "asdfghjkl".toList()
private val ROW3 = "zxcvbnm".toList()

@Composable
fun AbsurdmanKeyboard(
    guessedLetters: Set<Char>,
    presentLetters: Set<Char>,
    onKey: (Char) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gapDp = 5.dp
        val availDp = maxWidth - 8.dp
        val letterKeyW: Dp = ((availDp - gapDp * 9) / 10).coerceAtMost(36.dp)
        val keyHeight = (letterKeyW * 1.4f).coerceAtMost(48.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gapDp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW1) Key(ch, guessedLetters, presentLetters, onKey, enabled, letterKeyW, keyHeight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW2) Key(ch, guessedLetters, presentLetters, onKey, enabled, letterKeyW, keyHeight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW3) Key(ch, guessedLetters, presentLetters, onKey, enabled, letterKeyW, keyHeight)
            }
        }
    }
}

@Composable
private fun Key(
    ch: Char,
    guessedLetters: Set<Char>,
    presentLetters: Set<Char>,
    onKey: (Char) -> Unit,
    enabled: Boolean,
    width: Dp,
    height: Dp
) {
    val guessed = ch in guessedLetters
    val present = ch in presentLetters
    val bg = when {
        guessed && present -> ColorGood
        guessed             -> ColorRust
        else                -> Color(0xFF2A2A3C)
    }
    val density = LocalDensity.current
    val fontSize = with(density) { (width.toPx() * 0.42f).toSp() }.let { if (it.value > 15f) 15.sp else it }

    Box(
        modifier = Modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(enabled = enabled && !guessed) { onKey(ch) },
        contentAlignment = Alignment.Center
    ) {
        Text(ch.uppercaseChar().toString(), color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}
