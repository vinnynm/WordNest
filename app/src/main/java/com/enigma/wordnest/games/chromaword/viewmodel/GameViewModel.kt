package com.enigma.wordnest.games.chromaword.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.chromaword.data.StatsRepository
import com.enigma.wordnest.games.chromaword.data.WordRepository
import com.enigma.wordnest.games.chromaword.model.Difficulty
import com.enigma.wordnest.games.chromaword.model.GameState
import com.enigma.wordnest.games.chromaword.model.GuessEvaluator
import com.enigma.wordnest.games.chromaword.model.LetterColor
import com.enigma.wordnest.games.chromaword.model.Stats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo  = WordRepository(application)
    private val statsRepo = StatsRepository(application)

    private val _state   = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showStats    = MutableStateFlow(false)
    val showStats: StateFlow<Boolean> = _showStats.asStateFlow()

    private val _showHowTo    = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    private val _showDifficulty = MutableStateFlow(false)
    val showDifficulty: StateFlow<Boolean> = _showDifficulty.asStateFlow()

    val stats: StateFlow<Stats> = statsRepo.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
            startNewGame()
        }
    }

    // ── New game ──────────────────────────────────────────────────────────────

    fun startNewGame(difficulty: Difficulty = _state.value.difficulty) {
        if (!wordRepo.isLoaded()) return
        val target = wordRepo.randomWord(4..8)
        _state.value = GameState(
            targetWord = target,
            difficulty = difficulty,
            roundNumber = _state.value.roundNumber + 1
        )
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    fun onInputChanged(text: String) {
        val s = _state.value
        if (!s.isActive) return
        _state.value = s.copy(
            currentInput = text.lowercase().filter { it.isLetter() }.take(s.wordLength),
            errorMessage = null
        )
    }

    fun onKeyPress(ch: Char) {
        val s = _state.value
        if (!s.isActive) return
        if (s.currentInput.length < s.wordLength) {
            _state.value = s.copy(
                currentInput = s.currentInput + ch.lowercaseChar(),
                errorMessage = null
            )
        }
    }

    fun onBackspace() {
        val s = _state.value
        if (!s.isActive) return
        if (s.currentInput.isNotEmpty()) {
            _state.value = s.copy(currentInput = s.currentInput.dropLast(1))
        }
    }

    fun submitGuess() {
        val s = _state.value
        if (!s.isActive) return
        val guess = s.currentInput.trim()

        if (guess.length != s.wordLength) {
            _state.value = s.copy(errorMessage = "Need ${s.wordLength} letters")
            return
        }
        if (!wordRepo.contains(guess)) {
            _state.value = s.copy(errorMessage = "\"$guess\" not in word list")
            return
        }

        val evaluated = GuessEvaluator.evaluate(guess, s.targetWord)
        val newGuesses = s.guesses + evaluated
        val newKeyboard = GuessEvaluator.updateKeyboard(s.keyboardState, evaluated)

        val won = evaluated.letters.all { it.color == LetterColor.GREEN }
        val outOfGuesses = s.difficulty.maxGuesses?.let { newGuesses.size >= it } ?: false
        val gameOver = won || outOfGuesses

        _state.value = s.copy(
            guesses       = newGuesses,
            currentInput  = "",
            isWon         = won,
            isGameOver    = gameOver,
            keyboardState = newKeyboard,
            errorMessage  = null
        )

        if (gameOver) {
            viewModelScope.launch {
                statsRepo.recordResult(won, newGuesses.size, stats.value)
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    fun dismissError() { _state.update { it.copy(errorMessage = null) } }
    fun toggleStats()      { _showStats.value    = !_showStats.value }
    fun toggleHowTo()      { _showHowTo.value    = !_showHowTo.value }
    fun toggleDifficulty() { _showDifficulty.value = !_showDifficulty.value }

    fun buildShareText(): String {
        val s = _state.value
        val count = if (s.isWon) "${s.guessesUsed}" else "X"
        val max   = s.difficulty.maxGuesses?.toString() ?: "∞"
        val sb    = StringBuilder()
        sb.appendLine("ChromaWord #${s.roundNumber}  $count/$max  [${s.difficulty.label}]")
        sb.appendLine("Word: ${s.wordLength} letters")
        for (g in s.guesses) {
            sb.appendLine(g.letters.joinToString("") { it.color.emoji() })
        }
        return sb.toString().trim()
    }
}

private fun LetterColor.emoji() = when (this) {
    LetterColor.GREEN      -> "🟩"
    LetterColor.YELLOW     -> "🟨"
    LetterColor.LIGHT_BLUE -> "🔵"
    LetterColor.ROYAL_BLUE -> "🟦"
    LetterColor.RED_EXTRA  -> "🟥"
    LetterColor.RED_ABSENT -> "🟥"
    LetterColor.EMPTY      -> "⬜"
}
