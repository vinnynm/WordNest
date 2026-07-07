package com.enigma.wordnest.games.codeword.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.games.codeword.ui.theme.ColorVioletLight

/** A 26-letter grid picker — same interaction shape as Lexicon's blank-tile picker dialog. */
@Composable
fun LetterPickerDialog(
    number: Int,
    onLetterChosen: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    val letters = ('A'..'Z').toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What letter is $number?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(letters) { letter ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ColorVioletLight.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .clickable { onLetterChosen(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letter.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
