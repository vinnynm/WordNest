package com.enigma.wordnest.games.absurdle.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.absurdle.model.TileColor
import com.enigma.wordnest.games.absurdle.ui.theme.keyBackground

private val ROW1 = "qwertyuiop".toList()
private val ROW2 = "asdfghjkl".toList()
private val ROW3 = "zxcvbnm".toList()

@Composable
fun AbsurdleKeyboard(
    keyStates: Map<Char, TileColor>,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KeyRow(ROW1, keyStates, onKey, enabled)
        KeyRow(ROW2, keyStates, onKey, enabled)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionKey("⌫", onBackspace, enabled)
            for (ch in ROW3) LetterKey(ch, keyStates[ch] ?: TileColor.EMPTY, { onKey(ch) }, enabled)
            ActionKey("↵", onEnter, enabled)
        }
    }
}

@Composable
private fun KeyRow(letters: List<Char>, states: Map<Char, TileColor>,
                   onKey: (Char) -> Unit, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (ch in letters) LetterKey(ch, states[ch] ?: TileColor.EMPTY, { onKey(ch) }, enabled)
    }
}

@Composable
private fun LetterKey(ch: Char, color: TileColor, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .width(33.dp).height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(keyBackground(color))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(ch.uppercaseChar().toString(), color = Color.White,
            fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionKey(label: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .width(50.dp).height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF565670))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
