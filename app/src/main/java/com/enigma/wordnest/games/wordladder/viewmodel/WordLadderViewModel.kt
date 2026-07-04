package com.enigma.wordnest.games.wordladder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.wordladder.data.WordRepository
import com.enigma.wordnest.games.wordladder.model.LadderStep
import com.enigma.wordnest.games.wordladder.model.WordLadderSolver
import com.enigma.wordnest.games.wordladder.model.WordLadderState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WordLadderViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)

    private val _state = MutableStateFlow(WordLadderState())
    val state: StateFlow<WordLadderState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _showHowTo = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    private var currentAdjacency: Map<String, List<String>> = emptyMap()

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
        }
    }

    fun startGame(wordLength: Int = _state.value.wordLength.takeIf { it > 0 } ?: 4) {
        if (!wordRepo.isLoaded()) return
        viewModelScope.launch {
            _isGenerating.value = true
            val adjacency = wordRepo.adjacencyFor(wordLength)
            currentAdjacency = adjacency
            val puzzle = WordLadderSolver.generatePuzzle(adjacency, minSteps = 3, maxSteps = 6)
            _isGenerating.value = false

            if (puzzle == null) {
                _state.update {
                    it.copy(errorMessage = "Couldn't build a $wordLength-letter puzzle — try another length")
                }
                return@launch
            }
            val (start, target, shortest) = puzzle
            _state.value = WordLadderState(
                startWord = start,
                targetWord = target,
                wordLength = wordLength,
                minSteps = shortest.size - 1,
                path = listOf(LadderStep(start, null)),
                isGameStarted = true
            )
        }
    }

    fun onInputChanged(text: String) {
        val s = _state.value
        if (!s.isActive) return
        _state.update {
            it.copy(currentInput = text.lowercase().filter { c -> c.isLetter() }.take(s.wordLength), errorMessage = null)
        }
    }

    fun submitWord() {
        val s = _state.value
        if (!s.isActive) return
        val guess = s.currentInput.trim()
        val current = s.currentWord

        if (guess.length != s.wordLength) {
            _state.update { it.copy(errorMessage = "Need ${s.wordLength} letters") }
            return
        }
        if (!wordRepo.contains(guess)) {
            _state.update { it.copy(errorMessage = "\"$guess\" not in word list") }
            return
        }
        if (!WordLadderSolver.differsByOne(current, guess)) {
            _state.update { it.copy(errorMessage = "Change exactly one letter from \"${current.uppercase()}\"") }
            return
        }
        if (s.path.any { it.word == guess }) {
            _state.update { it.copy(errorMessage = "You already used that word") }
            return
        }

        val changedIndex = current.indices.first { current[it] != guess[it] }
        val won = guess == s.targetWord

        _state.update {
            it.copy(
                path = it.path + LadderStep(guess, changedIndex),
                currentInput = "",
                isWon = won,
                errorMessage = null
            )
        }
    }

    /** Reveals the next correct step (costs a hint) without ending the game. */
    fun useHint() {
        val s = _state.value
        if (!s.isActive) return
        val remainingPath = WordLadderSolver.shortestPath(s.currentWord, s.targetWord, currentAdjacency) ?: return
        if (remainingPath.size < 2) return
        val next = remainingPath[1]
        val changedIndex = s.currentWord.indices.first { s.currentWord[it] != next[it] }
        val won = next == s.targetWord
        _state.update {
            it.copy(
                path = it.path + LadderStep(next, changedIndex),
                hintsUsed = it.hintsUsed + 1,
                isWon = won,
                errorMessage = null
            )
        }
    }

    fun toggleHowTo() { _showHowTo.value = !_showHowTo.value }

    fun buildShareText(): String {
        val s = _state.value
        val sb = StringBuilder()
        sb.appendLine("Word Ladder: ${s.startWord.uppercase()} \u2192 ${s.targetWord.uppercase()}")
        sb.appendLine("Solved in ${s.stepsSoFar} steps (par ${s.minSteps}, ${s.hintsUsed} hint${if (s.hintsUsed != 1) "s" else ""})")
        sb.appendLine(s.path.joinToString(" \u2192 ") { it.word.uppercase() })
        return sb.toString().trim()
    }
}
