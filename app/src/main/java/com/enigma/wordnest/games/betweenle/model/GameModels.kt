package com.enigma.wordnest.games.betweenle.model

/**
 * Represents the result of a single guess in Betweenle.
 *
 * In Betweenle the player is shown two "boundary" words (wordA, wordB) that are
 * alphabetically adjacent in the shuffled dictionary and must guess any word that
 * falls **between** them alphabetically.
 */
data class GuessResult(
    val word: String,
    val feedback: GuessFeedback,
    val distanceHint: DistanceHint = DistanceHint.NONE
)

enum class GuessFeedback {
    /** Word is between the two boundaries — correct! */
    CORRECT,
    /** Word is alphabetically before the lower boundary */
    TOO_LOW,
    /** Word is alphabetically after the upper boundary */
    TOO_HIGH,
    /** Word is not in the dictionary */
    NOT_IN_DICTIONARY,
    /** Word is exactly one of the boundary words */
    IS_BOUNDARY
}

/**
 * Rough proximity hint so the player has a feel for how close they are.
 * NONE means the game hasn't computed one (e.g. invalid guess).
 */
enum class DistanceHint {
    NONE,
    /** Hundreds of words away */
    FAR,
    /** Tens of words away */
    WARM,
    /** Single-digit words away */
    HOT,
    /** The answer is exactly adjacent to a boundary */
    BURNING
}

data class GameState(
    val wordA: String = "",
    val wordB: String = "",
    val targetWord: String = "",
    val guesses: List<GuessResult> = emptyList(),
    val isWon: Boolean = false,
    val isGameOver: Boolean = false,
    val maxGuesses: Int = 10,
    val roundNumber: Int = 1,
    /** All valid words between the two boundaries (for end-of-round reveal) */
    val solutionWords: List<String> = emptyList()
) {
    val remainingGuesses: Int get() = maxGuesses - guesses.size
    val isActive: Boolean get() = !isWon && !isGameOver
}

data class Stats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    /** Distribution: index = guess number (0-based), value = count */
    val guessDistribution: List<Int> = List(10) { 0 }
) {
    val winRate: Float get() = if (gamesPlayed == 0) 0f else gamesWon.toFloat() / gamesPlayed
}
