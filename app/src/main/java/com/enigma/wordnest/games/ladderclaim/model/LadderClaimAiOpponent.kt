package com.enigma.wordnest.games.ladderclaim.model

/**
 * LadderClaimAiOpponent — legal-move generation mirrors Lexicon's AiOpponent
 * (findAllMoves / isFeasible / tryPlaceWord), stripped of all scoring, premium
 * squares, and blank-tile handling, since Ladder Claim has none of those.
 * Ladder Claim's legality is a strict subset of Lexicon's (dictionary word +
 * connects-to-board + single-row/col contiguous), so the same feasibility
 * prefilter — cheaply ruling out words the rack + board letters could never
 * spell before doing the expensive placement scan — applies unchanged.
 *
 * Unlike Lexicon, there's no point score to weigh moves against, so the AI's
 * target-selection heuristic simply greedily maximizes claimed/colored cells
 * this turn (evaluating every legal move against every eligible target word
 * on the board and picking the single best combination). This is intentionally
 * simple: Ladder Claim has no "best achievable score" oracle to lean on, and a
 * greedy per-turn maximizer is enough to make the AI feel purposeful without
 * needing lookahead.
 */
class LadderClaimAiOpponent(private val dictionary: Set<String>) {

    data class AiMove(
        val tiles: List<PlacedLadderTile>,
        val word: String,
        val isHorizontal: Boolean
    )

    data class AiDecision(
        val move: AiMove,
        /** Best target word id to select for this move, or null for a deliberate neutral play. */
        val targetWordId: String?,
        val projectedGain: Int
    )

    /** Pre-bucket dictionary by length once; reused across every decideMove() call. */
    private val vocabByLength: Map<Int, List<String>> by lazy {
        dictionary.groupBy { it.length }
    }

    /**
     * Chooses the AI's play for this turn, or null if no legal move exists
     * (caller should fall back to exchange/skip, same as Lexicon's AiOpponent).
     */
    fun decideMove(
        board: Array<Array<LadderTile?>>,
        rack: List<Char>,
        words: List<LadderWord>,
        claimMode: ClaimMode,
        fullCreditBar: Int
    ): AiDecision? {
        val boardEmpty = board.all { row -> row.all { it == null } }
        val candidates = findAllMoves(board, rack, boardEmpty)
        if (candidates.isEmpty()) return null

        var best: AiDecision? = null
        for (move in candidates) {
            // Try every existing word as a candidate target, plus "no target" (neutral).
            var bestForMove: AiDecision = AiDecision(move, null, 0)
            for (target in words) {
                val gain = estimateGain(board, move, target, claimMode, fullCreditBar)
                if (gain > bestForMove.projectedGain) {
                    bestForMove = AiDecision(move, target.id, gain)
                }
            }
            if (best == null || bestForMove.projectedGain > best.projectedGain) {
                best = bestForMove
            }
        }
        return best
    }

    private fun estimateGain(
        board: Array<Array<LadderTile?>>,
        move: AiMove,
        target: LadderWord,
        claimMode: ClaimMode,
        fullCreditBar: Int
    ): Int {
        return when (claimMode) {
            ClaimMode.OWN_TILES -> {
                val (outcome, matchedIndicesInNew) =
                    LadderClaimEngine.classifyOwnTiles(target.word, move.word, fullCreditBar)
                when (outcome) {
                    ColorOutcome.FULL -> move.word.length +
                        (if (LadderClaimEngine.isWordFullyNeutral(board, target)) target.word.length else 0)
                    ColorOutcome.PARTIAL -> matchedIndicesInNew.size
                    ColorOutcome.NEUTRAL -> 0
                }
            }
            ClaimMode.TARGET_TILES -> {
                val (outcome, matchedIndicesInTarget) =
                    LadderClaimEngine.classify(target.word, move.word, fullCreditBar)
                if (outcome == ColorOutcome.NEUTRAL) return 0
                // Rough estimate: count target cells that are neutral or opponent-owned
                // (a real reclaim also needs a qualifying crossing word, but this greedy
                // heuristic doesn't simulate the full placement — it's a reasonable proxy
                // since most AI plays will be simple non-crossing extensions anyway).
                var r = target.startRow; var c = target.startCol
                var claimable = 0
                for (i in target.word.indices) {
                    if (i in matchedIndicesInTarget && board[r][c] != null) claimable++
                    r += if (target.isHorizontal) 0 else 1
                    c += if (target.isHorizontal) 1 else 0
                }
                claimable
            }
            ClaimMode.FAIR_CLAIM -> {
                val (outcome, _) = LadderClaimEngine.classifyOwnTiles(target.word, move.word, fullCreditBar)
                if (outcome == ColorOutcome.NEUTRAL) return 0
                val matchedIndicesInTarget = LadderClaimEngine.computeMatch(target.word, move.word).matchedIndicesInTarget
                var r = target.startRow; var c = target.startCol
                var neutralClaimable = 0
                for (i in target.word.indices) {
                    if (i in matchedIndicesInTarget && board[r][c]?.ownerId == null) neutralClaimable++
                    r += if (target.isHorizontal) 0 else 1
                    c += if (target.isHorizontal) 1 else 0
                }
                move.word.length + neutralClaimable
            }
        }
    }

