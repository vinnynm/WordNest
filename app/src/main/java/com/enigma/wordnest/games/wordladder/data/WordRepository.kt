package com.enigma.wordnest.games.wordladder.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import com.enigma.wordnest.games.wordladder.model.WordLadderSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var byLength: Map<Int, Set<String>> = emptyMap()

    /** word length -> adjacency graph (built lazily per length, only once needed) */
    private val adjacencyCache = mutableMapOf<Int, Map<String, List<String>>>()

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords()
        val clean = raw.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        byLength = clean.groupBy { it.length }.mapValues { it.value.toSet() }
    }

    fun isLoaded() = byLength.isNotEmpty()

    fun wordsOfLength(length: Int): Set<String> = byLength[length] ?: emptySet()

    fun contains(word: String): Boolean = byLength[word.length]?.contains(word.lowercase()) == true

    fun availableLengths(): List<Int> = byLength.keys.sorted()

    /** Builds (and caches) the differs-by-one adjacency graph for a given word length. */
    suspend fun adjacencyFor(length: Int): Map<String, List<String>> = withContext(Dispatchers.Default) {
        adjacencyCache.getOrPut(length) {
            WordLadderSolver.buildAdjacency(wordsOfLength(length))
        }
    }
}
