package com.enigma.wordnest.games.absurdle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.absurdle.model.WORD_LENGTH_OPTIONS
import com.enigma.wordnest.games.absurdle.ui.theme.ColorPurpleLight
import com.enigma.wordnest.games.absurdle.ui.theme.ColorSubtle

@Composable
fun AbsurdleStartScreen(
    onStartGame: (Int, Boolean) -> Unit
) {
    var wordLength by remember { mutableIntStateOf(5) }
    var hardMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ABSURDLE",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = ColorPurpleLight,
            letterSpacing = 4.sp
        )
        Text(
            text = "The adversarial word game",
            fontSize = 16.sp,
            color = ColorSubtle,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Text(
            text = "WORD LENGTH",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            WORD_LENGTH_OPTIONS.forEach { length ->
                val selected = wordLength == length
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ColorPurpleLight else Color(0xFF2A2A2C))
                        .clickable { wordLength = length },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = length.toString(),
                        color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2A2C))
                .clickable { hardMode = !hardMode }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Hard Mode",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Any revealed hints must be used",
                    color = ColorSubtle,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = hardMode,
                onCheckedChange = { hardMode = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorPurpleLight,
                    checkedTrackColor = ColorPurpleLight.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onStartGame(wordLength, hardMode) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorPurpleLight),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "START GAME",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}
