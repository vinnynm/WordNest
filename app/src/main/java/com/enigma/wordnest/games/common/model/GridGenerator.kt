package com.enigma.wordnest.games.common.model

import android.util.Log

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * QUICK REVIEW SUMMARY (read this first)
 * ═══════════════════════════════════════════════════════════════════════════
 * Purpose: shared grid-fill engine for Crossword + Codeword. Fills a 15x15 or
 * 11x11 block pattern with dictionary words via backtracking + forward checking.
 *
 * Known-good invariants:
 *   - fillGrid() never returns a partially-filled map — it's all-or-nothing (null
 *     on failure).
 *   - No word is ever placed twice in the same grid.
 *   - generate() always tries every pattern at the requested size before giving
 *     up, and falls back to 11x11 if a 15x15 request exhausts its budget.
 *
 * Performance history:
 *   - v1: linear domain scan on every forward-check → could hang 10+ minutes on
 *     sparse dictionaries at long slot lengths.
 *   - v2 (bounded time): added wall-clock deadline per attempt + empty-domain
 *     fail-fast. Stopped the hangs, didn't address the underlying slowness.
 *   - v3 (this version): added an indexed word lookup (length → position →
 *     letter → words) so forward-checking no longer scans full domains;
 *     crossings/slots are now computed once per PATTERN instead of once per
 *     ATTEMPT (was being rebuilt on every retry of the same pattern); pattern
 *     try-order is now shuffled so one "unlucky first" pattern doesn't eat the
 *     whole attempt budget; patterns are validated once before any solving is
 *     attempted, so a malformed pattern fails immediately instead of via a
 *     wasted search.
 *
 * What to check if generation is slow/hanging again:
 *   1. Grep logcat for tag "GridGenerator" — every attempt logs backtracks/
 *      elapsedMs/success, and empty-domain aborts + pattern validation
 *      failures are logged explicitly and separately.
 *   2. "has 0 candidate words" → dictionary coverage problem at that length,
 *      not a solver bug.
 *   3. "pattern rejected" → the block pattern itself is malformed (see
 *      validatePattern) — this should never fire for the built-in patterns;
 *      if it does after editing BLOCK_PATTERNS_15/11, the edit broke symmetry
 *      or created a 1-cell slot.
 *   4. High backtracks with no empty-domain/validation issues → dictionary has
 *      words but the constraint graph is genuinely tight. Consider LCV value
 *      ordering (not yet implemented — flagged as a follow-up, not needed yet).
 * ═══════════════════════════════════════════════════════════════════════════
 */

/**
 * A single across/down word slot on the grid — a maximal run of >=2 open
 * (non-blocked) cells in one direction.
 *
 * @property id unique within one [detectSlots] call; used as the key into
 *   [GeneratedGrid.fill] and throughout the solver's domain/assignment maps.
 * @property length number of cells in the slot; determines which dictionary
 *   words are even eligible.
 */
data class GridSlot(
    val id: Int,
    val startRow: Int,
    val startCol: Int,
    val length: Int,
    val isHorizontal: Boolean
) {
    /** Ordered list of (row, col) cells this slot occupies, start to end. */
    fun cells(): List<Pair<Int, Int>> = (0 until length).map { i ->
        if (isHorizontal) startRow to (startCol + i) else (startRow + i) to startCol
    }
}

/**
 * Records that [slotB]'s letter at [indexB] must equal the owning slot's
 * letter at [indexA] — i.e. the two slots share a board cell. Built once per
 * PATTERN by [buildCrossings] (see [PatternInfo]) and reused across every fill
 * attempt against that pattern, since crossings depend only on geometry, never
 * on word choice.
 */
data class SlotCrossing(val slotB: Int, val indexA: Int, val indexB: Int)

/**
 * A fully-solved grid: the block pattern, the slots detected from it, and the
 * winning word assignment. [fill] is guaranteed complete (one word per slot)
 * if this object exists at all — [GridGenerator.generate] only ever returns
 * this on full success, never partial.
 */
data class GeneratedGrid(
    val size: Int,
    val blocked: Set<Pair<Int, Int>>,
    val slots: List<GridSlot>,
    /** slotId -> filled word. */
    val fill: Map<Int, String>
)

/**
 * Everything derivable from a block pattern's GEOMETRY alone — computed once
 * per pattern and reused across every retry attempt against it. Previously
 * [detectSlots] and [buildCrossings] were both re-run inside every single
 * `fillGrid` call, so retrying the same pattern 3x meant recomputing identical
 * geometry 3x for no reason. This struct is the fix: build it once in
 * [GridGenerator.generate], pass it into every attempt.
 */
