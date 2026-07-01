package com.enigma.wordnest.games.lexicon

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.enigma.wordnest.games.lexicon.model.AiDifficulty
import com.enigma.wordnest.games.lexicon.ui.GameState
import com.enigma.wordnest.games.lexicon.ui.GameUiState
import com.enigma.wordnest.games.lexicon.ui.ScrabbleGameViewModel
import com.enigma.wordnest.games.lexicon.ui.components.Board
import com.enigma.wordnest.games.lexicon.ui.components.PlayerScoreCard
import com.enigma.wordnest.games.lexicon.ui.components.Rack
import com.enigma.wordnest.games.lexicon.ui.theme.LexiconTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrabbleGameApp(viewModel: ScrabbleGameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.lastPlayMessage) {
        if (uiState.lastPlayMessage.isNotEmpty()) {
            delay(2500)
            viewModel.clearMessage()
        }
    }

    LexiconTheme {
        Scaffold(
            topBar = {
                if (uiState.gameState != GameState.MENU) {
                    TopAppBar(
                        title = {
                            Text(
                                "LEXICON",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                        },
                        actions = {
                            if (uiState.isDictionaryLoaded) {
                                FilterChip(
                                    onClick = {},
                                    modifier = Modifier.padding(end = 8.dp),
                                    enabled = true,
                                    label = { Text("${uiState.dictionarySize} words") },
                                    leadingIcon = {},
                                    trailingIcon = {},
                                    shape = MaterialTheme.shapes.extraSmall,
                                    elevation = FilterChipDefaults.filterChipElevation(),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    selected = false
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (uiState.gameState) {
                    GameState.MENU     -> MenuScreen(viewModel, uiState)
                    GameState.PLAYING  -> GameScreen(viewModel, uiState)
                    GameState.GAME_OVER -> GameOverScreen(viewModel, uiState)
                }

                // Snackbar
                if (uiState.lastPlayMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.lastPlayScore > 0)
                                Color(0xFF4CAF50)
                            else
                                MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Text(
                            uiState.lastPlayMessage,
                            modifier = Modifier.padding(12.dp, 8.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ── Blank tile letter picker dialog ────────────────────────────
            if (uiState.showBlankPicker) {
                BlankTilePickerDialog(
                    onLetterChosen = { viewModel.confirmBlankLetter(it) },
                    onDismiss      = { viewModel.cancelBlankPicker() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Blank Tile Picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BlankTilePickerDialog(
    onLetterChosen: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    val letters = ('A'..'Z').toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choose a letter for your blank tile",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(letters) { letter ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFFF2C97E),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .clickable { onLetterChosen(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1208)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Menu Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MenuScreen(viewModel: ScrabbleGameViewModel, uiState: GameUiState) {
    var player1Name by remember { mutableStateOf("PLAYER 1") }
    var player2Name by remember { mutableStateOf("PLAYER 2") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var vsAiMode by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1000f,
                    center = Offset(0.5f, 0.4f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "LEXICON",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "TWO-PLAYER WORD STRATEGY",
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        listOf("vs AI", "2 Players").forEachIndexed { i, label ->
                            FilterChip(
                                selected = vsAiMode == (i == 0),
                                onClick  = { vsAiMode = i == 0 },
                                label    = { Text(label) }
                            )
                        }
                    }

                    if (!vsAiMode) {
                        OutlinedTextField(
                            value = player1Name,
                            onValueChange = { player1Name = it.uppercase() },
                            label = { Text("Player 1 Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = player2Name,
                            onValueChange = { player2Name = it.uppercase() },
                            label = { Text("Player 2 Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row {
                            AiDifficulty.entries.forEach { d ->
                                FilterChip(
                                    selected = selectedDifficulty == d,
                                    onClick  = { selectedDifficulty = d },
                                    label    = { Text(d.displayName) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.canResume) {
                        Button(
                            onClick = { viewModel.resumeGame() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESUME GAME")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (vsAiMode) viewModel.startVsAi(player1Name, selectedDifficulty)
                            else          viewModel.startGame(player1Name, player2Name)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = uiState.isDictionaryLoaded && !uiState.isLoading
                    ) {
                        Text(if (uiState.canResume) "START NEW GAME" else "START GAME")
                    }

                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = 16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Loading dictionary...",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (uiState.errorMessage != null) {
                        Text(
                            uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        TextButton(
                            onClick = {  },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("RETRY")
                        }
                    }
                }
            }

            if (uiState.updateAvailable != null && !uiState.isDownloading) {
                OutlinedButton(
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(Icons.Default.Update, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Word Library Update Available")
                }
            }

            if (uiState.isDownloading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                Text("Downloading update...", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    if (showUpdateDialog && uiState.updateAvailable != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Dictionary") },
            text = {
                Column {
                    Text("A new word library is available!")
                    Text(
                        "Version ${uiState.updateAvailable}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Released: ${uiState.updateAvailable}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showUpdateDialog = false; viewModel.downloadUpdate() }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Later") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Game Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameScreen(viewModel: ScrabbleGameViewModel, uiState: GameUiState) {
    val configuration = LocalConfiguration.current
    val cellSize = (configuration.screenWidthDp / 18).dp

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        // Scoreboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerScoreCard(
                name     = uiState.player1.name,
                score    = uiState.player1.score,
                isActive = uiState.currentPlayer == 0,
                modifier = Modifier.weight(1f)
            )
            PlayerScoreCard(
                name     = uiState.player2.name,
                score    = uiState.player2.score,
                isActive = uiState.currentPlayer == 1,
                modifier = Modifier.weight(1f)
            )
        }

        // Turn indicator / AI thinking bar
        if (uiState.isAiThinking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Text(
                "${uiState.player2.name} is thinking…",
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                fontSize  = 12.sp,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "${if (uiState.currentPlayer == 0) uiState.player1.name else uiState.player2.name}'S TURN",
                    modifier      = Modifier.padding(8.dp),
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign     = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Board
        Board(
            board          = uiState.board,
            placedThisTurn = uiState.placedThisTurn,
            selectedTile   = uiState.selectedTile,
            onCellClick    = { row, col -> viewModel.onCellClick(row, col) },
            cellSize       = cellSize
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rack — hide AI rack when vs AI and it's the AI's turn
        val showRack = !(uiState.isVsAi && uiState.currentPlayer == 1)
        if (showRack) {
            Rack(
                rack        = if (uiState.currentPlayer == 0) uiState.player1.rack else uiState.player2.rack,
                selectedTile = uiState.selectedTile,
                onTileClick = { index ->
                    if (!uiState.isAiThinking) viewModel.selectTile(index)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val humanTurn = !uiState.isAiThinking

            Button(
                onClick  = { viewModel.playWord() },
                modifier = Modifier.weight(1f),
                enabled  = humanTurn && uiState.placedThisTurn.isNotEmpty()
            ) {
                Text("PLAY")
            }

            OutlinedButton(
                onClick  = { viewModel.shuffleRack() },
                modifier = Modifier.weight(1f),
                enabled  = humanTurn
            ) {
                Text("SHUFFLE")
            }

            OutlinedButton(
                onClick  = { viewModel.exchangeTiles() },
                modifier = Modifier.weight(1f),
                enabled  = humanTurn
            ) {
                Text("EXCHANGE")
            }

            TextButton(
                onClick  = { viewModel.recallAllTiles() },
                modifier = Modifier.weight(1f),
                enabled  = humanTurn
            ) {
                Text("CLEAR")
            }

            TextButton(
                onClick  = { viewModel.skipTurn() },
                modifier = Modifier.weight(1f),
                enabled  = humanTurn
            ) {
                Text("SKIP")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Game Over Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GameOverScreen(viewModel: ScrabbleGameViewModel, uiState: GameUiState) {
    val winner = if (uiState.player1.score >= uiState.player2.score) uiState.player1 else uiState.player2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "GAME OVER",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆 WINNER 🏆", fontSize = 14.sp, letterSpacing = 2.sp)
                Text(winner.name, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${winner.score} points",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(uiState.player1, uiState.player2).forEach { player ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(player.name, fontSize = 14.sp)
                        Text(
                            player.score.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (player == uiState.player1) Spacer(modifier = Modifier.width(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick  = { viewModel.resetGame() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("PLAY AGAIN")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Letter point values (shared utility)
// ─────────────────────────────────────────────────────────────────────────────

fun getLetterPoints(letter: Char): Int = when (letter.uppercaseChar()) {
    'A','E','I','O','U','L','N','S','T','R' -> 1
    'D','G'                                  -> 2
    'B','C','M','P'                          -> 3
    'F','H','V','W','Y'                      -> 4
    'K'                                      -> 5
    'J','X'                                  -> 8
    'Q','Z'                                  -> 10
    else                                     -> 0
}
