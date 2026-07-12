package com.enigma.wordnest.games.synthetix.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.wordnest.games.synthetix.model.AiDecision
import com.enigma.wordnest.games.synthetix.model.AiDifficulty
import com.enigma.wordnest.games.synthetix.model.AiMove
import com.enigma.wordnest.games.synthetix.model.AiOpponent
import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.model.TileSetConfig
import com.google.gson.Gson
import com.enigma.wordnest.games.synthetix.data.BoardRepository
import com.enigma.wordnest.games.synthetix.data.TileSetRepository
import com.enigma.wordnest.games.synthetix.data.WordDictionaryManager
import com.enigma.wordnest.games.synthetix.model.SymmetryEngine
import com.enigma.wordnest.games.synthetix.model.SymmetryMode
import com.enigma.wordnest.games.synthetix.model.Tile
import com.enigma.wordnest.games.synthetix.model.PlacedTile
import com.enigma.wordnest.games.synthetix.model.Player
import com.enigma.wordnest.games.synthetix.model.PlayResult
import com.enigma.wordnest.games.synthetix.model.SquareType
import com.enigma.wordnest.games.synthetix.model.SynthetixGame
import com.enigma.wordnest.games.synthetix.network.ChatMessage
import com.enigma.wordnest.games.synthetix.network.HelloPayload
import com.enigma.wordnest.games.synthetix.network.OnlineMultiplayerManager
import com.enigma.wordnest.games.synthetix.network.OnlineRoomMeta
import com.enigma.wordnest.games.synthetix.network.OnlineState
import com.enigma.wordnest.games.synthetix.network.PlaceTilePayload
import com.enigma.wordnest.games.synthetix.network.WifiConnectionState
import com.enigma.wordnest.games.synthetix.network.WifiGameSnapshot
import com.enigma.wordnest.games.synthetix.network.WifiMessage
import com.enigma.wordnest.games.synthetix.network.WifiMsgType
import com.enigma.wordnest.games.synthetix.network.WifiMultiplayerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ─────────────────────────────────────────────────────────────────────────────
//  UI state
// ─────────────────────────────────────────────────────────────────────────────

enum class GameState { MENU, PLAYING, GAME_OVER, BOARD_BUILDER, TILE_SET_EDITOR, THEME_PICKER, MULTIPLAYER_HUB, WIFI_LOBBY, ONLINE_LOBBY }

data class BoardBuilderState(
    val boardName: String = "Custom Board",
    val size: Int = 15,
    val rackSize: Int = 7,
    val bagSize: Int = 100,
    val bingoBonus: Int = 50,
    val anchorRow: Int = 7,
    val anchorCol: Int = 7,
    val grid: List<List<String?>> = List(15) { r ->
        List(15) { c -> if (r == 7 && c == 7) "anchor" else null }
    },
    val selectedSquareType: SquareType = SquareType.TW,
    val symmetryMode: SymmetryMode = SymmetryMode.ROTATE_180
)

data class GameUiState(
    val gameState: GameState = GameState.MENU,
    val currentPlayer: Int = 0,
    val player1: Player = Player("Player 1", 0, emptyList()),
    val player2: Player = Player("Player 2", 0, emptyList()),
    val board: List<List<Tile?>> = List(15) { List(15) { null } },
    val boardConfig: BoardConfig = BoardConfig.blank(),
    val placedThisTurn: List<PlacedTile> = emptyList(),
    val selectedTile: Int? = null,
    val lastPlayMessage: String = "",
    val lastPlayScore: Int = 0,
    val isDictionaryLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val dictionarySize: Int = 0,
    val bagSize: Int = 0,
    val isVsAi: Boolean = false,
    val aiDifficulty: AiDifficulty? = null,
    val isAiThinking: Boolean = false,
    val canResume: Boolean = false,
    val showBlankPicker: Boolean = false,
    val pendingBlankPickerRow: Int = -1,
    val pendingBlankPickerCol: Int = -1,
    val savedBoardNames: List<String> = emptyList(),
    val savedTileSetNames: List<String> = emptyList(),
    val activeTileSet: TileSetConfig = TileSetConfig(),
    val presetBoardNames: List<String> = emptyList(),
    val boardBuilder: BoardBuilderState = BoardBuilderState(),
    // Hints stored locally (earned via ad watch)
    val hintsAvailable: Int = 3,
    val currentHint: AiMove? = null,
    // Ad state
    // Dialogs
    val showHintSourceDialog: Boolean = false,
    val showNoAdAvailableDialog: Boolean = false,

    // Multiplayer State
    val wifiState: WifiConnectionState = WifiConnectionState.Idle,
    val discoveredHosts: List<NsdServiceInfo> = emptyList(),
    val isMultiplayer: Boolean = false,
    val myPlayerIndex: Int = 0, // 0 for host, 1 for guest
    val chatMessages: List<ChatMessage> = emptyList(),
    val myName: String = "Player",
    val onlineState: OnlineState = OnlineState.Idle,
    val openOnlineRooms: List<OnlineRoomMeta> = emptyList()
)

