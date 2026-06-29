package com.enigma.wordnest.games.chromaword.model

// ── Difficulty ────────────────────────────────────────────────────────────────

enum class Difficulty(
    val label: String,
    val emoji: String,
    /** null = unlimited */
    val maxGuesses: Int?,
    /** null = show all history; 0 = show none */
    val historyVisible: Int?
) {
    BABY  ("Baby",   "🍼", null, null),   // unlimited guesses, full history
    EASY  ("Easy",   "😊", 8,    8),      // 8 guesses, show all 8 rows
    NORMAL("Normal", "😐", 6,    6),      // 6 guesses, show last 6 rows
    HARD  ("Hard",   "💀", 4,    4);      // 4 guesses, show last 4 rows
}

// ── Per-letter colour state ───────────────────────────────────────────────────

/**
 * ChromaWord colour rules (applied per-letter, per position):
 *
 *  GREEN      – this position is correct
 *  YELLOW     – letter is in the word but this position is wrong
 *               (and this is the only occurrence of that letter)
 *  LIGHT_BLUE – letter appears 2+ times in the word; this position is wrong
 *               but at least one OTHER position of the same letter IS correct
 *  ROYAL_BLUE – letter appears 2+ times in the word; ALL positions of that
 *               letter are wrong
 *  RED_EXTRA  – all correct positions of this letter are already green, but
 *               the guess still has extra (surplus) instances at wrong places
 *  RED_ABSENT – letter does not appear in the target word at all
 */
enum class LetterColor {
    GREEN,
    YELLOW,
    LIGHT_BLUE,
    ROYAL_BLUE,
    RED_EXTRA,
    RED_ABSENT,
    EMPTY       // unsubmitted tile
}

// ── A single evaluated letter in a guess ──────────────────────────────────────

data class EvaluatedLetter(
    val char: Char,
    val color: LetterColor
)

// ── A full evaluated guess row ────────────────────────────────────────────────

data class EvaluatedGuess(
    val letters: List<EvaluatedLetter>
)

// ── Keyboard key state (best colour seen so far for that key) ─────────────────

data class KeyState(
    val color: LetterColor = LetterColor.EMPTY
)

// ── Overall game state ────────────────────────────────────────────────────────

data class GameState(
    val targetWord: String = "",
    val difficulty: Difficulty = Difficulty.NORMAL,
    val guesses: List<EvaluatedGuess> = emptyList(),
    val currentInput: String = "",
    val isWon: Boolean = false,
    val isGameOver: Boolean = false,
    val keyboardState: Map<Char, LetterColor> = emptyMap(),
    val errorMessage: String? = null,
    val roundNumber: Int = 0
) {
    val wordLength: Int get() = targetWord.length
    val guessesUsed: Int get() = guesses.size
    val guessesRemaining: Int? get() = difficulty.maxGuesses?.minus(guessesUsed)
    val isActive: Boolean get() = !isWon && !isGameOver

    /** Rows to display — null means show all, otherwise cap at historyVisible */
    fun visibleGuesses(): List<EvaluatedGuess> {
        val limit = difficulty.historyVisible ?: guesses.size
        return if (guesses.size <= limit) guesses else guesses.takeLast(limit)
    }

    /** Empty placeholder rows still to be played (capped by difficulty) */
    fun emptyRowsRemaining(): Int {
        val maxRows = difficulty.historyVisible ?: (difficulty.maxGuesses ?: 6)
        val shown = visibleGuesses().size
        val remaining = guessesRemaining
        val emptySlots = if (remaining != null) minOf(remaining, maxRows - shown) else maxRows - shown
        return emptySlots.coerceAtLeast(0)
    }
}

// ── Stats ─────────────────────────────────────────────────────────────────────

data class Stats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val guessDistribution: List<Int> = List(10) { 0 }
) {
    val winRate: Float get() = if (gamesPlayed == 0) 0f else gamesWon.toFloat() / gamesPlayed
}