    // ── Move finder (adapted from AiOpponent.findAllMoves / generateMovesAt / tryPlaceWord) ──

    private fun findAllMoves(
        board: Array<Array<LadderTile?>>, rack: List<Char>, boardEmpty: Boolean
    ): List<AiMove> {
        val moves = mutableListOf<AiMove>()
        val anchors = if (boardEmpty) listOf(7 to 7) else findAnchors(board)

        val rackFreq = mutableMapOf<Char, Int>()
        for (c in rack) rackFreq[c] = (rackFreq[c] ?: 0) + 1
        val boardFreq = mutableMapOf<Char, Int>()
        for (row in board) for (t in row) if (t != null) boardFreq[t.letter] = (boardFreq[t.letter] ?: 0) + 1

        val feasibleVocab = vocabByLength.mapValues { (_, wordsOfLen) ->
            wordsOfLen.filter { word -> isFeasible(word, rackFreq, boardFreq) }
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

    private fun isFeasible(word: String, rackFreq: Map<Char, Int>, boardFreq: Map<Char, Int>): Boolean {
        val wordFreq = mutableMapOf<Char, Int>()
        for (c in word) wordFreq[c.uppercaseChar()] = (wordFreq[c.uppercaseChar()] ?: 0) + 1
        for ((c, need) in wordFreq) {
            val haveOnBoard = boardFreq[c] ?: 0
            val haveInRack = rackFreq[c] ?: 0
            if (need - haveOnBoard - haveInRack > 0) return false
        }
        return true
    }

    private fun findAnchors(board: Array<Array<LadderTile?>>): List<Pair<Int, Int>> {
        val anchors = mutableListOf<Pair<Int, Int>>()
        for (r in 0..14) for (c in 0..14) {
            if (board[r][c] != null) continue
            val neighbors = listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
            if (neighbors.any { (nr, nc) -> nr in 0..14 && nc in 0..14 && board[nr][nc] != null }) anchors += r to c
        }
        return anchors
    }

    private fun generateMovesAt(
        board: Array<Array<LadderTile?>>, rack: List<Char>,
        anchorRow: Int, anchorCol: Int, isHorizontal: Boolean, boardEmpty: Boolean,
        feasibleVocab: Map<Int, List<String>>
    ): List<AiMove> {
        val results = mutableListOf<AiMove>()
        val maxLen = minOf(15, rack.size + 4)
        for (len in 2..maxLen) {
            val wordsOfLen = feasibleVocab[len] ?: continue
            for (word in wordsOfLen) {
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

                    val move = tryPlaceWord(board, word, startRow, startCol, isHorizontal, rack, boardEmpty) ?: continue
                    results += move
                }
            }
        }
        return results
    }

    private fun tryPlaceWord(
        board: Array<Array<LadderTile?>>, word: String, startRow: Int, startCol: Int, isHorizontal: Boolean,
        availableRack: List<Char>, boardEmpty: Boolean
    ): AiMove? {
        val tempRack = availableRack.toMutableList()
        val newTiles = mutableListOf<PlacedLadderTile>()

        for (i in word.indices) {
            val r = if (isHorizontal) startRow else startRow + i
            val c = if (isHorizontal) startCol + i else startCol
            val letter = word[i].uppercaseChar()
            val existing = board[r][c]
            if (existing != null) {
                if (existing.letter.uppercaseChar() != letter) return null
            } else {
                if (!tempRack.remove(letter)) return null
                newTiles.add(PlacedLadderTile(r, c, letter))
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
            if (crossWord.length >= 2 && !dictionary.contains(crossWord.uppercase())) return null
        }

        return AiMove(newTiles, word.uppercase(), isHorizontal)
    }

    private fun buildCrossWord(
        board: Array<Array<LadderTile?>>, row: Int, col: Int, isHorizontal: Boolean, newTiles: List<PlacedLadderTile>
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
