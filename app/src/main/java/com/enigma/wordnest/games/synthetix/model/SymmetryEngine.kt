package com.enigma.wordnest.games.synthetix.model

// ─────────────────────────────────────────────────────────────────────────────
//  Symmetry mode — describes how painted cells are mirrored
// ─────────────────────────────────────────────────────────────────────────────

enum class SymmetryMode(val label: String, val emoji: String, val description: String) {
    NONE            ("None",         "✏️",  "Free paint — no mirroring"),
    ROTATE_180      ("180° Rotate",  "↺",   "Most common in word games — rotationally symmetric"),
    REFLECT_H       ("Horizontal",   "↔",  "Mirror left ↔ right"),
    REFLECT_V       ("Vertical",     "↕",  "Mirror top ↕ bottom"),
    REFLECT_BOTH    ("4-fold",       "✛",  "Mirror both axes simultaneously"),
    ROTATE_90       ("90° Rotate",   "⟳",  "Full 4-way rotational symmetry")
}

// ─────────────────────────────────────────────────────────────────────────────
//  Symmetry engine
//  Given a grid size and a symmetry mode, returns all (row, col) positions
//  that should receive the same square type whenever the user paints a cell.
// ─────────────────────────────────────────────────────────────────────────────

object SymmetryEngine {

    /**
     * Returns the set of (row, col) positions that should be painted whenever
     * the user taps [row],[col] on a [size]×[size] grid in [mode].
     * Always includes the original position.
     */
    fun mirrors(row: Int, col: Int, size: Int, mode: SymmetryMode): Set<Pair<Int, Int>> {
        val max = size - 1
        return when (mode) {
            SymmetryMode.NONE -> setOf(row to col)

            SymmetryMode.ROTATE_180 -> setOf(
                row to col,
                max - row to max - col
            )

            SymmetryMode.REFLECT_H -> setOf(
                row to col,
                row to max - col
            )

            SymmetryMode.REFLECT_V -> setOf(
                row to col,
                max - row to col
            )

            SymmetryMode.REFLECT_BOTH -> setOf(
                row to col,
                row to max - col,
                max - row to col,
                max - row to max - col
            )

            SymmetryMode.ROTATE_90 -> setOf(
                row       to col,
                col       to max - row,
                max - row to max - col,
                max - col to row
            )
        }.filter { (r, c) -> r in 0 until size && c in 0 until size }.toSet()
    }

    /**
     * Apply a square type code to all mirror positions in [grid].
     * Protects the anchor cell — anchor is never overwritten by paint.
     * Returns a new grid (immutable copy).
     */
    fun applyWithSymmetry(
        grid: List<List<String?>>,
        row: Int,
        col: Int,
        code: String?,
        size: Int,
        mode: SymmetryMode,
        anchorRow: Int,
        anchorCol: Int,
        selectedSquareType: SquareType
    ): List<List<String?>> {
        val positions = mirrors(row, col, size, mode)
        val mutable   = grid.map { it.toMutableList() }.toMutableList()

        for ((r, c) in positions) {
            // Don't overwrite anchor with non-anchor paint (unless user selected ANCHOR)
            if (r == anchorRow && c == anchorCol && selectedSquareType != SquareType.ANCHOR) continue
            mutable[r][c] = code
        }
        return mutable.map { it.toList() }
    }

    /**
     * Re-apply a symmetry mode to an existing grid — useful when the user
     * switches modes mid-edit. Each cell that is non-empty gets its mirrors
     * populated. Anchor is preserved.
     */
    fun reSymmetrize(
        grid: List<List<String?>>,
        size: Int,
        mode: SymmetryMode,
        anchorRow: Int,
        anchorCol: Int
    ): List<List<String?>> {
        if (mode == SymmetryMode.NONE) return grid

        val result = grid.map { it.toMutableList() }.toMutableList()

        for (r in 0 until size) {
            for (c in 0 until size) {
                val code = grid[r][c] ?: continue
                if (code == "anchor") continue     // anchor stays where it is
                val positions = mirrors(r, c, size, mode)
                for ((mr, mc) in positions) {
                    if (result[mr][mc] == "anchor") continue
                    result[mr][mc] = code
                }
            }
        }
        return result.map { it.toList() }
    }

    /**
     * Returns true when the given grid satisfies the selected symmetry.
     * Used to show a visual indicator in the builder UI.
     */
    fun isSymmetric(grid: List<List<String?>>, size: Int, mode: SymmetryMode): Boolean {
        if (mode == SymmetryMode.NONE) return true
        for (r in 0 until size) {
            for (c in 0 until size) {
                val code = grid[r][c]
                val positions = mirrors(r, c, size, mode)
                if (positions.any { (mr, mc) -> grid[mr][mc] != code }) return false
            }
        }
        return true
    }
}
