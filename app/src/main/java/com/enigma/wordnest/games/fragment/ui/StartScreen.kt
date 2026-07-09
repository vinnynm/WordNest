package com.enigma.wordnest.games.fragment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enigma.wordnest.games.fragment.model.WORD_LENGTH_OPTIONS
import com.enigma.wordnest.games.fragment.ui.theme.ColorAmberLight
import com.enigma.wordnest.games.fragment.ui.theme.ColorSubtle

@Composable
fun FragmentStartScreen(isGenerating: Boolean, onStart: (Int) -> Unit) {
    var wordLength by remember { mutableIntStateOf(6) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement=  Arrangement.Center) {
        Text("FRAGMENT", fontSize = 40.sp, fontWeight = FontWeight.Black, color = ColorAmberLight, letterSpacing = 2.sp)
        Text("Solve the clues, then anagram the mystery word", fontSize = 14.sp, color = ColorSubtle,
            modifier = Modifier.padding(bottom = 40.dp))
        Text("WORD LENGTH", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 32.dp)) {
            WORD_LENGTH_OPTIONS.forEach { len ->
                val selected = wordLength == len
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ColorAmberLight else Color(0xFF2A2118))
                        .clickable { wordLength = len },
                    Alignment.Center
                ) { Text(len.toString(), color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        Button(
            onClick = { onStart(wordLength) }, enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorAmberLight)
        ) {
            if (isGenerating) CircularProgressIndicator(Modifier.size(20.dp), Color.Black, strokeWidth = 2.dp)
            else Text("START GAME", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
fun FragmentHowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("How To Play", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                }
                Text("You're shown a set of clue words with a few letters blanked out. Every blank is a letter from the hidden mystery word.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Solve each clue word to collect its letters into a jumbled pool. Once every clue is solved, arrange the pool into the mystery word.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Watch out: some letters are deliberately blanked in more than one clue, so you may end up with extra tiles you don't need.",
                    style = MaterialTheme.typography.bodySmall)
                Text("A wrong clue guess or a wrong arrangement costs a life — you have 6 total.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}