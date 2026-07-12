package com.enigma.wordnest.games.synthetix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.synthetix.ui.theme.BoardTheme
import com.enigma.wordnest.games.synthetix.ui.theme.drawTileTexture

// ─────────────────────────────────────────────────────────────────────────────
//  Themed Rack
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemedRack(
    rack: List<Char>,
    selectedTile: Int?,
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
                ThemedTilePiece(
                    letter     = letter,
                    points     = getLetterPointsInternal(letter),
                    isSelected = selectedTile == index,
                    onClick    = { onTileClick(index) },
                    theme      = theme
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Themed Tile Piece
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemedTilePiece(
    letter: Char,
    points: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: BoardTheme,
    sizeOverride: Dp = 54.dp
) {
    val px = sizeOverride.value

    Box(
        modifier = Modifier
            .size(sizeOverride)
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
        // Base + texture via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(theme.tileFaceColor)
            drawTileTexture(theme.tileTextureType, size.width, isNew = false)
        }

        // Letter + points overlay
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

        // Selected glow overlay
        if (isSelected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(theme.tileSelectedBorder.copy(alpha = 0.12f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Themed Player Score Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemedPlayerScoreCard(
    name: String,
    score: Int,
    isActive: Boolean,
    theme: BoardTheme,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) theme.primaryAccent else theme.gridLineColor,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) theme.activePlayerCard else theme.inactivePlayerCard,
        tonalElevation = if (isActive) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                name,
                fontSize     = 10.sp,
                letterSpacing = 0.8.sp,
                color        = if (isActive) theme.primaryAccent else theme.onSurfaceMuted
            )
            Text(
                score.toString(),
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isActive) theme.onSurface else theme.onSurfaceMuted
            )
        }
    }
}

// ── Internal letter points (no external import needed) ──────────────────────

private fun getLetterPointsInternal(letter: Char): Int = when (letter.uppercaseChar()) {
    'A','E','I','O','U','L','N','S','T','R' -> 1
    'D','G'                                  -> 2
    'B','C','M','P'                          -> 3
    'F','H','V','W','Y'                      -> 4
    'K'                                      -> 5
    'J','X'                                  -> 8
    'Q','Z'                                  -> 10
    else                                     -> 0
}
