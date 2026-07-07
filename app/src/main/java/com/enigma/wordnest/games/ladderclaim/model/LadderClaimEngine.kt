package com.enigma.wordnest.games.ladderclaim.model

import kotlin.math.abs

/**
 * LadderClaimEngine — pure functions for board legality and the FULL/PARTIAL/
 * NEUTRAL coloring rule against a manually-selected target word.
 *
 * Legality (connects-to-board, single-row/col contiguous placement) mirrors
 * ScrabbleGame's rules exactly — there is no ladder requirement to make a
 * legal move at all, only to earn territory.
 */
object LadderClaimEngine {

    // ── Legality ──────────────────────────────────────────────────────────

    fun connectsToBoard(board: Array<Array<LadderTile?>>, placed: List<PlacedLadderTile>): Boolean {
        for (t in placed) {
            val neighbors = listOf(t.row - 1 to t.col, t.row + 1 to t.col, t.row to t.col - 1, t.row to t.col + 1)
            for ((r, c) in neighbors) if (r in 0..14 && c in 0..14 && board[r][c] != null) return true
        }
        return false
    }

    fun isContiguous(board: Array<Array<LadderTile?>>, placed: List<PlacedLadderTile>, isHorizontal: Boolean): Boolean {
        if (placed.size <= 1) return true
        val sorted = placed.sortedBy { if (isHorizontal) it.col else it.row }
        val fixed = if (isHorizontal) sorted[0].row else sorted[0].col
        for (i in 0 until sorted.size - 1) {
            val cur  = if (isHorizontal) sorted[i].col else sorted[i].row
            val next = if (isHorizontal) sorted[i + 1].col else sorted[i + 1].row
            for (pos in cur + 1 until next) {
                val r = if (isHorizontal) fixed else pos
                val c = if (isHorizontal) pos else fixed
                if (board[r][c] == null && placed.none { it.row == r && it.col == c }) return false
            }
        }
        return true
    }

    private fun tileAt(board: Array<Array<LadderTile?>>, placed: List<PlacedLadderTile>, r: Int, c: Int): Char? {
        placed.find { it.row == r && it.col == c }?.let { return it.letter }
        return board[r][c]?.letter
    }

    /** Full word (existing + newly placed) running through the given cell, plus its start coords. */
    fun buildWord(
        board: Array<Array<LadderTile?>>, placed: List<PlacedLadderTile>,
        row: Int, col: Int, isHorizontal: Boolean
    ): Triple<String, Int, Int> {
        var sr = row; var sc = col
        while (true) {
            val nr = if (isHorizontal) sr else sr - 1
            val nc = if (isHorizontal) sc - 1 else sc
            if (nr in 0..14 && nc in 0..14 && tileAt(board, placed, nr, nc) != null) { sr = nr; sc = nc } else break
        }
        val sb = StringBuilder(); var r = sr; var c = sc
        while (r in 0..14 && c in 0..14) {
            val ch = tileAt(board, placed, r, c) ?: break
            sb.append(ch)
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return Triple(sb.toString(), sr, sc)
    }

    /** All words (main + crosswords) formed by this turn's placement. */
    fun collectFormedWords(
        board: Array<Array<LadderTile?>>, placed: List<PlacedLadderTile>, isHorizontal: Boolean
    ): List<LadderWord> {
        val words = mutableListOf<LadderWord>()
        val first = placed.first()
        val (main, msr, msc) = buildWord(board, placed, first.row, first.col, isHorizontal)
        if (main.length >= 2) words += LadderWord(main, msr, msc, isHorizontal)
        placed.forEach { t ->
            val (cross, csr, csc) = buildWord(board, placed, t.row, t.col, !isHorizontal)
            if (cross.length >= 2) words += LadderWord(cross, csr, csc, !isHorizontal)
        }
        return words.distinctBy { it.id }
    }

    // ── Coloring ──────────────────────────────────────────────────────────

    data class MatchResult(val matches: Int, val matchedIndicesInNew: Set<Int>)

    /**
     * Best-alignment matching-position count between the target word and the
     * newly played word. Same length → direct position comparison. Length
     * differing by exactly 1 → try every insertion/deletion offset and keep
     * whichever gives the most matches (always the most generous reading).
     * Length differing by more than 1 → no valid comparison (0 matches).
     */
    fun computeMatch(target: String, new: String): MatchResult {
        val diff = new.length - target.length
        if (diff == 0) {
            val idx = new.indices.filter { new[it] == target[it] }
            return MatchResult(idx.size, idx.toSet())
        }
        if (abs(diff) != 1) return MatchResult(0, emptySet())

        val newIsLonger = diff > 0
        val longer = if (newIsLonger) new else target
        val shorter = if (newIsLonger) target else new

        var bestMatches = -1
        var bestSkip = 0
        for (skip in longer.indices) {
            var m = 0
            for (j in shorter.indices) {
                val longerIdx = if (j < skip) j else j + 1
                if (longerIdx < longer.length && shorter[j] == longer[longerIdx]) m++
            }
            if (m > bestMatches) { bestMatches = m; bestSkip = skip }
        }

        val matchedInLonger = shorter.indices.mapNotNull { j ->
            val longerIdx = if (j < bestSkip) j else j + 1
            if (longerIdx < longer.length && shorter[j] == longer[longerIdx]) longerIdx else null
        }.toSet()

        val matchedIndicesInNew = if (newIsLonger) matchedInLonger
        else matchedInLonger.map { li -> if (li < bestSkip) li else li - 1 }.toSet()

        return MatchResult(bestMatches, matchedIndicesInNew)
    }

    /** Returns the outcome plus which indices of the *new* word should be colored. */
    fun classify(target: String, new: String, fullCreditBar: Int = 1): Pair<ColorOutcome, Set<Int>> {
        val (matches, matchedIndices) = computeMatch(target, new)
        val l = maxOf(target.length, new.length)
        return when {
            matches > 0 && matches >= l - fullCreditBar -> ColorOutcome.FULL to new.indices.toSet()
            matches > 0                                 -> ColorOutcome.PARTIAL to matchedIndices
            else                                         -> ColorOutcome.NEUTRAL to emptySet()
        }
    }
}