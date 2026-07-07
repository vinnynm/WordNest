package com.enigma.wordnest.games.codeword.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Tune
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
import com.enigma.wordnest.games.codeword.model.CodewordDifficulty
import com.enigma.wordnest.games.codeword.model.StarterCellStyle
import com.enigma.wordnest.games.codeword.ui.theme.CodewordTheme
import com.enigma.wordnest.games.codeword.ui.theme.ColorStarter
import com.enigma.wordnest.games.codeword.ui.theme.ColorSubtle
import com.enigma.wordnest.games.codeword.ui.theme.ColorVioletLight
import com.enigma.wordnest.games.codeword.viewmodel.CodewordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodewordGameScreen(vm: com.enigma.wordnest.games.codeword.viewmodel.CodewordViewModel = viewModel()) {
    _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.CodewordTheme {
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
        val puzzle by vm.puzzle.collectAsStateWithLifecycle()
        val playerState by vm.playerState.collectAsStateWithLifecycle()
        val editingNumber by vm.editingNumber.collectAsStateWithLifecycle()
        val starterStyle by vm.starterStyle.collectAsStateWithLifecycle()

        var showSettings by remember { mutableStateOf(false) }

        editingNumber?.let { number ->
            _root_ide_package_.com.enigma.wordnest.games.codeword.ui.LetterPickerDialog(
                number = number,
                onLetterChosen = vm::confirmLetter,
                onDismiss = vm::cancelEditing
            )
        }

        if (showSettings) {
            _root_ide_package_.com.enigma.wordnest.games.codeword.ui.StarterStyleDialog(
                current = starterStyle,
                onSelect = { vm.setStarterStyle(it); showSettings = false },
                onDismiss = { showSettings = false }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Codeword", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Tune, "Starter cell style")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->

            if (isLoading) {
                _root_ide_package_.com.enigma.wordnest.games.codeword.ui.CenteredMessage(
                    Modifier.fillMaxSize().padding(padding), "Loading dictionary…"
                )
                return@Scaffold
            }

            val p = puzzle
            if (p == null) {
                _root_ide_package_.com.enigma.wordnest.games.codeword.ui.CodewordStartScreen(
                    isGenerating = isGenerating,
                    onStart = { diff -> vm.startNewGame(diff) },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
                return@Scaffold
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)) {

                Text(
                    "Tap a numbered cell, choose its letter — every matching number fills at once.",
                    fontSize = 12.sp,
                    color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorSubtle,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                AnimatedVisibility(visible = playerState.isComplete) {
                    Text(
                        "🔓 Decoded! Nice work.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorVioletLight,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    _root_ide_package_.com.enigma.wordnest.games.codeword.ui.CodewordGridView(
                        puzzle = p,
                        numberToPlayerLetter = playerState.numberToPlayerLetter,
                        revealedNumbers = playerState.revealedNumbers,
                        starterStyle = starterStyle,
                        selectedNumber = editingNumber,
                        onCellClick = vm::beginEditing
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val remaining = p.letterToNumber.values.firstOrNull {
                            it !in playerState.numberToPlayerLetter.keys
                        }
                        remaining?.let(vm::revealNumber)
                    },
                    enabled = !playerState.isComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorStarter),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.RemoveRedEye, null, Modifier.size(18.dp), tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Reveal a number", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StarterStyleDialog(
    current: com.enigma.wordnest.games.codeword.model.StarterCellStyle,
    onSelect: (com.enigma.wordnest.games.codeword.model.StarterCellStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Starter cell look") },
        text = {
            Column {
                _root_ide_package_.com.enigma.wordnest.games.codeword.model.StarterCellStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (style == _root_ide_package_.com.enigma.wordnest.games.codeword.model.StarterCellStyle.BLUR) "Frosted blur" else "Low-opacity (no blur)")
                        if (style == current) Text("✓", color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorVioletLight, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
private fun CodewordStartScreen(
    isGenerating: Boolean,
    onStart: (com.enigma.wordnest.games.codeword.model.CodewordDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CODEWORD", fontSize = 40.sp, fontWeight = FontWeight.Black,
            color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorVioletLight, letterSpacing = 2.sp)
        Text("Decode the grid, one number at a time", fontSize = 14.sp, color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorSubtle,
            modifier = Modifier.padding(bottom = 40.dp))

        _root_ide_package_.com.enigma.wordnest.games.codeword.model.CodewordDifficulty.entries.forEach { diff ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF241C33))
                    .clickable(enabled = !isGenerating) { onStart(diff) }
                    .padding(18.dp)
            ) {
                Column {
                    Text(diff.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (diff.starterCount == 0) "No starter letters" else "${diff.starterCount} starter letters given",
                        fontSize = 12.sp, color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorSubtle
                    )
                }
            }
        }

        if (isGenerating) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorVioletLight)
            Spacer(Modifier.height(8.dp))
            Text("Generating puzzle…", fontSize = 12.sp, color = _root_ide_package_.com.enigma.wordnest.games.codeword.ui.theme.ColorSubtle)
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
