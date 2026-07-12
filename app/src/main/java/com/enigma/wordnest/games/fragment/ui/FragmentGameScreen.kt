package com.enigma.wordnest.games.fragment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enigma.wordnest.games.common.ui.GameOverTemplate
import com.enigma.wordnest.games.common.ui.PhysicalKeyboardInput
import com.enigma.wordnest.games.fragment.model.*
import com.enigma.wordnest.games.fragment.ui.theme.*
import com.enigma.wordnest.games.fragment.viewmodel.FragmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragmentGameScreen(vm: FragmentViewModel = viewModel()) {
    FragmentTheme {
        val state by vm.state.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val isGenerating by vm.isGenerating.collectAsStateWithLifecycle()
        val showHowTo by vm.showHowTo.collectAsStateWithLifecycle()

        if (showHowTo) FragmentHowToPlayDialog(onDismiss = vm::toggleHowTo)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Fragment", fontWeight = FontWeight.Black, fontSize = 22.sp, color = ColorAmberLight)
                            if (state.isGameStarted) Text("♥ ${state.livesRemaining}  ·  ${state.wordLength} letters",
                                fontSize = 11.sp, color = ColorSubtle)
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::toggleHowTo) { Icon(Icons.AutoMirrored.Filled.Help, "How to play") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->

            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = ColorAmberLight) }
                return@Scaffold
            }

            if (!state.isGameStarted) {
                FragmentStartScreen(isGenerating = isGenerating, onStart = vm::startGame)
                return@Scaffold
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.errorMessage?.let { msg ->
                    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                        Text(msg, Modifier.padding(10.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }

                when (state.phase) {
                    FragmentPhase.SOLVING_HINTS -> HintWordsSection(state, vm)
                    FragmentPhase.ANAGRAM -> AnagramSection(state, vm)
                    // If the player died before finishing the clue words, keep showing
                    // the (now fully revealed) hint words instead of the anagram section
                    // — that's the furthest they actually got.
                    FragmentPhase.GAME_OVER -> if (state.lostDuringHints) HintWordsSection(state, vm)
                }

                if (state.phase == FragmentPhase.GAME_OVER) {
                    GameOverTemplate(
                        title = if (state.isWon) "🧩 Solved!" else "💔 Out of lives",
                        titleColor = if (state.isWon) ColorGood else MaterialTheme.colorScheme.error,
                        subtitle = when {
                            state.isWon -> null
                            state.lostDuringHints -> "You didn't make it to the anagram — here's what those clue words were:"
                            else -> "The word was ${state.mysteryWord.uppercase()}"
                        },
                        onNewGame = { vm.startGame(state.wordLength) },
                        onShare = {},
                        accentColor = ColorAmberLight
                    )
                }
            }
        }
    }
}

@Composable
private fun HintWordsSection(state: FragmentState, vm: FragmentViewModel) {
    Text("Solve the clue words to collect letters", fontSize = 13.sp, color = ColorSubtle)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.hintWords.forEach { hint ->
            HintWordRow(hint, isActive = hint.id == state.activeHintWordId, onClick = { vm.selectHintWord(hint.id) })
        }
    }
    val active = state.hintWords.find { it.id == state.activeHintWordId }
    if (active != null && state.phase == FragmentPhase.SOLVING_HINTS) {
        Spacer(Modifier.height(4.dp))
        PhysicalKeyboardInput(
            value = active.currentInput,
            onChange = vm::onHintInputChanged,
            onSubmit = vm::submitHintWord,
            hint = "Type the ${active.word.length}-letter word…",
            accentColor = ColorAmberLight
        )
    }
    if (state.collectedTiles.isNotEmpty() && state.phase == FragmentPhase.SOLVING_HINTS) {
        Spacer(Modifier.height(4.dp))
        Text("Collected letters", fontSize = 12.sp, color = ColorSubtle)
        TilePool(tiles = state.collectedTiles, usedIds = emptyList(), onTap = {})
    }
}

@Composable
private fun HintWordRow(hint: HintWord, isActive: Boolean, onClick: () -> Unit) {
    val bg = when {
        hint.isSolved -> ColorGood.copy(alpha = 0.15f)
        isActive -> ColorAmberLight.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
            .clickable(enabled = !hint.isSolved, onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        hint.word.indices.forEach { i ->
            val blanked = i in hint.blankIndices
            val ch = if (blanked && !hint.isSolved) "_" else hint.word[i].uppercaseChar().toString()
            Text(
                ch, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = if (blanked) ColorAmberLight else MaterialTheme.colorScheme.onSurface
            )
        }
        if (hint.isSolved) { Spacer(Modifier.weight(1f)); Text("✓", color = ColorGood, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AnagramSection(state: FragmentState, vm: FragmentViewModel) {
    Text("All clues solved — arrange the letters into the mystery word", fontSize = 13.sp, color = ColorSubtle)
    Text("${state.mysteryWord.length} letters — watch for extra tiles that don't belong",
        fontSize = 11.sp, color = ColorSubtle)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(state.mysteryWord.length) { i ->
            val ch = state.anagramInput.getOrNull(i)
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (ch != null) ColorAmberLight.copy(alpha = 0.25f) else ColorSurface),
                Alignment.Center
            ) { Text(ch?.uppercaseChar()?.toString() ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }

    val availableTiles = state.collectedTiles.filter { it.id !in state.usedTileIds }
    TilePool(tiles = availableTiles, usedIds = state.usedTileIds, onTap = vm::tapTile)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = vm::removeLastTile) { Text("Undo") }
        Button(onClick = vm::submitAnagram, colors = ButtonDefaults.buttonColors(containerColor = ColorAmberLight)) {
            Text("Submit", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TilePool(tiles: List<LetterTile>, usedIds: List<Int>, onTap: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.forEach { tile ->
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(ColorAmber)
                    .clickable { onTap(tile.id) },
                Alignment.Center
            ) { Text(tile.letter.uppercaseChar().toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
        }
    }
}