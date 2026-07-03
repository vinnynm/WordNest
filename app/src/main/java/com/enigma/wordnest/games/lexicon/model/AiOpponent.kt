package com.enigma.wordnest.games.lexicon.model

enum class AiDifficulty(
    val displayName: String,
    val vocabCoverage: Float,
    val exchangeThreshold: Int
) {
    EASY("Easy", 0.4f, 6),
    MEDIUM("Medium", 0.6f, 10),
    HARD("Hard", 0.85f, 16),
    EXPERT("Expert", 1.00f, 22)
}

data class AiMove(val tiles: List<PlacedTile>, val score: Int, val word: String, val isHorizontal: Boolean)

sealed class AiDecision {
    data class PlayWord(val move: AiMove) : AiDecision()
    object ExchangeTiles : AiDecision()
    object Skip : AiDecision()
}

class AiOpponent(fullDictionary: Set<String>, val difficulty: AiDifficulty) {

    private val coreWords: Set<String> = setOf(
        "AA","AE","AI","OE","OI","OU","AB","AD","AG","AH","AM","AN","AR","AS","AT","AW","AX","AY",
        "BA","BE","BI","BO","BY","DA","DE","DO","ED","EF","EH","EL","EM","EN","ER","ES","ET","EW","EX",
        "FA","FE","GI","GO","HA","HE","HI","HM","HO","ID","IF","IN","IS","IT","JO","KA","KI","LA","LI","LO",
        "MA","ME","MI","MM","MO","MU","MY","NA","NE","NO","NU","OD","OF","OH","OM","ON","OP","OR","OS","OW","OX","OY",
        "PA","PE","PI","PO","QI","RE","SH","SI","SO","TA","TI","TO","UH","UM","UN","UP","UR","US","UT",
        "WE","WO","XI","XU","YA","YE","YO","ZA","ZO"
    )

    val vocabulary: Set<String> = buildVocabulary(fullDictionary)

    private fun buildVocabulary(full: Set<String>): Set<String> {
        if (difficulty.vocabCoverage >= 1.0f) return full + coreWords
        val nonCore = full.filter { it !in coreWords }
        val sampleSize = (nonCore.size * difficulty.vocabCoverage).toInt()
        return nonCore.shuffled().take(sampleSize).toSet() + coreWords
    }

    /** Pre-bucket vocabulary by length so we never scan words that obviously can't fit a slot. */
    private val vocabByLength: Map<Int, List<String>> = vocabulary.groupBy { it.length }

    private val premiumSquares = BoardConfig.buildPremiumMap()

    // ── Public entry point ───────────────────────────────────────────────────

    fun decideMove(board: Array<Array<Tile?>>, rack: List<Char>, bagSize: Int): AiDecision {
        val boardEmpty = board.all { row -> row.all { it == null } }
        val candidates = findAllMoves(board, rack, boardEmpty)

        if (candidates.isEmpty()) {
            return if (bagSize >= 7) AiDecision.ExchangeTiles else AiDecision.Skip
        }

        val sorted = candidates.sortedByDescending { it.score }
        val best = sorted.first()
        if (best.score < difficulty.exchangeThreshold && bagSize >= 7) return AiDecision.ExchangeTiles

        return AiDecision.PlayWord(pickMove(sorted))
    }

    private fun pickMove(sorted: List<AiMove>): AiMove {
        val x = sorted.size
        return when (difficulty) {
            AiDifficulty.EASY -> {
                val poolSize = if (x / 4 > 6) 6 else maxOf(x / 4, 1)
                if (poolSize >= x) sorted.last() else sorted.takeLast(poolSize).random()
            }
            AiDifficulty.MEDIUM -> {
                val poolSize = minOf(10, x)
                val medianIdx = x / 2
                val from = maxOf(0, medianIdx - poolSize / 2)
                val to = minOf(x, from + poolSize)
                sorted.subList(from, to).random()
            }
            AiDifficulty.HARD -> {
                val poolSize = if (x / 4 > 6) 6 else maxOf(x / 4, 1)
                if (poolSize >= x) sorted.first() else sorted.take(poolSize).random()
            }
            AiDifficulty.EXPERT -> sorted.first()
        }
    }

    // ── Move finder ──────────────────────────────────────────────────────────