private data class PatternInfo(
    val blocked: Set<Pair<Int, Int>>,
    val slots: List<GridSlot>,
    val crossings: Map<Int, List<SlotCrossing>>
)

/**
 * Fast lookup structure: for a given word length, which words have letter [c]
 * at position [pos]. Replaces the old approach of linearly scanning a slot's
 * entire remaining domain on every forward-check.
 *
 * Built once per [GridGenerator.generate] call (words don't change between
 * attempts/patterns within one generation request) and reused everywhere.
 *
 * Shape: length -> position -> letter -> set of words (that length, that
 * letter, at that position). Note this indexes the FULL dictionary, not any
 * slot's shrinking domain — domain shrinkage is handled separately by
 * intersecting a slot's current domain set against this index's match set.
 */
private class LetterPositionIndex(wordsByLength: Map<Int, List<String>>) {
    private val index: Map<Int, Map<Int, Map<Char, Set<String>>>> =
        wordsByLength.mapValues { (length, words) ->
            (0 until length).associateWith { pos ->
                words.groupBy { it[pos] }.mapValues { it.value.toSet() }
            }
        }

    /** All words of [length] that have [letter] at [position]. Empty set if none. */
    fun wordsWithLetterAt(length: Int, position: Int, letter: Char): Set<String> =
        index[length]?.get(position)?.get(letter) ?: emptySet()
}

object GridGenerator {

    private const val TAG = "GridGenerator"

    /**
     * Hand-designed, 180°-rotationally-symmetric 15x15 block patterns (design
     * doc §4.3 step 1). Tried in a SHUFFLED order by [generate] (see
     * [PatternInfo] docs on why) until one yields a complete fill.
     */
    val BLOCK_PATTERNS_15: List<Set<Pair<Int, Int>>> = listOf(pattern15A(), pattern15B())

    /** Smaller pattern set used for Codeword and Crossword's Quick mode. */
    val BLOCK_PATTERNS_11: List<Set<Pair<Int, Int>>> = listOf(pattern11A())

    /** Mirrors [base] blocked cells 180° around the grid center to guarantee symmetry. */
    private fun symmetric(size: Int, base: List<Pair<Int, Int>>): Set<Pair<Int, Int>> {
        val blocks = mutableSetOf<Pair<Int, Int>>()
        for ((r, c) in base) {
            blocks += r to c
            blocks += (size - 1 - r) to (size - 1 - c)
        }
        return blocks
    }

    private fun pattern15A() = symmetric(
        15,
        listOf(
            0 to 4, 0 to 10, 1 to 4, 1 to 10, 2 to 7,
            3 to 0, 3 to 1, 3 to 13, 3 to 14, 4 to 4, 4 to 10,
            5 to 7, 6 to 2, 6 to 12, 7 to 5, 7 to 9
        )
    )

    private fun pattern15B() = symmetric(
        15,
        listOf(
            0 to 5, 0 to 9, 1 to 5, 1 to 9, 2 to 2, 2 to 12,
            3 to 7, 4 to 0, 4 to 4, 4 to 10, 4 to 14, 5 to 7,
            6 to 3, 6 to 11
        )
    )

    private fun pattern11A() = symmetric(
        11,
        listOf(0 to 3, 0 to 7, 1 to 3, 1 to 7, 2 to 5, 3 to 0, 3 to 10, 4 to 2, 4 to 8)
    )

    /**
     * Scans [blocked] to find every maximal run of >=2 open cells in each
     * direction and turns each run into a [GridSlot]. Runs of length 0-1 are
     * not slots (nothing to fill / nothing to cross).
     */
    fun detectSlots(size: Int, blocked: Set<Pair<Int, Int>>): List<GridSlot> {
        val slots = mutableListOf<GridSlot>()
        var id = 0

        for (r in 0 until size) {
            var c = 0
            while (c < size) {
                if ((r to c) in blocked) { c++; continue }
                val start = c
                while (c < size && (r to c) !in blocked) c++
                val len = c - start
                if (len >= 2) slots += GridSlot(id++, r, start, len, isHorizontal = true)
            }
        }
        for (c in 0 until size) {
            var r = 0
            while (r < size) {
                if ((r to c) in blocked) { r++; continue }
                val start = r
                while (r < size && (r to c) !in blocked) r++
                val len = r - start
                if (len >= 2) slots += GridSlot(id++, start, c, len, isHorizontal = false)
            }
        }
        return slots
    }

