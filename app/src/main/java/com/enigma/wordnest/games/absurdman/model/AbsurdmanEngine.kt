package com.enigma.wordnest.games.absurdman.model

/**
 * AbsurdmanEngine — the adversarial hangman brain.
 *
 * On every letter guess the engine:
 *   1. Groups all remaining candidates by the "reveal pattern" that letter
 *      would produce against them — i.e. which positions (if any) contain
 *      that letter. A word with no occurrences of the letter is its own bucket.
 *   2. Picks the bucket with the MOST candidates — keeping the game alive
 *      as long as possible (same rule as AbsurdleEngine.chooseBucket).
 *   3. Eliminates all candidates NOT in the chosen bucket.
 *   4. Reports whether the guess was a "hit" (letter present) and, if so,
 *      the position(s) revealed.
 *
 * This directly reuses the bucket-selection idea from AbsurdleEngine, just
 * keyed on single-letter occurrence patterns instead of full G/Y/X patterns.
 */
object AbsurdmanEngine {

    data class GuessResult(
        val isHit: Boolean,
        val newCandidates: Set<String>,
        val revealPositions: List<Int>,   // positions where the letter now appears
        val isWon: Boolean,
        /** Non-null once the candidate set has collapsed to a single word */
        val committedWord: String?
    )

    /**
     * Process a single-letter guess against the current candidate set.
     *
     * @param letter      the guessed letter (case-insensitive)
     * @param candidates  current surviving candidate set (all same length)
     * @param revealed    current revealed pattern (nulls = not yet known)
     */
    fun process(
        letter: Char,
        candidates: Set<String>,
        revealed: List<Char?>
    ): GuessResult {
        require(candidates.isNotEmpty()) { "Candidate set is empty" }
        val ch = letter.lowercaseChar()
        val length = revealed.size

        // 1. Bucket every candidate by the set of positions this letter occupies.
        //    Key = sorted position list as a string, e.g. "" (absent), "0,3", "2"
        val buckets = mutableMapOf<String, MutableList<String>>()
        for (candidate in candidates) {
            val positions = candidate.indices.filter { candidate[it] == ch }
            val key = positions.joinToString(",")
            buckets.getOrPut(key) { mutableListOf() }.add(candidate)
        }

        // 2. Choose the bucket that's worst for the player: prefer "absent" (key == "")
        //    when it ties for largest, since that's strictly worse news than a hit
        //    of the same bucket size (delays the win the same amount, costs a life).
        val chosenKey = buckets.entries.maxWithOrNull { a, b ->
            val sizeCmp = a.value.size.compareTo(b.value.size)
            if (sizeCmp != 0) return@maxWithOrNull sizeCmp
            // tie-break: absent bucket beats any present bucket
            val aAbsent = if (a.key.isEmpty()) 1 else 0
            val bAbsent = if (b.key.isEmpty()) 1 else 0
            val absentCmp = aAbsent.compareTo(bAbsent)
            if (absentCmp != 0) return@maxWithOrNull absentCmp
            // fewer revealed positions is worse for the player
            val aCount = if (a.key.isEmpty()) 0 else a.key.split(",").size
            val bCount = if (b.key.isEmpty()) 0 else b.key.split(",").size
            bCount.compareTo(aCount)
        }!!.key

        val newCandidates = buckets[chosenKey]!!.toSet()
        val isHit = chosenKey.isNotEmpty()
        val positions = if (isHit) chosenKey.split(",").map { it.toInt() } else emptyList()

        val newRevealed = revealed.toMutableList()
        if (isHit) positions.forEach { newRevealed[it] = ch }

        val won = newRevealed.all { it != null }
        val committed = if (newCandidates.size == 1) newCandidates.first() else null

        return GuessResult(
            isHit = isHit,
            newCandidates = newCandidates,
            revealPositions = positions,
            isWon = won,
            committedWord = committed
        )
    }
}
