package com.enigma.wordnest.games.codeword.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodewordWordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var byLength: Map<Int, List<String>> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords()
        val clean = raw.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        byLength = clean.groupBy { it.length }
    }

    fun isLoaded() = byLength.isNotEmpty()
    fun wordsByLength(): Map<Int, List<String>> = byLength
}
