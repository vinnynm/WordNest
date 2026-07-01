package com.enigma.wordnest.games.processors

import android.content.Context
import android.util.Log
import com.enigma.wordnest.R
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader

class OptimizedWordRepository(private val context: Context) {

    // ── Large dictionary (used by Lexicon for word-legality checks) ─────────────
    private val largeLibrary: Set<String> by lazy {
        val words = mutableSetOf<String>()
        try {
            context.resources.openRawResource(R.raw.large_wordlib).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName()
                        reader.beginArray()
                        while (reader.hasNext()) words.add(reader.nextString().uppercase())
                        reader.endArray()
                    }
                    reader.endObject()
                }
            }
        } catch (e: Exception) {
            Log.e("OptimizedWordRepository", "Error loading large dictionary", e)
            throw e
        }
        words
    }

    private val smallerLibrary: Set<String> by lazy {
        val words = mutableSetOf<String>()
        try {
            context.resources.openRawResource(R.raw.largelib).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName()
                        reader.beginArray()
                        while (reader.hasNext()) words.add(reader.nextString().uppercase())
                        reader.endArray()
                    }
                    reader.endObject()
                }
            }
        } catch (e: Exception) {
            Log.e("OptimizedWordRepository", "Error loading large dictionary", e)
            throw e
        }
        words
    }

    fun getAllWordsLargeLibrary(): Set<String> = largeLibrary
    fun isWordValidLargeLibrary(word: String): Boolean = largeLibrary.contains(word.uppercase())
    private val allOfTheWords: Set<String> by lazy {
        val words = mutableSetOf<String>()
        try {
            context.resources.openRawResource(R.raw.wordlib500).use { inputStream ->
                JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName() // letter key, e.g. "A"
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

    val sortedWords: List<String> by lazy { allOfTheWords.toList().sorted() }

    private val wordsByLength: Map<Int, List<String>> by lazy {
        allOfTheWords.groupBy { it.length }
    }

    private val wordsByFirstLetter: Map<Char, List<String>> by lazy {
        allOfTheWords.groupBy { it.firstOrNull()?.uppercaseChar() ?: '?' }
    }

    fun getRandomWord(): String = allOfTheWords.random()

    fun getWordsStartingWith(letter: Char): List<String> =
        wordsByFirstLetter[letter.uppercaseChar()] ?: emptyList()

    fun getAllWords(): Set<String> = allOfTheWords

    fun isWordValid(word: String): Boolean = allOfTheWords.contains(word.uppercase())

    fun getWordsOfLength(length: Int): List<String> = wordsByLength[length] ?: emptyList()

    fun getAvailableLengths(): List<Int> = wordsByLength.keys.sorted()
}
