package com.enigma.wordnest.games.codeword.model

enum class StarterCellStyle { BLUR, ALPHA_SCRAMBLE }

enum class CodewordDifficulty(val label: String, val starterCount: Int) {
    GENTLE("Gentle", 4),
    CLASSIC("Classic", 2),
    BLANK_START("Blank Start", 0)
}

data class CodewordCell(
    val row: Int,
    val col: Int,
    val isBlocked: Boolean,
    val number: Int?,           // null if blocked
    val solutionLetter: Char?   // null if blocked; never shown directly except via blur/reveal
)

data class CodewordPuzzle(
    val grid: List<List<com.enigma.wordnest.games.codeword.model.CodewordCell>>,
    val letterToNumber: Map<Char, Int>,
    val blurredStarterNumbers: Set<Int>,
    val gridSize: Int,
    val difficulty: com.enigma.wordnest.games.codeword.model.CodewordDifficulty
) {
    val numberToLetter: Map<Int, Char> by lazy { letterToNumber.entries.associate { (l, n) -> n to l } }
}

/** [numberToPlayerLetter] is the ripple-effect mapping: filling number N fills every cell showing N. */
data class CodewordPlayerState(
    val numberToPlayerLetter: Map<Int, Char> = emptyMap(),
    val revealedNumbers: Set<Int> = emptySet(),
    val isComplete: Boolean = false,
    val elapsedSeconds: Int = 0
)
