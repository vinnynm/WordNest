package com.enigma.wordnest.games.absurdle.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.enigma.wordnest.games.absurdle.model.TileColor
import com.enigma.wordnest.games.absurdle.ui.theme.keyBackground
import com.enigma.wordnest.games.common.ui.GameKeyboard

@Composable
fun AbsurdleKeyboard(
    keyStates: Map<Char, TileColor>,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    GameKeyboard(
        keyStates = keyStates,
        colorOf = { keyBackground(it ?: TileColor.EMPTY) },
        onKey = onKey,
        onBackspace = onBackspace,
        onEnter = onEnter,
        enabled = enabled,
        actionKeyBackground = Color(0xFF565670),
        modifier = modifier
    )
}
