package com.enigma.wordnest.games.absurdman.model

/**
 * ClueGenerator — picks a small set of true-for-every-candidate clues and
 * filters the candidate pool down to match them.
 *
 * Because Hell Mode has no fixed word (AbsurdmanEngine keeps a whole
 * candidate set alive and only commits when forced), a clue can't describe
 * "the word" directly — it has to describe a PROPERTY that every surviving
 * candidate shares, applied once at game start. Filtering candidates down to
 * only words matching the chosen clues means whichever word the engine is
 * eventually forced into will always honor them. No contradictions, no
 * "wait, the clue said it ends in R" complaints later.
 *
 * Classic mode reuses the same generator: clues are derived from the pool
 * BEFORE the fixed target word is picked, then the target is drawn from the
 * filtered pool — so clues stay guaranteed-true there too, not just decorative.
 */
object ClueGenerator {

    data class ClueResult(
        val clues: List<String>,
        val remaining: Set<String>
    )

    /**
     * @param desiredCount   how many clues to try to find
     * @param minRemaining   never filter the pool below this size (keeps the
     *                       game from collapsing to a near-single-word reveal
     *                       before a single letter's been guessed)
     */
    fun generate(
        candidates: Set<String>,
        desiredCount: Int = 3,
        minRemainingFraction: Double = 0.05,
        minRemainingAbsolute: Int = 5
    ): ClueResult {
        if (candidates.isEmpty()) return ClueResult(emptyList(), candidates)

        var pool = candidates
        val clues = mutableListOf<String>()
        val minRemaining = maxOf(minRemainingAbsolute, (candidates.size * minRemainingFraction).toInt())

        val builders: List<() -> Pair<String, (String) -> Boolean>?> = listOf(
            // Ends with a specific letter
            {
                val letter = pool.random().last()
                "Ends with '${letter.uppercaseChar()}'" to { w: String -> w.last() == letter }
            },
            // Contains a specific vowel
            {
                val vowel = "aeiou".toList().shuffled().firstOrNull { v -> pool.any { it.contains(v) } }
                vowel?.let { v -> "Contains the letter '${v.uppercaseChar()}'" to { w: String -> w.contains(v) } }
            },
            // Has a repeated letter
            {
                val anyRepeats = pool.any { hasRepeatedLetter(it) }
                if (anyRepeats) "Has a letter that appears twice" to { w: String -> hasRepeatedLetter(w) } else null
            },
            // Approximate syllable count (mode across the current pool)
            {
                val mode = pool.map { estimateSyllables(it) }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                mode?.let { m -> "$m syllable${if (m != 1) "s" else ""} (approx.)" to { w: String -> estimateSyllables(w) == m } }
            },
            // Starts with a specific letter
            {
                val letter = pool.random().first()
                "Starts with '${letter.uppercaseChar()}'" to { w: String -> w.first() == letter }
            }
        ).shuffled()

        for (build in builders) {
            if (clues.size >= desiredCount) break
            val (label, predicate) = build() ?: continue
            val filtered = pool.filter(predicate).toSet()
            // Only accept the clue if it doesn't shrink the pool too aggressively
            // AND it isn't already implied (i.e. it must actually narrow something).
            if (filtered.size >= minRemaining && filtered.size < pool.size) {
                pool = filtered
                clues += label
            }
        }

        return ClueResult(clues, pool)
    }

    private fun hasRepeatedLetter(word: String): Boolean =
        word.groupingBy { it }.eachCount().values.any { it >= 2 }

    /** Rough vowel-group syllable estimate — not linguistically perfect, good enough for a hint. */
    private fun estimateSyllables(word: String): Int {
        val vowels = "aeiouy"
        var count = 0
        var prevWasVowel = false
        for (c in word.lowercase()) {
            val isVowel = c in vowels
            if (isVowel && !prevWasVowel) count++
            prevWasVowel = isVowel
        }
        if (word.endsWith("e", ignoreCase = true) && count > 1) count--
        return count.coerceAtLeast(1)
    }
}