package com.enigma.wordnest.games.absurdle.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /** Is this word in the dictionary (any length)? */
    fun contains(word: String): Boolean =
        byLength[word.length]?.contains(word.lowercase()) == true

    /** Available lengths sorted */
    fun availableLengths(): List<Int> = byLength.keys.sorted()

    /** Count of words at a given length */
    fun countOfLength(length: Int): Int = byLength[length]?.size ?: 0
}
