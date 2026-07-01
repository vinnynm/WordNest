package com.enigma.wordnest.games.lexicon.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class WordDictionaryManager(context: Context) {
    private val _dictionary = MutableStateFlow<Set<String>>(emptySet())
    val dictionary: StateFlow<Set<String>> = _dictionary

    private val _largeDictionary = MutableStateFlow<Set<String>>(emptySet())
    val largeDictionary: StateFlow<Set<String>> = _largeDictionary

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val wordRepository = OptimizedWordRepository(context)

    suspend fun load() {
        _error.value = null
        try {
            _dictionary.value = withContext(Dispatchers.IO) { wordRepository.getAllWords() }
        } catch (e: Exception) {
            _error.value = "Failed to load dictionary: ${e.localizedMessage}"
            _dictionary.value = emptySet()
        }
    }

    suspend fun loadLargeDictionary() {
        try {
            _largeDictionary.value = withContext(Dispatchers.IO) { wordRepository.getAllWordsLargeLibrary() }
        } catch (e: Exception) {
            _error.value = "Failed to load dictionary: ${e.localizedMessage}"
            _largeDictionary.value = emptySet()
        }
    }
}