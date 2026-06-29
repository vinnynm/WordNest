package com.enigma.wordnest.games.absurdle.model

/**
 * AbsurdleEngine — the adversarial word-game brain.
 *
 * How Absurdle works:
 * ──────────────────
 * Instead of picking ONE secret word at the start, the game maintains a
 * CANDIDATE SET — all words in the dictionary of the chosen length.
 *
 * On every guess the engine:
 *   1. Groups all remaining candidates by the pattern they would produce
 *      against the guess (e.g. "GYGXX", "XXGXY", …).
 *   2. Picks the bucket with the MOST candidates — this is the response that
 *      keeps the game alive as long as possible (adversarial).
 *   3. Eliminates all candidates NOT in the chosen bucket.
 *   4. Returns the pattern to the player as feedback.
 *
 * The player wins only when the candidate set collapses to one word AND the
 * pattern returned is all-green.  This is provably the hardest possible
 * Wordle variant.
 *
 * Tie-breaking: when two buckets have equal size we prefer the one with the
 * fewest greens (delays wins further).  Secondary tie-break: lexicographic
 * order of the pattern string (deterministic behaviour).
 */
object AbsurdleEngine {

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process a guess against the current candidate set.
     *
     * @param guess      The player's word (must be same length as candidates)
     * @param candidates Current surviving candidate set
     * @param hardMode   If true the engine also avoids any response that would
     *                   immediately reveal the word (single candidate left = win),
     *                   unless there's truly no other choice.
     *
     * @return [EngineResult] containing the chosen pattern, new candidate set,
     *         and the scored guess row for display.
     */
    fun process(
        guess: String,
        candidates: Set<String>,
        hardMode: Boolean = false
    ): EngineResult {
        require(candidates.isNotEmpty()) { "Candidate set is empty" }
        val g = guess.lowercase()

        // 1. Bucket every candidate by the pattern it would generate
        val buckets = mutableMapOf<String, MutableList<String>>()
        for (candidate in candidates) {
            val pattern = scorePattern(g, candidate)
            buckets.getOrPut(pattern) { mutableListOf() }.add(candidate)
        }

        // 2. Choose the worst bucket for the player
        val chosenPattern = chooseBucket(buckets, hardMode)
        val newCandidates = buckets[chosenPattern]!!.toSet()

        // 3. Build the coloured row
        val scored = patternToScoredGuess(g, chosenPattern)

        // 4. The game is won when ALL tiles are green
        val won = chosenPattern.all { it == 'G' }

        // 5. If the candidate set is down to one, that IS the answer
        val committed = if (newCandidates.size == 1) newCandidates.first() else null

        return EngineResult(
            pattern       = chosenPattern,
            newCandidates = newCandidates,
            scoredGuess   = scored,
            isWon         = won,
            committedWord = committed
        )
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    /**
     * Standard Wordle scoring — returns a compact pattern string of G/Y/X.
     * Uses the same double-letter logic as the real game:
     *  - first pass marks exact matches (G)
     *  - second pass marks yellows using remaining target letter counts
     */
    fun scorePattern(guess: String, target: String): String {
        val g = guess.lowercase().toCharArray()
        val t = target.lowercase().toCharArray()
        val n = g.size
        val result = Array(n) { 'X' }
        val targetRemaining = IntArray(26)

        // Pass 1 — greens
        for (i in 0 until n) {
            if (g[i] == t[i]) {
                result[i] = 'G'
            } else {
                val idx = t[i] - 'a'
                if (idx in 0 until 26) {
                    targetRemaining[idx]++
                }
            }
        }

        // Pass 2 — yellows
        for (i in 0 until n) {
            if (result[i] == 'G') continue
            val idx = g[i] - 'a'
            if (idx in 0 until 26 && targetRemaining[idx] > 0) {
                result[i] = 'Y'
                targetRemaining[idx]--
            }
        }

        return String(result.toCharArray())
    }

    // ── Bucket selection ──────────────────────────────────────────────────────

    private fun chooseBucket(
        buckets: Map<String, List<String>>,
        hardMode: Boolean
    ): String {
        // In hard mode, prefer not to win immediately if other options exist
        val eligible = if (hardMode && buckets.size > 1) {
            buckets.filterKeys { !it.all { c -> c == 'G' } }
        } else {
            buckets
        }
        val pool = eligible.ifEmpty { buckets }

        return pool.entries.maxWithOrNull { a, b ->
            // Primary: larger bucket wins (more candidates = harder for player)
            val sizeCmp = a.value.size.compareTo(b.value.size)
            if (sizeCmp != 0) return@maxWithOrNull sizeCmp

            // Secondary: fewer greens in pattern (delay wins)
            val aGreens = a.key.count { it == 'G' }
            val bGreens = b.key.count { it == 'G' }
            val greenCmp = bGreens.compareTo(aGreens) // fewer greens = better
            if (greenCmp != 0) return@maxWithOrNull greenCmp

            // Tertiary: lexicographic (deterministic)
            b.key.compareTo(a.key)
        }!!.key
    }

    // ── Pattern → display ─────────────────────────────────────────────────────

    private fun patternToScoredGuess(guess: String, pattern: String): ScoredGuess {
        return ScoredGuess(
            letters = guess.indices.map { i ->
                ScoredLetter(
                    char = guess[i],
                    color = when (pattern[i]) {
                        'G' -> TileColor.GREEN
                        'Y' -> TileColor.YELLOW
                        else -> TileColor.GRAY
                    }
                )
            }
        )
    }

    // ── Keyboard merge ────────────────────────────────────────────────────────

    /** Best colour wins: GREEN > YELLOW > GRAY > EMPTY */
    fun mergeKeyboard(
        current: Map<Char, TileColor>,
        guess: ScoredGuess
    ): Map<Char, TileColor> {
        val updated = current.toMutableMap()
        for (sl in guess.letters) {
            val existing = updated[sl.char] ?: TileColor.EMPTY
            updated[sl.char] = betterColor(existing, sl.color)
        }
        return updated
    }

    private fun colorRank(c: TileColor) = when (c) {
        TileColor.GREEN  -> 3
        TileColor.YELLOW -> 2
        TileColor.GRAY   -> 1
        TileColor.EMPTY  -> 0
    }

    private fun betterColor(a: TileColor, b: TileColor) =
        if (colorRank(a) >= colorRank(b)) a else b
}

// ── Result data class ─────────────────────────────────────────────────────────

data class EngineResult(
    val pattern: String,
    val newCandidates: Set<String>,
    val scoredGuess: ScoredGuess,
    val isWon: Boolean,
    /** Non-null when only one candidate remains */
    val committedWord: String?
)
