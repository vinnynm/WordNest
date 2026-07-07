package com.enigma.wordnest.games.crossword.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.common.model.GridGenerator
import com.enigma.wordnest.games.crossword.data.ClueSource
import com.enigma.wordnest.games.crossword.data.CrosswordWordRepository
import com.enigma.wordnest.games.crossword.data.FallbackClueSource
import com.enigma.wordnest.games.crossword.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrosswordViewModel(
    application: Application,
    private val clueSource: ClueSource = FallbackClueSource()
) : AndroidViewModel(application) {

    val wordRepo = CrosswordWordRepository(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _puzzle = MutableStateFlow<CrosswordPuzzle?>(null)
    val puzzle: StateFlow<CrosswordPuzzle?> = _puzzle.asStateFlow()

    private val _playerState = MutableStateFlow(CrosswordPlayerState())
    val playerState: StateFlow<CrosswordPlayerState> = _playerState.asStateFlow()

    private val _selectedCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCell: StateFlow<Pair<Int, Int>?> = _selectedCell.asStateFlow()

    private val _selectedDirection = MutableStateFlow(ClueDirection.ACROSS)
    val selectedDirection: StateFlow<ClueDirection> = _selectedDirection.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
        }
    }

    fun startNewGame(difficulty: CrosswordDifficulty = CrosswordDifficulty.DAILY) {
        if (!wordRepo.isLoaded()) return
        viewModelScope.launch {
            _isGenerating.value = true
            val generated = withContext(Dispatchers.Default) {
                GridGenerator.generate(size = difficulty.gridSize, wordsByLength = wordRepo.wordsByLength())
            }
            if (generated == null) {
                _isGenerating.value = false
                _errorMessage.value = "Couldn't generate a puzzle — try again"
                return@launch
            }
            val built = buildPuzzle(generated, difficulty)
            _puzzle.value = built
            _playerState.value = CrosswordPlayerState()
            _selectedCell.value = built.clues.minByOrNull { it.number }?.let { it.startRow to it.startCol }
            _selectedDirection.value = ClueDirection.ACROSS
            _isGenerating.value = false
        }
    }

    private suspend fun buildPuzzle(generated: com.enigma.wordnest.games.common.model.GeneratedGrid, difficulty: CrosswordDifficulty): CrosswordPuzzle {
        val size = generated.size
        val letterAt = Array(size) { arrayOfNulls<Char>(size) }
        for (slot in generated.slots) {
            val word = generated.fill[slot.id] ?: continue
            slot.cells().forEachIndexed { i, (r, c) -> letterAt[r][c] = word[i] }
        }

        // Standard numbering: scan row-major, number any cell that starts an across or down slot.
        val startsAcross = generated.slots.filter { it.isHorizontal }.associateBy { it.startRow to it.startCol }
        val startsDown = generated.slots.filter { !it.isHorizontal }.associateBy { it.startRow to it.startCol }

        var nextNumber = 1
        val numberAt = mutableMapOf<Pair<Int, Int>, Int>()
        for (r in 0 until size) for (c in 0 until size) {
            val key = r to c
            if (key in startsAcross || key in startsDown) {
                numberAt[key] = nextNumber++
            }
        }

        val clues = mutableListOf<CrosswordClue>()
        for ((key, slot) in startsAcross) {
            val word = generated.fill[slot.id] ?: continue
            val clueText = clueSource.clueFor(word) ?: "${word.length}-letter word"
            clues += CrosswordClue(numberAt.getValue(key), ClueDirection.ACROSS, word, clueText, slot.startRow, slot.startCol)
        }
        for ((key, slot) in startsDown) {
            val word = generated.fill[slot.id] ?: continue
            val clueText = clueSource.clueFor(word) ?: "${word.length}-letter word"
            clues += CrosswordClue(numberAt.getValue(key), ClueDirection.DOWN, word, clueText, slot.startRow, slot.startCol)
        }

        val grid = (0 until size).map { r ->
            (0 until size).map { c ->
                val key = r to c
                val blocked = key in generated.blocked
                CrosswordCell(
                    row = r,
                    col = c,
                    isBlocked = blocked,
                    letter = if (blocked) null else letterAt[r][c],
                    clueNumberAcross = if (key in startsAcross) numberAt[key] else null,
                    clueNumberDown = if (key in startsDown) numberAt[key] else null
                )
            }
        }

        return CrosswordPuzzle(grid, clues, size, difficulty)
    }

    // ── Player input ──────────────────────────────────────────────────────────

    fun selectCell(row: Int, col: Int) {
        val p = _puzzle.value ?: return
        val cell = p.grid.getOrNull(row)?.getOrNull(col) ?: return
        if (cell.isBlocked) return
        if (_selectedCell.value == (row to col)) {
            // tapping the same cell again toggles direction, if both exist
            val hasAcross = cell.clueNumberAcross != null || p.acrossClues.any { row to col in it.cells() }
            val hasDown = cell.clueNumberDown != null || p.downClues.any { row to col in it.cells() }
            if (hasAcross && hasDown) {
                _selectedDirection.value =
                    if (_selectedDirection.value == ClueDirection.ACROSS) ClueDirection.DOWN else ClueDirection.ACROSS
            }
        }
        _selectedCell.value = row to col
    }

    fun onLetterInput(letter: Char) {
        val (row, col) = _selectedCell.value ?: return
        val p = _puzzle.value ?: return
        val cell = p.grid.getOrNull(row)?.getOrNull(col) ?: return
        if (cell.isBlocked) return

        _playerState.update { it.copy(filledLetters = it.filledLetters + ((row to col) to letter.lowercaseChar())) }
        advanceSelection()
        checkCompletion()
    }

    fun onBackspace() {
        val (row, col) = _selectedCell.value ?: return
        _playerState.update { it.copy(filledLetters = it.filledLetters - (row to col)) }
    }

    private fun advanceSelection() {
        val (row, col) = _selectedCell.value ?: return
        val p = _puzzle.value ?: return
        val (nr, nc) = if (_selectedDirection.value == ClueDirection.ACROSS) row to col + 1 else row + 1 to col
        val next = p.grid.getOrNull(nr)?.getOrNull(nc)
        if (next != null && !next.isBlocked) _selectedCell.value = nr to nc
    }

    fun checkCell(row: Int, col: Int) {
        val p = _puzzle.value ?: return
        val solution = p.grid.getOrNull(row)?.getOrNull(col)?.letter ?: return
        val filled = _playerState.value.filledLetters[row to col]
        _playerState.update {
            val wrong = if (filled != null && filled != solution) it.wrongCells + (row to col) else it.wrongCells - (row to col)
            it.copy(checkedCells = it.checkedCells + (row to col), wrongCells = wrong)
        }
    }

    fun checkAll() {
        val p = _puzzle.value ?: return
        for (row in p.grid) for (cell in row) if (!cell.isBlocked) checkCell(cell.row, cell.col)
    }

    fun revealCell(row: Int, col: Int) {
        val p = _puzzle.value ?: return
        val solution = p.grid.getOrNull(row)?.getOrNull(col)?.letter ?: return
        _playerState.update {
            it.copy(
                filledLetters = it.filledLetters + ((row to col) to solution),
                revealedCells = it.revealedCells + (row to col),
                wrongCells = it.wrongCells - (row to col)
            )
        }
        checkCompletion()
    }

    private fun checkCompletion() {
        val p = _puzzle.value ?: return
        val state = _playerState.value
        val allCorrect = p.grid.all { row ->
            row.all { cell -> cell.isBlocked || state.filledLetters[cell.row to cell.col] == cell.letter }
        }
        if (allCorrect) _playerState.update { it.copy(isComplete = true) }
    }

    fun dismissError() { _errorMessage.value = null }
}