    private fun findAllMoves(board: Array<Array<Tile?>>, rack: List<Char>, boardEmpty: Boolean): List<AiMove> {
        val moves = mutableListOf<AiMove>()
        val anchors = if (boardEmpty) listOf(7 to 7) else findAnchors(board)

        // Feasibility prefilter operates once per turn, not per anchor.
        val rackFreq = mutableMapOf<Char, Int>()
        var blanks = 0
        for (c in rack) if (c == '?') blanks++ else rackFreq[c] = (rackFreq[c] ?: 0) + 1
        val boardFreq = mutableMapOf<Char, Int>()
        for (row in board) for (t in row) if (t != null) boardFreq[t.letter] = (boardFreq[t.letter] ?: 0) + 1

        // Words whose "missing" letters (not already somewhere on the board) exceed
        // what the rack + blanks could ever supply are impossible — skip immediately.
        val feasibleVocab = vocabByLength.mapValues { (_, words) ->
            words.filter { word -> isFeasible(word, rackFreq, blanks, boardFreq) }
        }

        for ((anchorRow, anchorCol) in anchors) {
            for (isHorizontal in listOf(true, false)) {
                moves += generateMovesAt(board, rack, anchorRow, anchorCol, isHorizontal, boardEmpty, feasibleVocab)
            }
        }

        return moves.distinctBy {
            "${it.word}:${it.tiles.minOf { t -> if (it.isHorizontal) t.col else t.row }}:${it.isHorizontal}"
        }
    }

    /** Necessary (not sufficient) condition: cheap to compute, prunes the vast majority of words. */
    private fun isFeasible(word: String, rackFreq: Map<Char, Int>, blanks: Int, boardFreq: Map<Char, Int>): Boolean {
        val wordFreq = mutableMapOf<Char, Int>()
        for (c in word) wordFreq[c] = (wordFreq[c] ?: 0) + 1
        var deficit = 0
        for ((c, need) in wordFreq) {
            val haveOnBoard = boardFreq[c] ?: 0
            val haveInRack = rackFreq[c] ?: 0
            val missing = (need - haveOnBoard - haveInRack).coerceAtLeast(0)
            deficit += missing
        }
        return deficit <= blanks
    }

    private fun findAnchors(board: Array<Array<Tile?>>): List<Pair<Int, Int>> {
        val anchors = mutableListOf<Pair<Int, Int>>()
        for (r in 0..14) for (c in 0..14) {
            if (board[r][c] != null) continue
            val neighbors = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
            if (neighbors.any { (nr, nc) -> nr in 0..14 && nc in 0..14 && board[nr][nc] != null }) anchors += r to c
        }
        return anchors
    }

    private fun generateMovesAt(
        board: Array<Array<Tile?>>, rack: List<Char>,
        anchorRow: Int, anchorCol: Int, isHorizontal: Boolean, boardEmpty: Boolean,
        feasibleVocab: Map<Int, List<String>>
    ): List<AiMove> {
        val results = mutableListOf<AiMove>()
        val rackLetters = rack.toMutableList()
        val hasBlank = '?' in rackLetters

        // Words longer than ~9 rarely fit a 7-tile rack + a couple overlaps; cap the scan.
        val maxLen = minOf(15, rack.size + 4)
        for (len in 2..maxLen) {
            val words = feasibleVocab[len] ?: continue
            for (word in words) {
                for (offset in word.indices) {
                    val startRow = if (isHorizontal) anchorRow else anchorRow - offset
                    val startCol = if (isHorizontal) anchorCol - offset else anchorCol
                    if (startRow < 0 || startCol < 0) continue
                    val endRow = if (isHorizontal) startRow else startRow + word.length - 1
                    val endCol = if (isHorizontal) startCol + word.length - 1 else startCol
                    if (endRow > 14 || endCol > 14) continue

                    if (boardEmpty) {
                        val coversCenter = (0 until word.length).any { i ->
                            val r = if (isHorizontal) startRow else startRow + i
                            val c = if (isHorizontal) startCol + i else startCol
                            r == 7 && c == 7
                        }
                        if (!coversCenter) continue
                    }

                    val move = tryPlaceWord(board, word, startRow, startCol, isHorizontal, rackLetters, hasBlank, boardEmpty)
                        ?: continue
                    results += move
                }
            }
        }
        return results
    }

