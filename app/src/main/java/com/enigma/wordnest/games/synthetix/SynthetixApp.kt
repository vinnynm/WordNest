package com.enigma.wordnest.games.synthetix

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enigma.wordnest.games.synthetix.model.AiDifficulty
import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.ui.GameState
import com.enigma.wordnest.games.synthetix.ui.GameUiState
import com.enigma.wordnest.games.synthetix.ui.SynthetixViewModel
import com.enigma.wordnest.games.synthetix.ui.components.Board
import com.enigma.wordnest.games.synthetix.ui.components.ThemedPlayerScoreCard
import com.enigma.wordnest.games.synthetix.ui.components.ThemedRack
import com.enigma.wordnest.games.synthetix.ui.components.ThemedTilePiece
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme
import com.enigma.wordnest.games.synthetix.ui.theme.SynthetixTheme
import com.enigma.wordnest.games.synthetix.ui.theme.ThemeManager
import com.enigma.wordnest.games.synthetix.ui.screens.BoardBuilderScreen
import com.enigma.wordnest.games.synthetix.ui.screens.ChatPanel
import com.enigma.wordnest.games.synthetix.ui.screens.MultiplayerHubScreen
import com.enigma.wordnest.games.synthetix.ui.screens.OnlineLobbyScreen
import com.enigma.wordnest.games.synthetix.ui.screens.ThemePickerScreen
import com.enigma.wordnest.games.synthetix.ui.screens.TileSetEditorScreen
import com.enigma.wordnest.games.synthetix.ui.screens.WifiLobbyScreen
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Root composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthetixApp(
    viewModel: SynthetixViewModel,
    themeManager: ThemeManager,
    activity: Activity
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val theme   by themeManager.activeTheme.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.lastPlayMessage) {
        if (uiState.lastPlayMessage.isNotEmpty()) { delay(2500); viewModel.clearMessage() }
    }

    SynthetixTheme {
        when (uiState.gameState) {
            GameState.BOARD_BUILDER -> BoardBuilderScreen(
                viewModel,
                uiState = uiState,
                theme = theme,
                onBack = { viewModel.backToMenu() })
            GameState.TILE_SET_EDITOR -> TileSetEditorScreen(
                viewModel,
                uiState,
                onBack = { viewModel.backToMenu() })
            GameState.THEME_PICKER -> ThemePickerScreen(
                themeManager,
                onBack = { viewModel.backToMenu() })
            else -> Scaffold(
                topBar = {
                    if (uiState.gameState != GameState.MENU) {
                        TopAppBar(
                            title = {
                                Text("SYNTHETIX", fontSize = 20.sp, fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp, color = theme.appBarContent)
                            },
                            navigationIcon = {
                                if (uiState.gameState == GameState.PLAYING) {
                                    IconButton(onClick = { viewModel.resetGame() }) {
                                        Icon(Icons.Default.Home, "Menu", tint = theme.appBarContent)
                                    }
                                }
                            },
                            actions = {
                                if (uiState.isDictionaryLoaded) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = theme.primaryAccent.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            "${uiState.dictionarySize} words",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = theme.primaryAccent
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.appBarBackground)
                        )
                    }
                },
                containerColor = theme.surfaceBackground
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    when (uiState.gameState) {
                        GameState.MENU      -> MenuScreen(viewModel, uiState, theme, themeManager)
                        GameState.MULTIPLAYER_HUB -> MultiplayerHubScreen(
                            theme = theme,
                            onOnlineSelected = { viewModel.openOnlineLobby() },
                            onWifiSelected = { viewModel.openWifiLobby() },
                            onBack = { viewModel.backToMenu() }
                        )
                        GameState.WIFI_LOBBY -> WifiLobbyScreen(
                            theme = theme,
                            connectionState = uiState.wifiState,
                            discoveredHosts = uiState.discoveredHosts,
                            onHost = { name -> viewModel.startWifiHost(name) },
                            onScan = { viewModel.scanWifiGames() },
                            onConnect = { info ->
                                viewModel.connectToWifiGame(
                                    info,
                                    uiState.myName
                                )
                            },
                            onBack = { viewModel.backToMenu() }
                        )
                        GameState.ONLINE_LOBBY -> OnlineLobbyScreen(
                            theme = theme,
                            onlineState = uiState.onlineState,
                            openRooms = uiState.openOnlineRooms,
                            onCreateRoom = { /* viewModel.createOnlineRoom(it) */ },
                            onJoinRoom = { name, code -> /* viewModel.joinOnlineRoom(name, code) */ },
                            onRefreshRooms = { /* viewModel.refreshOnlineRooms() */ },
                            onBack = { viewModel.backToMenu() }
                        )
                        GameState.PLAYING   -> GameScreen(viewModel, uiState, theme,  activity)
                        GameState.GAME_OVER -> GameOverScreen(viewModel, uiState, theme)
                        else -> {}
                    }

                    // Message snackbar
                    if (uiState.lastPlayMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).animateContentSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.lastPlayScore > 0) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Text(uiState.lastPlayMessage, modifier = Modifier.padding(12.dp, 8.dp), fontSize = 14.sp)
                        }
                    }
                }

                // ── Blank tile picker ──────────────────────────────────────
                if (uiState.showBlankPicker) {
                    BlankTilePickerDialog(
                        theme          = theme,
                        onLetterChosen = { viewModel.confirmBlankLetter(it) },
                        onDismiss      = { viewModel.cancelBlankPicker() }
                    )
                }


            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Blank tile picker (themed) — unchanged from original
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BlankTilePickerDialog(theme: BoardTheme, onLetterChosen: (Char) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = theme.surfaceCard,
        title = { Text("Choose a letter", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = theme.onSurface) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(('A'..'Z').toList()) { letter ->
                    ThemedTilePiece(letter = letter, points = 0, isSelected = false,
                        onClick = { onLetterChosen(letter) }, theme = theme, sizeOverride = 38.dp)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = theme.primaryAccent) } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Menu Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MenuScreen(
    viewModel: SynthetixViewModel,
    uiState: GameUiState,
    theme: BoardTheme,
    themeManager: ThemeManager
) {
    var player1Name        by remember { mutableStateOf("PLAYER 1") }
    var player2Name        by remember { mutableStateOf("PLAYER 2") }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var vsAiMode           by remember { mutableStateOf(true) }
    var showBoardPicker    by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBoard(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.surfaceBackground)) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(theme.primaryAccent.copy(0.08f), Color.Transparent),
                    radius = 800f, center = Offset(0.5f, 0.3f)
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(theme.emoji, fontSize = 40.sp)
            Text("SYNTHETIX", fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = theme.primaryAccent)
            Text("WORD STRATEGY GAME", fontSize = 10.sp, letterSpacing = 4.sp, color = theme.onSurfaceMuted, modifier = Modifier.padding(bottom = 32.dp))

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = theme.surfaceCard, tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("vs AI", "2 Players").forEachIndexed { i, label ->
                            FilterChip(selected = vsAiMode == (i == 0), onClick = { vsAiMode = i == 0 }, label = { Text(label) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (!vsAiMode) {
                        OutlinedTextField(value = player1Name, onValueChange = { player1Name = it.uppercase() }, label = { Text("Player 1 Name", color = theme.onSurfaceMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = player2Name, onValueChange = { player2Name = it.uppercase() }, label = { Text("Player 2 Name", color = theme.onSurfaceMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AiDifficulty.entries.forEach { d ->
                                FilterChip(selected = selectedDifficulty == d, onClick = { selectedDifficulty = d }, label = { Text(d.displayName, fontSize = 11.sp) })
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Board: ${uiState.boardConfig.name} (${uiState.boardConfig.size}×${uiState.boardConfig.size})", fontSize = 11.sp, color = theme.primaryAccent, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { showBoardPicker = true }) { Text("Change", fontSize = 11.sp, color = theme.primaryAccent) }
                    }

                    Spacer(Modifier.height(8.dp))
                    if (uiState.canResume) {
                        Button(onClick = { viewModel.resumeGame() }, modifier = Modifier.fillMaxWidth()) { Text("▶  RESUME GAME") }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { if (vsAiMode) viewModel.startVsAi(player1Name, selectedDifficulty) else viewModel.startGame(player1Name, player2Name) }, modifier = Modifier.fillMaxWidth(), enabled = uiState.isDictionaryLoaded && !uiState.isLoading) { Text("NEW GAME") }
                    } else {
                        Button(onClick = { if (vsAiMode) viewModel.startVsAi(player1Name, selectedDifficulty) else viewModel.startGame(player1Name, player2Name) }, modifier = Modifier.fillMaxWidth(), enabled = uiState.isDictionaryLoaded && !uiState.isLoading) { Text("START GAME") }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.openBoardBuilder() }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.GridOn, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Boards", fontSize = 11.sp) }
                OutlinedButton(onClick = { viewModel.openTileSetEditor() }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Tune, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Tiles", fontSize = 11.sp) }
                OutlinedButton(onClick = { viewModel.openThemePicker() }, modifier = Modifier.weight(1f)) { Text("${themeManager.current.emoji} Theme", fontSize = 11.sp) }
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Icon(Icons.Default.Upload, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Import Board JSON", fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.openMultiplayerHub() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent)
            ) {
                Icon(Icons.Default.People, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("MULTIPLAYER")
            }
        }
    }

    if (showBoardPicker) {
        BoardPickerDialog(
            theme = theme, savedNames = uiState.savedBoardNames, presetNames = uiState.presetBoardNames,
            onSelectPreset = { name -> viewModel.loadPresetBoard(name); showBoardPicker = false },
            onSelectSaved  = { name -> viewModel.loadSavedBoard(name);  showBoardPicker = false },
            onSelectBlank  = { viewModel.selectBoard(BoardConfig.blank(15, "Standard 15×15")); showBoardPicker = false },
            onDelete       = { name -> viewModel.deleteSavedBoard(name) },
            onDismiss      = { showBoardPicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Game Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameScreen(
    viewModel: SynthetixViewModel,
    uiState: GameUiState,
    theme: BoardTheme,
    activity: Activity
) {
    val configuration = LocalConfiguration.current
    val boardSize = uiState.boardConfig.size
    val cellSize  = ((configuration.screenWidthDp - 16) / boardSize.toFloat()).dp.coerceIn(14.dp, 36.dp)

    Column(
        modifier = Modifier.fillMaxSize().background(theme.surfaceBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scoreboard
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ThemedPlayerScoreCard(uiState.player1.name, uiState.player1.score, uiState.currentPlayer == 0, theme, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            ThemedPlayerScoreCard(uiState.player2.name, uiState.player2.score, uiState.currentPlayer == 1, theme, Modifier.weight(1f))
        }

        // Turn indicator / AI bar
        if (uiState.isAiThinking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), color = theme.primaryAccent, trackColor = theme.surfaceCard)
            Text("${uiState.player2.name} is thinking…", modifier = Modifier.fillMaxWidth().padding(2.dp), fontSize = 11.sp, textAlign = TextAlign.Center, color = theme.onSurfaceMuted)
        } else {
            Surface(modifier = Modifier.padding(horizontal = 12.dp), shape = RoundedCornerShape(8.dp), color = theme.surfaceCard) {
                Text(
                    "${if (uiState.currentPlayer == 0) uiState.player1.name else uiState.player2.name}'S TURN",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    color = theme.primaryAccent, textAlign = TextAlign.Center
                )
            }
        }

        // Board
        Board(
            board = uiState.board, boardConfig = uiState.boardConfig,
            placedThisTurn = uiState.placedThisTurn, selectedTile = uiState.selectedTile,
            onCellClick = { row, col -> viewModel.onCellClick(row, col) },
            cellSize = cellSize, theme = theme
        )

        Spacer(Modifier.height(4.dp))

        // Rack
        val showRack = !(uiState.isVsAi && uiState.currentPlayer == 1)
        if (showRack) {
            Column {
                ThemedRack(
                    rack = if (uiState.currentPlayer == 0) uiState.player1.rack else uiState.player2.rack,
                    selectedTile = uiState.selectedTile,
                    onTileClick  = { index -> if (!uiState.isAiThinking) viewModel.selectTile(index) },
                    theme = theme,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                if (uiState.isMultiplayer) {
                    ChatPanel(
                        messages = uiState.chatMessages,
                        myName = uiState.myName,
                        theme = theme,
                        onSend = { viewModel.sendChat(it) },
                        modifier = Modifier.height(120.dp).padding(8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Action buttons
        val humanTurn = !uiState.isAiThinking
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick  = { viewModel.playWord() },
                modifier = Modifier.weight(1.2f),
                enabled  = humanTurn && uiState.placedThisTurn.isNotEmpty()
            ) { Text("PLAY", fontSize = 11.sp) }

            OutlinedButton(onClick = { viewModel.shuffleRack() }, modifier = Modifier.weight(1f), enabled = humanTurn) { Text("↺", fontSize = 16.sp) }
            OutlinedButton(onClick = { viewModel.exchangeTiles() }, modifier = Modifier.weight(1f), enabled = humanTurn) { Text("↔", fontSize = 14.sp) }
            TextButton(onClick = { viewModel.recallAllTiles() }, modifier = Modifier.weight(1f), enabled = humanTurn) { Text("CLR", fontSize = 11.sp, color = theme.onSurfaceMuted) }
            TextButton(onClick = { viewModel.skipTurn() }, modifier = Modifier.weight(1f), enabled = humanTurn) { Text("SKIP", fontSize = 11.sp, color = theme.onSurfaceMuted) }

            // ── Ad-aware hint button ─────────────────────────────────────

        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Game Over Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameOverScreen(viewModel: SynthetixViewModel, uiState: GameUiState, theme: BoardTheme) {
    val winner = if (uiState.player1.score >= uiState.player2.score) uiState.player1 else uiState.player2
    Column(
        modifier = Modifier.fillMaxSize().background(theme.surfaceBackground).padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("${theme.emoji}", fontSize = 48.sp)
        Text("GAME OVER", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = theme.primaryAccent)
        Spacer(Modifier.height(32.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = theme.activePlayerCard) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆 WINNER", fontSize = 12.sp, letterSpacing = 2.sp, color = theme.onSurfaceMuted)
                Text(winner.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = theme.onSurface)
                Text("${winner.score}", fontSize = 52.sp, fontWeight = FontWeight.Black, color = theme.primaryAccent)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(uiState.player1, uiState.player2).forEach { p ->
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = theme.surfaceCard) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.name, fontSize = 12.sp, color = theme.onSurfaceMuted)
                        Text(p.score.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = theme.onSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = { viewModel.resetGame() }, modifier = Modifier.fillMaxWidth()) { Text("PLAY AGAIN") }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BoardPickerDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BoardPickerDialog(
    theme: BoardTheme, savedNames: List<String>, presetNames: List<String>,
    onSelectPreset: (String) -> Unit, onSelectSaved: (String) -> Unit,
    onSelectBlank: () -> Unit, onDelete: (String) -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = theme.surfaceCard,
        title = { Text("Choose Board", fontWeight = FontWeight.Bold, color = theme.onSurface) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                item { SectionHeader("Default", theme) }
                item { BoardPickerRow("Standard 15×15 (blank)", theme.primaryAccent, onSelectBlank, theme) }
                if (presetNames.isNotEmpty()) {
                    item { SectionHeader("Built-in Boards", theme) }
                    items(presetNames) { name ->
                        BoardPickerRow(name, theme.primaryAccent, { onSelectPreset(name) }, theme,
                            icon = { Icon(Icons.Default.Star, null, tint = theme.primaryAccent.copy(0.7f), modifier = Modifier.size(14.dp)) })
                    }
                }
                if (savedNames.isNotEmpty()) {
                    item { SectionHeader("My Saved Boards", theme) }
                    items(savedNames) { name ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onSelectSaved(name) }, modifier = Modifier.weight(1f)) { Text(name, color = theme.onSurface, fontSize = 13.sp) }
                            IconButton(onClick = { onDelete(name) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = theme.primaryAccent) } }
    )
}

@Composable
private fun SectionHeader(title: String, theme: BoardTheme) {
    Text(title.uppercase(), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
        color = theme.onSurfaceMuted, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp))
}

@Composable
private fun BoardPickerRow(
    name: String, color: Color, onClick: () -> Unit, theme: BoardTheme,
    icon: (@Composable () -> Unit)? = null
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) { icon(); Spacer(Modifier.width(4.dp)) }
        Text(name, color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

fun getLetterPoints(letter: Char): Int = when (letter.uppercaseChar()) {
    'A','E','I','O','U','L','N','S','T','R' -> 1; 'D','G' -> 2; 'B','C','M','P' -> 3
    'F','H','V','W','Y' -> 4; 'K' -> 5; 'J','X' -> 8; 'Q','Z' -> 10; else -> 0
}
