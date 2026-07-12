package com.enigma.wordnest.games.synthetix.data

import android.content.Context
import android.util.Log
import com.enigma.wordnest.games.synthetix.model.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Loads and exposes the word dictionaries used for move validation.
 *
 * Two tiers:
 *  - [dictionary]      – standard library used for AI vocabulary sampling
 *  - [largeDictionary] – extended library used for play validation
 *
 * ✅ FIX (B6): [load] and [loadLargeDictionary] are now guarded against
 * concurrent calls. If a load is already in progress (or already succeeded),
 * subsequent calls are ignored, preventing redundant file I/O on rotation.
 */
class WordDictionaryManager(private val context: Context) {

    private val _dictionary = MutableStateFlow<Set<String>>(emptySet())
    val dictionary: StateFlow<Set<String>> = _dictionary

    private val _largeDictionary = MutableStateFlow<Set<String>>(emptySet())
    val largeDictionary: StateFlow<Set<String>> = _largeDictionary

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ✅ FIX: Track whether each dictionary has been loaded to avoid redundant work.
    @Volatile private var dictionaryLoaded = false
    @Volatile private var largeDictionaryLoaded = false

    private val wordRepository = OptimizedWordRepository(context)

    /**
     * Load the standard dictionary. Safe to call multiple times — subsequent
     * calls while a load is in progress or after a successful load are no-ops.
     */
    suspend fun load() {
        // ✅ Skip if already loaded successfully
        if (dictionaryLoaded && _dictionary.value.isNotEmpty()) return
        // ✅ Skip if another coroutine is already loading
        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null
        try {
            val words = withContext(Dispatchers.IO) { wordRepository.getAllWords().toSet() }
            _dictionary.value = words
            dictionaryLoaded = true
        } catch (e: Exception) {
            Log.e("WordDictionaryManager", "Failed to load dictionary", e)
            _error.value = "Failed to load dictionary: ${e.localizedMessage}"
            _dictionary.value = emptySet()
            dictionaryLoaded = false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load the large validation dictionary. Safe to call multiple times.
     */
    suspend fun loadLargeDictionary() {
        // ✅ Skip if already loaded
        if (largeDictionaryLoaded && _largeDictionary.value.isNotEmpty()) return

        try {
            val words = withContext(Dispatchers.IO) { wordRepository.getAllWordsLargeLibrary().toSet() }
            _largeDictionary.value = words
            largeDictionaryLoaded = true
        } catch (e: Exception) {
            Log.e("WordDictionaryManager", "Failed to load large dictionary", e)
            _largeDictionary.value = emptySet()
            largeDictionaryLoaded = false
        }
    }

    /**
     * Force a full reload of both dictionaries, ignoring the cached state.
     * Use this when the user explicitly requests a retry after an error.
     */
    suspend fun forceReload() {
        dictionaryLoaded = false
        largeDictionaryLoaded = false
        _isLoading.value = false   // Reset guard so load() proceeds
        load()
        loadLargeDictionary()
    }

    fun isWordValid(word: String) = _largeDictionary.value.contains(word.uppercase())
    fun getWordCount() = _largeDictionary.value.size
}
