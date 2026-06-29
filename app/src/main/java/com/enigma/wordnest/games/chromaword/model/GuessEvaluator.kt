package com.enigma.wordnest.games.chromaword.model

import kotlin.text.iterator

/**
 * Evaluates a guess against the target word and returns a list of coloured letters.
 *
 * Colour rules:
 *
 *  GREEN      → position matches
 *  YELLOW     → letter is in word, position wrong, letter appears only once in target
 *  LIGHT_BLUE → letter appears 2+ times in target; this position is wrong,
 *               but at least one other position of THIS LETTER in the guess IS correct (green)
 *  ROYAL_BLUE → letter appears 2+ times in target; ALL positions of this letter
 *               in the guess are wrong
 *  RED_EXTRA  → all correct positions of this letter are already green, but the
 *               guess contains surplus instances of the letter at wrong positions
 *  RED_ABSENT → letter not in target at all
 */
object GuessEvaluator {

    fun evaluate(guess: String, target: String): EvaluatedGuess {
        require(guess.length == target.length) { "Lengths must match" }
        val g = guess.lowercase()
        val t = target.lowercase()
        val n = t.length

        // --- pre-compute target letter frequencies ---
        val targetFreq = mutableMapOf<Char, Int>()
        for (c in t) targetFreq[c] = (targetFreq[c] ?: 0) + 1

        // --- first pass: find exact matches (green) ---
        val result = Array<LetterColor>(n) { LetterColor.EMPTY }
        val greenCount = mutableMapOf<Char, Int>()   // greens per letter in guess
        for (i in 0 until n) {
            if (g[i] == t[i]) {
                result[i] = LetterColor.GREEN
                greenCount[g[i]] = (greenCount[g[i]] ?: 0) + 1
            }
        }

        // --- second pass: assign non-green positions ---
        for (i in 0 until n) {
            if (result[i] == LetterColor.GREEN) continue
            val ch = g[i]
            val freq = targetFreq[ch] ?: 0   // how many times ch appears in target

            if (freq == 0) {
                // not in word at all
                result[i] = LetterColor.RED_ABSENT
                continue
            }

            val greensOfCh = greenCount[ch] ?: 0   // how many of this letter are green in guess

            if (freq == 1) {
                // letter is in word exactly once
                if (greensOfCh >= 1) {
                    // the one correct position is already green → surplus
                    result[i] = LetterColor.RED_EXTRA
                } else {
                    result[i] = LetterColor.YELLOW
                }
            } else {
                // letter appears 2+ times in target
                if (greensOfCh >= freq) {
                    // all target positions already resolved as green → surplus
                    result[i] = LetterColor.RED_EXTRA
                } else if (greensOfCh > 0) {
                    // at least one green, but not all resolved → light blue (partial correct)
                    result[i] = LetterColor.LIGHT_BLUE
                } else {
                    // none of the positions are correct → royal blue
                    result[i] = LetterColor.ROYAL_BLUE
                }
            }
        }

        return EvaluatedGuess(
            letters = g.indices.map { i ->
                EvaluatedLetter(
                    g[i],
                    result[i]
                )
            }
        )
    }

    /**
     * Merges a new evaluated guess into the keyboard colour map.
     * Best colour wins (GREEN > YELLOW > LIGHT_BLUE > ROYAL_BLUE > RED_EXTRA > RED_ABSENT > EMPTY)
     */
    fun updateKeyboard(
        current: Map<Char, LetterColor>,
        guess: EvaluatedGuess
    ): Map<Char, LetterColor> {
        val updated = current.toMutableMap()
        for (letter in guess.letters) {
            val existing = updated[letter.char] ?: LetterColor.EMPTY
            updated[letter.char] = better(existing, letter.color)
        }
        return updated
    }

    private fun colorRank(c: LetterColor) = when (c) {
        LetterColor.GREEN      -> 6
        LetterColor.YELLOW     -> 5
        LetterColor.LIGHT_BLUE -> 4
        LetterColor.ROYAL_BLUE -> 3
        LetterColor.RED_EXTRA  -> 2
        LetterColor.RED_ABSENT -> 1
        LetterColor.EMPTY      -> 0
    }

    private fun better(a: LetterColor, b: LetterColor) =
        if (colorRank(a) >= colorRank(b)) a else b
}
