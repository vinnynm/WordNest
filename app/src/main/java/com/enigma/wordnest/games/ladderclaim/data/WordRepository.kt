package com.enigma.wordnest.games.ladderclaim.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var dictionary: Set<String> = emptySet()
    private var byLength: Map<Int, List<String>> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        // Prefer the large Scrabble-legal library (needed since any connecting
        // word is legal here, same as Lexicon's word-legality check).
        val large = optimizedRepo.getAllWordsLargeLibrary()
        dictionary = large.ifEmpty { optimizedRepo.getAllWords() }
        byLength = dictionary.groupBy { it.length }
    }

    fun isLoaded() = dictionary.isNotEmpty()
    fun dictionarySet(): Set<String> = dictionary

    fun randomSeedWord(length: Int = 4): String {
        val pool = byLength[length] ?: byLength.values.flatten()
        return pool.random()
    }
}