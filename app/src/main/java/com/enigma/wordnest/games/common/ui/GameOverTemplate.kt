package com.enigma.wordnest.games.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameOverTemplate(
    title: String,
    titleColor: Color,
    subtitle: String? = null,
    onNewGame: () -> Unit,
    onShare: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = titleColor)
            
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            content()

            HorizontalDivider(
                Modifier.padding(vertical = 4.dp),
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
                Button(
                    onClick = onNewGame,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Filled.Replay, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New game")
                }
            }
        }
    }
}
