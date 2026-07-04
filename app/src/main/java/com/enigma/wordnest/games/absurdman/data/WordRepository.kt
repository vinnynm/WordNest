package com.enigma.wordnest.games.absurdman.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around OptimizedWordRepository, grouped by length —
 * same shape as Absurdle's WordRepository, since Absurdman needs the
 * identical "all words of length N" candidate pool.
 */
class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var byLength: Map<Int, Set<String>> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords()
        val clean = raw.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        byLength = clean.groupBy { it.length }.mapValues { it.value.toSet() }
    }

    fun isLoaded() = byLength.isNotEmpty()

    /** All words of a given length — the initial candidate set */
    fun wordsOfLength(length: Int): Set<String> = byLength[length] ?: emptySet()

    fun countOfLength(length: Int): Int = byLength[length]?.size ?: 0

    fun availableLengths(): List<Int> = byLength.keys.sorted()
}