    /**
     * Precomputes every pairwise crossing between slots that share a board
     * cell. Depends only on slot geometry, never on word content — see
     * [PatternInfo] for why this is now cached per-pattern instead of
     * recomputed per-attempt.
     *
     * @return slotId -> list of crossings *from that slot's perspective*
     *   (both directions of a crossing are stored, once under each owning slot).
     */
    fun buildCrossings(slots: List<GridSlot>): Map<Int, List<SlotCrossing>> {
        val cellOwners = mutableMapOf<Pair<Int, Int>, MutableList<Pair<Int, Int>>>()
        for (slot in slots) {
            slot.cells().forEachIndexed { idx, cell ->
                cellOwners.getOrPut(cell) { mutableListOf() }.add(slot.id to idx)
            }
        }
        val crossings = mutableMapOf<Int, MutableList<SlotCrossing>>()
        for ((_, owners) in cellOwners) {
            if (owners.size < 2) continue
            for (i in owners.indices) for (j in owners.indices) {
                if (i == j) continue
                val (slotA, idxA) = owners[i]
                val (slotB, idxB) = owners[j]
                if (slotA == slotB) continue
                crossings.getOrPut(slotA) { mutableListOf() }.add(SlotCrossing(slotB, idxA, idxB))
            }
        }
        return crossings
    }

    /**
     * Cheap sanity check on a block pattern, run once before any solving is
     * attempted. Catches malformed patterns (e.g. a hand-edit to
     * BLOCK_PATTERNS_15/11 that breaks symmetry or creates a degenerate
     * 1-cell slot) immediately instead of discovering the problem via a
     * doomed multi-second search.
     *
     * Checks:
     *  - Pattern is 180°-rotationally symmetric.
     *  - No slot has length < 2 (detectSlots already excludes these, so this
     *    mainly guards against a future refactor accidentally changing that).
     *  - At least one slot exists in each direction (a pattern that's all
     *    blocked cells, or has only across words and no down words, is
     *    useless for a crossword).
     *
     * Does NOT check dictionary coverage — that's handled separately by
     * fillGrid's empty-domain fail-fast, since it depends on the word list,
     * not the pattern itself.
     */
    private fun validatePattern(size: Int, blocked: Set<Pair<Int, Int>>, slots: List<GridSlot>): Boolean {
        val symmetric = blocked.all { (r, c) -> (size - 1 - r) to (size - 1 - c) in blocked }
        if (!symmetric) {
            Log.w(TAG, "pattern rejected: not rotationally symmetric")
            return false
        }
        if (slots.any { it.length < 2 }) {
            Log.w(TAG, "pattern rejected: contains a slot shorter than 2 cells")
            return false
        }
        val hasAcross = slots.any { it.isHorizontal }
        val hasDown = slots.any { !it.isHorizontal }
        if (!hasAcross || !hasDown) {
            Log.w(TAG, "pattern rejected: missing across or down slots entirely")
            return false
        }
        return true
    }

