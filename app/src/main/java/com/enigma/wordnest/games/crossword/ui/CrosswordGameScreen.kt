package com.enigma.wordnest.games.crossword.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.common.ui.GameKeyboard
import com.enigma.wordnest.games.crossword.model.ClueDirection
import com.enigma.wordnest.games.crossword.model.CrosswordDifficulty
import com.enigma.wordnest.games.crossword.ui.theme.ColorAccent
import com.enigma.wordnest.games.crossword.ui.theme.ColorSubtle
import com.enigma.wordnest.games.crossword.ui.theme.CrosswordTheme
import com.enigma.wordnest.games.crossword.viewmodel.CrosswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosswordGameScreen(vm: CrosswordViewModel = viewModel()) {
    CrosswordTheme {
        val isLoading      by vm.isLoading.collectAsStateWithLifecycle()
        val isGenerating   by vm.isGenerating.collectAsStateWithLifecycle()
        val puzzle         by vm.puzzle.collectAsStateWithLifecycle()
        val playerState    by vm.playerState.collectAsStateWithLifecycle()
        val selectedCell    by vm.selectedCell.collectAsStateWithLifecycle()
        val selectedDirection by vm.selectedDirection.collectAsStateWithLifecycle()
        val errorMessage   by vm.errorMessage.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Crossword", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                    actions = {
                        if (puzzle != null) {
                            IconButton(onClick = { vm.checkAll() }) {
                                Icon(Icons.Filled.CheckCircle, "Check all")
                            }
                            selectedCell?.let { (r, c) ->
                                IconButton(onClick = { vm.revealCell(r, c) }) {
                                    Icon(Icons.Filled.RemoveRedEye, "Reveal cell")
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->

            if (isLoading) {
                CenteredMessage(Modifier.fillMaxSize().padding(padding), "Loading dictionary…")
                return@Scaffold
            }

            val p = puzzle
            if (p == null) {
                CrosswordStartScreen(
                    isGenerating = isGenerating,
                    onStart = { diff -> vm.startNewGame(diff) },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
                return@Scaffold
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)) {

                val activeClue = remember(selectedCell, selectedDirection, p) {
                    selectedCell?.let { (r, c) ->
                        val list = if (selectedDirection == ClueDirection.ACROSS) p.acrossClues else p.downClues
                        list.firstOrNull { r to c in it.cells() }
                    }
                }

                activeClue?.let { clue ->
                    Surface(
                        color = ColorAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Text(
                            "${clue.number} ${if (clue.direction == ClueDirection.ACROSS) "Across" else "Down"}: ${clue.clueText}",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AnimatedVisibility(visible = playerState.isComplete) {
                    Text("🎉 Solved! Great work.", fontSize = 16.sp, fontWeight = FontWeight.Black,
                        color = ColorAccent, modifier = Modifier.padding(bottom = 6.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    CrosswordGridView(
                        puzzle = p,
                        filledLetters = playerState.filledLetters,
                        checkedCells = playerState.checkedCells,
                        wrongCells = playerState.wrongCells,
                        revealedCells = playerState.revealedCells,
                        selectedCell = selectedCell,
                        selectedDirection = selectedDirection,
                        onCellClick = vm::selectCell
                    )
                }

                Spacer(Modifier.height(6.dp))

                GameKeyboard(
                    keyStates = emptyMap<Char, Unit>(),
                    colorOf = { Color(0xFF3A3F4A) },
                    onKey = vm::onLetterInput,
                    onBackspace = vm::onBackspace,
                    onEnter = { },
                    enabled = !playerState.isComplete,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CrosswordStartScreen(
    isGenerating: Boolean,
    onStart: (CrosswordDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CROSSWORD", fontSize = 40.sp, fontWeight = FontWeight.Black,
            color = ColorAccent, letterSpacing = 2.sp)
        Text("Generated fresh from the dictionary", fontSize = 14.sp, color = ColorSubtle,
            modifier = Modifier.padding(bottom = 40.dp))

        CrosswordDifficulty.entries.forEach { diff ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C2531))
                    .clickable(enabled = !isGenerating) { onStart(diff) }
                    .padding(18.dp)
            ) {
                Column {
                    Text(diff.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${diff.gridSize}×${diff.gridSize} grid", fontSize = 12.sp, color = ColorSubtle)
                }
            }
        }

        if (isGenerating) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = ColorAccent)
            Spacer(Modifier.height(8.dp))
            Text("Generating puzzle…", fontSize = 12.sp, color = ColorSubtle)
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, text: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(text)
        }
    }
}
