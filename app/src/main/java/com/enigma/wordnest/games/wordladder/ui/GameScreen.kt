package com.enigma.wordnest.games.wordladder.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.wordladder.ui.theme.ColorAmber
import com.enigma.wordnest.games.wordladder.ui.theme.ColorSubtle
import com.enigma.wordnest.games.wordladder.ui.theme.ColorTealLight
import com.enigma.wordnest.games.wordladder.ui.theme.WordLadderTheme
import com.enigma.wordnest.games.wordladder.viewmodel.WordLadderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLadderGameScreen(vm: WordLadderViewModel = viewModel()) {
    val state        by vm.state.collectAsStateWithLifecycle()
    val isLoading    by vm.isLoading.collectAsStateWithLifecycle()
    val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
    val showHowTo    by vm.showHowTo.collectAsStateWithLifecycle()
    val context      = LocalContext.current

    WordLadderTheme {
        if (showHowTo) WordLadderHowToPlayDialog(onDismiss = vm::toggleHowTo)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Word Ladder", fontWeight = FontWeight.Black, fontSize = 22.sp,
                                color = ColorTealLight)
                            if (state.isGameStarted) {
                                Text("${state.wordLength} letters  ·  par ${state.minSteps}",
                                    fontSize = 11.sp, color = ColorSubtle)
                            }
                        }
                    },
                    actions = {
                        if (state.isActive) {
                            IconButton(onClick = vm::useHint) {
                                Icon(Icons.Filled.Lightbulb, "Hint", tint = ColorAmber)
                            }
                        }
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
                        CircularProgressIndicator(color = ColorTealLight)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading dictionary…", color = ColorSubtle)
                    }
                }
                return@Scaffold
            }

            if (!state.isGameStarted) {
                WordLadderStartScreen(onStartGame = { len -> vm.startGame(len) }, isGenerating = isGenerating)
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
                Text(
                    "${state.stepsSoFar} step${if (state.stepsSoFar != 1) "s" else ""} so far" +
                        if (state.hintsUsed > 0) "  ·  ${state.hintsUsed} hint${if (state.hintsUsed != 1) "s" else ""} used" else "",
                    fontSize = 12.sp, color = ColorSubtle
                )

                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    state.errorMessage?.let { msg ->
                        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small) {
                            Text(text = msg, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                LadderChain(path = state.path, targetWord = state.targetWord)

                AnimatedVisibility(visible = state.isWon) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val overPar = state.stepsSoFar - state.minSteps
                        Text(
                            if (overPar <= 0) "🏆 Perfect climb!" else "🎉 Reached the top!",
                            fontSize = 22.sp, fontWeight = FontWeight.Black, color = ColorTealLight
                        )
                        Text(
                            "${state.stepsSoFar} steps" + if (overPar > 0) " ($overPar over par)" else " — matched par!",
                            fontSize = 13.sp, color = ColorSubtle
                        )
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
                                onClick = { vm.startGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorTealLight)
                            ) {
                                Icon(Icons.Filled.Replay, null, Modifier.size(16.dp), tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("New game", color = Color.Black)
                            }
                        }
                    }
                }

                if (state.isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.currentInput,
                            onValueChange = vm::onInputChanged,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Next word…") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { vm.submitWord() }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorTealLight,
                                cursorColor = ColorTealLight
                            )
                        )
                        Button(
                            onClick = vm::submitWord,
                            enabled = state.currentInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorTealLight),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
