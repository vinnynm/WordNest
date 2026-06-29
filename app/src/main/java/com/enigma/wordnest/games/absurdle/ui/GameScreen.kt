package com.enigma.wordnest.games.absurdle.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Flag
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
import com.enigma.wordnest.games.absurdle.ui.theme.AbsurdleTheme
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurpleLight
import com.enigma.wordnest.games.absurdle.ui.theme.ColorSubtle
import com.enigma.wordnest.games.absurdle.viewmodel.AbsurdleViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsurdleGameScreen(vm: AbsurdleViewModel = viewModel()) {
    val state        by vm.state.collectAsStateWithLifecycle()
    val isLoading    by vm.isLoading.collectAsStateWithLifecycle()
    val showHowTo    by vm.showHowTo.collectAsStateWithLifecycle()
    val showSettings by vm.showSettings.collectAsStateWithLifecycle()
    val context      = LocalContext.current

    var shaking by remember { mutableStateOf(false) }
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            shaking = true
            delay(500)
            shaking = false
        }
    }

    AbsurdleTheme {
        // Dialogs
        if (showHowTo) HowToPlayDialog(onDismiss = vm::toggleHowTo)
        if (showSettings) SettingsDialog(
            wordLength = state.wordLength,
            hardMode = state.hardMode,
            showCandidateCount = state.showCandidateCount,
            onWordLength = vm::setWordLength,
            onHardMode = vm::setHardMode,
            onToggleCandidateCount = vm::toggleCandidateCount,
            onDismiss = vm::toggleSettings
        )

        // Give-up confirmation
        var confirmGiveUp by remember { mutableStateOf(false) }
        if (confirmGiveUp) {
            AlertDialog(
                onDismissRequest = { confirmGiveUp = false },
                title = { Text("Give up?") },
                text  = { Text("The game will reveal a word it could have used. Your progress won't count.") },
                confirmButton = {
                    TextButton(onClick = { vm.giveUp(); confirmGiveUp = false }) {
                        Text("Give up", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmGiveUp = false }) { Text("Keep trying") }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Absurdle",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = ColorPurpleLight
                            )
                            Text(
                                "${state.wordLength} letters  ·  ${if (state.hardMode) "Hard 💀" else "Normal"}",
                                fontSize = 11.sp,
                                color = ColorSubtle
                            )
                        }
                    },
                    actions = {
                        // Give up button (only when game active)
                        if (state.isActive && state.guessCount > 0) {
                            IconButton(onClick = { confirmGiveUp = true }) {
                                Icon(Icons.Filled.Flag, "Give up",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                        IconButton(onClick = vm::toggleHowTo) {
                            Icon(Icons.AutoMirrored.Filled.Help, "How to play")
                        }
                        IconButton(onClick = vm::toggleSettings) {
                            Icon(Icons.Filled.Settings, "Settings")
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
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorPurpleLight)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading dictionary…", color = ColorSubtle)
                    }
                }
                return@Scaffold
            }

            if (!state.isGameStarted) {
                AbsurdleStartScreen(
                    onStartGame = { len, hard -> vm.startGame(len, hard) }
                )
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Candidate pool bar
                if (state.showCandidateCount) {
                    val total = vm.wordRepo.countOfLength(state.wordLength)
                    CandidateCountBar(
                        current = state.candidates.size,
                        total = total
                    )
                }

                // Error message
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
                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Guess count
                if (state.guessCount > 0) {
                    Text(
                        text = "${state.guessCount} guess${if (state.guessCount != 1) "es" else ""} so far",
                        fontSize = 12.sp,
                        color = ColorSubtle
                    )
                } else {
                    Text(
                        text = "😈 The game is watching your every move",
                        fontSize = 12.sp,
                        color = ColorSubtle,
                        textAlign = TextAlign.Center
                    )
                }

                // Guess grid
                GuessGrid(
                    wordLength = state.wordLength,
                    submittedGuesses = state.guesses,
                    candidateHistory = state.candidateHistory,
                    currentInput = state.currentInput,
                    isActive = state.isActive,
                    showCandidateCount = state.showCandidateCount,
                    isShaking = shaking
                )

                // Result card
                AnimatedVisibility(
                    visible = !state.isActive,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    GameResultCard(
                        isWon = state.isWon,
                        guessCount = state.guessCount,
                        revealedWord = state.revealedWord,
                        onNewGame = { vm.startNewGame() },
                        onShare = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, vm.buildShareText())
                                    }, "Share result"
                                )
                            )
                        }
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Keyboard
                AbsurdleKeyboard(
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
