package com.enigma.wordnest.games.chromaword.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.chromaword.ui.theme.ChromaWordTheme
import com.enigma.wordnest.games.chromaword.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaWordGameScreen(vm: GameViewModel = viewModel()) {
    ChromaWordTheme {
        val state         by vm.state.collectAsStateWithLifecycle()
        val isLoading     by vm.isLoading.collectAsStateWithLifecycle()
        val showStats     by vm.showStats.collectAsStateWithLifecycle()
        val showHowTo     by vm.showHowTo.collectAsStateWithLifecycle()
        val showDifficulty by vm.showDifficulty.collectAsStateWithLifecycle()
        val stats         by vm.stats.collectAsStateWithLifecycle()
        val context       = LocalContext.current

        // Track whether to shake the current row on invalid submit
        var shaking by remember { mutableStateOf(false) }
        LaunchedEffect(state.errorMessage) {
            if (state.errorMessage != null) {
                shaking = true
                delay(500)
                shaking = false
            }
        }

        // Dialogs
        if (showStats) StatsDialog(stats = stats, onDismiss = vm::toggleStats)
        if (showHowTo) HowToPlayDialog(onDismiss = vm::toggleHowTo)
        if (showDifficulty) DifficultyDialog(
            current = state.difficulty,
            onSelect = { vm.startNewGame(it) },
            onDismiss = vm::toggleDifficulty
        )



        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "ChromaWord",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "${state.difficulty.emoji} ${state.difficulty.label}  ·  ${state.wordLength} letters",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::toggleHowTo) {
                            Icon(Icons.AutoMirrored.Filled.Help, "How to play")
                        }
                        IconButton(onClick = vm::toggleStats) {
                            Icon(Icons.Filled.BarChart, "Statistics")
                        }
                        IconButton(onClick = vm::toggleDifficulty) {
                            Icon(Icons.Filled.Settings, "Difficulty")
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
                LoadingScreen(Modifier.fillMaxSize().padding(padding))
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Colour legend
                ColourLegendRow()

                Spacer(Modifier.height(2.dp))

                // Guess counter (hidden in Baby mode)
                if (state.difficulty.maxGuesses != null) {
                    val remaining = state.guessesRemaining ?: 0
                    Text(
                        text = "$remaining guess${if (remaining != 1) "es" else ""} remaining",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (remaining <= 1) 0.9f else 0.45f
                        ),
                        fontWeight = if (remaining <= 1) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Error snackbar inline
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    state.errorMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Guess grid ──────────────────────────────────────────────────
                GuessGrid(
                    wordLength = state.wordLength,
                    submittedGuesses = state.visibleGuesses(),
                    currentInput = if (state.isActive) state.currentInput else "",
                    emptyRows = if (state.isActive) state.emptyRowsRemaining() else 0,
                    isShaking = shaking,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // ── Game over banner ────────────────────────────────────────────
                AnimatedVisibility(
                    visible = state.isGameOver,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    GameOverBanner(
                        state = state,
                        onNewGame = { vm.startNewGame() },
                        onShare = {
                            val text = vm.buildShareText()
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }, "Share result"
                                )
                            )
                        }
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ── On-screen keyboard (always shown, disabled after game over) ─
                ChromaKeyboard(
                    keyStates = state.keyboardState,
                    onKey = vm::onKeyPress,
                    onBackspace = vm::onBackspace,
                    onEnter = vm::submitGuess,
                    enabled = state.isActive,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                "Loading dictionary…",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
