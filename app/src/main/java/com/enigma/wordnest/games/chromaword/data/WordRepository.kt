package com.enigma.wordnest.games.chromaword.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var allWords: List<String> = emptyList()
    private var wordSet: Set<String> = emptySet()

    // words grouped by length for random target selection
    private var byLength: Map<Int, List<String>> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords()
        allWords = raw.map { it.trim().lowercase() }.distinct().sorted()
        wordSet  = allWords.toHashSet()
        byLength = allWords.groupBy { it.length }
    }

    fun isLoaded() = allWords.isNotEmpty()
    fun contains(word: String) = wordSet.contains(word.lowercase())

    /**
     * Pick a random word of a random length (weighted by pool size so
     * common lengths appear more often).
     */
    fun randomWord(allowedLengths: IntRange = 4..8): String {
        val pool = byLength
            .filterKeys { it in allowedLengths }
            .values.flatten()
        require(pool.isNotEmpty()) { "No words in length range $allowedLengths" }
        return pool.random()
    }

    /** Available lengths in the dictionary */
    fun availableLengths(): List<Int> = byLength.keys.sorted()
}
