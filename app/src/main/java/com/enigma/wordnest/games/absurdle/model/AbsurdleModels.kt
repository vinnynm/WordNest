package com.enigma.wordnest.games.absurdle.model

// ── Per-letter tile colour (standard Wordle palette) ─────────────────────────

enum class TileColor {
    GREEN,   // correct position
    YELLOW,  // in word, wrong position
    GRAY,    // not in word
    EMPTY    // not yet submitted
}

// ── A single scored letter ────────────────────────────────────────────────────

data class ScoredLetter(val char: Char, val color: TileColor)

// ── A full scored row ─────────────────────────────────────────────────────────

data class ScoredGuess(val letters: List<ScoredLetter>) {
    /** Compact pattern string used as a bucket key, e.g. "GYGGG" */
    val pattern: String get() = letters.joinToString("") { it.color.code() }
    val isAllGreen: Boolean get() = letters.all { it.color == TileColor.GREEN }
}

private fun TileColor.code() = when (this) {
    TileColor.GREEN  -> "G"
    TileColor.YELLOW -> "Y"
    TileColor.GRAY   -> "X"
    TileColor.EMPTY  -> "_"
}

// ── Game state ────────────────────────────────────────────────────────────────

data class AbsurdleState(
    /** The surviving candidate set — the game picks among these each turn */
    val candidates: Set<String>      = emptySet(),
    val guesses: List<ScoredGuess>   = emptyList(),
    val currentInput: String         = "",
    val wordLength: Int              = 5,
    val isWon: Boolean               = false,
    /** The word the game was forced to commit to (revealed on win or give-up) */
    val revealedWord: String?        = null,
    val errorMessage: String?        = null,
    val keyboardState: Map<Char, TileColor> = emptyMap(),
    /** How many candidates remained when each guess was made (for transparency) */
    val candidateHistory: List<Int>  = emptyList(),
    val showCandidateCount: Boolean  = true,
    val hardMode: Boolean            = false,
    val isGameStarted: Boolean       = false,
    val usePhysicalKeyboard: Boolean = false
) {
    val guessCount: Int get() = guesses.size
    val isActive: Boolean get() = !isWon && revealedWord == null
}

// ── Word-length options ───────────────────────────────────────────────────────

val WORD_LENGTH_OPTIONS = listOf(4, 5, 6, 7)
