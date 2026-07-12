package com.enigma.wordnest.games.common.model

import android.util.Log

/**
 * Growth-based grid-fill engine for Crossword and Codeword.
 *
 * ── Quick-review summary ─────────────────────────────────────────────────
 * Builds the grid the way human constructors do: start with one word, then
 * repeatedly graft new words onto it, crossing only at matching letters.
 * A strict "no incidental adjacency" rule means every word ever placed
 * stays exactly as placed — no backtracking, no risk of an unsolvable
 * skeleton, no accidentally-formed fragment words. Black squares are a
 * *result* of growth, not a constraint imposed up front.
 *
 * Two passes:
 *   1. Frontier growth — repeatedly finds a letter that's missing one of
 *      its two directions (across/down) and tries to graft a crossing word
 *      onto it.
 *   2. Gap densification — after growth stalls, scans every remaining
 *      blank run between two already-placed words (row-wise and
 *      column-wise) and tries to bridge it with a dictionary word that
 *      legally crosses whatever's around it.
 *
 * Both passes reuse the same [canPlace]/[placeWord] primitives, so gap
 * densification can only ever add legal words — it can't introduce any
 * adjacency growth wouldn't have allowed.
 * ──────────────────────────────────────────────────────────────────────
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

data class GeneratedGrid(
    val size: Int,
    val blocked: Set<Pair<Int, Int>>,
    val slots: List<GridSlot>,
    /** slotId -> filled word */
    val fill: Map<Int, String>
)





object GridGenerator {

    private const val TAG = "GridGenerator"

    /**
     * Generates a grid by growing outward from a seed word, then densifying
     * remaining gaps.
     *
     * @param size              board is [size]x[size]; placements are clamped inside it
     * @param wordsByLength     dictionary, length -> word list
     * @param targetFillRatio   stop frontier-growth once this fraction of the board
     *                          is covered by letters (soft target — growth also
     *                          stops if it runs out of valid placements first)
     * @param maxWords          hard cap on words placed, regardless of density
     * @param maxAttempts       total generation attempts (fresh seed + shuffled
     *                          pools) before giving up
     * @param maxCandidatesPerFrontierCell how many candidate words to try per
     *                          frontier/gap slot per pass — bounds worst-case cost
     * @param allowTwoLetterBridges whether densify may bridge 2-letter gaps
     *                          using the dictionary's 2-letter words (if the
     *                          pool has any). Off by default to avoid a grid
     *                          cluttered with AA/OX/IT-style filler; turn on
     *                          if you want maximum density over "clean" fill.
     */
    fun generate(
        size: Int,
        wordsByLength: Map<Int, List<String>>,
        targetFillRatio: Double = 0.72,
        maxWords: Int = Int.MAX_VALUE,
        maxAttempts: Int = 10,
        maxCandidatesPerFrontierCell: Int = 24,
        allowTwoLetterBridges: Boolean = false
    ): GeneratedGrid? {
        val minWordLen = if (allowTwoLetterBridges) 2 else 3
        // Only usable-length words (2/3+ letter slots) and words must fit the board.
        val pool: Map<Int, List<String>> = wordsByLength
            .filterKeys { it in minWordLen..size }
            .mapValues { (_, words) -> words.map { it.uppercase() }.distinct() }

        if (pool.values.sumOf { it.size } == 0) {
            Log.w(TAG, "generate: no usable words for size=$size (need length $minWordLen..$size)")
            return null
        }

        var best: GeneratedGrid? = null
        var bestFillRatio = 0.0

        repeat(maxAttempts) { attempt ->
            Log.d(TAG, "generate: size=$size attempt=$attempt")
            val result = growOnce(size, pool, targetFillRatio, maxWords, maxCandidatesPerFrontierCell)
            if (result != null) {
                val filledCells = size * size - result.blocked.size
                val ratio = filledCells.toDouble() / (size * size)
                Log.d(TAG, "generate: attempt=$attempt words=${result.slots.size} fillRatio=${"%.2f".format(ratio)}")

                if (ratio > bestFillRatio) {
                    best = result
                    bestFillRatio = ratio
                }
                // Good enough — stop early rather than burning through all attempts.
                if (ratio >= targetFillRatio) return best
            }
        }

        if (best != null) {
            Log.d(TAG, "generate: returning best attempt at fillRatio=${"%.2f".format(bestFillRatio)}")
        } else {
            Log.w(TAG, "generate: size=$size exhausted $maxAttempts attempts — dictionary likely lacks coverage")
        }
        return best
    }

    // ── Single generation attempt (growth + densify) ─────────────────────────

    private data class Placement(val word: String, val row: Int, val col: Int, val horizontal: Boolean)

