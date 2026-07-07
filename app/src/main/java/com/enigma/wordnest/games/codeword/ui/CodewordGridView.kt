package com.enigma.wordnest.games.codeword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.codeword.model.CodewordCell
import com.enigma.wordnest.games.codeword.model.CodewordPuzzle
import com.enigma.wordnest.games.codeword.model.StarterCellStyle
import com.enigma.wordnest.games.codeword.ui.theme.*

/**
 * Renders the codeword grid. The "ripple effect" (one number -> every matching cell)
 * is handled by keying a [SnapshotStateMap] on NUMBER rather than (row, col): every cell
 * sharing a number reads the same map entry, so filling number N recomposes exactly the
 * cells showing N — not the whole grid, and not cells showing other numbers.
 */
@Composable
fun CodewordGridView(
    puzzle: com.enigma.wordnest.games.codeword.model.CodewordPuzzle,
    numberToPlayerLetter: Map<Int, Char>,
    revealedNumbers: Set<Int>,
    starterStyle: com.enigma.wordnest.games.codeword.model.StarterCellStyle,
    selectedNumber: Int?,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val letterMap: SnapshotStateMap<Int, Char> = remember { mutableStateMapOf() }
    LaunchedEffect(numberToPlayerLetter) {
        letterMap.clear()
        letterMap.putAll(numberToPlayerLetter)
    }

    val cellSize = if (puzzle.gridSize <= 11) 30.dp else 24.dp

    Column(modifier = modifier.horizontalScroll(rememberScrollState())) {
        for (row in puzzle.grid) {
            Row {
                for (cell in row) {
                    CodewordCellSlot(
                        cell = cell,
                        letterMap = letterMap,
                        isStarter = cell.number != null && cell.number in puzzle.blurredStarterNumbers,
                        isRevealed = cell.number != null && cell.number in revealedNumbers,
                        isSelected = cell.number != null && cell.number == selectedNumber,
                        starterStyle = starterStyle,
                        size = cellSize,
                        onClick = { cell.number?.let(onCellClick) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CodewordCellSlot(
    cell: CodewordCell,
    letterMap: SnapshotStateMap<Int, Char>,
    isStarter: Boolean,
    isRevealed: Boolean,
    isSelected: Boolean,
    starterStyle: StarterCellStyle,
    size: Dp,
    onClick: () -> Unit
) {
    if (cell.isBlocked || cell.number == null) {
        Box(Modifier.size(size).background(ColorGridLine.copy(alpha = 0.4f)))
        return
    }

    val number = cell.number
    // Keyed on `number`, not (row, col): every cell sharing this number reads the exact
    // same derived value, so they recompose together as a group when — and only when —
    // this number's entry changes. This is the ripple effect's Compose-level implementation.
    val letter by remember(number) { derivedStateOf { letterMap[number] } }

    val bg = when {
        isSelected -> ColorViolet.copy(alpha = 0.35f)
        isRevealed -> ColorCorrect.copy(alpha = 0.2f)
        isStarter -> ColorStarter.copy(alpha = 0.15f)
        else -> ColorSurface
    }

    Box(
        modifier = Modifier
            .size(size)
            .border(0.5.dp, ColorGridLine)
            .background(bg)
            .clickable(enabled = !isStarter) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = (size.value * 0.22f).sp,
            color = ColorSubtle,
            modifier = Modifier.align(Alignment.TopStart).padding(1.dp)
        )

        if (isStarter) {
            StarterLetter(cell.solutionLetter, size, starterStyle)
        } else if (letter != null) {
            Text(
                text = letter!!.uppercaseChar().toString(),
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Bold,
                color = if (isRevealed) ColorCorrect else ColorText
            )
        }
    }
}

/**
 * Two rendering strategies for a "known but still needs confirming" starter cell, per the
 * audit debate: a literal Modifier.blur (closer to the "frosted glass" design intent) vs.
 * a cheap alpha + scrambled-glyph fallback (avoids any blur-shader cost). Since a puzzle
 * only has 2-4 starter cells out of ~100+, the literal blur's cost is negligible here, so
 * it's the default — the alpha/scramble path is kept as a real, working alternative rather
 * than a stub, in case profiling on a target device says otherwise.
 */
@Composable
private fun StarterLetter(letter: Char?, size: Dp, style: StarterCellStyle) {
    if (letter == null) return
    when (style) {
        StarterCellStyle.BLUR -> {
            Text(
                text = letter.uppercaseChar().toString(),
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Bold,
                color = ColorStarter,
                modifier = Modifier.blur(6.dp)
            )
        }
        StarterCellStyle.ALPHA_SCRAMBLE -> {
            // Shows the REAL letter (starter cells must convey real information either way)
            // just at low opacity, so it takes a deliberate look to confirm rather than a
            // literal blur shader. A decoy glyph would misinform the player, not obscure it.
            Text(
                text = letter.uppercaseChar().toString(),
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Bold,
                color = ColorStarter.copy(alpha = 0.3f)
            )
        }
    }
}
