package com.enigma.wordnest.games.fragment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.fragment.data.StatsRepository
import com.enigma.wordnest.games.fragment.data.WordRepository
import com.enigma.wordnest.games.fragment.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    private val statsRepo = StatsRepository(application)

    private val _state = MutableStateFlow(FragmentState())
    val state: StateFlow<FragmentState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _showHowTo = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    val stats: StateFlow<FragmentStats> = statsRepo.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FragmentStats())

    private var statsRecorded = false

    init {
        viewModelScope.launch {
            wordRepo.load()
            _isLoading.value = false
        }
    }

    fun startGame(wordLength: Int) {
        if (!wordRepo.isLoaded()) return
        statsRecorded = false
        viewModelScope.launch {
            _isGenerating.value = true
            val puzzle = withContext(Dispatchers.Default) {
                var attempt: FragmentPuzzle? = null
                var tries = 0
                val pool = wordRepo.wordsOfLength(wordLength)
                while (attempt == null && tries < 20 && pool.isNotEmpty()) {
                    val mystery = pool.random()
                    attempt = FragmentEngine.generate(mystery, wordRepo.wordsByLength())
                    tries++
                }
                attempt
            }
            _isGenerating.value = false
            if (puzzle == null) {
                _state.update { it.copy(errorMessage = "Couldn't build a $wordLength-letter puzzle — try another length") }
                return@launch
            }
            _state.value = FragmentState(
                mysteryWord = puzzle.mysteryWord,
                wordLength = wordLength,
                hintWords = puzzle.hintWords,
                isGameStarted = true
            )
        }
    }

    // ── Hint-word solving ────────────────────────────────────────────────────

    fun selectHintWord(id: Int) {
        val s = _state.value
        if (!s.isActive || s.phase != FragmentPhase.SOLVING_HINTS) return
        if (s.hintWords.find { it.id == id }?.isSolved == true) return
        _state.update { it.copy(activeHintWordId = id, errorMessage = null) }
    }

    fun onHintInputChanged(text: String) {
        val s = _state.value
        val activeId = s.activeHintWordId ?: return
        val hint = s.hintWords.find { it.id == activeId } ?: return
        val filtered = text.lowercase().filter { it.isLetter() }.take(hint.word.length)
        _state.update { st ->
            st.copy(hintWords = st.hintWords.map { if (it.id == activeId) it.copy(currentInput = filtered) else it })
        }
    }

    fun submitHintWord() {
        val s = _state.value
        val activeId = s.activeHintWordId ?: return
        val hint = s.hintWords.find { it.id == activeId } ?: return
        val guess = hint.currentInput.trim()

        if (guess.length != hint.word.length) {
            _state.update { it.copy(errorMessage = "Need ${hint.word.length} letters") }
            return
        }
        if (guess != hint.word) {
            loseLife("Not quite — that word doesn't fit")
            return
        }

        val newTiles = hint.blankLetters.mapIndexed { i, ch ->
            LetterTile(id = (hint.id * 10) + i, letter = ch, sourceHintWordId = hint.id)
        }

        _state.update { st ->
            val updatedHints = st.hintWords.map { if (it.id == activeId) it.copy(isSolved = true, currentInput = "") else it }
            val nextActive = updatedHints.firstOrNull { !it.isSolved }?.id
            val allSolved = updatedHints.all { it.isSolved }
            st.copy(
                hintWords = updatedHints,
                collectedTiles = st.collectedTiles + newTiles,
                activeHintWordId = nextActive,
                errorMessage = null,
                phase = if (allSolved) FragmentPhase.ANAGRAM else FragmentPhase.SOLVING_HINTS
            )
        }
    }

    // ── Anagram phase ────────────────────────────────────────────────────────

    fun tapTile(tileId: Int) {
        val s = _state.value
        if (s.phase != FragmentPhase.ANAGRAM || !s.isActive) return
        if (tileId in s.usedTileIds) return
        val tile = s.collectedTiles.find { it.id == tileId } ?: return
        if (s.anagramInput.length >= s.mysteryWord.length) return
        _state.update {
            it.copy(anagramInput = it.anagramInput + tile.letter, usedTileIds = it.usedTileIds + tileId, errorMessage = null)
        }
    }

    fun removeLastTile() {
        val s = _state.value
        if (s.usedTileIds.isEmpty()) return
        _state.update {
            it.copy(anagramInput = it.anagramInput.dropLast(1), usedTileIds = it.usedTileIds.dropLast(1))
        }
    }

    fun submitAnagram() {
        val s = _state.value
        if (s.phase != FragmentPhase.ANAGRAM || !s.isActive) return
        if (s.anagramInput.length != s.mysteryWord.length) {
            _state.update { it.copy(errorMessage = "Use all ${s.mysteryWord.length} letters") }
            return
        }
        if (s.anagramInput == s.mysteryWord) {
            _state.update { it.copy(isWon = true, phase = FragmentPhase.GAME_OVER, errorMessage = null) }
            recordStats(true)
        } else {
            loseLife("Wrong arrangement — try again", resetAnagram = true)
        }
    }

    // ── Shared life system ───────────────────────────────────────────────────

    private fun loseLife(message: String, resetAnagram: Boolean = false) {
        _state.update { s ->
            val newLives = s.lives - 1
            val lost = newLives <= 0
            s.copy(
                lives = newLives,
                errorMessage = message,
                isLost = lost,
                phase = if (lost) FragmentPhase.GAME_OVER else s.phase,
                anagramInput = if (resetAnagram) "" else s.anagramInput,
                usedTileIds = if (resetAnagram) emptyList() else s.usedTileIds,
                hintWords = s.hintWords.map { if (it.id == s.activeHintWordId) it.copy(currentInput = "") else it }
            )
        }
        if (_state.value.isLost) recordStats(false)
    }

    private fun recordStats(won: Boolean) {
        if (statsRecorded) return
        statsRecorded = true
        viewModelScope.launch { statsRepo.recordResult(won, stats.value) }
    }

    fun toggleHowTo() { _showHowTo.value = !_showHowTo.value }
    fun dismissError() { _state.update { it.copy(errorMessage = null) } }

    fun buildShareText(): String {
        val s = _state.value
        val result = if (s.isWon) "Solved" else "Lost"
        return "Fragment — $result (${s.wordLength} letters, ${s.livesRemaining} lives left)"
    }
}