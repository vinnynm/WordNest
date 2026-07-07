package com.enigma.wordnest.games.crossword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.crossword.model.ClueDirection
import com.enigma.wordnest.games.crossword.model.CrosswordClue
import com.enigma.wordnest.games.crossword.ui.theme.ColorAccent

/** Scrolling clue list — a LazyColumn here is legitimate (items scroll in/out of view). */
@Composable
fun ClueList(
    acrossClues: List<CrosswordClue>,
    downClues: List<CrosswordClue>,
    activeClue: CrosswordClue?,
    onClueClick: (CrosswordClue) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { SectionHeader("Across") }
        items(acrossClues, key = { "A${it.number}" }) { clue ->
            ClueRow(clue, isActive = clue == activeClue, onClick = { onClueClick(clue) })
        }
        item { Spacer(Modifier.height(8.dp)); SectionHeader("Down") }
        items(downClues, key = { "D${it.number}" }) { clue ->
            ClueRow(clue, isActive = clue == activeClue, onClick = { onClueClick(clue) })
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = ColorAccent,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ClueRow(clue: CrosswordClue, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) ColorAccent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text("${clue.number}.", fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.width(28.dp))
        Text(clue.clueText, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
    }
}
