package com.enigma.wordnest.games.fragment.model

/**
 * FragmentEngine — generation + the adversarial "obscure the letter count" logic.
 *
 * Coverage rule (mandatory, never violated): every letter OCCURRENCE in the mystery
 * word must end up blanked in at least one hint word, so the collected tile pool can
 * always spell the mystery word.
 *
 * Ambiguity rule (adversarial, best-effort within budget): for letters that occur
 * exactly once in the mystery word, the engine tries to also blank that same letter
 * in a second, unrelated hint word — planting a red-herring duplicate tile the player
 * must recognize and discard at anagram time. Mirrors AbsurdleEngine/AbsurdmanEngine's
 * "pick whatever is worst for the player, within a hard fairness/solvability floor"
 * philosophy — here the floor is "the puzzle must always remain solvable."
 */
object FragmentEngine {

    fun generate(
        mysteryWord: String,
        wordsByLength: Map<Int, List<String>>,
        maxAttempts: Int = 500
    ): FragmentPuzzle? {
        val target = mysteryWord.lowercase()
        val letterCounts = target.groupingBy { it }.eachCount()

        // Needed coverage queue: one entry per letter OCCURRENCE (mandatory),
        // plus a red-herring duplicate for singly-occurring letters (adversarial).
        val needed = mutableListOf<Char>()
        letterCounts.forEach { (ch, count) -> repeat(count) { needed += ch } }
        val herringBudget = (letterCounts.count { it.value == 1 } / 2).coerceAtLeast(1)
        letterCounts.filterValues { it == 1 }.keys.shuffled().take(herringBudget).forEach { needed += it }
        needed.shuffle()

        // Candidate clue-word pool: 3-6 letter real words, excluding the mystery word itself.
        val candidatePool = (3..6).flatMap { wordsByLength[it].orEmpty() }
            .map { it.lowercase() }
            .filter { it != target && it.length in 3..6 }
            .shuffled()
            .toMutableList()

        val hintWords = mutableListOf<HintWord>()
        var nextId = 0
        var attempts = 0

        while (needed.isNotEmpty() && attempts < maxAttempts) {
            attempts++
            val wantedLetter = needed.first()
            val candidateIdx = candidatePool.indexOfFirst { it.contains(wantedLetter) }
            if (candidateIdx == -1) {
                // No word left containing this letter at all — puzzle can't be built.
                return null
            }
            val word = candidatePool.removeAt(candidateIdx)

            // Blank up to 3 positions in this word: the wanted letter, plus any other
            // still-needed letters this same word happens to contain (greedy coverage).
            val blanks = mutableSetOf<Int>()
            word.indices.forEach { i ->
                if (blanks.size >= 3) return@forEach
                if (word[i] in needed && needed.count { it == word[i] } > 0) {
                    blanks += i
                }
            }
            if (blanks.isEmpty()) {
                val firstIdx = word.indexOf(wantedLetter)
                if (firstIdx >= 0) blanks += firstIdx
            }
            if (blanks.isEmpty()) continue

            // Remove exactly one covered occurrence per blanked letter from the needed queue.
            blanks.forEach { idx ->
                val ch = word[idx]
                val pos = needed.indexOf(ch)
                if (pos >= 0) needed.removeAt(pos)
            }

            hintWords += HintWord(id = nextId++, word = word, blankIndices = blanks)
        }

        if (needed.isNotEmpty()) return null // budget exhausted, bail — caller retries/regenerates
        return FragmentPuzzle(target, hintWords)
    }
}