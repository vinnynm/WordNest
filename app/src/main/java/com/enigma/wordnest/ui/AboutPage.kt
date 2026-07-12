package com.enigma.wordnest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.wordnest.ui.theme.NestSubtle

/**
 * About / Attributions screen.
 *
 * Exists primarily to satisfy WordNet's license requirement that its copyright
 * notice ship with any distributed product using its data (used by Crossword's
 * clue definitions). Also credits the base dictionary source and lists the
 * open-source libraries the app depends on.
 *
 * Wire into MainActivity's NavHost, e.g.:
 *   composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
 * and add a route/entry point from HomeScreen (e.g. a small "About" text link
 * or icon button near the top of the games list).
 */

private data class AttributionEntry(
    val title: String,
    val body: String
)

private val attributions = listOf(
    AttributionEntry(
        title = "WordNet® 3.1",
        body = "Crossword's clue definitions are sourced from WordNet, a lexical " +
                "database of English maintained by Princeton University.\n\n" +
                "\"This software and database is being provided to you, the LICENSEE, by " +
                "Princeton University under the following license. By obtaining, using " +
                "and/or copying this software and database, you agree that you have read, " +
                "understood, and will comply with these terms and conditions.:\n\n" +
                "Permission to use, copy, modify and distribute this software and database " +
                "and its documentation for any purpose and without fee or royalty is hereby " +
                "granted, provided that you agree to comply with the following copyright " +
                "notice and statements, including the disclaimer, and that the same appear " +
                "on ALL copies of the software, database and documentation, including " +
                "modifications that you make for internal use or for distribution.\n\n" +
                "WordNet 3.1 Copyright 2011 by Princeton University. All rights reserved.\n\n" +
                "THIS SOFTWARE AND DATABASE IS PROVIDED \"AS IS\" AND PRINCETON UNIVERSITY " +
                "MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED. BY WAY OF " +
                "EXAMPLE, BUT NOT LIMITATION, PRINCETON UNIVERSITY MAKES NO REPRESENTATIONS " +
                "OR WARRANTIES OF MERCHANT-ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR " +
                "THAT THE USE OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION WILL NOT " +
                "INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.\"\n\n" +
                "See https://wordnet.princeton.edu/license-and-commercial-use for full terms."
    ),
    AttributionEntry(
        title = "Dictionary word list",
        body = "WordNest's core word list is derived from a large open word list, " +
                "cleaned and curated with the app's own filtering tools before use. " +
                "No proprietary or tournament-specific word list is used."
    ),
    AttributionEntry(
        title = "Open source libraries",
        body = "WordNest is built with Jetpack Compose, Kotlin Coroutines, " +
                "AndroidX DataStore, Room, and Gson, each licensed under the " +
                "Apache License 2.0 by their respective maintainers."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Attributions", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "WordNest 💌",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "A collection of original and reimagined word games.",
                        fontSize = 13.sp,
                        color = NestSubtle,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            items(attributions) { entry ->
                AttributionCard(entry)
            }

            item {
                Text(
                    "Thank you for playing.",
                    fontSize = 12.sp,
                    color = NestSubtle,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AttributionCard(entry: AttributionEntry) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                entry.body,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}