package com.enigma.wordnest.games.common.utils

import androidx.compose.ui.text.toUpperCase
import com.enigma.wordnest.games.wordladder.model.WordLadderSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Locale.getDefault

interface RepositoryInterface {


    var allWords: List<String>

    suspend fun getAllWOrds(): Set<String>



    fun getWordsStartingBy(letter: Char) = allWords.filter {
        it.uppercase(getDefault()).startsWith(letter.uppercaseChar())
    }

    var byLength: Map<Int, Set<String>>


    fun isLoaded() = byLength.isNotEmpty()

    fun wordsOfLength(length: Int): Set<String> = byLength[length] ?: emptySet()

    fun contains(word: String): Boolean = byLength[word.length]?.contains(word.lowercase()) == true

    fun availableLengths(): List<Int> = byLength.keys.sorted()



}