package com.enigma.wordnest.games.absurdle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enigma.wordnest.games.absurdle.model.WORD_LENGTH_OPTIONS
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurple

@Composable
fun SettingsDialog(
    wordLength: Int,
    hardMode: Boolean,
    showCandidateCount: Boolean,
    onWordLength: (Int) -> Unit,
    onHardMode: (Boolean) -> Unit,
    onToggleCandidateCount: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }

                // Word length
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Word length", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Changes start a new game",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WORD_LENGTH_OPTIONS.forEach { len ->
                            val selected = len == wordLength
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) ColorPurple else Color.Transparent)
                                    .border(
                                        if (selected) 0.dp else 1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onWordLength(len) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$len",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (selected) Color.White
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // Hard mode
                ToggleRow(
                    label    = "Hard mode",
                    subtitle = "Engine avoids winning immediately unless forced",
                    checked  = hardMode,
                    onToggle = onHardMode
                )

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // Candidate count
                ToggleRow(
                    label    = "Show candidate count",
                    subtitle = "Displays remaining possible words after each guess",
                    checked  = showCandidateCount,
                    onToggle = { onToggleCandidateCount() }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                checkedTrackColor = ColorPurple)
        )
    }
}
