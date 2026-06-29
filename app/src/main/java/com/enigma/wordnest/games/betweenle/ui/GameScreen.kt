package com.enigma.wordnest.games.betweenle.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.betweenle.model.GameState
import com.enigma.wordnest.games.betweenle.ui.theme.BetweenleTheme
import com.enigma.wordnest.games.betweenle.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetweenleGameScreen(vm: GameViewModel = viewModel()) {
    BetweenleTheme {
        val gameState    by vm.gameState.collectAsStateWithLifecycle()
        val input        by vm.currentInput.collectAsStateWithLifecycle()
        val isLoading    by vm.isLoading.collectAsStateWithLifecycle()
        val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
        val showStats    by vm.showStats.collectAsStateWithLifecycle()
        val showHow      by vm.showHowToPlay.collectAsStateWithLifecycle()
        val stats        by vm.stats.collectAsStateWithLifecycle()
        val context      = LocalContext.current

        val gapSize = remember(gameState.wordA, gameState.wordB) {
            if (vm.wordRepo.isLoaded()) vm.wordRepo.gapSize(gameState.wordA, gameState.wordB) else 0
        }

        if (showStats) StatsDialog(stats = stats, onDismiss = vm::toggleStats)
        if (showHow) HowToPlayDialog(onDismiss = vm::toggleHowToPlay)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Betweenle",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = vm::toggleHowToPlay) {
                            Icon(Icons.AutoMirrored.Filled.Help, "How to play")
                        }
                        IconButton(onClick = vm::toggleStats) {
                            Icon(Icons.Filled.BarChart, "Statistics")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading dictionary…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Boundary words ──────────────────────────────────────
                BoundaryCard(
                    wordA = gameState.wordA,
                    wordB = gameState.wordB,
                    gapSize = gapSize
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // ── Guess list ───────────────────────────────────────────
                GuessList(state = gameState)

                // ── Input (only when active) ─────────────────────────────
                AnimatedVisibility(
                    visible = gameState.isActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    InputRow(
                        value = input,
                        onValueChange = vm::onInputChanged,
                        onSubmit = vm::submitGuess,
                        enabled = gameState.isActive,
                        errorMessage = errorMessage
                    )
                }

                // ── Game over card ───────────────────────────────────────
                AnimatedVisibility(
                    visible = gameState.isGameOver,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GameOverCard(
                        state = gameState,
                        onNewGame = vm::startNewGame,
                        onShare = {
                            val text = vm.buildShareText()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share result"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuessList(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Filled rows
        state.guesses.forEachIndexed { idx, result ->
            GuessRow(
                result = result,
                animate = idx == state.guesses.lastIndex
            )
        }
        // Empty placeholder rows
        val emptyCount = state.maxGuesses - state.guesses.size
        repeat(emptyCount) {
            EmptyGuessRow()
        }
    }
}
