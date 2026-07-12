package com.enigma.wordnest.games.synthetix.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.LetterConfig
import com.enigma.wordnest.games.synthetix.model.TileSetConfig
import com.enigma.wordnest.games.synthetix.ui.SynthetixViewModel
import com.enigma.wordnest.games.synthetix.ui.GameUiState
import com.enigma.wordnest.games.synthetix.ui.components.NumberSpinner

// ─────────────────────────────────────────────────────────────────────────────
//  Tile Set Editor Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileSetEditorScreen(
    viewModel: SynthetixViewModel,
    uiState: GameUiState,
    onBack: () -> Unit
) {
    var tileSetName by remember { mutableStateOf(uiState.activeTileSet.name) }
    var rackSize    by remember { mutableStateOf(uiState.activeTileSet.rackSize) }
    var bagSize     by remember { mutableStateOf(uiState.activeTileSet.bagSize) }
    var bingoBonus  by remember { mutableStateOf(uiState.activeTileSet.bingoBonus) }

    // Local letter list — mutable
    var letters by remember {
        mutableStateOf(uiState.activeTileSet.letters.map { it.copy() })
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

    val totalTiles = letters.sumOf { it.count }

    fun buildConfig() = TileSetConfig(
        name       = tileSetName,
        rackSize   = rackSize,
        bagSize    = bagSize,
        bingoBonus = bingoBonus,
        letters    = letters
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tile Set Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        letters = TileSetConfig.defaultLetters()
                        rackSize = 7; bagSize = 100; bingoBonus = 50
                    }) { Icon(Icons.Default.RestartAlt, "Reset to default") }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Global settings ───────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = tileSetName,
                            onValueChange = { tileSetName = it },
                            label = { Text("Tile Set Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NumberSpinner(
                                label = "Rack Size",
                                value = rackSize,
                                range = 5..10,
                                onValueChange = { rackSize = it },
                                modifier = Modifier.weight(1f)
                            )
                            NumberSpinner(
                                label = "Bingo Bonus",
                                value = bingoBonus,
                                range = 0..100,
                                step = 5,
                                onValueChange = { bingoBonus = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Total tiles: $totalTiles",
                            fontSize = 13.sp,
                            color = if (totalTiles in 80..120) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Column headers ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Letter", modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("Count", modifier = Modifier.weight(2f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Points", modifier = Modifier.weight(2f), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            // ── Letter rows ───────────────────────────────────────────────────
            items(letters.indices.toList()) { idx ->
                val lc = letters[idx]
                LetterRow(
                    config = lc,
                    onCountChange = { newCount ->
                        letters = letters.toMutableList().also { it[idx] = lc.copy(count = newCount) }
                    },
                    onPointsChange = { newPts ->
                        letters = letters.toMutableList().also { it[idx] = lc.copy(points = newPts) }
                    }
                )
            }

            // ── Apply button ──────────────────────────────────────────────────
            item {
                Button(
                    onClick = { viewModel.saveTileSet(buildConfig()) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("APPLY TILE SET") }
            }

            // ── Load saved ────────────────────────────────────────────────────
            if (uiState.savedTileSetNames.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { showLoadDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) { Text("LOAD SAVED TILE SET") }
                }
            }
        }
    }

    // ── Save dialog ───────────────────────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Tile Set") },
            text = {
                OutlinedTextField(
                    value = tileSetName,
                    onValueChange = { tileSetName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveTileSet(buildConfig())
                    showSaveDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Load dialog ───────────────────────────────────────────────────────────
    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Load Tile Set") },
            text = {
                Column {
                    uiState.savedTileSetNames.forEach { name ->
                        TextButton(
                            onClick = {
                                viewModel.loadTileSet(name)
                                showLoadDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLoadDialog = false }) { Text("Close") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Single letter row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LetterRow(
    config: LetterConfig,
    onCountChange: (Int) -> Unit,
    onPointsChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Letter badge
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (config.letter == '?') "★" else config.letter.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(8.dp))

        // Count spinner
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(
                onClick = { if (config.count > 0) onCountChange(config.count - 1) },
                modifier = Modifier.size(28.dp)
            ) { Text("−") }
            Text(
                config.count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { if (config.count < 20) onCountChange(config.count + 1) },
                modifier = Modifier.size(28.dp)
            ) { Text("+") }
        }

        // Points spinner
        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(
                onClick = { if (config.points > 0) onPointsChange(config.points - 1) },
                modifier = Modifier.size(28.dp)
            ) { Text("−") }
            Text(
                config.points.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { if (config.points < 20) onPointsChange(config.points + 1) },
                modifier = Modifier.size(28.dp)
            ) { Text("+") }
        }
    }
}
