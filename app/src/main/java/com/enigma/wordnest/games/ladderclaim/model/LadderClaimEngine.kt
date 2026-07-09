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

    // ── Cell helpers ──────────────────────────────────────────────────────

    fun cellInWord(word: LadderWord, row: Int, col: Int): Boolean =
        if (word.isHorizontal) row == word.startRow && col in word.startCol until word.startCol + word.word.length
        else col == word.startCol && row in word.startRow until word.startRow + word.word.length

    /**
     * True only if EVERY cell in this word's span is currently unowned on the board.
     *
     * This is the single source of truth for "is this word neutral" for theft purposes.
     * [LadderWord.ownerId] is NOT reliable for this check: a PARTIAL play (or, in
     * TARGET_TILES mode, a partial reclaim) can leave a word with mixed per-tile
     * ownership that a scalar field on LadderWord can never correctly represent —
     * trusting that field was the root cause of the original neutral-theft bug, where
     * a word that had already been fully/partially claimed onto the board could still
     * be re-stolen because the word-list copy of it never got updated to reflect that.
     */
    fun isWordFullyNeutral(board: Array<Array<LadderTile?>>, word: LadderWord): Boolean {
        var r = word.startRow
        var c = word.startCol
        for (i in word.word.indices) {
            val tile = board[r][c] ?: return false
            if (tile.ownerId != null) return false
            r += if (word.isHorizontal) 0 else 1
            c += if (word.isHorizontal) 1 else 0
        }
        return true
    }

    /**
     * The single owner of [word]'s cells (ignoring [excludeCell]), or null if the word
     * is unowned, or if ownership is mixed across its cells. Mixed ownership deliberately
     * returns null rather than picking a majority owner — an ambiguous crossing word
     * shouldn't be treated as cleanly "belonging to the opponent" for reclaim-eligibility
     * purposes; only a word that is unambiguously the opponent's blocks a reclaim.
     */
    fun soleOwnerExcluding(board: Array<Array<LadderTile?>>, word: LadderWord, excludeCell: Pair<Int, Int>?): Int? {
        var owner: Int? = null
        var r = word.startRow
        var c = word.startCol
        for (i in word.word.indices) {
            if (excludeCell == null || (r to c) != excludeCell) {
                val cellOwner = board[r][c]?.ownerId
                if (cellOwner != null) {
                    if (owner == null) owner = cellOwner
                    else if (owner != cellOwner) return null
                }
            }
            r += if (word.isHorizontal) 0 else 1
            c += if (word.isHorizontal) 1 else 0
        }
        return owner
    }

    // ── Coloring ──────────────────────────────────────────────────────────

    data class MatchResult(
        val matches: Int,
        val matchedIndicesInNew: Set<Int>,
        val matchedIndicesInTarget: Set<Int>
    )

    /**
     * Best-alignment matching-position count between the target word and the
     * newly played word. Same length -> direct position comparison. Length
     * differing by exactly 1 -> try every insertion/deletion offset and keep
     * whichever gives the most matches (always the most generous reading).
     * Length differing by more than 1 -> no valid comparison (0 matches).
     *
     * Returns match indices in BOTH the new word's coordinate space and the
     * target word's coordinate space, since OWN_TILES mode colors the new
     * word's tiles while TARGET_TILES mode claims the target word's tiles.
     */
    fun computeMatch(target: String, new: String): MatchResult {
        val diff = new.length - target.length
        if (diff == 0) {
            val idx = new.indices.filter { new[it] == target[it] }.toSet()
            return MatchResult(idx.size, idx, idx)
        }
        if (abs(diff) != 1) return MatchResult(0, emptySet(), emptySet())

        val newIsLonger = diff > 0
        val longer = if (newIsLonger) new else target
        val shorter = if (newIsLonger) target else new

        var bestMatches = -1
        var bestSkip = 0
        var bestMatchedShorterIdx: Set<Int> = emptySet()
        for (skip in longer.indices) {
            val matchedJ = mutableSetOf<Int>()
            for (j in shorter.indices) {
                val longerIdx = if (j < skip) j else j + 1
                if (longerIdx < longer.length && shorter[j] == longer[longerIdx]) matchedJ += j
            }
            if (matchedJ.size > bestMatches) {
                bestMatches = matchedJ.size
                bestSkip = skip
                bestMatchedShorterIdx = matchedJ
            }
        }

        val matchedInLonger = bestMatchedShorterIdx.mapNotNull { j ->
            val longerIdx = if (j < bestSkip) j else j + 1
            if (longerIdx < longer.length) longerIdx else null
        }.toSet()

        // shorter/longer map back onto (new, target) depending on which one was longer.
        val matchedIndicesInNew = if (newIsLonger) matchedInLonger else bestMatchedShorterIdx
        val matchedIndicesInTarget = if (newIsLonger) bestMatchedShorterIdx else matchedInLonger

        return MatchResult(bestMatches, matchedIndicesInNew, matchedIndicesInTarget)
    }

    /**
     * Returns the outcome plus which indices are eligible to be colored/claimed —
     * in the TARGET word's coordinate space, since that's what both claim modes
     * ultimately need (OWN_TILES mode maps its own colored cells 1:1 positionally
     * for same-length plays; for length-differing plays callers needing the NEW
     * word's indices should use [computeMatch] directly instead).
     */
    fun classify(target: String, new: String, fullCreditBar: Int = 1): Pair<ColorOutcome, Set<Int>> {
        val result = computeMatch(target, new)
        val l = maxOf(target.length, new.length)
        return when {
            result.matches > 0 && result.matches >= l - fullCreditBar ->
                ColorOutcome.FULL to target.indices.toSet()
            result.matches > 0 ->
                ColorOutcome.PARTIAL to result.matchedIndicesInTarget
            else -> ColorOutcome.NEUTRAL to emptySet()
        }
    }

    /** Same as [classify] but also returns the NEW word's matched indices, for OWN_TILES mode. */
    fun classifyOwnTiles(target: String, new: String, fullCreditBar: Int = 1): Pair<ColorOutcome, Set<Int>> {
        val result = computeMatch(target, new)
        val l = maxOf(target.length, new.length)
        return when {
            result.matches > 0 && result.matches >= l - fullCreditBar ->
                ColorOutcome.FULL to new.indices.toSet()
            result.matches > 0 ->
                ColorOutcome.PARTIAL to result.matchedIndicesInNew
            else -> ColorOutcome.NEUTRAL to emptySet()
        }
    }
}
