package com.enigma.wordnest.games.absurdauction.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.absurdauction.data.WordRepository
import com.enigma.wordnest.games.absurdauction.model.AuctionGame
import com.enigma.wordnest.games.absurdauction.model.AuctionGameState
import com.enigma.wordnest.games.absurdauction.model.BankerStance
import com.enigma.wordnest.games.lexicon.model.PlayResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuctionViewModel(application: Application) : AndroidViewModel(application) {

    val wordRepo = WordRepository(application)
    private val game = AuctionGame()

    private val _state = MutableStateFlow(AuctionGameState())
    val state: StateFlow<AuctionGameState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTile = MutableStateFlow<Int?>(null)
    val selectedTile: StateFlow<Int?> = _selectedTile.asStateFlow()

    private val _lastMessage = MutableStateFlow("")
    val lastMessage: StateFlow<String> = _lastMessage.asStateFlow()

    private val _showHowTo = MutableStateFlow(false)
    val showHowTo: StateFlow<Boolean> = _showHowTo.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepo.load()
            game.updateDictionary(wordRepo.dictionarySet())
            game.updateLargeDictionary(wordRepo.largeDictionarySet())
            _isLoading.value = false
        }
    }

    fun startGame(player1Name: String, player2Name: String, stance: BankerStance) {
        if (!wordRepo.isLoaded()) return
        game.startGame(player1Name.ifBlank { "PLAYER 1" }, player2Name.ifBlank { "PLAYER 2" }, stance)
        syncState()
    }

    fun toggleHowTo() { _showHowTo.value = !_showHowTo.value }
    fun selectTile(index: Int) {
        _selectedTile.update { current -> if (current == index) null else index }
    }

    fun onCellClick(row: Int, col: Int) {
        val existingPlaced = game.placedThisTurn.find { it.row == row && it.col == col }
        if (existingPlaced != null) {
            game.recallTile(row, col)
            syncState()
            return
        }
        val idx = _selectedTile.value ?: return
        val player = game.players.getOrNull(game.currentPlayer) ?: return
        if (idx >= player.rack.size) return
        val letter = player.rack[idx]
        if (game.placeTile(row, col, letter)) {
            _selectedTile.value = null
            syncState()
        }
    }

    fun recallAllTiles() { game.recallAllTiles(); _selectedTile.value = null; syncState() }
    fun shuffleRack() { game.shuffleRack(); syncState() }

    fun playWord() {
        viewModelScope.launch {
            _state.update { it.copy(isBankerThinking = true) }
            delay(500) // masks Banker evaluation latency, per §2.6
            when (val result = game.playWord()) {
                is PlayResult.Success -> {
                    _lastMessage.value = "Played ${result.words.joinToString(", ")} for +${result.score}"
                }
                is PlayResult.Error -> _lastMessage.value = result.message
            }
            syncState()
        }
    }

    fun skipTurn() {
        game.skipTurn(); syncState()
    }

    fun exchangeTiles() {
        if (!game.exchangeTiles()) _lastMessage.value = "Not enough tiles in the pool"
        syncState()
    }

    fun resetGame() {
        game.reset()
        _state.value = AuctionGameState()
        _lastMessage.value = ""
    }

    fun clearMessage() { _lastMessage.value = "" }

    private fun syncState() {
        _state.update {
            it.copy(
                board = Array(15) { r -> Array(15) { c -> game.board[r][c] } },
                players = game.players.toList(),
                remainingPool = game.pool.toList(),
                currentPlayer = game.currentPlayer,
                bankerStance = game.stance,
                lastDecision = game.lastDecisions.lastOrNull(),
                isBankerThinking = false,
                isGameStarted = true,
                isGameOver = game.isGameOver
            )
        }
    }
}