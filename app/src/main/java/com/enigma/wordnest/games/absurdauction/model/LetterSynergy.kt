package com.enigma.wordnest.games.absurdauction.model

/**
 * Static letter-synergy data feeding the Heuristic Rack Scorer (design doc §2.4, item 2).
 *
 * This replaces a full `AiOpponent.findAllMoves` board search — running that oracle
 * 12–20 times per draw, every draw, every turn is too heavy for on-device use. Instead
 * we score a candidate rack with cheap, precomputed lookups:
 *
 *  - VOWELS / isVowel(): for vowel/consonant ratio scoring.
 *  - BLEND_BONUS: common useful letter pairs/fragments that make racks feel "workable"
 *    (adjacent-pair check only — cheap, no substring search needed against a dictionary).
 *  - ISOLATE_PENALTY: high-point letters that are awkward without a connector present.
 *  - "Q without U" is handled as a special case in [RackScorer], not here, since it's a
 *    single-letter conditional rather than a pairwise lookup.
 */
object LetterSynergy {

    val VOWELS = setOf('A', 'E', 'I', 'O', 'U')

    fun isVowel(c: Char) = c.uppercaseChar() in VOWELS

    /**
     * Adjacency-independent "does this pair of letters, both present in the rack,
     * suggest a workable fragment" bonus. Keyed by an unordered pair (stored as a
     * sorted 2-char string) so lookup doesn't care which letter appears first in
     * the rack.
     */
    private val BLEND_BONUS: Map<String, Int> = buildMap {
        // common blends / digraph fragments
        listOf(
            "ST", "ER", "IN", "ON", "AN", "EN", "RE", "TH", "IT", "OR",
            "AT", "ES", "TE", "ND", "TI", "TO", "AR", "TE", "SE", "HA"
        ).forEach { pair ->
            val key = pair.toCharArray().sorted().joinToString("")
            this[key] = maxOf(this[key] ?: 0, 3)
        }
    }

    /** High-point letters that need a connector (vowel or common consonant) to feel usable. */
    private val NEEDS_CONNECTOR = mapOf(
        'Q' to setOf('U'),
        'X' to VOWELS,
        'Z' to VOWELS,
        'J' to VOWELS
    )

    fun blendBonus(a: Char, b: Char): Int {
        val key = charArrayOf(a.uppercaseChar(), b.uppercaseChar()).sorted().joinToString("")
        return BLEND_BONUS[key] ?: 0
    }

    /** True if [letter] is present in the rack but none of its useful connectors are. */
    fun isIsolated(letter: Char, rackUpper: Set<Char>): Boolean {
        val connectors = NEEDS_CONNECTOR[letter.uppercaseChar()] ?: return false
        return connectors.none { it in rackUpper }
    }
}