    private fun growOnce(
        size: Int,
        pool: Map<Int, List<String>>,
        targetFillRatio: Double,
        maxWords: Int,
        maxCandidatesPerFrontierCell: Int
    ): GeneratedGrid? {
        val grid = Array(size) { CharArray(size) { ' ' } }
        // Which direction(s) already run through a given cell — a cell may host at
        // most one across word and one down word (never two of the same direction).
        val acrossAt = HashSet<Pair<Int, Int>>()
        val downAt = HashSet<Pair<Int, Int>>()
        val usedWords = HashSet<String>()          // O(1) duplicate check
        val placements = mutableListOf<Placement>()

        // ── Seed word: longest-ish word available, placed horizontally near center ──
        val seedLength = pool.keys.filter { it >= 4 }.maxOrNull() ?: pool.keys.maxOrNull() ?: return null
        val seedCandidates = pool[seedLength]?.shuffled() ?: return null
        val seedWord = seedCandidates.firstOrNull() ?: return null
        val seedRow = size / 2
        val seedCol = (size - seedWord.length) / 2
        if (seedCol < 0) return null

        placeWord(grid, acrossAt, downAt, seedWord, seedRow, seedCol, horizontal = true)
        placements += Placement(seedWord, seedRow, seedCol, true)
        usedWords += seedWord

        val totalCells = size * size
        var consecutiveFailedPasses = 0

        // ── Pass 1: frontier growth ─────────────────────────────────────────
        while (placements.size < maxWords) {
            val filledCells = placements.sumOf { it.word.length } - overlapCorrection(placements)
            if (filledCells.toDouble() / totalCells >= targetFillRatio) break

            val frontier = frontierCells(grid, acrossAt, downAt, size).shuffled()
            if (frontier.isEmpty()) break

            var placedThisPass = false

            for ((r, c) in frontier) {
                val letter = grid[r][c]
                if (letter == ' ') continue
                val wantsAcross = (r to c) !in acrossAt
                val wantsDown = (r to c) !in downAt
                if (!wantsAcross && !wantsDown) continue

                val candidates = candidateWordsForLetter(pool, letter, usedWords, maxCandidatesPerFrontierCell)
                var placedHere = false

                for (candidate in candidates) {
                    if (placedHere) break
                    for (idx in candidate.indices) {
                        if (candidate[idx] != letter) continue

                        if (wantsDown) {
                            val startRow = r - idx
                            if (canPlace(grid, acrossAt, downAt, size, candidate, startRow, c, horizontal = false)) {
                                placeWord(grid, acrossAt, downAt, candidate, startRow, c, horizontal = false)
                                placements += Placement(candidate, startRow, c, false)
                                usedWords += candidate
                                placedHere = true
                                break
                            }
                        }
                        if (!placedHere && wantsAcross) {
                            val startCol = c - idx
                            if (canPlace(grid, acrossAt, downAt, size, candidate, r, startCol, horizontal = true)) {
                                placeWord(grid, acrossAt, downAt, candidate, r, startCol, horizontal = true)
                                placements += Placement(candidate, r, startCol, true)
                                usedWords += candidate
                                placedHere = true
                                break
                            }
                        }
                    }
                }

                if (placedHere) {
                    placedThisPass = true
                    if (placements.size >= maxWords) break
                }
            }

            if (placedThisPass) {
                consecutiveFailedPasses = 0
            } else {
                consecutiveFailedPasses++
                if (consecutiveFailedPasses >= 4) break // frontier genuinely exhausted
            }
        }

        // ── Pass 2: bridge remaining gaps between already-placed words ──────
        densify(size, grid, acrossAt, downAt, placements, usedWords, pool, maxCandidatesPerFrontierCell)

        // Need at least a handful of words for this to be a real puzzle, not just the seed.
        if (placements.size < 3) return null

        return buildGeneratedGrid(size, placements)
    }

    // ── Pass 2: gap-fill densification ──────────────────────────────────────
    //
    // After growth stalls, some blank runs sit between two already-placed
    // words with no crossing of their own — e.g. ROSE at (0, 11:14) and
    // PRAWN at (2, 10:14) leave row 1 blank between them. If a dictionary
    // word matches whatever letters would cross it at each column, it can
    // bridge the gap. This pass is strictly additive over already-valid
    // growth output: it reuses canPlace/placeWord, so it enforces the exact
    // same no-incidental-adjacency rule as pass 1 and can only add valid
    // words, never break anything already placed.

