package com.enigma.wordnest.games.betweenle.data

import android.content.Context
import com.enigma.wordnest.games.processors.OptimizedWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Loads the word library from OptimizedWordRepository and provides
 * sorted lookup utilities for the game engine.
 */
class WordRepository(context: Context) {
    private val optimizedRepo = OptimizedWordRepository(context)

    /** All words from the dictionary, sorted alphabetically (lowercase). */
    private var sortedWords: List<String> = emptyList()

    /** Fast O(log n) binary-search set */
    private var wordSet: Set<String> = emptySet()

    suspend fun load() = withContext(Dispatchers.IO) {
        val raw = optimizedRepo.getAllWords()
        sortedWords = raw.map { it.trim().lowercase() }.distinct().sorted()
        wordSet = sortedWords.toHashSet()
    }

    /** Returns true when the repository has been loaded. */
    fun isLoaded() = sortedWords.isNotEmpty()

    /** Is the word in the dictionary? */
    fun contains(word: String) = wordSet.contains(word.lowercase())

    /** Total number of words in the library. */
    fun size() = sortedWords.size

    /**
     * Picks a random pair of words (wordA, wordB) such that:
     * - wordA < wordB alphabetically
     * - there is at least [minGap] and at most [maxGap] words between them
     *   in the sorted list
     */
    fun randomBoundaryPair(minGap: Int = 3, maxGap: Int = 200): Pair<String, String> {
        if (sortedWords.size < 2) return Pair("", "")
        
        val actualMax = maxGap.coerceAtMost(sortedWords.size - 2).coerceAtLeast(1)
        val actualMin = minGap.coerceAtMost(actualMax)

        var attempts = 0
        while (attempts < 1000) {
            val iA = sortedWords.indices.random()
            val gap = (actualMin..actualMax).random()
            val iB = iA + gap + 1
            if (iB < sortedWords.size) {
                return Pair(sortedWords[iA], sortedWords[iB])
            }
            attempts++
        }
        // fallback: use simple spread
        val iA = 0
        val iB = (sortedWords.size - 1).coerceAtLeast(1)
        return Pair(sortedWords[iA], sortedWords[iB])
    }

    /**
     * Returns all words strictly between wordA and wordB in the sorted list.
     * Both boundary words must be in the dictionary.
     */
    fun wordsBetween(wordA: String, wordB: String): List<String> {
        val a = wordA.lowercase()
        val b = wordB.lowercase()
        val iA = sortedWords.binarySearch(a)
        val iB = sortedWords.binarySearch(b)
        if (iA < 0 || iB < 0 || iA >= iB) return emptyList()
        return sortedWords.subList(iA + 1, iB)
    }

    /**
     * Compares a guess against the two boundaries.
     * Returns:
     *  -1  → guess < wordA  (too low)
     *   0  → wordA < guess < wordB  (between / correct)
     *   1  → guess > wordB  (too high)
     *   2  → guess == wordA or guess == wordB  (is boundary)
     */
    fun compareGuess(guess: String, target: String): Int {
        val g = guess.lowercase()
        val t = target.lowercase()
        return when {
            g == t -> 0
            g < t -> -1
            else -> 1
        }
    }

    /**
     * Returns how many dictionary words separate the guess from the target word.
     */
    fun distanceToTarget(guess: String, target: String): Int {
        val g = guess.lowercase()
        val t = target.lowercase()
        val iG = sortedWords.binarySearch(g).let { if (it < 0) -(it + 1) else it }
        val iT = sortedWords.binarySearch(t).let { if (it < 0) -(it + 1) else it }
        return abs(iG - iT)
    }

    /**
     * Returns how many dictionary words separate the guess from the nearest
     * boundary it's outside of.  Used for the distance hint.
     */
    fun distanceToRange(guess: String, wordA: String, wordB: String): Int {
        val g = guess.lowercase()
        val a = wordA.lowercase()
        val b = wordB.lowercase()
        val iA = sortedWords.binarySearch(a).let { if (it < 0) -(it + 1) else it }
        val iB = sortedWords.binarySearch(b).let { if (it < 0) -(it + 1) else it }
        val iG = sortedWords.binarySearch(g).let { if (it < 0) -(it + 1) else it }
        return when {
            iG in iA..iB -> 0
            iG < iA -> iA - iG
            else -> iG - iB
        }
    }

    /** How many words are strictly between the two boundary words. */
    fun gapSize(wordA: String, wordB: String): Int = wordsBetween(wordA, wordB).size
}