data class SynthetixSaveData(
    val board: List<List<Tile?>>,
    val players: List<Player>,
    val currentPlayer: Int,
    val bag: List<Char>,
    val consecutiveSkips: Int,
    val isVsAi: Boolean,
    val aiDifficulty: AiDifficulty?,
    val boardConfigJson: String,
    val tileSetJson: String,
    val hintsAvailable: Int = 3
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class SynthetixViewModel(
    private val dictionaryManager: WordDictionaryManager,
    private val boardRepo: BoardRepository,
    private val tileSetRepo: TileSetRepository,
    context: Context,
    private val wifiManager: WifiMultiplayerManager,
    private val onlineManager: OnlineMultiplayerManager
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("synthetix_game", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var aiOpponent: AiOpponent? = null
    private val _isVsAi = MutableStateFlow(false)
    private val game = SynthetixGame()

    private var pendingBlankRow = -1
    private var pendingBlankCol = -1
    private var dictionaryLoadJob: Job? = null

    init {
        loadDictionaries()
        observeWifiState()
        observeOnlineState()
    }

    private fun observeWifiState() {
        viewModelScope.launch {
            wifiManager.connectionState.collect { state ->
                _uiState.update { it.copy(wifiState = state) }
                if (state is WifiConnectionState.Connected) {
                    if (wifiManager.isHost) {
                        wifiManager.sendHello(
                            peerName = _uiState.value.myName,
                            playerIndex = 0,
                            boardConfig = game.boardConfig,
                            tileSet = game.tileSet
                        )
                        syncWifiGameState()
                    }
                    _uiState.update { it.copy(gameState = GameState.PLAYING, isMultiplayer = true) }
                }
            }
        }
        viewModelScope.launch {
            wifiManager.discoveredHosts.collect { hosts ->
                _uiState.update { it.copy(discoveredHosts = hosts) }
            }
        }
        viewModelScope.launch {
            wifiManager.incomingMessages.collect { msg ->
                msg?.let { handleWifiMessage(it) }
            }
        }
    }

    private fun observeOnlineState() {
        viewModelScope.launch {
            onlineManager.onlineState.collect { state ->
                _uiState.update { it.copy(onlineState = state) }
            }
        }
        viewModelScope.launch {
            onlineManager.openRooms.collect { rooms ->
                _uiState.update { it.copy(openOnlineRooms = rooms) }
            }
        }
        viewModelScope.launch {
            onlineManager.chatMessages.collect { messages ->
                _uiState.update { it.copy(chatMessages = messages) }
            }
        }
    }

    private fun handleWifiMessage(msg: WifiMessage) {
        when (msg.type) {
            WifiMsgType.HELLO -> {
                val payload = gson.fromJson(msg.payload, HelloPayload::class.java)
                val boardCfg = gson.fromJson(payload.boardConfigJson, BoardConfig::class.java)
                val tileSetCfg = gson.fromJson(payload.tileSetJson, TileSetConfig::class.java)
                game.configure(boardCfg, tileSetCfg)
                _uiState.update {
                    it.copy(
                        myPlayerIndex = 1,
                        boardConfig = boardCfg,
                        activeTileSet = tileSetCfg
                    )
                }
            }
            WifiMsgType.GAME_STATE -> {
                val snapshot = gson.fromJson(msg.payload, WifiGameSnapshot::class.java)
                game.restoreBoard(snapshot.board)
                game.restorePlayers(snapshot.players)
                game.restoreBag(snapshot.bag)
                game.restoreCurrentPlayer(snapshot.currentPlayer)
                game.restoreConsecutiveSkips(snapshot.consecutiveSkips)
                _uiState.update {
                    it.copy(
                        lastPlayMessage = snapshot.lastPlayMessage,
                        lastPlayScore = snapshot.lastPlayScore
                    )
                }
                updateStateFromGame()
            }
            WifiMsgType.PLACE_TILE -> {
                if (wifiManager.isHost) {
                    val p = gson.fromJson(msg.payload, PlaceTilePayload::class.java)
                    game.placeTile(p.row, p.col, p.letter, p.blankAs)
                    updateStateFromGame()
                    syncWifiGameState()
                }
            }
            WifiMsgType.PLAY_WORD -> {
                if (wifiManager.isHost) {
                    playWord()
                }
            }
            WifiMsgType.RECALL_ALL -> {
                if (wifiManager.isHost) {
                    recallAllTiles()
                    syncWifiGameState()
                }
            }
            WifiMsgType.SKIP -> {
                if (wifiManager.isHost) {
                    skipTurn()
                    syncWifiGameState()
                }
            }
            WifiMsgType.EXCHANGE -> {
                if (wifiManager.isHost) {
                    exchangeTiles()
                    syncWifiGameState()
                }
            }
            WifiMsgType.SHUFFLE -> {
                if (wifiManager.isHost) {
                    shuffleRack()
                    syncWifiGameState()
                }
            }
            WifiMsgType.CHAT -> {
                val sender = if (wifiManager.isHost) _uiState.value.player2.name else _uiState.value.player1.name
                val chatMsg = ChatMessage(
                    sender = sender,
                    text = msg.payload,
                    timestamp = System.currentTimeMillis()
                )
                _uiState.update { it.copy(chatMessages = it.chatMessages + chatMsg) }
            }
            else -> {}
        }
    }

    private fun syncWifiGameState() {
        if (!wifiManager.isHost) return
        val snapshot = WifiGameSnapshot(
            board = game.board.map { it.toList() },
            players = game.players.toList(),
            currentPlayer = game.currentPlayer,
            bag = game.bag.toList(),
            consecutiveSkips = game.consecutiveSkips,
            placedThisTurn = game.placedThisTurn.toList(),
            isGameOver = game.isGameOver,
            lastPlayMessage = _uiState.value.lastPlayMessage,
            lastPlayScore = _uiState.value.lastPlayScore
        )
        wifiManager.sendGameState(snapshot)
    }

    // ── Observe AdManager flows ───────────────────────────────────────────────



    // ── Dictionary loading ────────────────────────────────────────────────────

    private fun loadDictionaries() {
        dictionaryLoadJob?.cancel()
        dictionaryLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dictionaryManager.load()
            dictionaryManager.loadLargeDictionary()

            game.updateDictionary(dictionaryManager.dictionary.value)
            game.updateLargeDictionary(dictionaryManager.largeDictionary.value)

            val savedBoard = boardRepo.loadActiveBoard()
            game.configure(savedBoard, TileSetConfig())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isDictionaryLoaded = dictionaryManager.dictionary.value.isNotEmpty(),
                    dictionarySize = dictionaryManager.dictionary.value.size,
                    errorMessage = dictionaryManager.error.value,
                    canResume = prefs.contains("game_state"),
                    boardConfig = savedBoard,
                    savedBoardNames = boardRepo.savedBoardNames(),
                    savedTileSetNames = tileSetRepo.savedNames(),
                    presetBoardNames = boardRepo.presetBoards.map { p -> p.displayName }
                )
            }
        }
    }

    // ── Board config management ───────────────────────────────────────────────

    fun selectBoard(config: BoardConfig) {
        game.configure(config)
        boardRepo.saveActiveBoard(config)
        _uiState.update {
            it.copy(
                boardConfig = config,
                activeTileSet = TileSetConfig(bagSize = config.bag, bingoBonus = config.bingo)
            )
        }
    }

    fun loadPresetBoard(displayName: String) {
        val config = boardRepo.loadPresetByName(displayName) ?: return
        selectBoard(config)
    }

    fun importBoard(uri: Uri): Boolean {
        val config = boardRepo.importFromUri(uri) ?: return false
        boardRepo.saveBoard(config)
        selectBoard(config)
        _uiState.update { it.copy(savedBoardNames = boardRepo.savedBoardNames()) }
        return true
    }

    fun exportBoard(uri: Uri): Boolean =
        boardRepo.exportToUri(_uiState.value.boardConfig, uri)

    fun exportBoardJson(): String = boardRepo.exportToJson(_uiState.value.boardConfig)

    fun saveBoard(config: BoardConfig) {
        boardRepo.saveBoard(config)
        _uiState.update { it.copy(savedBoardNames = boardRepo.savedBoardNames()) }
    }

    fun loadSavedBoard(name: String) {
        boardRepo.loadBoard(name)?.let { selectBoard(it) }
    }

    fun deleteSavedBoard(name: String) {
        boardRepo.deleteBoard(name)
        _uiState.update { it.copy(savedBoardNames = boardRepo.savedBoardNames()) }
    }

    // ── Board builder ─────────────────────────────────────────────────────────

    fun updateBoardBuilder(update: BoardBuilderState.() -> BoardBuilderState) {
        _uiState.update { it.copy(boardBuilder = it.boardBuilder.update()) }
    }

    fun resizeBoardBuilder(newSize: Int) {
        val centre = newSize / 2
        val newGrid = List(newSize) { r ->
            List(newSize) { c -> if (r == centre && c == centre) "anchor" else null }
        }
        _uiState.update {
            it.copy(
                boardBuilder = it.boardBuilder.copy(
                    size = newSize, grid = newGrid,
                    anchorRow = centre, anchorCol = centre
                )
            )
        }
    }

    fun toggleBuilderCell(r: Int, c: Int) {
        val builder = _uiState.value.boardBuilder
        val newGrid = builder.grid.map { it.toMutableList() }.toMutableList()

        if (builder.selectedSquareType == SquareType.ANCHOR) {
            for (gr in newGrid.indices) for (gc in newGrid[gr].indices)
                if (newGrid[gr][gc] == "anchor") newGrid[gr][gc] = null
            newGrid[r][c] = "anchor"
            _uiState.update {
                it.copy(boardBuilder = builder.copy(
                    grid = newGrid.map { it.toList() }, anchorRow = r, anchorCol = c
                ))
            }
        } else {
            val current = SquareType.fromCode(newGrid[r][c])
            val newCode = if (current == builder.selectedSquareType) null
            else builder.selectedSquareType.code.ifEmpty { null }

            val symmetryGrid = SymmetryEngine.applyWithSymmetry(
                grid = builder.grid, row = r, col = c, code = newCode,
                size = builder.size, mode = builder.symmetryMode,
                anchorRow = builder.anchorRow, anchorCol = builder.anchorCol,
                selectedSquareType = builder.selectedSquareType
            )
            _uiState.update { it.copy(boardBuilder = builder.copy(grid = symmetryGrid)) }
        }
    }

    fun applySymmetryToBoard() {
        val builder = _uiState.value.boardBuilder
        val newGrid = SymmetryEngine.reSymmetrize(
            grid = builder.grid, size = builder.size, mode = builder.symmetryMode,
            anchorRow = builder.anchorRow, anchorCol = builder.anchorCol
        )
        updateBoardBuilder { copy(grid = newGrid) }
    }

    fun clearBuilderBoard() {
        val builder = _uiState.value.boardBuilder
        val centre = builder.size / 2
        val newGrid = List(builder.size) { r ->
            List(builder.size) { c -> if (r == centre && c == centre) "anchor" else null }
        }
        updateBoardBuilder { copy(grid = newGrid, anchorRow = centre, anchorCol = centre) }
    }

    fun buildConfigFromBuilder(): BoardConfig {
        val b = _uiState.value.boardBuilder
        return BoardConfig(
            name = b.boardName, size = b.size, rack = b.rackSize,
            bag = b.bagSize, bingo = b.bingoBonus,
            anchor = "${b.anchorRow},${b.anchorCol}", grid = b.grid
        )
    }

    // ── Tile-set management ───────────────────────────────────────────────────

    fun saveTileSet(config: TileSetConfig) {
        tileSetRepo.save(config)
        game.configure(game.boardConfig, config)
        _uiState.update { it.copy(activeTileSet = config, savedTileSetNames = tileSetRepo.savedNames()) }
    }

    fun loadTileSet(name: String) {
        tileSetRepo.load(name)?.let { ts ->
            game.configure(game.boardConfig, ts)
            _uiState.update { it.copy(activeTileSet = ts) }
        }
    }

    // ── Screen navigation ─────────────────────────────────────────────────────

    fun openBoardBuilder()   { _uiState.update { it.copy(gameState = GameState.BOARD_BUILDER) } }
    fun openTileSetEditor()  { _uiState.update { it.copy(gameState = GameState.TILE_SET_EDITOR) } }
    fun openThemePicker()    { _uiState.update { it.copy(gameState = GameState.THEME_PICKER) } }
    fun openMultiplayerHub() { _uiState.update { it.copy(gameState = GameState.MULTIPLAYER_HUB) } }
    fun openWifiLobby()      { _uiState.update { it.copy(gameState = GameState.WIFI_LOBBY) } }
    fun openOnlineLobby() {
        _uiState.update { it.copy(gameState = GameState.ONLINE_LOBBY) }
        onlineManager.listenForOpenRooms()
    }

    fun startWifiHost(playerName: String) {
        _uiState.update { it.copy(myName = playerName, myPlayerIndex = 0) }
        game.startGame(playerName, "GUEST")
        wifiManager.startHosting(playerName)
    }

    fun scanWifiGames() {
        wifiManager.startDiscovery()
    }

    fun connectToWifiGame(info: NsdServiceInfo, playerName: String) {
        _uiState.update { it.copy(myName = playerName, myPlayerIndex = 1) }
        wifiManager.connectToHost(info)
    }

    fun sendChat(text: String) {
        val myName = _uiState.value.myName
        val msg = ChatMessage(sender = myName, text = text, timestamp = System.currentTimeMillis())
        _uiState.update { it.copy(chatMessages = it.chatMessages + msg) }
        if (_uiState.value.isMultiplayer) {
            wifiManager.sendChat(text)
        }
    }

    fun backToMenu() {
        wifiManager.disconnect()
        onlineManager.leaveRoom()
        _uiState.update {
            it.copy(
                gameState = GameState.MENU,
                canResume = prefs.contains("game_state"),
                isMultiplayer = false,
                chatMessages = emptyList()
            )
        }
    }

    // ── Game start ────────────────────────────────────────────────────────────

    fun startVsAi(player1Name: String, difficulty: AiDifficulty) {
        if (!_uiState.value.isDictionaryLoaded) return
        _isVsAi.value = true
        aiOpponent = AiOpponent(
            fullDictionary = dictionaryManager.dictionary.value,
            validationDictionary = dictionaryManager.largeDictionary.value,
            difficulty = difficulty
        )
        game.startGame(player1Name, difficulty.displayName)
        _uiState.update { it.copy(gameState = GameState.PLAYING, isVsAi = true, aiDifficulty = difficulty) }
        updateStateFromGame()
        saveGame()
    }

    fun startGame(player1Name: String, player2Name: String) {
        if (!_uiState.value.isDictionaryLoaded) return
        _isVsAi.value = false
        aiOpponent = null
        game.startGame(player1Name, player2Name)
        _uiState.update { it.copy(gameState = GameState.PLAYING, isVsAi = false, aiDifficulty = null) }
        updateStateFromGame()
        saveGame()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun saveGame() {
        val boardSnapshot = game.board.map { row -> row.toList() }
        val save = SynthetixSaveData(
            board = boardSnapshot, players = game.players.toList(),
            currentPlayer = game.currentPlayer, bag = game.bag.toList(),
            consecutiveSkips = game.consecutiveSkips, isVsAi = _isVsAi.value,
            aiDifficulty = _uiState.value.aiDifficulty,
            boardConfigJson = gson.toJson(game.boardConfig),
            tileSetJson = gson.toJson(game.tileSet),
            hintsAvailable = _uiState.value.hintsAvailable
        )
        prefs.edit().putString("game_state", gson.toJson(save)).apply()
        _uiState.update { it.copy(canResume = true) }
    }

    private fun clearSave() {
        prefs.edit().remove("game_state").apply()
        _uiState.update { it.copy(canResume = false) }
    }

    fun resumeGame() {
        val json = prefs.getString("game_state", null) ?: return
        try {
            val save: SynthetixSaveData = gson.fromJson(json, SynthetixSaveData::class.java)
            val boardCfg   = gson.fromJson(save.boardConfigJson, BoardConfig::class.java)
            val tileSetCfg = gson.fromJson(save.tileSetJson, TileSetConfig::class.java)

            game.reset()
            game.configure(boardCfg, tileSetCfg)
            game.restoreBoard(save.board)
            game.restorePlayers(save.players)
            game.restoreBag(save.bag)
            game.restoreCurrentPlayer(save.currentPlayer)
            game.restoreConsecutiveSkips(save.consecutiveSkips)

            _isVsAi.value = save.isVsAi
            aiOpponent = if (save.isVsAi && save.aiDifficulty != null)
                AiOpponent(
                    fullDictionary = dictionaryManager.dictionary.value,
                    validationDictionary = dictionaryManager.largeDictionary.value,
                    difficulty = save.aiDifficulty
                ) else null

            _uiState.update {
                it.copy(
                    gameState = GameState.PLAYING, isVsAi = save.isVsAi,
                    aiDifficulty = save.aiDifficulty, boardConfig = boardCfg,
                    canResume = true, hintsAvailable = save.hintsAvailable
                )
            }
            updateStateFromGame()
            if (save.isVsAi && game.currentPlayer == 1) maybeRunAiTurn()
        } catch (e: Exception) {
            Log.e("SynthetixViewModel", "Failed to resume game", e)
            clearSave()
        }
    }

    fun retryLoadingDictionary() { loadDictionaries() }

    // ── AI turn ───────────────────────────────────────────────────────────────

    private fun maybeRunAiTurn() {
        val ai = aiOpponent ?: return
        if (!_isVsAi.value) return
        if (game.currentPlayer != 1) return
        if (game.isGameOver) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }
            delay(800)
            val decision = withContext(Dispatchers.Default) {
                ai.decideMove(
                    board = game.board, rack = game.players[1].rack,
                    bagSize = game.getBagSize(), boardConfig = game.boardConfig,
                    tileSet = game.tileSet
                )
            }
            when (decision) {
                is AiDecision.PlayWord -> {
                    decision.move.tiles.forEach { t ->
                        game.placeTile(t.row, t.col, if (t.isBlank) '?' else t.letter,
                            if (t.isBlank) t.letter else null)
                    }
                    when (val result = game.playWord()) {
                        is PlayResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    lastPlayMessage = "${ai.difficulty.displayName} played ${result.words.joinToString(", ")} for +${result.score}",
                                    lastPlayScore = result.score, isAiThinking = false
                                )
                            }
                            saveGame()
                        }
                        is PlayResult.Error -> {
                            game.recallAllTiles(); game.skipTurn()
                            _uiState.update { it.copy(lastPlayMessage = "${ai.difficulty.displayName} skipped (invalid play)", isAiThinking = false) }
                            saveGame()
                        }
                    }
                }
                is AiDecision.ExchangeTiles -> {
                    game.exchangeTiles()
                    _uiState.update { it.copy(lastPlayMessage = "${ai.difficulty.displayName} exchanged tiles", isAiThinking = false) }
                    saveGame()
                }
                is AiDecision.Skip -> {
                    game.skipTurn()
                    _uiState.update { it.copy(lastPlayMessage = "${ai.difficulty.displayName} skipped", isAiThinking = false) }
                    saveGame()
                }
            }
            updateStateFromGame()
        }
    }

    // ── Game actions ──────────────────────────────────────────────────────────

    private fun updateStateFromGame() {
        val boardSnapshot = game.board.map { row -> row.toList() }
        _uiState.update {
            it.copy(
                gameState = if (game.isGameOver) GameState.GAME_OVER else GameState.PLAYING,
                currentPlayer = game.currentPlayer,
                player1 = game.players[0].copy(), player2 = game.players[1].copy(),
                board = boardSnapshot, boardConfig = game.boardConfig,
                placedThisTurn = game.placedThisTurn.toList(),
                bagSize = game.getBagSize()
            )
        }
    }

    fun selectTile(rackIndex: Int) {
        _uiState.update { state ->
            state.copy(selectedTile = if (state.selectedTile == rackIndex) null else rackIndex)
        }
    }

    fun placeTile(row: Int, col: Int) {
        val selectedIdx = _uiState.value.selectedTile ?: return
        val player = game.players[game.currentPlayer]
        if (selectedIdx >= player.rack.size) return
        val letter = player.rack[selectedIdx]

        if (uiState.value.isMultiplayer && uiState.value.currentPlayer != uiState.value.myPlayerIndex) return

        if (letter == '?') {
            pendingBlankRow = row; pendingBlankCol = col
            _uiState.update {
                it.copy(pendingBlankPickerRow = row, pendingBlankPickerCol = col, showBlankPicker = true)
            }
        } else {
            if (uiState.value.isMultiplayer && !wifiManager.isHost) {
                wifiManager.sendPlaceTile(row, col, letter, null)
            } else {
                if (game.placeTile(row, col, letter)) {
                    _uiState.update { it.copy(selectedTile = null, lastPlayMessage = "") }
                    updateStateFromGame()
                    if (wifiManager.isHost) syncWifiGameState()
                }
            }
        }
    }

    fun onCellClick(row: Int, col: Int) {
        val state = _uiState.value
        if (state.placedThisTurn.any { it.row == row && it.col == col }) {
            game.recallTile(row, col); updateStateFromGame()
        } else {
            placeTile(row, col)
        }
    }

    fun confirmBlankLetter(chosenLetter: Char) {
        val row = pendingBlankRow; val col = pendingBlankCol
        if (row < 0 || col < 0) return
        val safeChosen = chosenLetter.uppercaseChar().takeIf { it in 'A'..'Z' } ?: return

        if (uiState.value.isMultiplayer && !wifiManager.isHost) {
            wifiManager.sendPlaceTile(row, col, '?', safeChosen)
        } else {
            game.placeTile(row, col, '?', safeChosen)
            if (wifiManager.isHost) syncWifiGameState()
        }

        pendingBlankRow = -1; pendingBlankCol = -1
        _uiState.update {
            it.copy(
                selectedTile = null, showBlankPicker = false,
                pendingBlankPickerRow = -1, pendingBlankPickerCol = -1
            )
        }
        updateStateFromGame()
    }

    fun cancelBlankPicker() {
        pendingBlankRow = -1; pendingBlankCol = -1
        _uiState.update {
            it.copy(showBlankPicker = false, pendingBlankPickerRow = -1, pendingBlankPickerCol = -1)
        }
    }

    fun recallAllTiles() {
        if (uiState.value.isMultiplayer && uiState.value.currentPlayer != uiState.value.myPlayerIndex) return
        if (uiState.value.isMultiplayer && !wifiManager.isHost) {
            wifiManager.sendRecallAll()
        } else {
            game.recallAllTiles()
            _uiState.update { it.copy(selectedTile = null, currentHint = null) }
            updateStateFromGame()
            if (wifiManager.isHost) syncWifiGameState()
        }
    }

    fun playWord() {
        if (uiState.value.isMultiplayer && uiState.value.currentPlayer != uiState.value.myPlayerIndex) return
        if (uiState.value.isMultiplayer && !wifiManager.isHost) {
            wifiManager.sendPlayWord()
            return
        }
        clearHint()
        when (val result = game.playWord()) {
            is PlayResult.Success -> {
                updateStateFromGame()
                _uiState.update {
                    it.copy(
                        lastPlayMessage = "Played ${result.words.joinToString(", ")} for +${result.score}",
                        lastPlayScore = result.score, selectedTile = null
                    )
                }
                if (wifiManager.isHost) syncWifiGameState()
                if (game.isGameOver) clearSave() else { saveGame(); maybeRunAiTurn() }
            }
            is PlayResult.Error -> {
                _uiState.update {
                    it.copy(lastPlayMessage = result.message, lastPlayScore = 0)
                }
                if (wifiManager.isHost) syncWifiGameState()
            }
        }
    }

    fun skipTurn() {
        if (uiState.value.isMultiplayer && uiState.value.currentPlayer != uiState.value.myPlayerIndex) return
        if (uiState.value.isMultiplayer && !wifiManager.isHost) {
            wifiManager.sendSkip()
            return
        }
        clearHint(); game.skipTurn(); updateStateFromGame()
        if (wifiManager.isHost) syncWifiGameState()
        if (game.isGameOver) clearSave() else { saveGame(); maybeRunAiTurn() }
    }

    fun exchangeTiles() {
        if (uiState.value.isMultiplayer && uiState.value.currentPlayer != uiState.value.myPlayerIndex) return
        if (uiState.value.isMultiplayer && !wifiManager.isHost) {
            wifiManager.sendExchange()
            return
        }
        clearHint()
        if (game.exchangeTiles()) {
            updateStateFromGame(); saveGame(); maybeRunAiTurn()
            if (wifiManager.isHost) syncWifiGameState()
        } else {
            _uiState.update { it.copy(lastPlayMessage = "Not enough tiles in bag to exchange") }
        }
    }

    fun shuffleRack() {
        if (uiState.value.isMultiplayer && uiState.value.currentPlayer != uiState.value.myPlayerIndex) return
        if (uiState.value.isMultiplayer && !wifiManager.isHost) {
            wifiManager.sendShuffle()
        } else {
            game.shuffleRack(); updateStateFromGame()
            if (wifiManager.isHost) syncWifiGameState()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hint System (ad-aware)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Entry point when the user taps the HINT button.
     *
     * Flow:
     *  1. If stored hints > 0 → use one immediately and place tiles on board
     *  2. Else → show HintSourceDialog (watch ad to earn a hint)
     */
    fun requestHint() {
        val state = _uiState.value
        if (state.isVsAi && game.currentPlayer != 0) return  // not human's turn

        if (state.hintsAvailable > 0) {
            executeHint()
        } else {
            _uiState.update { it.copy(showHintSourceDialog = true) }
        }
    }

    /** Called after the player successfully watches a rewarded ad. */
    fun onRewardedAdComplete(hintsEarned: Int) {
        _uiState.update {
            it.copy(
                hintsAvailable = it.hintsAvailable + hintsEarned,
                showHintSourceDialog = false,
                lastPlayMessage = "🎉 +$hintsEarned hint earned! Tap HINT to use it."
            )
        }
        saveGame()
    }

    /** Called when the rewarded ad fails to load or show. */
    fun onRewardedAdFailed() {
        _uiState.update {
            it.copy(
                showHintSourceDialog = false,
                showNoAdAvailableDialog = true
            )
        }
    }

    fun dismissHintSourceDialog()  { _uiState.update { it.copy(showHintSourceDialog = false) } }
    fun dismissNoAdDialog()        { _uiState.update { it.copy(showNoAdAvailableDialog = false) } }

    /**
     * Finds the best available move, recalls any tiles the player has staged,
     * then places the hint tiles onto the board as a staged (uncommitted) play.
     * The player can tap PLAY to confirm or CLR to reject and rearrange freely.
     */
    private fun executeHint() {
        if (game.currentPlayer != 0 && _uiState.value.isVsAi) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val bestMove = withContext(Dispatchers.Default) {
                val hintAi = aiOpponent ?: AiOpponent(
                    fullDictionary = dictionaryManager.dictionary.value,
                    validationDictionary = dictionaryManager.largeDictionary.value,
                    difficulty = AiDifficulty.EXPERT
                )
                hintAi.findBestMove(
                    board = game.board,
                    rack = game.players[game.currentPlayer].rack,
                    boardConfig = game.boardConfig,
                    tileSet = game.tileSet
                )
            }

            if (bestMove == null) {
                _uiState.update {
                    it.copy(isLoading = false, lastPlayMessage = "No moves found!")
                }
                return@launch
            }

            // Clear any tiles the player had already staged before applying hint
            game.recallAllTiles()

            // Place each hint tile onto the board as a staged (not yet committed) play
            for (tile in bestMove.tiles) {
                game.placeTile(
                    row     = tile.row,
                    col     = tile.col,
                    letter  = if (tile.isBlank) '?' else tile.letter,
                    blankAs = if (tile.isBlank) tile.letter else null
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentHint = bestMove,
                    selectedTile = null,
                    hintsAvailable = (it.hintsAvailable - 1).coerceAtLeast(0),
                    lastPlayMessage = "Hint: ${bestMove.word} (${bestMove.score} pts) — tap PLAY to confirm"
                )
            }
            updateStateFromGame()
            saveGame()
        }
    }

    fun clearHint() { _uiState.update { it.copy(currentHint = null) } }

    fun resetGame() {
        game.reset(); clearSave()
        _uiState.update {
            GameUiState(
                isDictionaryLoaded = it.isDictionaryLoaded,
                dictionarySize = it.dictionarySize,
                gameState = GameState.MENU,
                boardConfig = it.boardConfig,
                savedBoardNames = it.savedBoardNames,
                savedTileSetNames = it.savedTileSetNames,
                presetBoardNames = it.presetBoardNames,
                 // preserve ad state across resets
            )
        }
    }

    fun clearMessage() { _uiState.update { it.copy(lastPlayMessage = "", lastPlayScore = 0) } }
}
