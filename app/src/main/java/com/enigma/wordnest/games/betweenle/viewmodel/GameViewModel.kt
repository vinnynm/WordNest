package com.enigma.wordnest.games.betweenle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.betweenle.data.StatsRepository
import com.enigma.wordnest.games.betweenle.data.WordRepository
import com.enigma.wordnest.games.betweenle.model.DistanceHint
import com.enigma.wordnest.games.betweenle.model.GameState
import com.enigma.wordnest.games.betweenle.model.GuessFeedback
import com.enigma.wordnest.games.betweenle.model.GuessResult
import com.enigma.wordnest.games.betweenle.model.Stats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    private val statsRepo = StatsRepository(application)

    // ── UI State ─────────────────────────────────────────────────────────────

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showStats = MutableStateFlow(false)
    val showStats: StateFlow<Boolean> = _showStats.asStateFlow()

    private val _showHowToPlay = MutableStateFlow(false)
    val showHowToPlay: StateFlow<Boolean> = _showHowToPlay.asStateFlow()

    val stats: StateFlow<Stats> = statsRepo.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats())

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
            startNewGame()
        }
    }

    // ── Game Actions ──────────────────────────────────────────────────────────

    fun startNewGame() {
        if (!wordRepo.isLoaded()) return
        val (a, b) = wordRepo.randomBoundaryPair(minGap = 20, maxGap = 500)
        val solutions = wordRepo.wordsBetween(a, b)
        val target = solutions.random()
        
        _gameState.value = GameState(
            wordA = a,
            wordB = b,
            targetWord = target,
            solutionWords = solutions,
            roundNumber = _gameState.value.roundNumber + 1,
            maxGuesses = 10
        )
        _currentInput.value = ""
        _errorMessage.value = null
    }

    fun onInputChanged(text: String) {
        _currentInput.value = text.lowercase().filter { it.isLetter() }
        _errorMessage.value = null
    }

    fun submitGuess() {
        val state = _gameState.value
        if (!state.isActive) return

        val guess = _currentInput.value.trim().lowercase()
        if (guess.isBlank()) return

        // Validate
        if (!wordRepo.contains(guess)) {
            _errorMessage.value = "\"$guess\" is not in the word list"
            return
        }

        val comparison = wordRepo.compareGuess(guess, state.targetWord)
        
        var newWordA = state.wordA
        var newWordB = state.wordB
        
        val feedback = when {
            comparison == 0 -> GuessFeedback.CORRECT
            guess == state.wordA || guess == state.wordB -> GuessFeedback.IS_BOUNDARY
            guess < state.wordA -> {
                // Keep WordA as is, feedback is TOO_LOW
                GuessFeedback.TOO_LOW
            }
            guess > state.wordB -> {
                // Keep WordB as is, feedback is TOO_HIGH
                GuessFeedback.TOO_HIGH
            }
            comparison < 0 -> {
                // Guess is between WordA and Target
                newWordA = guess
                GuessFeedback.TOO_LOW
            }
            else -> {
                // Guess is between Target and WordB
                newWordB = guess
                GuessFeedback.TOO_HIGH
            }
        }

        val distance = wordRepo.distanceToTarget(guess, state.targetWord)
        val distHint = when {
            feedback == GuessFeedback.CORRECT -> DistanceHint.NONE
            distance == 0 -> DistanceHint.NONE
            distance == 1 -> DistanceHint.BURNING
            distance <= 10 -> DistanceHint.HOT
            distance <= 50 -> DistanceHint.WARM
            else -> DistanceHint.FAR
        }

        val result = GuessResult(guess, feedback, distHint)
        val newGuesses = state.guesses + result
        val won = feedback == GuessFeedback.CORRECT
        val gameOver = won || newGuesses.size >= state.maxGuesses

        _gameState.value = state.copy(
            wordA = newWordA,
            wordB = newWordB,
            guesses = newGuesses,
            isWon = won,
            isGameOver = gameOver
        )
        _currentInput.value = ""

        if (gameOver) {
            viewModelScope.launch {
                val currentStats = stats.value
                statsRepo.recordResult(won, newGuesses.size, currentStats)
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    fun dismissError() { _errorMessage.value = null }

    fun toggleStats() { _showStats.value = !_showStats.value }
    fun toggleHowToPlay() { _showHowToPlay.value = !_showHowToPlay.value }

    /** Share text for the result */
    fun buildShareText(): String {
        val state = _gameState.value
        val guessCount = if (state.isWon) state.guesses.size.toString() else "X"
        val sb = StringBuilder()
        sb.appendLine("Betweenle ${state.roundNumber} $guessCount/${state.maxGuesses}")
        sb.appendLine("${state.wordA.uppercase()} ← ? → ${state.wordB.uppercase()}")
        for (g in state.guesses) {
            sb.appendLine(g.word + "  " + g.feedback.emoji())
        }
        return sb.toString().trim()
    }
}

private fun GuessFeedback.emoji() = when (this) {
    GuessFeedback.CORRECT         -> "✅"
    GuessFeedback.TOO_LOW         -> "⬇️"
    GuessFeedback.TOO_HIGH        -> "⬆️"
    GuessFeedback.IS_BOUNDARY     -> "🚧"
    GuessFeedback.NOT_IN_DICTIONARY -> "❌"
}
