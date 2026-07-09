package com.enigma.wordnest.games.fragment.model

val WORD_LENGTH_OPTIONS = listOf(4, 5, 6, 7, 8)
const val MAX_LIVES = 6

enum class FragmentPhase { SOLVING_HINTS, ANAGRAM, GAME_OVER }

data class HintWord(
    val id: Int,
    val word: String,                 // full solution word, e.g. "APRON"
    val blankIndices: Set<Int>,       // positions blanked
    val isSolved: Boolean = false,
    val currentInput: String = ""
) {
    val displayLength: Int get() = word.length
    /** Letters revealed by solving this hint word, in index order. */
    val blankLetters: List<Char> get() = blankIndices.sorted().map { word[it] }
}

data class LetterTile(
    val id: Int,
    val letter: Char,
    val sourceHintWordId: Int
)

data class FragmentState(
    val mysteryWord: String = "",
    val wordLength: Int = 6,
    val hintWords: List<HintWord> = emptyList(),
    val activeHintWordId: Int? = null,
    val collectedTiles: List<LetterTile> = emptyList(),
    val anagramInput: String = "",
    val usedTileIds: List<Int> = emptyList(),   // tiles currently placed in anagramInput, in order
    val lives: Int = MAX_LIVES,
    val phase: FragmentPhase = FragmentPhase.SOLVING_HINTS,
    val isWon: Boolean = false,
    val isLost: Boolean = false,
    val errorMessage: String? = null,
    val isGameStarted: Boolean = false
) {
    val isActive: Boolean get() = isGameStarted && !isWon && !isLost
    val livesRemaining: Int get() = lives.coerceAtLeast(0)
    val allHintWordsSolved: Boolean get() = hintWords.isNotEmpty() && hintWords.all { it.isSolved }
}

data class FragmentPuzzle(
    val mysteryWord: String,
    val hintWords: List<HintWord>
)

data class FragmentStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0
) {
    val winRate: Float get() = if (gamesPlayed == 0) 0f else gamesWon.toFloat() / gamesPlayed
}