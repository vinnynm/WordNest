package com.enigma.wordnest.games.codeword.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.codeword.data.CodewordWordRepository
import com.enigma.wordnest.games.codeword.model.*
import com.enigma.wordnest.games.common.model.GeneratedGrid
import com.enigma.wordnest.games.common.model.GridGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodewordViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = CodewordWordRepository(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _puzzle = MutableStateFlow<CodewordPuzzle?>(null)
    val puzzle: StateFlow<CodewordPuzzle?> = _puzzle.asStateFlow()

    private val _playerState = MutableStateFlow(CodewordPlayerState())
    val playerState: StateFlow<CodewordPlayerState> = _playerState.asStateFlow()

    /** The number currently being edited via the letter-picker dialog, or null if none. */
    private val _editingNumber = MutableStateFlow<Int?>(null)
    val editingNumber: StateFlow<Int?> = _editingNumber.asStateFlow()

    private val _starterStyle = MutableStateFlow(StarterCellStyle.BLUR)
    val starterStyle: StateFlow<StarterCellStyle> = _starterStyle.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
        }
    }

    fun setStarterStyle(style: StarterCellStyle) { _starterStyle.value = style }

    fun startNewGame(difficulty: CodewordDifficulty = CodewordDifficulty.GENTLE, size: Int = 11) {
        if (!wordRepo.isLoaded()) return
        viewModelScope.launch {
            _isGenerating.value = true
            val generated = withContext(Dispatchers.Default) {
                GridGenerator.generate(size = size, wordsByLength = wordRepo.wordsByLength())
            }
            if (generated == null) {
                _isGenerating.value = false
                _errorMessage.value = "Couldn't generate a puzzle — try again"
                return@launch
            }
            _puzzle.value = buildPuzzle(generated, difficulty)
            _playerState.value = CodewordPlayerState(
                numberToPlayerLetter = _puzzle.value!!.blurredStarterNumbers
                    .associateWith { number -> _puzzle.value!!.numberToLetter.getValue(number) },
                revealedNumbers = emptySet()
            )
            _editingNumber.value = null
            _isGenerating.value = false
        }
    }

    private fun buildPuzzle(generated: GeneratedGrid, difficulty: CodewordDifficulty): CodewordPuzzle {
        val size = generated.size
        val letterAt = Array(size) { arrayOfNulls<Char>(size) }
        for (slot in generated.slots) {
            val word = generated.fill[slot.id] ?: continue
            slot.cells().forEachIndexed { i, (r, c) -> letterAt[r][c] = word[i] }
        }

        val usedLetters = letterAt.flatten().filterNotNull().distinct().shuffled()
        val numberPool = (1..26).shuffled().take(usedLetters.size)
        val letterToNumber = usedLetters.zip(numberPool).toMap()

        val grid = (0 until size).map { r ->
            (0 until size).map { c ->
                val key = r to c
                val blocked = key in generated.blocked
                val letter = if (blocked) null else letterAt[r][c]
                CodewordCell(
                    row = r, col = c, isBlocked = blocked,
                    number = letter?.let { letterToNumber[it] },
                    solutionLetter = letter
                )
            }
        }

        // Blurred starter cells: pick a handful of DISTINCT numbers (not cells) to reveal,
        // per the design doc's "known starting letter-number pair" — revealing a number
        // reveals it everywhere that number appears on the grid, same ripple semantics
        // as ordinary play.
        val allNumbersUsed = letterToNumber.values.toList()
        val starterNumbers = allNumbersUsed.shuffled().take(difficulty.starterCount).toSet()

        return CodewordPuzzle(grid, letterToNumber, starterNumbers, size, difficulty)
    }

    // ── Player input ──────────────────────────────────────────────────────────

    fun beginEditing(number: Int) {
        if (number in (_puzzle.value?.blurredStarterNumbers ?: emptySet())) return // starter cells are fixed
        _editingNumber.value = number
    }

    fun cancelEditing() { _editingNumber.value = null }

    /** The core ripple mechanic: one number -> one letter, applied everywhere that number appears. */
    fun confirmLetter(letter: Char) {
        val number = _editingNumber.value ?: return
        _playerState.update { it.copy(numberToPlayerLetter = it.numberToPlayerLetter + (number to letter.lowercaseChar())) }
        _editingNumber.value = null
        checkCompletion()
    }

    fun clearNumber(number: Int) {
        if (number in (_puzzle.value?.blurredStarterNumbers ?: emptySet())) return
        _playerState.update { it.copy(numberToPlayerLetter = it.numberToPlayerLetter - number) }
    }

    fun revealNumber(number: Int) {
        val p = _puzzle.value ?: return
        val letter = p.numberToLetter[number] ?: return
        _playerState.update {
            it.copy(
                numberToPlayerLetter = it.numberToPlayerLetter + (number to letter),
                revealedNumbers = it.revealedNumbers + number
            )
        }
        checkCompletion()
    }

    private fun checkCompletion() {
        val p = _puzzle.value ?: return
        val state = _playerState.value
        val allNumbers = p.letterToNumber.values.toSet()
        val complete = allNumbers.all { state.numberToPlayerLetter[it] == p.numberToLetter[it] }
        if (complete) _playerState.update { it.copy(isComplete = true) }
    }

    fun dismissError() { _errorMessage.value = null }
}
