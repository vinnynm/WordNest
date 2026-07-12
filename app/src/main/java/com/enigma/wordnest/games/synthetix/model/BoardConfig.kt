package com.enigma.wordnest.games.synthetix.model

import kotlin.collections.get

// ─────────────────────────────────────────────────────────────────────────────
//  Square type — every premium, obstacle, negative, and special cell
// ─────────────────────────────────────────────────────────────────────────────

enum class SquareType(
    val code: String,
    val displayName: String,
    val wordMultiplier: Int  = 1,
    val letterMultiplier: Int = 1,
    val isNegative: Boolean  = false,   // subtracts the word score
    val isObstacle: Boolean  = false,   // tile cannot be placed here
    val isAnchor: Boolean    = false    // starting square (like Scrabble center)
) {
    EMPTY     ("",    "Empty",          1, 1),
    ANCHOR    ("anchor","★",            2, 1, isAnchor = true),   // first word must cover this
    DL        ("dl",  "Double Letter",  1, 2),
    TL        ("tl",  "Triple Letter",  1, 3),
    DW        ("dw",  "Double Word",    2, 1),
    TW        ("tw",  "Triple Word",    3, 1),
    QL        ("ql",  "Quad Letter",    1, 4),
    QW        ("qw",  "Quad Word",      4, 1),
    VW        ("vw",  "5× Word",        5, 1),
    NEG       ("neg", "Negative",       1, 1, isNegative  = true),
    OBS       ("obs", "Obstacle",       1, 1, isObstacle  = true);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromCode(code: String?): SquareType = byCode[code] ?: EMPTY

        /** Cycle order used in the board builder palette */
        val paletteOrder = listOf(EMPTY, ANCHOR, DL, TL, DW, TW, QL, QW, VW, NEG, OBS)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Board configuration — serialisable to / from JSON
// ─────────────────────────────────────────────────────────────────────────────

data class BoardConfig(
    val name: String          = "Custom Board",
    val size: Int             = 15,
    val rack: Int             = 7,
    val bag: Int              = 100,
    val bingo: Int            = 50,
    val anchor: String        = "",          // "row,col"  e.g. "7,7"
    val timestamp: String     = "",
    val grid: List<List<String?>> = emptyList()
) {
    /** Parse anchor string to a Pair(row, col), defaulting to centre */
    fun anchorCell(): Pair<Int, Int> {
        val parts = anchor.split(",")
        return if (parts.size == 2) {
            val r = parts[0].trim().toIntOrNull() ?: (size / 2)
            val c = parts[1].trim().toIntOrNull() ?: (size / 2)
            r to c
        } else (size / 2) to (size / 2)
    }

    fun squareAt(row: Int, col: Int): SquareType {
        val code = grid.getOrNull(row)?.getOrNull(col)
        if (code == null) {
            val (ar, ac) = anchorCell()
            return if (row == ar && col == ac) SquareType.ANCHOR else SquareType.EMPTY
        }
        return SquareType.fromCode(code)
    }

    companion object {
        /** Build a blank board of a given size with anchor in the centre */
        fun blank(size: Int = 15, name: String = "New Board"): BoardConfig {
            val centre = size / 2
            val grid = List(size) { r ->
                List(size) { c ->
                    if (r == centre && c == centre) "anchor" else null
                }
            }
            return BoardConfig(
                name   = name,
                size   = size,
                rack   = 7,
                bag    = 100,
                bingo  = 50,
                anchor = "$centre,$centre",
                grid   = grid
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tile set configuration
// ─────────────────────────────────────────────────────────────────────────────

data class LetterConfig(
    val letter: Char,
    val count: Int,
    val points: Int
)

data class TileSetConfig(
    val name: String = "Standard",
    val rackSize: Int = 7,
    val bagSize: Int = 100,
    val bingoBonus: Int = 50,
    val letters: List<LetterConfig> = defaultLetters()
) {
    fun toBag(): MutableList<Char> {
        val bag = mutableListOf<Char>()
        letters.forEach { lc -> repeat(lc.count) { bag.add(lc.letter) } }
        return bag.shuffled().toMutableList()
    }

    fun pointsFor(letter: Char): Int =
        letters.find { it.letter.uppercaseChar() == letter.uppercaseChar() }?.points ?: 0

    companion object {
        fun defaultLetters(): List<LetterConfig> = listOf(
            LetterConfig('A', 9, 1),  LetterConfig('B', 2, 3),
            LetterConfig('C', 2, 3),  LetterConfig('D', 4, 2),
            LetterConfig('E', 12, 1), LetterConfig('F', 2, 4),
            LetterConfig('G', 3, 2),  LetterConfig('H', 2, 4),
            LetterConfig('I', 9, 1),  LetterConfig('J', 1, 8),
            LetterConfig('K', 1, 5),  LetterConfig('L', 4, 1),
            LetterConfig('M', 2, 3),  LetterConfig('N', 6, 1),
            LetterConfig('O', 8, 1),  LetterConfig('P', 2, 3),
            LetterConfig('Q', 1, 10), LetterConfig('R', 6, 1),
            LetterConfig('S', 4, 1),  LetterConfig('T', 6, 1),
            LetterConfig('U', 4, 1),  LetterConfig('V', 2, 4),
            LetterConfig('W', 2, 4),  LetterConfig('X', 1, 8),
            LetterConfig('Y', 2, 4),  LetterConfig('Z', 1, 10),
            LetterConfig('?', 2, 0)
        )
    }
}
