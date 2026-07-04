package com.enigma.wordnest.games.absurdman.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.absurdman.model.MAX_WRONG_GUESSES
import com.enigma.wordnest.games.absurdman.ui.theme.AbsurdmanTheme
import com.enigma.wordnest.games.absurdman.ui.theme.ColorGood
import com.enigma.wordnest.games.absurdman.ui.theme.ColorRustLight
import com.enigma.wordnest.games.absurdman.ui.theme.ColorSubtle
import com.enigma.wordnest.games.absurdman.viewmodel.AbsurdmanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsurdmanGameScreen(vm: AbsurdmanViewModel = viewModel()) {
    val state     by vm.state.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val showHowTo by vm.showHowTo.collectAsStateWithLifecycle()
    val context   = LocalContext.current

    AbsurdmanTheme {
        if (showHowTo) AbsurdmanHowToPlayDialog(onDismiss = vm::toggleHowTo)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Absurdman", fontWeight = FontWeight.Black, fontSize = 22.sp,
                                color = ColorRustLight)
                            if (state.isGameStarted) {
                                Text("${state.wordLength} letters", fontSize = 11.sp, color = ColorSubtle)
                            }
                        }
                    },
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorRustLight)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading dictionary…", color = ColorSubtle)
                    }
                }
                return@Scaffold
            }

            if (!state.isGameStarted) {
                AbsurdmanStartScreen(onStartGame = { len -> vm.startGame(len) })
                return@Scaffold
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${state.wrongGuessesRemaining} wrong guess${if (state.wrongGuessesRemaining != 1) "es" else ""} left",
                    fontSize = 13.sp,
                    fontWeight = if (state.wrongGuessesRemaining <= 2) FontWeight.Bold else FontWeight.Normal,
                    color = if (state.wrongGuessesRemaining <= 2) MaterialTheme.colorScheme.error else ColorSubtle
                )

                HangmanFigure(
                    wrongGuesses = state.wrongGuesses,
                    color = if (state.wrongGuesses >= MAX_WRONG_GUESSES) MaterialTheme.colorScheme.error else ColorRustLight
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.revealedPattern.forEach { ch ->
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(44.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = ch?.uppercaseChar()?.toString() ?: "",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !state.isActive) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isWon) {
                            Text("🎉 You beat it!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = ColorGood)
                        } else if (state.isLost) {
                            Text("💀 Game over", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                            state.revealedWord?.let {
                                Text("The word was ${it.uppercase()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, vm.buildShareText())
                                    }, "Share result"
                                ))
                            }) {
                                Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                            Button(
                                onClick = { vm.startNewGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorRustLight)
                            ) {
                                Icon(Icons.Filled.Replay, null, Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("New game", color = androidx.compose.ui.graphics.Color.Black)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                if (state.isActive) {
                    AbsurdmanKeyboard(
                        guessedLetters = state.guessedLetters,
                        presentLetters = state.revealedPattern.filterNotNull().toSet(),
                        onKey = vm::guessLetter,
                        enabled = true,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}