    private fun densify(
        size: Int,
        grid: Array<CharArray>,
        acrossAt: MutableSet<Pair<Int, Int>>,
        downAt: MutableSet<Pair<Int, Int>>,
        placements: MutableList<Placement>,
        usedWords: MutableSet<String>,
        pool: Map<Int, List<String>>,
        maxCandidatesPerGap: Int
    ) {
        val minGapLen = pool.keys.minOrNull() ?: 3
        var addedAny = true
        var safetyPasses = 0
        var totalAdded = 0
        // Repeat until a full pass adds nothing — filling one gap can create
        // new crossing letters that make an adjacent gap fillable too.
        while (addedAny && safetyPasses < 10) {
            addedAny = false
            safetyPasses++

            // Vertical gaps: scan each column for blank runs, try to place a DOWN word.
            for (c in 0 until size) {
                var r = 0
                while (r < size) {
                    if (grid[r][c] != ' ') { r++; continue }
                    val runStart = r
                    while (r < size && grid[r][c] == ' ') r++
                    val runLen = r - runStart
                    if (runLen < minGapLen) continue
                    if (tryFillGap(size, grid, acrossAt, downAt, placements, usedWords, pool,
                            runStart, c, runLen, horizontal = false, maxCandidatesPerGap)) {
                        addedAny = true
                        totalAdded++
                    }
                }
            }

            // Horizontal gaps: same idea, scanning rows for an ACROSS word.
            for (r in 0 until size) {
                var c = 0
                while (c < size) {
                    if (grid[r][c] != ' ') { c++; continue }
                    val runStart = c
                    while (c < size && grid[r][c] == ' ') c++
                    val runLen = c - runStart
                    if (runLen < minGapLen) continue
                    if (tryFillGap(size, grid, acrossAt, downAt, placements, usedWords, pool,
                            r, runStart, runLen, horizontal = true, maxCandidatesPerGap)) {
                        addedAny = true
                        totalAdded++
                    }
                }
            }
        }
        if (totalAdded > 0) Log.d(TAG, "densify: added $totalAdded bridge word(s)")
    }

    /**
     * Tries to fill (some or all of) a blank run of length [runLen] starting at
     * ([runStart], [fixedCoord]) with a word. Tries every length from [runLen]
     * down to the shortest usable length, at every valid offset within the run,
     * preferring longer/more-central fits first. A shorter word that only fills
     * part of the run is fine: [densify]'s outer loop rescans afterward and will
     * find the remaining sub-run(s) on the next pass.
     */
    private fun tryFillGap(
        size: Int,
        grid: Array<CharArray>,
        acrossAt: MutableSet<Pair<Int, Int>>,
        downAt: MutableSet<Pair<Int, Int>>,
        placements: MutableList<Placement>,
        usedWords: MutableSet<String>,
        pool: Map<Int, List<String>>,
        runStart: Int,
        fixedCoord: Int,
        runLen: Int,
        horizontal: Boolean,
        maxCandidates: Int
    ): Boolean {
        val minLen = pool.keys.minOrNull() ?: 3
        // Longer fits first (fills more of the run per placement, fewer total
        // words needed), each length tried at a shuffled set of offsets so we
        // don't always bias toward one edge of the run.
        for (length in runLen downTo minLen) {
            val maxOffset = runLen - length
            val offsets = (0..maxOffset).shuffled()

            for (offset in offsets) {
                val startRow = if (horizontal) fixedCoord else runStart + offset
                val startCol = if (horizontal) runStart + offset else fixedCoord

                val candidates = pool[length].orEmpty()
                    .asSequence()
                    .filter { it !in usedWords }
                    .shuffled()
                    .take(maxCandidates * 8) // oversample since most will fail canPlace
                    .toList()

                for (word in candidates) {
                    if (canPlace(grid, acrossAt, downAt, size, word, startRow, startCol, horizontal)) {
                        placeWord(grid, acrossAt, downAt, word, startRow, startCol, horizontal)
                        placements += Placement(word, startRow, startCol, horizontal)
                        usedWords += word
                        return true
                    }
                }
            }
        }
        return false
    }

    // ── Placement validity ───────────────────────────────────────────────────

