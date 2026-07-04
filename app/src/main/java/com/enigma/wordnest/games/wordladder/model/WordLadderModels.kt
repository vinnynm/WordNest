package com.enigma.wordnest.games.wordladder.model

/**
 * Word Ladder — transform a start word into a target word, changing exactly
 * one letter per step, with every intermediate word required to be a valid
 * dictionary word of the same length.
 */

val WORD_LENGTH_OPTIONS = listOf(3, 4, 5, 6)

data class LadderStep(
    val word: String,
    /** Index of the letter changed from the previous step; null for the start word */
    val changedIndex: Int?
)

data class WordLadderState(
    val startWord: String = "",
    val targetWord: String = "",
    val wordLength: Int = 4,
    val minSteps: Int = 0,              // length of the shortest known solution (steps, not words)
    val path: List<LadderStep> = emptyList(),   // player's confirmed chain, path[0] = start
    val currentInput: String = "",
    val isWon: Boolean = false,
    val isGameStarted: Boolean = false,
    val errorMessage: String? = null,
    val hintsUsed: Int = 0
) {
    val isActive: Boolean get() = isGameStarted && !isWon
    val currentWord: String get() = path.lastOrNull()?.word ?: startWord
    val stepsSoFar: Int get() = (path.size - 1).coerceAtLeast(0)
}

data class LadderStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestStepsOverPar: Int = Int.MAX_VALUE, // best (stepsSoFar - minSteps); lower is better
    val currentStreak: Int = 0,
    val maxStreak: Int = 0
)
