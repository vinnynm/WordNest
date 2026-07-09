package com.enigma.wordnest.games.absurdauction.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.absurdauction.model.BankerStance
import com.enigma.wordnest.games.absurdauction.ui.theme.AbsurdAuctionTheme
import com.enigma.wordnest.games.absurdauction.ui.theme.ColorPurpleLight
import com.enigma.wordnest.games.absurdauction.viewmodel.AuctionViewModel
import com.enigma.wordnest.games.lexicon.ui.components.Board
import com.enigma.wordnest.games.lexicon.ui.components.PlayerScoreCard
import com.enigma.wordnest.games.lexicon.ui.components.Rack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsurdAuctionGameScreen(vm: AuctionViewModel = viewModel()) {
    AbsurdAuctionTheme {
        val state by vm.state.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val selectedTile by vm.selectedTile.collectAsStateWithLifecycle()
        val lastMessage by vm.lastMessage.collectAsStateWithLifecycle()
        val showHowTo by vm.showHowTo.collectAsStateWithLifecycle()

        if (showHowTo) AbsurdAuctionHowToPlayDialog(onDismiss = vm::toggleHowTo)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Absurd Auction", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                    actions = {
                        IconButton(onClick = vm::toggleHowTo) {
                            Icon(Icons.AutoMirrored.Filled.Help, "How to play")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->

            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorPurpleLight)
                }
                return@Scaffold
            }

            if (!state.isGameStarted) {
                AuctionStartScreen(onStart = { p1, p2, stance -> vm.startGame(p1, p2, stance) })
                return@Scaffold
            }

            val configuration = LocalConfiguration.current
            val cellSize = (configuration.screenWidthDp / 18).dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    state.players.forEachIndexed { idx, p ->
                        PlayerScoreCard(
                            name = p.name, score = p.score,
                            isActive = state.currentPlayer == idx,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                DrawPoolTensionBar(
                    stance = state.bankerStance,
                    lastDecision = state.lastDecision,
                    isThinking = state.isBankerThinking,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (lastMessage.isNotEmpty()) {
                    Text(
                        lastMessage, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Board(
                    board = state.board,
                    placedThisTurn = emptyList(),
                    selectedTile = selectedTile,
                    onCellClick = vm::onCellClick,
                    cellSize = cellSize
                )

                Spacer(Modifier.height(8.dp))

                val rack = state.players.getOrNull(state.currentPlayer)?.rack ?: emptyList()
                Rack(
                    rack = rack,
                    selectedTile = selectedTile,
                    onTileClick = vm::selectTile,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = vm::playWord, modifier = Modifier.weight(1f)) { Text("PLAY") }
                    OutlinedButton(onClick = vm::shuffleRack, modifier = Modifier.weight(1f)) { Text("SHUFFLE") }
                    OutlinedButton(onClick = vm::exchangeTiles, modifier = Modifier.weight(1f)) { Text("EXCHANGE") }
                    TextButton(onClick = vm::recallAllTiles, modifier = Modifier.weight(1f)) { Text("CLEAR") }
                    TextButton(onClick = vm::skipTurn, modifier = Modifier.weight(1f)) { Text("SKIP") }
                }

                if (state.isGameOver) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.padding(16.dp)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GAME OVER", fontWeight = FontWeight.Black, fontSize = 22.sp)
                            val winner = state.players.maxByOrNull { it.score }
                            Text("${winner?.name} wins with ${winner?.score} points!", fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = vm::resetGame) { Text("New game") }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AuctionStartScreen(onStart: (String, String, BankerStance) -> Unit) {
    var p1 by remember { mutableStateOf("PLAYER 1") }
    var p2 by remember { mutableStateOf("PLAYER 2") }
    var stance by remember { mutableStateOf(BankerStance.EQUALIZING) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "ABSURD AUCTION", fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
            color = ColorPurpleLight
        )
        Text(
            "The Banker decides what you draw — and it's not on your side",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = p1, onValueChange = { p1 = it.uppercase() }, label = { Text("Player 1") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = p2, onValueChange = { p2 = it.uppercase() }, label = { Text("Player 2") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Text(
            "BANKER STANCE", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))

        // Phase 1: only NEUTRAL and EQUALIZING are wired up; CHAOTIC is shown
        // disabled as a preview of what's coming.
        listOf(BankerStance.NEUTRAL, BankerStance.EQUALIZING).forEach { s ->
            val selected = stance == s
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = { stance = s })
                Column {
                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (s == BankerStance.NEUTRAL) "True random draws — Banker disabled"
                        else "Leader gets starved, trailer gets favored",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onStart(p1, p2, stance) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("START GAME", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun AbsurdAuctionHowToPlayDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How To Play") },
        text = {
            Column {
                Text(
                    "Same rules as Lexicon — play valid dictionary words that connect to the board. " +
                            "But there's no random bag: every draw is chosen by the Banker.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "In EQUALIZING stance, if you're leading by a wide margin, the Banker gives you " +
                            "its worst available rack. If you're trailing, you get its best. The tension " +
                            "bar shows how many alternatives got rejected to make that happen.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("GOT IT") } }
    )
}