    /**
     * A placement is valid when:
     *  - it fits on the board
     *  - the cell immediately before the start and after the end (in the word's own
     *    direction) is off-board or blank — prevents merging with a collinear neighbor
     *  - every position either lands on a blank cell (whose perpendicular neighbors
     *    must ALSO be blank, and whose perpendicular direction is free) or lands on a
     *    matching letter that doesn't already have a word running this same direction
     *  - it crosses at least one existing letter (except for the very first/seed word)
     */
    private fun canPlace(
        grid: Array<CharArray>,
        acrossAt: Set<Pair<Int, Int>>,
        downAt: Set<Pair<Int, Int>>,
        size: Int,
        word: String,
        startRow: Int,
        startCol: Int,
        horizontal: Boolean
    ): Boolean {
        val endRow = if (horizontal) startRow else startRow + word.length - 1
        val endCol = if (horizontal) startCol + word.length - 1 else startCol
        if (startRow < 0 || startCol < 0 || endRow >= size || endCol >= size) return false

        // Boundary cells (just before start, just after end) must be blank/off-board.
        val beforeR = if (horizontal) startRow else startRow - 1
        val beforeC = if (horizontal) startCol - 1 else startCol
        if (beforeR in 0 until size && beforeC in 0 until size && grid[beforeR][beforeC] != ' ') return false

        val afterR = if (horizontal) endRow else endRow + 1
        val afterC = if (horizontal) endCol + 1 else endCol
        if (afterR in 0 until size && afterC in 0 until size && grid[afterR][afterC] != ' ') return false

        var hasAtLeastOneCrossing = false

        for (i in word.indices) {
            val r = if (horizontal) startRow else startRow + i
            val c = if (horizontal) startCol + i else startCol
            val existing = grid[r][c]

            if (existing == ' ') {
                // Blank cell: perpendicular neighbors must also be blank, or this word
                // would create an accidental adjacency (an unintended fragment slot).
                val (n1r, n1c) = if (horizontal) (r - 1) to c else r to (c - 1)
                val (n2r, n2c) = if (horizontal) (r + 1) to c else r to (c + 1)
                if (n1r in 0 until size && n1c in 0 until size && grid[n1r][n1c] != ' ') return false
                if (n2r in 0 until size && n2c in 0 until size && grid[n2r][n2c] != ' ') return false
            } else {
                // Occupied cell: must match, and this direction must not already be used here.
                if (existing != word[i]) return false
                val occupiedThisDirection = if (horizontal) (r to c) in acrossAt else (r to c) in downAt
                if (occupiedThisDirection) return false
                hasAtLeastOneCrossing = true
            }
        }

        // The seed word is placed directly (no crossing required); every subsequent
        // word MUST cross something, or it'd float disconnected from the puzzle.
        return hasAtLeastOneCrossing || (acrossAt.isEmpty() && downAt.isEmpty())
    }

    private fun placeWord(
        grid: Array<CharArray>,
        acrossAt: MutableSet<Pair<Int, Int>>,
        downAt: MutableSet<Pair<Int, Int>>,
        word: String,
        startRow: Int,
        startCol: Int,
        horizontal: Boolean
    ) {
        for (i in word.indices) {
            val r = if (horizontal) startRow else startRow + i
            val c = if (horizontal) startCol + i else startCol
            grid[r][c] = word[i]
            if (horizontal) acrossAt += r to c else downAt += r to c
        }
    }

    /** Cells that currently hold a letter but are missing one of the two directions. */
    private fun frontierCells(
        grid: Array<CharArray>,
        acrossAt: Set<Pair<Int, Int>>,
        downAt: Set<Pair<Int, Int>>,
        size: Int
    ): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) for (c in 0 until size) {
            if (grid[r][c] == ' ') continue
            val key = r to c
            if (key !in acrossAt || key !in downAt) result += key
        }
        return result
    }

    private fun candidateWordsForLetter(
        pool: Map<Int, List<String>>,
        letter: Char,
        usedWords: Set<String>,
        limit: Int
    ): List<String> {
        // Sample across a few lengths so growth doesn't always favor short/long words.
        val lengths = pool.keys.shuffled()
        val out = mutableListOf<String>()
        for (len in lengths) {
            if (out.size >= limit) break
            val words = pool[len] ?: continue
            for (w in words) {
                if (out.size >= limit) break
                if (w in usedWords) continue
                if (letter in w) out += w
            }
        }
        return out.shuffled().take(limit)
    }

    /** Cheap correction so the fill-ratio check isn't wildly over-counting crossing cells. */
    private fun overlapCorrection(placements: List<Placement>): Int {
        val covered = HashSet<Pair<Int, Int>>()
        var overlaps = 0
        for (p in placements) {
            for (i in p.word.indices) {
                val r = if (p.horizontal) p.row else p.row + i
                val c = if (p.horizontal) p.col + i else p.col
                if (!covered.add(r to c)) overlaps++
            }
        }
        return overlaps
    }

    // ── Final assembly ───────────────────────────────────────────────────────

    private fun buildGeneratedGrid(size: Int, placements: List<Placement>): GeneratedGrid {
        val covered = HashSet<Pair<Int, Int>>()
        val slots = mutableListOf<GridSlot>()
        val fill = mutableMapOf<Int, String>()

        placements.forEachIndexed { id, p ->
            slots += GridSlot(id, p.row, p.col, p.word.length, p.horizontal)
            fill[id] = p.word
            for (i in p.word.indices) {
                val r = if (p.horizontal) p.row else p.row + i
                val c = if (p.horizontal) p.col + i else p.col
                covered += r to c
            }
        }

        val blocked = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until size) for (c in 0 until size) {
            if ((r to c) !in covered) blocked += r to c
        }

        return GeneratedGrid(size, blocked, slots, fill)
    }
}