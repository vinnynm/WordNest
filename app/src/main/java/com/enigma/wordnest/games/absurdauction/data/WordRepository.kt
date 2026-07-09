package com.enigma.wordnest.games.absurdauction.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Same shape as Lexicon's dictionary manager — small dictionary for legality checks,
 * large dictionary for full cross-word validation. */
class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)
    private var dictionary: Set<String> = emptySet()
    private var largeDictionary: Set<String> = emptySet()

    suspend fun load() = withContext(Dispatchers.IO) {
        dictionary = optimizedRepo.getAllWords()
        largeDictionary = optimizedRepo.getAllWordsLargeLibrary()
    }

    fun isLoaded() = dictionary.isNotEmpty()
    fun dictionarySet(): Set<String> = dictionary
    fun largeDictionarySet(): Set<String> = largeDictionary
}