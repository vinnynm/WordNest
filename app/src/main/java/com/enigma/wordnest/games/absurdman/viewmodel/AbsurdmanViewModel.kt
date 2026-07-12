package com.enigma.wordnest.games.absurdman.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.absurdman.data.WordRepository
import com.enigma.wordnest.games.absurdman.model.AbsurdmanEngine
import com.enigma.wordnest.games.absurdman.model.AbsurdmanMode
import com.enigma.wordnest.games.absurdman.model.AbsurdmanState
import com.enigma.wordnest.games.absurdman.model.ClueGenerator
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

    // ── Game lifecycle ────────────────────────────────────────────────────────

    fun startGame(wordLength: Int, mode: AbsurdmanMode = AbsurdmanMode.HELL) {
        if (!wordRepo.isLoaded()) return
        val allCandidates = wordRepo.wordsOfLength(wordLength)
        if (allCandidates.isEmpty()) {
            _state.update { it.copy(errorMessage = "No $wordLength-letter words available") }
            return
        }

        // Clues are derived from the FULL pool first, in both modes, so whichever
        // word the game ends up on (fixed target or adversarial candidate set) is
        // guaranteed to satisfy every clue shown.
        val clueResult = ClueGenerator.generate(allCandidates)

        _state.value = if (mode == AbsurdmanMode.CLASSIC) {
            val target = clueResult.remaining.random()
            AbsurdmanState(
                candidates = setOf(target),
                wordLength = wordLength,
                revealedPattern = List(wordLength) { null },
                clues = clueResult.clues,
                mode = mode,
                targetWord = target,
                isGameStarted = true
            )
        } else {
            AbsurdmanState(
                candidates = clueResult.remaining,
                wordLength = wordLength,
                revealedPattern = List(wordLength) { null },
                clues = clueResult.clues,
                mode = mode,
                isGameStarted = true
            )
        }
    }

    fun startNewGame(
        wordLength: Int = _state.value.wordLength,
        mode: AbsurdmanMode = _state.value.mode
    ) = startGame(wordLength, mode)

    fun guessLetter(letter: Char) {
        val s = _state.value
        if (!s.isActive) return
        val ch = letter.lowercaseChar()
        if (ch in s.guessedLetters) return

        if (s.mode == AbsurdmanMode.CLASSIC) {
            guessLetterClassic(s, ch)
        } else {
            guessLetterHell(s, ch)
        }
    }

    /** Standard hangman: the target word never changes, we just check literal presence. */
    private fun guessLetterClassic(s: AbsurdmanState, ch: Char) {
        val target = s.targetWord ?: return
        val positions = target.indices.filter { target[it] == ch }
        val isHit = positions.isNotEmpty()

        val newPattern = s.revealedPattern.toMutableList()
        if (isHit) positions.forEach { newPattern[it] = ch }

        val newWrong = if (isHit) s.wrongGuesses else s.wrongGuesses + 1
        val won = newPattern.all { it != null }
        val lost = newWrong >= MAX_WRONG_GUESSES && !won

        _state.update {
            it.copy(
                revealedPattern = newPattern,
                guessedLetters = it.guessedLetters + ch,
                wrongGuesses = newWrong,
                isWon = won,
                isLost = lost,
                revealedWord = if (won || lost) target else null,
                lastGuessWasHit = isHit,
                errorMessage = null
            )
        }
    }

    /** Adversarial engine — dodges by always keeping the largest surviving candidate bucket. */
    private fun guessLetterHell(s: AbsurdmanState, ch: Char) {
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
        val modeLabel = if (s.mode == AbsurdmanMode.CLASSIC) "Classic" else "Hell Mode"
        return "Absurdman ($modeLabel) — $result with ${s.wrongGuesses}/$MAX_WRONG_GUESSES wrong guesses " +
                "(${s.wordLength} letters)"
    }
}