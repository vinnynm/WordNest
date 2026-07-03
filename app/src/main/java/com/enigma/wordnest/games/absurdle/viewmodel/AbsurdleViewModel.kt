package com.enigma.wordnest.games.absurdle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.absurdle.data.WordRepository
import com.enigma.wordnest.games.absurdle.model.AbsurdleEngine
import com.enigma.wordnest.games.absurdle.model.AbsurdleState
import com.enigma.wordnest.games.absurdle.model.GameOverReason
import com.enigma.wordnest.games.absurdle.model.ScoredGuess
import com.enigma.wordnest.games.absurdle.model.TileColor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AbsurdleViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)

    private val _state      = MutableStateFlow(AbsurdleState())
    val state: StateFlow<AbsurdleState> = _state.asStateFlow()

    private val _isLoading  = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showHowTo  = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
            // Don't start a new game automatically, wait for user selection
            _state.update { it.copy(isGameStarted = false) }
        }
    }

    // ── Game lifecycle ────────────────────────────────────────────────────────

    fun startGame(
        wordLength: Int,
        hardMode: Boolean
    ) {
        if (!wordRepo.isLoaded()) return
        val candidates = wordRepo.wordsOfLength(wordLength)
        _state.value = AbsurdleState(
            candidates = candidates,
            wordLength = wordLength,
            hardMode = hardMode,
            showCandidateCount = _state.value.showCandidateCount,
            isGameStarted = true
        )
    }

    fun startNewGame(
        wordLength: Int = _state.value.wordLength,
        hardMode: Boolean = _state.value.hardMode
    ) {
        if (!wordRepo.isLoaded()) return
        val candidates = wordRepo.wordsOfLength(wordLength)
        _state.value = AbsurdleState(
            candidates = candidates,
            wordLength = wordLength,
            hardMode = hardMode,
            showCandidateCount = _state.value.showCandidateCount,
            isGameStarted = true
        )
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    fun onKeyPress(ch: Char) {
        val s = _state.value
        if (!s.isActive) return
        if (s.currentInput.length < s.wordLength) {
            _state.update { it.copy(currentInput = it.currentInput + ch.lowercaseChar(), errorMessage = null) }
        }
    }

    fun onBackspace() {
        val s = _state.value
        if (!s.isActive) return
        if (s.currentInput.isNotEmpty()) {
            _state.update { it.copy(currentInput = it.currentInput.dropLast(1)) }
        }
    }

    fun onInputChanged(text: String) {
        val s = _state.value
        if (!s.isActive) return
        _state.update {
            it.copy(
                currentInput = text.lowercase().filter { c -> c.isLetter() }.take(s.wordLength),
                errorMessage = null
            )
        }
    }

    fun submitGuess() {
        val s = _state.value
        if (!s.isActive) return
        val guess = s.currentInput.trim()

        if (guess.length != s.wordLength) {
            _state.update { it.copy(errorMessage = "Need ${s.wordLength} letters") }
            return
        }
        if (!wordRepo.contains(guess)) {
            _state.update { it.copy(errorMessage = "\"$guess\" not in word list") }
            return
        }

        if (s.hardMode) {
            val hardModeError = validateHardMode(guess, s.guesses)
            if (hardModeError != null) {
                _state.update { it.copy(errorMessage = hardModeError) }
                return
            }
        }

        // Adversarial engine picks the worst response
        val result = AbsurdleEngine.process(guess, s.candidates, s.hardMode)

        val newKeyboard = AbsurdleEngine.mergeKeyboard(s.keyboardState, result.scoredGuess)
        val newHistory  = s.candidateHistory + result.newCandidates.size

        _state.update {
            it.copy(
                candidates        = result.newCandidates,
                guesses           = it.guesses + result.scoredGuess,
                currentInput      = "",
                isWon             = result.isWon,
                gameOverReason    = when {
                    result.isWon -> GameOverReason.WON
                    result.newCandidates.size == 1 -> GameOverReason.FORCED_REVEAL
                    else -> null
                },
                revealedWord      = result.committedWord ?: it.revealedWord,
                keyboardState     = newKeyboard,
                candidateHistory  = newHistory,
                errorMessage      = null,
                isGameStarted     = true
            )
        }
    }

    /** Player gives up — reveal the first surviving candidate */
    fun giveUp() {
        val s = _state.value
        if (!s.isActive) return
        val revealed = s.candidates.minOrNull() ?: return
        _state.update { it.copy(revealedWord = revealed, isWon = false, gameOverReason = GameOverReason.GAVE_UP) }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun setWordLength(length: Int) = startNewGame(wordLength = length)
    fun setHardMode(enabled: Boolean) = startNewGame(hardMode = enabled)
    fun toggleCandidateCount() {
        _state.update { it.copy(showCandidateCount = !it.showCandidateCount) }
    }

    fun togglePhysicalKeyboard() {
        _state.update { it.copy(usePhysicalKeyboard = !it.usePhysicalKeyboard) }
    }

    private fun validateHardMode(guess: String, history: List<ScoredGuess>): String? {
        for (prev in history) {
            for (i in prev.letters.indices) {
                val hint = prev.letters[i]
                if (hint.color == TileColor.GREEN) {
                    if (guess[i] != hint.char) {
                        return "${hint.char.uppercase()} must be at position ${i + 1}"
                    }
                }
            }
            // For yellows, we check if the guess contains at least as many of that char
            // as were revealed as yellow+green in the previous guess.
            val yellowOrGreenLetters = prev.letters.filter { it.color == TileColor.YELLOW || it.color == TileColor.GREEN }
            val counts = yellowOrGreenLetters.groupBy { it.char }.mapValues { it.value.size }
            for ((char, count) in counts) {
                val guessCount = guess.count { it == char }
                if (guessCount < count) {
                    return "Must contain ${count}x ${char.uppercase()}"
                }
            }
        }
        return null
    }

    // ── UI toggles ────────────────────────────────────────────────────────────

    fun toggleHowTo()    { _showHowTo.value    = !_showHowTo.value }
    fun toggleSettings() { _showSettings.value = !_showSettings.value }

    // ── Share ─────────────────────────────────────────────────────────────────

    fun buildShareText(): String {
        val s = _state.value
        val result = if (s.isWon) "Won in ${s.guessCount}" else "Gave up after ${s.guessCount}"
        val sb = StringBuilder()
        sb.appendLine("Absurdle — $result guess${if (s.guessCount != 1) "es" else ""}")
        sb.appendLine("Word length: ${s.wordLength}  |  Hard: ${if (s.hardMode) "ON" else "OFF"}")
        for ((idx, g) in s.guesses.withIndex()) {
            val count = s.candidateHistory.getOrNull(idx)?.let { " ($it left)" } ?: ""
            sb.appendLine(g.letters.joinToString("") { it.color.emoji() } + count)
        }
        return sb.toString().trim()
    }
}

private fun TileColor.emoji() = when (this) {
    TileColor.GREEN  -> "🟩"
    TileColor.YELLOW -> "🟨"
    TileColor.GRAY   -> "⬛"
    TileColor.EMPTY  -> "⬜"
}
