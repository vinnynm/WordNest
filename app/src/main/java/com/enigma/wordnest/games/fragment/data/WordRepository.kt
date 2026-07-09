package com.enigma.wordnest.games.fragment.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var byLength: Map<Int, List<String>> = emptyMap()
    private var wordSet: Set<String> = emptySet()

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords().map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        byLength = raw.groupBy { it.length }
        wordSet = raw.toHashSet()
    }

    fun isLoaded() = byLength.isNotEmpty()
    fun contains(word: String) = wordSet.contains(word.lowercase())
    fun wordsOfLength(length: Int): List<String> = byLength[length] ?: emptyList()
    fun wordsByLength(): Map<Int, List<String>> = byLength
}