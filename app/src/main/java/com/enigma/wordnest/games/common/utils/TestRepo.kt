package com.enigma.wordnest.games.common.utils

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import com.enigma.wordnest.games.wordladder.model.WordLadderSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TestRepo(val context: Context,

): RepositoryInterface{
    private val optimizedRepo = OptimizedWordRepository(context)
    override suspend fun getAllWOrds(): Set<String> {

        return load()
    }

    override var allWords = emptyList<String>()
        get() = field

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords()
        val clean = raw.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        byLength = clean.groupBy { it.length }.mapValues { it.value.toSet() }
        return@withContext raw

    }

    override var byLength: Map<Int, Set<String>> = emptyMap()

    /** word length -> adjacency graph (built lazily per length, only once needed) */
    private val adjacencyCache = mutableMapOf<Int, Map<String, List<String>>>()



    override fun availableLengths(): List<Int> = byLength.keys.sorted()

    /** Builds (and caches) the differs-by-one adjacency graph for a given word length. */
    suspend fun adjacencyFor(length: Int): Map<String, List<String>> = withContext(Dispatchers.Default) {
        adjacencyCache.getOrPut(length) {
            WordLadderSolver.buildAdjacency(wordsOfLength(length))
        }
    }

}


