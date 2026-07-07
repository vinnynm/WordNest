package com.enigma.wordnest.games.crossword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.crossword.model.ClueDirection
import com.enigma.wordnest.games.crossword.model.CrosswordCell
import com.enigma.wordnest.games.crossword.model.CrosswordPuzzle
import com.enigma.wordnest.games.crossword.ui.theme.*

/**
 * Renders the crossword grid using a plain Column/Row loop — same approach as Lexicon's
 * Board.kt — rather than LazyVerticalGrid. For a fully-visible fixed-size board (nothing
 * scrolls in/out), laziness buys nothing and only adds measurement overhead; the actual
 * recomposition-scoping win comes from [filledLetters] being read through a
 * [SnapshotStateMap] keyed by (row, col), so Compose tracks reads per-key and only the
 * cell whose value actually changed recomposes on each keystroke — not the whole grid.
 */
@Composable
fun CrosswordGridView(
    puzzle: CrosswordPuzzle,
    filledLetters: Map<Pair<Int, Int>, Char>,
    checkedCells: Set<Pair<Int, Int>>,
    wrongCells: Set<Pair<Int, Int>>,
    revealedCells: Set<Pair<Int, Int>>,
    selectedCell: Pair<Int, Int>?,
    selectedDirection: ClueDirection,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Real per-cell scoping: sync the immutable snapshot from the ViewModel into a
    // SnapshotStateMap once per change, so individual cell composables can read a single
    // key without depending on the whole map's identity.
    val filledMap: SnapshotStateMap<Pair<Int, Int>, Char> = remember { mutableStateMapOf() }
    LaunchedEffect(filledLetters) {
        filledMap.clear()
        filledMap.putAll(filledLetters)
    }

    val selectedWordCells = remember(selectedCell, selectedDirection, puzzle) {
        selectedCell?.let { (row, col) ->
            val clueList = if (selectedDirection == ClueDirection.ACROSS) puzzle.acrossClues else puzzle.downClues
            clueList.firstOrNull { row to col in it.cells() }?.cells()?.toSet() ?: emptySet()
        } ?: emptySet()
    }

    val cellSize = cellSizeFor(puzzle.gridSize)

    Column(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        for (row in puzzle.grid) {
            Row {
                for (cell in row) {
                    CrosswordCellSlot(
                        cell = cell,
                        filledMap = filledMap,
                        isChecked = (cell.row to cell.col) in checkedCells,
                        isWrong = (cell.row to cell.col) in wrongCells,
                        isRevealed = (cell.row to cell.col) in revealedCells,
                        isSelected = selectedCell == (cell.row to cell.col),
                        isInSelectedWord = (cell.row to cell.col) in selectedWordCells,
                        size = cellSize,
                        onClick = { onCellClick(cell.row, cell.col) }
                    )
                }
            }
        }
    }
}

private fun cellSizeFor(gridSize: Int): Dp = when {
    gridSize <= 11 -> 30.dp
    else -> 24.dp
}

@Composable
private fun CrosswordCellSlot(
    cell: CrosswordCell,
    filledMap: SnapshotStateMap<Pair<Int, Int>, Char>,
    isChecked: Boolean,
    isWrong: Boolean,
    isRevealed: Boolean,
    isSelected: Boolean,
    isInSelectedWord: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    if (cell.isBlocked) {
        Box(Modifier.size(size).background(ColorInk))
        return
    }

    val key = cell.row to cell.col
    // Keyed on (row, col): this derived read is stable across grid-level recompositions,
    // so this specific cell only recomposes when ITS entry in filledMap changes.
    val letter by remember(key) { derivedStateOf { filledMap[key] } }

    val bg = when {
        isSelected -> ColorAccent.copy(alpha = 0.35f)
        isInSelectedWord -> ColorAccent.copy(alpha = 0.15f)
        isWrong -> ColorWrong.copy(alpha = 0.25f)
        isRevealed -> ColorRevealed.copy(alpha = 0.25f)
        else -> ColorSurface
    }
    val textColor = when {
        isWrong -> ColorWrong
        isRevealed -> ColorRevealed
        else -> ColorText
    }
    val number = cell.clueNumberAcross ?: cell.clueNumberDown

    Box(
        modifier = Modifier
            .size(size)
            .border(0.5.dp, ColorGridLine)
            .background(bg)
            .clickable { onClick() }
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                fontSize = (size.value * 0.24f).sp,
                color = ColorSubtle,
                modifier = Modifier.align(Alignment.TopStart).padding(1.dp)
            )
        }
        Text(
            text = letter?.uppercaseChar()?.toString() ?: "",
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.align(Alignment.Center)
        )
        if (isChecked && !isWrong && letter != null) {
            Text(
                "✓",
                fontSize = (size.value * 0.22f).sp,
                color = ColorRevealed,
                modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp)
            )
        }
    }
}
