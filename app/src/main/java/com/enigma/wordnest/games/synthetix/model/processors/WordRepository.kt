package com.enigma.wordnest.games.synthetix.model.processors

import android.content.Context
import android.util.Log
import com.enigma.wordnest.R
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader

class OptimizedWordRepository(private val context: Context) {

    private val allOfTheWords: Set<String> by lazy {
        val words = mutableSetOf<String>()
        try {
            context.resources.openRawResource(R.raw.largelib_gb_augmented).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName() // Skip letter key
                        reader.beginArray()
                        while (reader.hasNext()) {
                            words.add(reader.nextString().uppercase())
                        }
                        reader.endArray()
                    }
                    reader.endObject()
                }
            }
        } catch (e: Exception) {
            Log.e("OptimizedWordRepository", "Error loading dictionary", e)
            throw e
        }
        words
    }

    private val largeLibrary: Set<String> by lazy {
        val words = mutableSetOf<String>()
        try {
            context.resources.openRawResource(R.raw.largelib_gb_augmented).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName() // Skip letter key
                        reader.beginArray()
                        while (reader.hasNext()) {
                            words.add(reader.nextString().uppercase())
                        }
                        reader.endArray()
                    }
                    reader.endObject()
                }
            }
        } catch (e: Exception) {
            Log.e("OptimizedWordRepository", "Error loading dictionary", e)
            throw e
        }
        words
    }

    val sortedLargeLib: List<String> by lazy {
        largeLibrary.toList().sorted()
    }

    val sortedWords: List<String> by lazy {
        allOfTheWords.toList().sorted()
    }

    private val wordsByLength: Map<Int, List<String>> by lazy {
        allOfTheWords.groupBy { it.length }
    }

    private val wordsByLengthLargeLib: Map<Int, List<String>> by lazy {
        largeLibrary.groupBy { it.length }
    }

    private val wordsByFirstLetter: Map<Char, List<String>> by lazy {
        allOfTheWords.groupBy { it.firstOrNull()?.uppercaseChar() ?: '?' }
    }

    private val wordsByFirstLetterLargeLibrary: Map<Char, List<String>> by lazy {
        largeLibrary.groupBy { it.firstOrNull()?.uppercaseChar() ?: '?' }
    }

    fun getRandomWord(): String = allOfTheWords.random()

    fun getRandomWordLargeLibrary(): String = largeLibrary.random()

    fun getWordsStartingWith(letter: Char): List<String> =
        wordsByFirstLetter[letter.uppercaseChar()] ?: emptyList()


    fun getAllWords(): Set<String> = allOfTheWords
    fun getAllWordsLargeLibrary(): Set<String> = largeLibrary

    fun isWordValid(word: String): Boolean = allOfTheWords.contains(word.uppercase())
    fun isWordValidLargeLibrary(word: String): Boolean = largeLibrary.contains(word.uppercase())

    fun getWordsOfLength(length: Int): List<String> = wordsByLength[length] ?: emptyList()
    fun getWordsOfLengthLargeLib(length: Int): List<String> = wordsByLengthLargeLib[length] ?: emptyList()

    fun getAvailableLengths(): List<Int> = wordsByLength.keys.sorted()
}
