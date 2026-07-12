package com.enigma.wordnest.games.absurdman.model

/**
 * Absurdman — two modes:
 *
 *  CLASSIC — a real, fixed target word is chosen at game start and never
 *            changes. Standard hangman rules. Clues (see ClueGenerator) are
 *            derived from the candidate pool BEFORE the target is drawn, so
 *            they're guaranteed true of the eventual word.
 *
 *  HELL    — the original adversarial engine. There is no secret word at the
 *            start. The engine keeps a candidate set of all words of the
 *            chosen length and, on every letter guess, picks whichever
 *            outcome (letter present vs. absent, and if present, WHERE)
 *            keeps the largest number of candidates alive.
 */

val WORD_LENGTH_OPTIONS = listOf(4, 5, 6, 7, 8)

/** How many wrong guesses the player is allowed before losing. */
const val MAX_WRONG_GUESSES = 6

enum class AbsurdmanMode { CLASSIC, HELL }

data class AbsurdmanState(
    val candidates: Set<String> = emptySet(),
    val wordLength: Int = 6,
    val revealedPattern: List<Char?> = emptyList(),  // null = not yet revealed
    val guessedLetters: Set<Char> = emptySet(),
    val wrongGuesses: Int = 0,
    val isWon: Boolean = false,
    val isLost: Boolean = false,
    /** Word the game is finally forced to commit to (shown on win or loss) */
    val revealedWord: String? = null,
    val errorMessage: String? = null,
    val isGameStarted: Boolean = false,
    val lastGuessWasHit: Boolean? = null,
    val clues: List<String> = emptyList(),
    val mode: AbsurdmanMode = AbsurdmanMode.HELL,
    /** Only ever set in CLASSIC mode — the single word the game committed to at start. */
    val targetWord: String? = null
) {
    val isActive: Boolean get() = isGameStarted && !isWon && !isLost
    val wrongGuessesRemaining: Int get() = (MAX_WRONG_GUESSES - wrongGuesses).coerceAtLeast(0)
}