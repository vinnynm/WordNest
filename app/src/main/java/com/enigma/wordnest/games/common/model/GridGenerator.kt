package com.enigma.wordnest.games.common.model

/**
 * Shared grid-fill engine for Crossword and Codeword (design doc §5.1: Codeword is
 * "close to a reskin" of the Crossword generator).
 *
 * Audit resolution: uses forward-checking + most-constrained-variable (MCV) ordering
 * rather than full AC-3. Real AC-3 propagates arc consistency across the *entire*
 * constraint graph on every assignment; forward-checking only prunes the domains of
 * slots directly crossing the one just filled. For a 15x15 grid regenerated once in
 * the background (not a live per-frame solve), forward-checking + MCV gets ~90% of the
 * pruning power of AC-3 for a fraction of the implementation complexity, and is combined
 * here with a bounded backtrack budget + multiple block-pattern fallbacks so generation
 * never hangs indefinitely on a bad pattern/dictionary combination.
 */

data class GridSlot(
    val id: Int,
    val startRow: Int,
    val startCol: Int,
    val length: Int,
    val isHorizontal: Boolean
) {
    fun cells(): List<Pair<Int, Int>> = (0 until length).map { i ->
        if (isHorizontal) startRow to (startCol + i) else (startRow + i) to startCol
    }
}

/** slotA's index [indexA] sits on the same board cell as slotB's index [indexB]. */
data class SlotCrossing(val slotB: Int, val indexA: Int, val indexB: Int)

data class GeneratedGrid(
    val size: Int,
    val blocked: Set<Pair<Int, Int>>,
    val slots: List<GridSlot>,
    /** slotId -> filled word */
    val fill: Map<Int, String>
)

object GridGenerator {

    /**
     * A small library of hand-designed, 180-degree-rotationally-symmetric block patterns,
     * per design doc §4.3 step 1 ("start with a handful of hand-designed 15x15 patterns
     * to avoid needing a pattern-generator as a separate project").
     */
    val BLOCK_PATTERNS_15: List<Set<Pair<Int, Int>>> = listOf(pattern15A(), pattern15B())

    /** Smaller pattern for Codeword/quick-mode grids. */
    val BLOCK_PATTERNS_11: List<Set<Pair<Int, Int>>> = listOf(pattern11A())

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

    /** Detects across/down slots (runs of >= 2 open cells) from a block pattern. */
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

    /** slotId -> list of crossings against other slots, precomputed once per slot layout. */
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
     * Fills slots using most-constrained-variable ordering + forward checking, bounded by
     * [maxBacktracks]. Returns null if no complete fill was found within budget.
     */
    fun fillGrid(
        slots: List<GridSlot>,
        wordsByLength: Map<Int, List<String>>,
        maxBacktracks: Int = 20_000
    ): Map<Int, String>? {
        if (slots.isEmpty()) return emptyMap()
        val crossings = buildCrossings(slots)

        // Domains seeded by length only; forward checking narrows them as we go.
        val domain: MutableMap<Int, MutableList<String>> = slots.associate { slot ->
            slot.id to (wordsByLength[slot.length].orEmpty()).shuffled().toMutableList()
        }.toMutableMap()

        val assignment = mutableMapOf<Int, String>()
        var backtrackCount = 0

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

        /** Prunes crossing slots' domains after assigning [word] to [slot]. Null = dead end. */
        fun forwardCheck(slot: GridSlot, word: String): MutableMap<Int, MutableList<String>>? {
            val removed = mutableMapOf<Int, MutableList<String>>()
            for (crossing in crossings[slot.id].orEmpty()) {
                val otherId = crossing.slotB
                if (otherId in assignment) continue
                val otherDomain = domain[otherId] ?: continue
                val letter = word.getOrNull(crossing.indexA) ?: continue
                val idxB = crossing.indexB
                val toRemove = otherDomain.filter { candidate ->
                    idxB >= candidate.length || candidate[idxB] != letter
                }
                if (toRemove.isNotEmpty()) {
                    otherDomain.removeAll(toRemove)
                    removed.getOrPut(otherId) { mutableListOf() }.addAll(toRemove)
                    if (otherDomain.isEmpty()) return null
                }
            }
            return removed
        }

        fun undoForwardCheck(removed: Map<Int, List<String>>) {
            for ((slotId, words) in removed) domain[slotId]?.addAll(words)
        }

        fun backtrack(unassigned: List<GridSlot>): Boolean {
            if (unassigned.isEmpty()) return true
            if (backtrackCount > maxBacktracks) return false

            val slot = mostConstrainedSlot(unassigned)
            val remaining = unassigned - slot
            val candidates = domain[slot.id]?.toList().orEmpty()

            for (word in candidates) {
                if (word in assignment.values) continue // no duplicate words in one grid
                if (!consistent(slot, word)) continue

                assignment[slot.id] = word
                val removedForUndo = forwardCheck(slot, word)
                if (removedForUndo != null) {
                    if (backtrack(remaining)) return true
                    undoForwardCheck(removedForUndo)
                }
                assignment.remove(slot.id)
                backtrackCount++
                if (backtrackCount > maxBacktracks) return false
            }
            return false
        }

        return if (backtrack(slots)) assignment.toMap() else null
    }

    /**
     * Generates a full grid: tries each block pattern in turn, retrying with fresh random
     * domain shuffles, until a fill succeeds or all patterns/attempts are exhausted.
     */
    fun generate(
        size: Int,
        wordsByLength: Map<Int, List<String>>,
        patterns: List<Set<Pair<Int, Int>>> = if (size <= 11) BLOCK_PATTERNS_11 else BLOCK_PATTERNS_15,
        attemptsPerPattern: Int = 5,
        maxBacktracksPerAttempt: Int = 20_000
    ): GeneratedGrid? {
        for (pattern in patterns) {
            val slots = detectSlots(size, pattern)
            repeat(attemptsPerPattern) {
                val fill = fillGrid(slots, wordsByLength, maxBacktracksPerAttempt)
                if (fill != null) return GeneratedGrid(size, pattern, slots, fill)
            }
        }
        return null
    }
}
