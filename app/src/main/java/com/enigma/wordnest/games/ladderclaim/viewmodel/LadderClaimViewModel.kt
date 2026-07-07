package com.enigma.wordnest.games.ladderclaim.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.ladderclaim.data.StatsRepository
import com.enigma.wordnest.games.ladderclaim.data.WordRepository
import com.enigma.wordnest.games.ladderclaim.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LadderClaimViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    private val statsRepo = StatsRepository(application)
    private val game = LadderClaimGame()
    private var statsRecorded = false

    private val _state = MutableStateFlow(LadderClaimState())
    val state: StateFlow<LadderClaimState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showHowTo = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    val stats: StateFlow<LadderClaimStats> = statsRepo.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LadderClaimStats())

    init {
        viewModelScope.launch {
            wordRepo.load()
            game.updateDictionary(wordRepo.dictionarySet())
            _isLoading.value = false
        }
    }

    fun startGame(player1: String, player2: String, variant: LadderClaimVariant) {
        if (!wordRepo.isLoaded()) return
        statsRecorded = false
        val seed = wordRepo.randomSeedWord(4)
        game.startGame(listOf(player1.ifBlank { "PLAYER 1" }, player2.ifBlank { "PLAYER 2" }), seed, variant)
        syncState()
    }

    private fun syncState() {
        _state.update {
            it.copy(
                board = Array(15) { r -> Array(15) { c -> game.board[r][c] } },
                words = game.words.toList(),
                players = game.players.toList(),
                currentPlayer = game.currentPlayer,
                placedThisTurn = game.placedThisTurn.toList(),
                variant = game.variant,
                turnCount = game.turnCount,
                maxTurns = game.maxTurns,
                isGameOver = game.isGameOver,
                isGameStarted = true,
                selectedTile = null,
                selectedTargetWordId = null,
                pendingTargetChoiceIds = emptyList()
            )
        }
        maybeRecordStats()
    }

    private fun maybeRecordStats() {
        if (game.isGameOver && !statsRecorded) {
            statsRecorded = true
            val winner = game.determineWinner()
            viewModelScope.launch { statsRepo.recordResult(winner, stats.value) }
        }
    }

    fun selectTile(index: Int) {
        _state.update { s -> s.copy(selectedTile = if (s.selectedTile == index) null else index) }
    }

    fun onCellClick(row: Int, col: Int) {
        val s = _state.value

        val placed = s.placedThisTurn.find { it.row == row && it.col == col }
        if (placed != null) {
            game.recallTile(row, col)
            refreshBoardAndRack()
            return
        }

        val settled = game.board[row][col]
        if (settled != null) {
            val candidates = game.wordsThroughCell(row, col)
            when (candidates.size) {
                0 -> return
                1 -> selectTargetWord(candidates.first().id)
                else -> _state.update { it.copy(pendingTargetChoiceIds = candidates.map { w -> w.id }) }
            }
            return
        }

        val idx = s.selectedTile ?: return
        val player = game.players.getOrNull(game.currentPlayer) ?: return
        if (idx >= player.rack.size) return
        val letter = player.rack[idx]
        game.placeTile(row, col, letter)
        refreshBoardAndRack()
    }

    /** Called from the disambiguation dialog when the player picks across-vs-down. */
    fun resolveTargetChoice(wordId: String) {
        _state.update { it.copy(pendingTargetChoiceIds = emptyList()) }
        selectTargetWord(wordId)
    }

    fun dismissTargetChoice() {
        _state.update { it.copy(pendingTargetChoiceIds = emptyList()) }
    }

    private fun refreshBoardAndRack() {
        _state.update {
            it.copy(
                board = Array(15) { r -> Array(15) { c -> game.board[r][c] } },
                players = game.players.toList(),
                placedThisTurn = game.placedThisTurn.toList(),
                selectedTile = null
            )
        }
    }

    fun selectTargetWord(wordId: String?) {
        _state.update { it.copy(selectedTargetWordId = if (it.selectedTargetWordId == wordId) null else wordId) }
    }

    /** Preview of what the coloring outcome would be, given the current placement + selected target. */
    fun previewOutcome(): Pair<ColorOutcome, Set<Int>>? {
        val s = _state.value
        if (s.placedThisTurn.isEmpty()) return null
        val targetId = s.selectedTargetWordId ?: return ColorOutcome.NEUTRAL to emptySet()
        val target = game.wordById(targetId) ?: return ColorOutcome.NEUTRAL to emptySet()

        val rows = s.placedThisTurn.map { it.row }.distinct()
        val isHorizontal = rows.size == 1
        val fixedDim = if (isHorizontal) s.placedThisTurn.first().row else s.placedThisTurn.first().col
        val formed = LadderClaimEngine.collectFormedWords(game.board, game.placedThisTurn, isHorizontal)
        val mainWord = formed.firstOrNull {
            it.isHorizontal == isHorizontal && (if (isHorizontal) it.startRow == fixedDim else it.startCol == fixedDim)
        } ?: return null

        return LadderClaimEngine.classify(target.word, mainWord.word, s.variant.fullCreditBar)
    }

    fun recallAllTiles() {
        game.recallAll()
        _state.update { it.copy(players = game.players.toList(), placedThisTurn = emptyList(), selectedTile = null) }
    }

    fun playWord() {
        val targetId = _state.value.selectedTargetWordId
        when (val result = game.playWord(targetId)) {
            is LadderMoveResult.Success -> {
                syncState()
                _state.update { it.copy(lastMessage = messageFor(result), lastOutcome = result.result.outcome) }
            }
            is LadderMoveResult.Error -> _state.update { it.copy(lastMessage = result.message) }
        }
    }

    private fun messageFor(result: LadderMoveResult.Success): String {
        val outcomeText = when (result.result.outcome) {
            ColorOutcome.FULL -> "Full claim!"
            ColorOutcome.PARTIAL -> "Partial claim"
            ColorOutcome.NEUTRAL -> "Neutral play"
        }
        val theft = if (result.result.targetWordConverted) " — target word flipped!" else ""
        return "Played ${result.playedWord} — $outcomeText$theft"
    }

    fun skipTurn() {
        game.skipTurn()
        syncState()
    }

    fun territoryTally(): Map<Int, Int> = game.tallyTerritory()
    fun determineWinner(): Int? = game.determineWinner()
    fun longestChain(playerId: Int): Int = game.longestOwnedChain(playerId)

    fun toggleHowTo() { _showHowTo.value = !_showHowTo.value }
    fun clearMessage() { _state.update { it.copy(lastMessage = "") } }
}