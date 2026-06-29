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
import androidx.compose.ui.text.font.FontWeight
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyRow(ROW1, keyStates, onKey, enabled)
        KeyRow(ROW2, keyStates, onKey, enabled)

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionKey(label = "⌫", onClick = onBackspace, enabled = enabled)
            for (ch in ROW3) {
                LetterKey(ch = ch, color = keyStates[ch] ?: LetterColor.EMPTY,
                    onClick = { onKey(ch) }, enabled = enabled)
            }
            ActionKey(label = "↵", onClick = onEnter, enabled = enabled)
        }
    }
}

@Composable
private fun KeyRow(
    letters: List<Char>,
    keyStates: Map<Char, LetterColor>,
    onKey: (Char) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (ch in letters) {
            LetterKey(
                ch = ch,
                color = keyStates[ch] ?: LetterColor.EMPTY,
                onClick = { onKey(ch) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun LetterKey(
    ch: Char,
    color: LetterColor,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val bg = when (color) {
        LetterColor.EMPTY -> Color(0xFF818384)
        else              -> letterColor(color)
    }
    Box(
        modifier = Modifier
            .width(33.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ch.uppercaseChar().toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionKey(label: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF818384))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
