package com.enigma.wordnest.games.chromaword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.chromaword.ui.theme.ColorGreen
import com.enigma.wordnest.games.chromaword.ui.theme.ColorLightBlue
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRedAbsent
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRedExtra
import com.enigma.wordnest.games.chromaword.ui.theme.ColorRoyalBlue
import com.enigma.wordnest.games.chromaword.ui.theme.ColorYellow

@Composable
fun ColourLegendRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(ColorGreen,     "Right place")
        LegendDot(ColorYellow,    "Wrong place")
        LegendDot(ColorLightBlue, "Partial ✓")
        LegendDot(ColorRoyalBlue, "All wrong")
        LegendDot(ColorRedExtra,  "Surplus")
        LegendDot(ColorRedAbsent, "Absent")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            fontWeight = FontWeight.Medium
        )
    }
}