    /**
     * Attempts to fill every slot in [pattern] with a distinct dictionary word
     * satisfying all crossing constraints, using most-constrained-variable
     * ordering and forward checking accelerated by [index].
     *
     * v4 fix (see log2.logcat diagnosis): the v3 index made forward-check's
     * MEMBERSHIP TEST O(1), but forwardCheck was still ITERATING the crossing
     * slot's full current domain regardless of size. Against a 150k+-word
     * dictionary, an unpruned domain for a common length (4-8) can be tens of
     * thousands of words, so that iteration dominated per-node cost even
     * though the search tree itself stayed small (confirmed by log2: 18-371
     * backtracks per attempt, every attempt still hitting the ~4s deadline).
     *
     * Fix: forwardCheck now iterates whichever of {matching, otherDomain} is
     * SMALLER, and prunes by swapping in a whole new domain Set by reference
     * (O(1) save/restore) instead of removeAll/addAll on a shared mutable Set
     * (O(domain size) either way). Also drops the old toList() copy of the
     * slot's own domain (unnecessary — it's never mutated during its own
     * candidate loop) and replaces the `word in assignment.values` linear
     * scan with an O(1) usedWords set.
     */
    private fun fillGrid(
        pattern: PatternInfo,
        wordsByLength: Map<Int, List<String>>,
        index: LetterPositionIndex,
        maxBacktracks: Int,
        deadlineMillis: Long,
        lcvSampleSize: Int = 60
    ): Map<Int, String>? {
        val slots = pattern.slots
        if (slots.isEmpty()) return emptyMap()
        val crossings = pattern.crossings
        val slotById = slots.associateBy { it.id }
        val startTime = System.currentTimeMillis()

        val domain: MutableMap<Int, MutableSet<String>> = slots.associate { slot ->
            slot.id to (wordsByLength[slot.length].orEmpty()).shuffled().toMutableSet()
        }.toMutableMap()

        val emptySlot = slots.firstOrNull { (domain[it.id]?.size ?: 0) == 0 }
        if (emptySlot != null) {
            Log.d(TAG, "Slot ${emptySlot.id} (len=${emptySlot.length}) has 0 candidate words — aborting attempt early")
            return null
        }

        // Diagnostic: surface the pattern's weakest slots before searching.
        // If a long slot sits at a tiny domain size here, repeated genuine
        // (non-budget) failures are a coverage problem LCV cannot fix.
        val smallestDomains = slots
            .sortedBy { domain[it.id]?.size ?: 0 }
            .take(5)
            .joinToString { "id=${it.id} len=${it.length} domainSize=${domain[it.id]?.size}" }
        Log.d(TAG, "smallest domains: $smallestDomains")

        val assignment = mutableMapOf<Int, String>()
        val usedWords = mutableSetOf<String>()
        var backtrackCount = 0

        fun timeUp() = System.currentTimeMillis() - startTime > deadlineMillis

        fun mostConstrainedSlot(unassigned: List<GridSlot>): GridSlot =
            unassigned.minByOrNull { domain[it.id]?.size ?: Int.MAX_VALUE }!!

        fun consistent(slot: GridSlot, word: String): Boolean {
            for (crossing in crossings[slot.id].orEmpty()) {
                val other = assignment[crossing.slotB] ?: continue
                if (crossing.indexB >= other.length || crossing.indexA >= word.length) continue
                if (word[crossing.indexA] != other[crossing.indexB]) return false
            }
            return true
        }

        /**
         * LCV cost for [word] in [slot]: estimated total number of candidates
         * that would be eliminated from unassigned crossing slots' domains if
         * [word] were chosen. Lower = less constraining = tried first.
         *
         * Uses the same min-side-iteration trick as forwardCheck to keep the
         * COUNT cheap (no set construction, just tallying), so scoring a
         * sample of candidates doesn't reintroduce the O(large domain) cost
         * the last patch removed.
         */
        fun lcvCost(slot: GridSlot, word: String): Int {
            var cost = 0
            for (crossing in crossings[slot.id].orEmpty()) {
                val otherId = crossing.slotB
                if (otherId in assignment) continue
                val otherDomain = domain[otherId] ?: continue
                val letter = word.getOrNull(crossing.indexA) ?: continue
                val otherSlot = slotById[otherId] ?: continue
                val idxB = crossing.indexB
                if (idxB >= otherSlot.length) continue

                val matching = index.wordsWithLetterAt(otherSlot.length, idxB, letter)
                val kept = if (matching.size < otherDomain.size) {
                    matching.count { it in otherDomain }
                } else {
                    otherDomain.count { it in matching }
                }
                cost += (otherDomain.size - kept)
            }
            return cost
        }

        /** LCV-ordered candidates: cheap sample sorted least-constraining-first, then the rest untouched. */
        fun orderedCandidates(slot: GridSlot): List<String> {
            val fullDomain = domain[slot.id].orEmpty()
            if (fullDomain.size <= lcvSampleSize) {
                return fullDomain.sortedBy { lcvCost(slot, it) }
            }
            val sample = fullDomain.asSequence().take(lcvSampleSize).toList()
            val sampleSet = sample.toHashSet()
            val scoredSample = sample.sortedBy { lcvCost(slot, it) }
            val rest = fullDomain.asSequence().filterNot { it in sampleSet }
            return scoredSample + rest
        }

        fun forwardCheck(slot: GridSlot, word: String): MutableMap<Int, MutableSet<String>>? {
            val savedDomains = mutableMapOf<Int, MutableSet<String>>()
            for (crossing in crossings[slot.id].orEmpty()) {
                val otherId = crossing.slotB
                if (otherId in assignment) continue
                val otherDomain = domain[otherId] ?: continue
                val letter = word.getOrNull(crossing.indexA) ?: continue
                val otherSlot = slotById[otherId] ?: continue
                val idxB = crossing.indexB
                if (idxB >= otherSlot.length) continue

                val matching = index.wordsWithLetterAt(otherSlot.length, idxB, letter)
                val newDomain: MutableSet<String> = if (matching.size < otherDomain.size) {
                    matching.filterTo(HashSet(minOf(matching.size, otherDomain.size))) { it in otherDomain }
                } else {
                    otherDomain.filterTo(HashSet()) { it in matching }
                }

                if (newDomain.size == otherDomain.size) continue

                savedDomains[otherId] = otherDomain
                domain[otherId] = newDomain
                if (newDomain.isEmpty()) return null
            }
            return savedDomains
        }

        fun undoForwardCheck(saved: Map<Int, MutableSet<String>>) {
            for ((slotId, oldDomain) in saved) domain[slotId] = oldDomain
        }

        fun backtrack(unassigned: List<GridSlot>): Boolean {
            if (unassigned.isEmpty()) return true
            if (backtrackCount > maxBacktracks || timeUp()) return false

            val slot = mostConstrainedSlot(unassigned)
            val remaining = unassigned - slot
            val candidates = orderedCandidates(slot)   // LCV ordering applied here

            for (word in candidates) {
                if (timeUp()) return false
                if (word in usedWords) continue
                if (!consistent(slot, word)) continue

                assignment[slot.id] = word
                usedWords.add(word)
                val savedDomains = forwardCheck(slot, word)
                if (savedDomains != null) {
                    if (backtrack(remaining)) return true
                    undoForwardCheck(savedDomains)
                }
                assignment.remove(slot.id)
                usedWords.remove(word)
                backtrackCount++
                if (backtrackCount > maxBacktracks || timeUp()) return false
            }
            return false
        }

        val ok = backtrack(slots)
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "fillGrid: success=$ok backtracks=$backtrackCount elapsedMs=$elapsed slots=${slots.size}")
        return if (ok) assignment.toMap() else null
    }

    /**
     * Top-level entry point: builds the letter-position index once, then
     * tries every pattern at [size] IN SHUFFLED ORDER (so a consistently
     * "unlucky" first pattern doesn't silently eat the whole attempt budget
     * every single generation call — each pattern gets a fair shot across
     * repeated plays). For each pattern, geometry (slots + crossings) is
     * computed exactly once and reused across all [attemptsPerPattern] retries
     * of that pattern, and the pattern is validated before any solving starts.
     *
     * Falls back to the smaller 11x11 pattern set once if every 15x15 pattern
     * exhausts its budget, rather than returning `null` and leaving the caller
     * with an indefinite spinner.
     *
     * @return `null` only if even the 11x11 fallback fails — check logcat tag
     *   "GridGenerator" for per-attempt diagnostics before assuming "no
     *   solution exists"; it usually means dictionary coverage is thin at some
     *   required length.
     */
    fun generate(
        size: Int,
        wordsByLength: Map<Int, List<String>>,
        patterns: List<Set<Pair<Int, Int>>> = if (size <= 11) BLOCK_PATTERNS_11 else BLOCK_PATTERNS_15,
        attemptsPerPattern: Int = 4,
        maxBacktracksPerAttempt: Int = 20_000,
        deadlineMillisPerAttempt: Long = 25_000L
    ): GeneratedGrid? {
        val index = LetterPositionIndex(wordsByLength)

        for (pattern in patterns.shuffled()) {
            val slots = detectSlots(size, pattern)
            if (!validatePattern(size, pattern, slots)) continue

            val patternInfo = PatternInfo(pattern, slots, buildCrossings(slots))

            repeat(attemptsPerPattern) { attempt ->
                Log.d(TAG, "generate: size=$size attempt=$attempt slots=${slots.size}")
                val fill = fillGrid(patternInfo, wordsByLength, index, maxBacktracksPerAttempt, deadlineMillisPerAttempt)
                if (fill != null) return GeneratedGrid(size, pattern, slots, fill)
            }
        }

        if (size > 11) {
            Log.w(TAG, "generate: size=$size exhausted all patterns — falling back to 11x11")
            return generate(11, wordsByLength, BLOCK_PATTERNS_11, attemptsPerPattern, maxBacktracksPerAttempt, deadlineMillisPerAttempt)
        }

        Log.w(TAG, "generate: no fill found even at 11x11 — dictionary likely lacks coverage at required lengths")
        return null
    }
}