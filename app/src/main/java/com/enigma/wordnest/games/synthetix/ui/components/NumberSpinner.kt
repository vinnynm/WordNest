package com.enigma.wordnest.games.synthetix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberSpinner(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { if (value - step >= range.first) onValueChange(value - step) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { if (value + step <= range.last) onValueChange(value + step) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