    private fun tryPlaceWord(
        board: Array<Array<Tile?>>, word: String, startRow: Int, startCol: Int, isHorizontal: Boolean,
        availableRack: List<Char>, hasBlank: Boolean, boardEmpty: Boolean
    ): AiMove? {
        val tempRack = availableRack.toMutableList()
        val newTiles = mutableListOf<PlacedTile>()

        for (i in word.indices) {
            val r = if (isHorizontal) startRow else startRow + i
            val c = if (isHorizontal) startCol + i else startCol
            val letter = word[i].uppercaseChar()
            val existing = board[r][c]
            if (existing != null) {
                if (existing.letter.uppercaseChar() != letter) return null
            } else {
                when {
                    tempRack.contains(letter) -> tempRack.remove(letter)
                    hasBlank && tempRack.contains('?') -> {
                        tempRack.remove('?')
                        newTiles.add(PlacedTile(r, c, letter, 0, isBlank = true))
                        continue
                    }
                    else -> return null
                }
                newTiles.add(PlacedTile(r, c, letter, BoardConfig.letterValues[letter] ?: 0, isBlank = false))
            }
        }

        if (newTiles.isEmpty()) return null

        val afterR = if (isHorizontal) startRow else startRow + word.length
        val afterC = if (isHorizontal) startCol + word.length else startCol
        if (afterR in 0..14 && afterC in 0..14 && board[afterR][afterC] != null) return null
        val beforeR = if (isHorizontal) startRow else startRow - 1
        val beforeC = if (isHorizontal) startCol - 1 else startCol
        if (beforeR in 0..14 && beforeC in 0..14 && board[beforeR][beforeC] != null) return null

        if (!boardEmpty) {
            val connects = newTiles.any { t ->
                listOf(t.row - 1 to t.col, t.row + 1 to t.col, t.row to t.col - 1, t.row to t.col + 1)
                    .any { (nr, nc) -> nr in 0..14 && nc in 0..14 && board[nr][nc] != null }
            }
            val overlaps = (0 until word.length).any { i ->
                val r = if (isHorizontal) startRow else startRow + i
                val c = if (isHorizontal) startCol + i else startCol
                board[r][c] != null
            }
            if (!connects && !overlaps) return null
        }

        for (tile in newTiles) {
            val crossWord = buildCrossWord(board, tile.row, tile.col, !isHorizontal, newTiles)
            if (crossWord.length >= 2 && !vocabulary.contains(crossWord.uppercase())) return null
        }

        val score = scoreMove(board, startRow, startCol, isHorizontal, newTiles)
        return AiMove(newTiles, score, word, isHorizontal)
    }

    private fun scoreMove(
        board: Array<Array<Tile?>>, startRow: Int, startCol: Int,
        isHorizontal: Boolean, newTiles: List<PlacedTile>
    ): Int {
        var total = scoreWord(board, startRow, startCol, isHorizontal, newTiles)
        for (tile in newTiles) {
            val crossWord = buildCrossWord(board, tile.row, tile.col, !isHorizontal, newTiles)
            if (crossWord.length >= 2) total += scoreWord(board, tile.row, tile.col, !isHorizontal, newTiles)
        }
        if (newTiles.size == 7) total += 50
        return total
    }

    private fun scoreWord(
        board: Array<Array<Tile?>>, startRow: Int, startCol: Int, isHorizontal: Boolean, newTiles: List<PlacedTile>
    ): Int {
        var sr = startRow; var sc = startCol
        while (true) {
            val nr = if (isHorizontal) sr else sr - 1
            val nc = if (isHorizontal) sc - 1 else sc
            if (nr in 0..14 && nc in 0..14 && (board[nr][nc] != null || newTiles.any { it.row == nr && it.col == nc })) {
                sr = nr; sc = nc
            } else break
        }
        var letterScore = 0; var wordMult = 1; var r = sr; var c = sc
        while (r in 0..14 && c in 0..14) {
            val newTile = newTiles.find { it.row == r && it.col == c }
            val boardTile = board[r][c]
            val tile = newTile ?: boardTile?.let { PlacedTile(r, c, it.letter, it.points, it.isBlank) } ?: break
            var lv = tile.points
            if (newTile != null) {
                when (premiumSquares["$r,$c"]) {
                    "DL" -> lv *= 2; "TL" -> lv *= 3
                    "DW" -> wordMult *= 2; "TW" -> wordMult *= 3
                }
                if (r == 7 && c == 7) wordMult *= 2
            }
            letterScore += lv
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return letterScore * wordMult
    }

    private fun buildCrossWord(
        board: Array<Array<Tile?>>, row: Int, col: Int, isHorizontal: Boolean, newTiles: List<PlacedTile>
    ): String {
        fun tileAt(r: Int, c: Int): Char? {
            newTiles.find { it.row == r && it.col == c }?.let { return it.letter }
            return board[r][c]?.letter
        }
        var sr = row; var sc = col
        while (true) {
            val nr = if (isHorizontal) sr else sr - 1
            val nc = if (isHorizontal) sc - 1 else sc
            if (nr in 0..14 && nc in 0..14 && tileAt(nr, nc) != null) { sr = nr; sc = nc } else break
        }
        val sb = StringBuilder(); var r = sr; var c = sc
        while (r in 0..14 && c in 0..14) {
            val ch = tileAt(r, c) ?: break
            sb.append(ch)
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return sb.toString()
    }
}