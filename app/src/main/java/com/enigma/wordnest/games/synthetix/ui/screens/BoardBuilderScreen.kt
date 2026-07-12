package com.enigma.wordnest.games.synthetix.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.SquareType
import com.enigma.wordnest.games.synthetix.model.SymmetryEngine
import com.enigma.wordnest.games.synthetix.model.SymmetryMode
import com.enigma.wordnest.games.synthetix.ui.SynthetixViewModel
import com.enigma.wordnest.games.synthetix.ui.GameUiState
import com.enigma.wordnest.games.synthetix.ui.components.NumberSpinner
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme

// ─────────────────────────────────────────────────────────────────────────────
//  Board Builder Screen  (with symmetry enforcement)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardBuilderScreen(
    viewModel: SynthetixViewModel,
    uiState: GameUiState,
    theme: BoardTheme,
    onBack: () -> Unit
) {
    val builder = uiState.boardBuilder
    val haptic  = LocalHapticFeedback.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }
    var showSymmetryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBoard(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBoard(it) } }

    // Symmetry validity badge
    val isSymmetric = SymmetryEngine.isSymmetric(
        builder.grid, builder.size, builder.symmetryMode
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Board Builder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    // Symmetry toggle button
                    IconButton(onClick = { showSymmetryDialog = true }) {
                        Text(builder.symmetryMode.emoji, fontSize = 18.sp)
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.Upload, "Import JSON")
                    }
                    IconButton(onClick = { exportLauncher.launch("${builder.boardName}.json") }) {
                        Icon(Icons.Default.Download, "Export JSON")
                    }
                    IconButton(onClick = { showNameDialog = true }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Board metadata ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = builder.boardName,
                        onValueChange = { viewModel.updateBoardBuilder { copy(boardName = it) } },
                        label = { Text("Board Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSizeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("${builder.size}×${builder.size}") }

                        NumberSpinner(
                            label = "Rack", value = builder.rackSize, range = 5..10,
                            onValueChange = { viewModel.updateBoardBuilder { copy(rackSize = it) } },
                            modifier = Modifier.weight(1f)
                        )
                        NumberSpinner(
                            label = "Bag", value = builder.bagSize, range = 50..200, step = 10,
                            onValueChange = { viewModel.updateBoardBuilder { copy(bagSize = it) } },
                            modifier = Modifier.weight(1f)
                        )
                        NumberSpinner(
                            label = "Bingo", value = builder.bingoBonus, range = 0..100, step = 5,
                            onValueChange = { viewModel.updateBoardBuilder { copy(bingoBonus = it) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Symmetry status strip ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSymmetric || builder.symmetryMode == SymmetryMode.NONE)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(builder.symmetryMode.emoji, fontSize = 12.sp)
                        Text(
                            builder.symmetryMode.label,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (builder.symmetryMode != SymmetryMode.NONE) {
                            Text(
                                if (isSymmetric) "✓" else "⚠",
                                fontSize = 11.sp,
                                color = if (isSymmetric) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Quick "Fix symmetry" button when mismatched
                if (!isSymmetric && builder.symmetryMode != SymmetryMode.NONE) {
                    TextButton(
                        onClick = { viewModel.applySymmetryToBoard() },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("Fix", fontSize = 11.sp) }
                }

                Spacer(Modifier.weight(1f))

                // Clear board button
                TextButton(
                    onClick = { viewModel.clearBuilderBoard() },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
            }

            // ── Square type palette ───────────────────────────────────────────
            Text(
                "Paint Square Type",
                fontSize = 12.sp, letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(SquareType.paletteOrder) { sq ->
                    PaletteChip(
                        squareType = sq,
                        isSelected = sq == builder.selectedSquareType,
                        theme      = theme,
                        onClick    = { viewModel.updateBoardBuilder { copy(selectedSquareType = sq) } }
                    )
                }
            }

            // ── Grid ──────────────────────────────────────────────────────────
            val cellSizeDp = (340f / builder.size).coerceIn(14f, 40f).dp
            Surface(
                modifier = Modifier.padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp)
                ) {
                    for (r in 0 until builder.size) {
                        Row {
                            for (c in 0 until builder.size) {
                                val code = builder.grid[r][c]
                                val sq   = SquareType.fromCode(code)

                                // Highlight mirror cells of the current hover
                                BuilderCell(
                                    squareType = sq,
                                    theme = theme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.toggleBuilderCell(r, c)
                                    },
                                    sizeDp = cellSizeDp
                                )
                            }
                        }
                    }
                }
            }

            // ── Apply button ──────────────────────────────────────────────────
            Button(
                onClick = { viewModel.selectBoard(viewModel.buildConfigFromBuilder()) },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) { Text("USE THIS BOARD") }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Save dialog ───────────────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Save Board") },
            text = {
                OutlinedTextField(
                    value = builder.boardName,
                    onValueChange = { viewModel.updateBoardBuilder { copy(boardName = it) } },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveBoard(viewModel.buildConfigFromBuilder())
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Size dialog ───────────────────────────────────────────────────────────
    if (showSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSizeDialog = false },
            title = { Text("Board Size") },
            text = {
                Column {
                    listOf(11, 13, 15, 17, 19, 21, 25).forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.resizeBoardBuilder(s); showSizeDialog = false }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${s}×${s}")
                            if (s == builder.size)
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSizeDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Symmetry dialog ───────────────────────────────────────────────────────
    if (showSymmetryDialog) {
        AlertDialog(
            onDismissRequest = { showSymmetryDialog = false },
            title = { Text("Board Symmetry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Symmetry automatically mirrors painted squares so your board is balanced.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SymmetryMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateBoardBuilder { copy(symmetryMode = mode) }
                                    showSymmetryDialog = false
                                }
                                .background(
                                    if (mode == builder.symmetryMode)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(mode.emoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))
                            Column {
                                Text(mode.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(mode.description, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.weight(1f))
                            if (mode == builder.symmetryMode)
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSymmetryDialog = false }) { Text("Close") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helper Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaletteChip(
    squareType: SquareType,
    isSelected: Boolean,
    theme: BoardTheme,
    onClick: () -> Unit
) {
    val bgColor = theme.colorForSquare(squareType)
    val labelColor = theme.labelColorForSquare(squareType)

    Surface(
        modifier = Modifier
            .width(72.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        color = bgColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = if (squareType == SquareType.ANCHOR) "★" else squareType.code.uppercase(),
                color = labelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = squareType.displayName,
                color = labelColor.copy(alpha = 0.7f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                lineHeight = 9.sp
            )
        }
    }
}

@Composable
fun BuilderCell(
    squareType: SquareType,
    theme: BoardTheme,
    onClick: () -> Unit,
    sizeDp: Dp
) {
    val bgColor = theme.colorForSquare(squareType)
    val labelColor = theme.labelColorForSquare(squareType)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .background(bgColor)
            .border(0.5.dp, theme.gridLineColor.copy(alpha = 0.3f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            squareType.isAnchor -> {
                Text("★", color = labelColor, fontSize = (sizeDp.value * 0.5f).sp, fontWeight = FontWeight.Bold)
            }
            squareType.isObstacle -> {
                Text("✕", color = labelColor.copy(alpha = 0.4f), fontSize = (sizeDp.value * 0.4f).sp)
            }
            squareType != SquareType.EMPTY -> {
                Text(
                    text = squareType.code.uppercase(),
                    color = labelColor,
                    fontSize = (sizeDp.value * 0.3f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
