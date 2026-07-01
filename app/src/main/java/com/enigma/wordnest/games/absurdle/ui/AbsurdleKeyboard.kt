package com.enigma.wordnest.games.absurdle.ui

import android.annotation.SuppressLint
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
import com.enigma.wordnest.games.absurdle.model.TileColor
import com.enigma.wordnest.games.absurdle.ui.theme.keyBackground

private val ROW1 = "qwertyuiop".toList()   // 10 keys
private val ROW2 = "asdfghjkl".toList()    //  9 keys
private val ROW3 = "zxcvbnm".toList()      //  7 keys + 2 action keys

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AbsurdleKeyboard(
    keyStates: Map<Char, TileColor>,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Measure available width and derive a key size that always fits.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Row 1 is the widest: 10 letter keys + 9 × 5dp gaps = 45dp of spacing.
        // Row 3: 7 letter + 2 action (each = 1.5 × letter) + 8 × 5dp gaps.
        //   total width = letterW × (7 + 2×1.5) + 8×5  =>  letterW = (W - 40) / 10
        // We use the more constraining of row-1 and row-3 calculations and cap at 36dp.
        val co = constraints.maxWidth
        val gapDp = 5.dp
        val availDp = maxWidth - 8.dp          // 4dp side padding each side
        val letterKeyW: Dp = minOf(
            (availDp - gapDp * 9) / 10,        // row 1: 10 keys, 9 gaps
            (availDp - gapDp * 8) / 10f        // row 3: 10 key-units (7 + 2×1.5), 8 gaps
        ).coerceAtMost(36.dp)
        val actionKeyW  = (letterKeyW * 1.5f)
        val keyHeight   = (letterKeyW * 1.55f).coerceAtMost(52.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gapDp)
        ) {
            // Row 1 — 10 letter keys
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW1) LetterKey(ch, keyStates[ch] ?: TileColor.EMPTY,
                    { onKey(ch) }, enabled, letterKeyW, keyHeight)
            }
            // Row 2 — 9 letter keys
            Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                for (ch in ROW2) LetterKey(ch, keyStates[ch] ?: TileColor.EMPTY,
                    { onKey(ch) }, enabled, letterKeyW, keyHeight)
            }
            // Row 3 — ⌫ · 7 letter · ↵
            Row(
                horizontalArrangement = Arrangement.spacedBy(gapDp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionKey("⌫", onBackspace, enabled, actionKeyW, keyHeight)
                for (ch in ROW3) LetterKey(ch, keyStates[ch] ?: TileColor.EMPTY,
                    { onKey(ch) }, enabled, letterKeyW, keyHeight)
                ActionKey("↵", onEnter, enabled, actionKeyW, keyHeight)
            }
        }
    }
}

@Composable
private fun LetterKey(
    ch: Char,
    color: TileColor,
    onClick: () -> Unit,
    enabled: Boolean,
    width: Dp,
    height: Dp
) {
    val density = LocalDensity.current
    val fontSize = with(density) { (width.toPx() * 0.42f).toSp() }.let { 
        if (it.value > 15f) 15.sp else it 
    }

    Box(
        modifier = Modifier
            .width(width).height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(keyBackground(color))
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
            .background(Color(0xFF565670))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
