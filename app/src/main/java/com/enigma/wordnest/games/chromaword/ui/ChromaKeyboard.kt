package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.enigma.wordnest.games.chromaword.model.LetterColor
import com.enigma.wordnest.games.chromaword.ui.theme.letterColor
import com.enigma.wordnest.games.common.ui.GameKeyboard

@Composable
fun ChromaKeyboard(
    keyStates: Map<Char, LetterColor>,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    GameKeyboard(
        keyStates = keyStates,
        colorOf = { 
            if (it == null || it == LetterColor.EMPTY) Color(0xFF818384)
            else letterColor(it)
        },
        onKey = onKey,
        onBackspace = onBackspace,
        onEnter = onEnter,
        enabled = enabled,
        actionKeyBackground = Color(0xFF818384),
        modifier = modifier
    )
}
