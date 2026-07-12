package com.enigma.wordnest.games.ladderclaim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.enigma.wordnest.games.ladderclaim.model.ColorOutcome
import com.enigma.wordnest.games.ladderclaim.model.LadderClaimState
import com.enigma.wordnest.games.ladderclaim.model.LadderClaimStats
import com.enigma.wordnest.games.ladderclaim.model.LadderClaimVariant
import com.enigma.wordnest.games.ladderclaim.ui.theme.LadderClaimTheme
import com.enigma.wordnest.games.ladderclaim.ui.theme.playerColor
import com.enigma.wordnest.games.ladderclaim.viewmodel.LadderClaimViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LadderClaimGameScreen(vm: LadderClaimViewModel = viewModel()) {
    LadderClaimTheme {
        val state by vm.state.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val showHowTo by vm.showHowTo.collectAsStateWithLifecycle()
        val stats by vm.stats.collectAsStateWithLifecycle()

        if (showHowTo) LadderClaimHowToPlayDialog(onDismiss = vm::toggleHowTo)

        if (state.pendingTargetChoiceIds.isNotEmpty()) {
            val choices = state.pendingTargetChoiceIds.mapNotNull { id -> state.words.find { it.id == id } }
            AlertDialog(
                onDismissRequest = vm::dismissTargetChoice,
                title = { Text("Which word?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This cell is shared by two words — pick your target.", fontSize = 13.sp)
                        choices.forEach { w ->
                            TextButton(
                                onClick = { vm.resolveTargetChoice(w.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${w.word}  (${if (w.isHorizontal) "across" else "down"})")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = vm::dismissTargetChoice) { Text("Cancel") } }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ladder Claim", fontWeight = FontWeight.Black, fontSize = 22.sp) },
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
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            if (!state.isGameStarted) {
                LadderClaimStartScreen(
                    stats = stats,
                    onStart = { p1, p2, variant -> vm.startGame(p1, p2, variant) },
                    onStartVsAi = { p1, variant -> vm.startVsAi(p1, variant) }
                )
                return@Scaffold
            }

            val configuration = LocalConfiguration.current
            val cellSize = (configuration.screenWidthDp / 18).dp
            val aiTurnActive = state.isVsAi && state.currentPlayer == 1 && !state.isGameOver

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TerritoryMeter(
                    players = state.players,
                    tally = vm.territoryTally(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (state.isAiThinking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Text(
                        "${state.players.getOrNull(1)?.name ?: "AI"} is thinking…",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else {
                    Text(
                        "${state.players.getOrNull(state.currentPlayer)?.name ?: ""}'s turn  ·  turn ${state.turnCount + 1}  ·  ${state.variant.displayName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                if (state.lastMessage.isNotEmpty()) {
                    Text(
                        state.lastMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LadderBoard(
                        board = state.board,
                        placedThisTurn = state.placedThisTurn,
                        onCellClick = vm::onCellClick,
                        cellSize = cellSize
                    )
                }



                Spacer(Modifier.height(8.dp))

                if (!aiTurnActive) {
                    if (state.selectedTargetWordId != null) {
                        OutcomePreviewChip(vm.previewOutcome()?.first)
                    } else {
                        Text(
                            "Tap a board word to set it as your target (optional)", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val rack = state.players.getOrNull(state.currentPlayer)?.rack ?: emptyList()
                    LadderRack(
                        rack = rack,
                        selectedTile = state.selectedTile,
                        onTileClick = vm::selectTile,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = vm::playWord, modifier = Modifier.weight(1f), enabled = state.placedThisTurn.isNotEmpty()) {
                            Text("PLAY")
                        }
                        OutlinedButton(onClick = vm::recallAllTiles, modifier = Modifier.weight(1f)) { Text("CLEAR") }
                        OutlinedButton(onClick = vm::exchangeTiles, modifier = Modifier.weight(1f)) { Text("EXCHANGE") }
                        TextButton(onClick = vm::skipTurn, modifier = Modifier.weight(1f)) { Text("SKIP") }
                    }
                }

                if (state.isGameOver) {
                    Spacer(Modifier.height(16.dp))
                    GameOverCard(vm = vm, state = state)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GameOverCard(vm: LadderClaimViewModel, state: LadderClaimState) {
    val tally = vm.territoryTally()
    val winnerIdx = vm.determineWinner()

    Card(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            if (winnerIdx != null) {
                Text(
                    "${state.players[winnerIdx].name} wins!",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, color = playerColor(winnerIdx)
                )
            } else {
                Text("It's a genuine tie!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            state.players.forEachIndexed { idx, p ->
                Text(
                    "${p.name}: ${tally[idx] ?: 0} tiles  ·  longest chain ${vm.longestChain(idx)}  ·  ${p.rack.size} tiles in rack",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun OutcomePreviewChip(outcome: ColorOutcome?) {
    val (label, color) = when (outcome) {
        ColorOutcome.FULL -> "FULL claim" to MaterialTheme.colorScheme.primary
        ColorOutcome.PARTIAL -> "PARTIAL claim" to MaterialTheme.colorScheme.secondary
        ColorOutcome.NEUTRAL, null -> "NEUTRAL — no territory" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    }
    Box(
        modifier = Modifier.background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun LadderClaimStartScreen(
    stats: LadderClaimStats,
    onStart: (String, String, LadderClaimVariant) -> Unit,
    onStartVsAi: (String, LadderClaimVariant) -> Unit
) {
    var p1 by remember { mutableStateOf("PLAYER 1") }
    var p2 by remember { mutableStateOf("PLAYER 2") }
    var variant by remember { mutableStateOf(LadderClaimVariant.CLASSIC) }
    var vsAiMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "LADDER CLAIM", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Claim territory by playing near ladder-legal words", fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ── vs AI / 2 Players toggle ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("vs AI" to true, "2 Players" to false).forEach { (label, isAi) ->
                val selected = vsAiMode == isAi
                FilterChip(
                    selected = selected,
                    onClick = { vsAiMode = isAi },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = p1, onValueChange = { p1 = it.uppercase() },
            label = { Text(if (vsAiMode) "Your name" else "Player 1") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        if (!vsAiMode) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = p2, onValueChange = { p2 = it.uppercase() }, label = { Text("Player 2") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("VARIANT", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LadderClaimVariant.entries.forEach { v ->
                val selected = variant == v
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { variant = v })
                    Column(Modifier.weight(1f)) {
                        Text(v.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(variantSubtitle(v), fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (vsAiMode) onStartVsAi(p1, variant) else onStart(p1, p2, variant)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("START GAME", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        if (stats.gamesPlayed > 0) {
            Spacer(Modifier.height(20.dp))
            Text(
                "${stats.gamesPlayed} games played  ·  P1 won ${stats.player1Wins}  ·  P2 won ${stats.player2Wins}" +
                        if (stats.ties > 0) "  ·  ${stats.ties} ties" else "",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

private fun variantSubtitle(v: LadderClaimVariant) = when (v) {
    LadderClaimVariant.QUICKFIRE -> "10 turns each — fast games"
    LadderClaimVariant.CLASSIC   -> "20 turns each — standard rules"
    LadderClaimVariant.GENEROUS  -> "Easier FULL claims (L-2 match)"
    LadderClaimVariant.STRICT    -> "No neutral-tile theft"
    LadderClaimVariant.TARGET_STRIKE -> "Claims land on the TARGET word, not your own tiles"
    LadderClaimVariant.FAIR_CLAIM -> "Matches claim neutral cells only — no whole-word theft, opponent tiles are always safe"
}
