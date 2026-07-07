package com.enigma.wordnest.games.crossword.model

enum class ClueDirection { ACROSS, DOWN }

enum class CrosswordDifficulty(val label: String, val gridSize: Int) {
    QUICK("Quick", 11),
    DAILY("Daily", 15)
}

data class CrosswordCell(
    val row: Int,
    val col: Int,
    val isBlocked: Boolean,
    val letter: Char?,               // solution letter, null if blocked
    val clueNumberAcross: Int? = null,
    val clueNumberDown: Int? = null
)

data class CrosswordClue(
    val number: Int,
    val direction: ClueDirection,
    val word: String,
    val clueText: String,
    val startRow: Int,
    val startCol: Int
) {
    fun cells(): List<Pair<Int, Int>> = (0 until word.length).map { i ->
        if (direction == ClueDirection.ACROSS) startRow to (startCol + i) else (startRow + i) to startCol
    }
}

data class CrosswordPuzzle(
    val grid: List<List<CrosswordCell>>,
    val clues: List<CrosswordClue>,
    val gridSize: Int,
    val difficulty: CrosswordDifficulty
) {
    val acrossClues get() = clues.filter { it.direction == ClueDirection.ACROSS }.sortedBy { it.number }
    val downClues get() = clues.filter { it.direction == ClueDirection.DOWN }.sortedBy { it.number }
}

/** Player progress. [filledLetters] keyed by (row, col). */
data class CrosswordPlayerState(
    val filledLetters: Map<Pair<Int, Int>, Char> = emptyMap(),
    val revealedCells: Set<Pair<Int, Int>> = emptySet(),
    val checkedCells: Set<Pair<Int, Int>> = emptySet(),
    val wrongCells: Set<Pair<Int, Int>> = emptySet(),
    val isComplete: Boolean = false,
    val elapsedSeconds: Int = 0
)
