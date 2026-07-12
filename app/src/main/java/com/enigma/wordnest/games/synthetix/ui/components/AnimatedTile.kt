package com.enigma.wordnest.games.synthetix.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.model.PlacedTile
import com.enigma.wordnest.games.synthetix.model.SquareType
import com.enigma.wordnest.games.synthetix.model.Tile
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme
import com.enigma.wordnest.games.synthetix.ui.theme.drawBoardTexture
import com.enigma.wordnest.games.synthetix.ui.theme.drawTileTexture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  Animated Board
//  Tracks which (row,col) positions are newly placed this turn and plays a
//  drop-in animation for each. Tiles already on the board are static.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedBoard(
    board: List<List<Tile?>>,
    boardConfig: BoardConfig,
    placedThisTurn: List<PlacedTile>,
    selectedTile: Int?,
    onCellClick: (Int, Int) -> Unit,
    cellSize: Dp,
    theme: BoardTheme
) {
    val size = boardConfig.size

    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
    ) {
        for (row in 0 until size) {
            Row {
                for (col in 0 until size) {
                    val placedTile = placedThisTurn.find { it.row == row && it.col == col }
                    val displayTile = board[row][col] ?: placedTile?.let {
                        Tile(it.letter, it.points, it.isBlank)
                    }
                    val isNew = placedTile != null

                    AnimatedBoardCell(
                        row              = row,
                        col              = col,
                        tile             = displayTile,
                        squareType       = boardConfig.squareAt(row, col),
                        isPlacedThisTurn = isNew,
                        onCellClick      = onCellClick,
                        size             = cellSize,
                        theme            = theme
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AnimatedBoardCell
//  When [isPlacedThisTurn] flips to true the tile plays a quick scale+fade
//  drop-in. Recall (flip back to false) plays a reverse fade.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedBoardCell(
    row: Int,
    col: Int,
    tile: Tile?,
    squareType: SquareType,
    isPlacedThisTurn: Boolean,
    onCellClick: (Int, Int) -> Unit,
    size: Dp,
    theme: BoardTheme
) {
    val px = size.value

    // Animate scale: new tiles drop in from 0 → 1, recalled tiles fade 1 → 0
    val animScale by animateFloatAsState(
        targetValue = if (tile != null && isPlacedThisTurn) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "tileScale"
    )

    // Track previous placement state to decide entry direction
    var wasPlaced by remember { mutableStateOf(false) }
    val tileVisible = tile != null && isPlacedThisTurn

    val dropAnim = remember { Animatable(if (tileVisible) 0f else 1f) }

    LaunchedEffect(tileVisible) {
        if (tileVisible && !wasPlaced) {
            // Entry: scale from 0.4 and drop from slight offset
            dropAnim.snapTo(0f)
            dropAnim.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
        } else if (!tileVisible && wasPlaced) {
            // Recall: quick fade-scale out
            dropAnim.animateTo(
                0f,
                animationSpec = tween(durationMillis = 120, easing = EaseOut)
            )
        }
        wasPlaced = tileVisible
    }

    val entryScale  = if (isPlacedThisTurn && tile != null) lerp(0.45f, 1f, dropAnim.value) else 1f
    val entryAlpha  = if (isPlacedThisTurn && tile != null) dropAnim.value.coerceIn(0f, 1f) else 1f
    val entryOffset = if (isPlacedThisTurn && tile != null) lerp(-px * 0.3f, 0f, dropAnim.value) else 0f

    val baseBg = when {
        tile != null && isPlacedThisTurn -> theme.tileNewColor
        tile != null                     -> theme.tileFaceColor
        squareType.isObstacle            -> theme.obsColor
        squareType.isNegative            -> theme.negColor
        squareType.isAnchor              -> theme.anchorColor
        else -> when (squareType) {
            SquareType.DL -> theme.dlColor
            SquareType.TL -> theme.tlColor
            SquareType.QL -> theme.qlColor
            SquareType.DW -> theme.dwColor
            SquareType.TW -> theme.twColor
            SquareType.QW -> theme.qwColor
            SquareType.VW -> theme.vwColor
            else          -> theme.emptySquare
        }
    }

    val borderColor = when {
        isPlacedThisTurn     -> theme.tileBorderColor
        squareType.isObstacle -> theme.obsColor.copy(alpha = 0.3f)
        else                 -> theme.gridLineColor
    }
    val borderWidth = if (isPlacedThisTurn) 1.5.dp else 0.5.dp

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX        = entryScale
                scaleY        = entryScale
                alpha         = entryAlpha
                translationY  = entryOffset
            }
            .border(borderWidth, borderColor, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .then(
                if (isPlacedThisTurn) Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(3.dp))
                else Modifier
            )
            .clickable(enabled = !squareType.isObstacle) { onCellClick(row, col) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(baseBg)
            if (squareType.isObstacle && tile == null) {
                drawObstacleHatch(this.size.width, this.size.height)
            } else if (tile != null) {
                drawTileTexture(theme.tileTextureType, this.size.width, isPlacedThisTurn)
            } else if (squareType == SquareType.EMPTY) {
                clipRect { drawBoardTexture(theme.boardTextureType) }
            }
        }

        when {
            squareType.isObstacle && tile == null -> { /* canvas draws the hatch */ }
            tile != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text       = tile.letter.toString(),
                        fontSize   = (px * 0.42).sp,
                        fontWeight = FontWeight.Bold,
                        color      = theme.tileTextColor
                    )
                    if (!tile.isBlank) {
                        Text(
                            text     = tile.points.toString(),
                            fontSize = (px * 0.16).sp,
                            color    = theme.tilePointsColor
                        )
                    }
                }
            }
            squareType.isAnchor -> {
                Text("★", fontSize = (px * 0.42).sp, color = theme.premiumLabelLight, fontWeight = FontWeight.Bold)
            }
            squareType != SquareType.EMPTY && !squareType.isObstacle && !squareType.isNegative -> {
                Text(
                    text       = squareType.code.uppercase(),
                    fontSize   = (px * 0.20).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = theme.labelColorForSquare(squareType)
                )
            }
            squareType.isNegative -> {
                Text("−", fontSize = (px * 0.38).sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6060))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Animated Rack
//  Each tile slides in from below when added and bounces on selection.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedRack(
    rack: List<Char>,
    selectedTile: Int?,
    tilePoints: (Char) -> Int,
    onTileClick: (Int) -> Unit,
    theme: BoardTheme,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.rackBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = theme.rackBackground,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scrollable(rememberScrollableState { 0f }, Orientation.Horizontal)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            rack.forEachIndexed { index, letter ->
                AnimatedRackTile(
                    letter     = letter,
                    points     = tilePoints(letter),
                    isSelected = selectedTile == index,
                    onClick    = { onTileClick(index) },
                    theme      = theme,
                    index      = index
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AnimatedRackTile
//  Slides in from below on first appearance; bounces scale on selection toggle.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedRackTile(
    letter: Char,
    points: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: BoardTheme,
    index: Int,
    sizeOverride: Dp = 54.dp
) {
    val px = sizeOverride.value

    // Slide-in on first composition, staggered by index
    val slideAnim = remember { Animatable(60f) }
    val fadeAnim  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Stagger each tile slightly so they cascade in
        delay(index * 40L)
        val spec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        slideAnim.animateTo(0f, animationSpec = spec)
        fadeAnim.animateTo(1f, animationSpec = tween(120))
    }

    // Selection bounce
    val selectScale by animateFloatAsState(
        targetValue   = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label         = "rackSelectScale"
    )

    Box(
        modifier = Modifier
            .size(sizeOverride)
            .graphicsLayer {
                translationY = slideAnim.value
                alpha        = fadeAnim.value
                scaleX       = selectScale
                scaleY       = selectScale
            }
            .shadow(if (isSelected) 10.dp else 3.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) theme.tileSelectedBorder else theme.tileBorderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(theme.tileFaceColor)
            drawTileTexture(theme.tileTextureType, size.width, isNew = false)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = if (letter == '?') "★" else letter.toString(),
                fontSize   = (px * 0.44).sp,
                fontWeight = FontWeight.Bold,
                color      = theme.tileTextColor
            )
            if (letter != '?') {
                Text(
                    text     = points.toString(),
                    fontSize = (px * 0.18).sp,
                    color    = theme.tilePointsColor
                )
            }
        }

        if (isSelected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(theme.tileSelectedBorder.copy(alpha = 0.12f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Score pop animation — shows +N flying up when a word is played
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScorePopLabel(score: Int, theme: BoardTheme) {
    if (score <= 0) return

    val offsetY = remember { Animatable(0f) }
    val alpha   = remember { Animatable(1f) }

    LaunchedEffect(score) {
        coroutineScope {
            launch { offsetY.animateTo(-80f, tween(900, easing = EaseOut)) }
            launch {
                delay(400)
                alpha.animateTo(0f, tween(500))
            }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer { translationY = offsetY.value; this.alpha = alpha.value }
            .padding(4.dp)
    ) {
        Text(
            text       = "+$score",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Black,
            color      = theme.primaryAccent
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun lerp(start: Float, stop: Float, fraction: Float) =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun DrawScope.drawObstacleHatch(w: Float, h: Float) {
    val lineColor = Color(0x40FFFFFF)
    val spacing   = w * 0.28f
    var d = -h
    while (d < w + h) {
        drawLine(lineColor, Offset(d, 0f), Offset(d + h, h), strokeWidth = w * 0.08f)
        d += spacing
    }
    d = w + h
    while (d > -h) {
        drawLine(Color(0x28FFFFFF), Offset(d, 0f), Offset(d - h, h), strokeWidth = w * 0.05f)
        d -= spacing * 1.4f
    }
    drawRect(Color(0x22000000), size = Size(w, h))
}
