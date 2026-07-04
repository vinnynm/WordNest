package com.enigma.wordnest.games.absurdman.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.absurdman.data.WordRepository
import com.enigma.wordnest.games.absurdman.model.AbsurdmanEngine
import com.enigma.wordnest.games.absurdman.model.AbsurdmanState
import com.enigma.wordnest.games.absurdman.model.MAX_WRONG_GUESSES
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AbsurdmanViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)

    private val _state = MutableStateFlow(AbsurdmanState())
    val state: StateFlow<AbsurdmanState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showHowTo = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
        }
    }

    fun startGame(wordLength: Int) {
        if (!wordRepo.isLoaded()) return
        val candidates = wordRepo.wordsOfLength(wordLength)
        if (candidates.isEmpty()) {
            _state.update { it.copy(errorMessage = "No $wordLength-letter words available") }
            return
        }
        _state.value = AbsurdmanState(
            candidates = candidates,
            wordLength = wordLength,
            revealedPattern = List(wordLength) { null },
            isGameStarted = true
        )
    }

    fun startNewGame(wordLength: Int = _state.value.wordLength) = startGame(wordLength)

    fun guessLetter(letter: Char) {
        val s = _state.value
        if (!s.isActive) return
        val ch = letter.lowercaseChar()
        if (ch in s.guessedLetters) return

        val result = AbsurdmanEngine.process(ch, s.candidates, s.revealedPattern)

        val newPattern = s.revealedPattern.toMutableList()
        if (result.isHit) result.revealPositions.forEach { newPattern[it] = ch }

        val newWrong = if (result.isHit) s.wrongGuesses else s.wrongGuesses + 1
        val lost = newWrong >= MAX_WRONG_GUESSES && !result.isWon

        _state.update {
            it.copy(
                candidates = result.newCandidates,
                revealedPattern = newPattern,
                guessedLetters = it.guessedLetters + ch,
                wrongGuesses = newWrong,
                isWon = result.isWon,
                isLost = lost,
                revealedWord = when {
                    result.isWon -> result.committedWord ?: newPattern.joinToString("") { c -> c.toString() }
                    lost -> result.committedWord ?: result.newCandidates.minOrNull()
                    else -> null
                },
                lastGuessWasHit = result.isHit,
                errorMessage = null
            )
        }
    }

    fun toggleHowTo() { _showHowTo.value = !_showHowTo.value }

    fun buildShareText(): String {
        val s = _state.value
        val result = if (s.isWon) "Won" else "Lost"
        return "Absurdman — $result with ${s.wrongGuesses}/$MAX_WRONG_GUESSES wrong guesses " +
            "(${s.wordLength} letters)"
    }
}
