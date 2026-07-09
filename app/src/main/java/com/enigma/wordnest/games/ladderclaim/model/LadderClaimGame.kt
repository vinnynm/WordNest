package com.enigma.wordnest.games.ladderclaim.model

import com.enigma.wordnest.games.lexicon.model.BoardConfig

class LadderClaimGame {
    val board = Array(15) { arrayOfNulls<LadderTile>(15) }
    val words = mutableListOf<LadderWord>()
    val players = mutableListOf<LadderPlayer>()
    var currentPlayer = 0
    val placedThisTurn = mutableListOf<PlacedLadderTile>()
    var bag = mutableListOf<Char>()
    var turnCount = 0
    var variant: LadderClaimVariant = LadderClaimVariant.CLASSIC
    val maxTurns: Int get() = variant.maxTurns
    private var consecutivePasses = 0
    var isGameOver = false

    private var dictionary: Set<String> = emptySet()
    fun updateDictionary(dict: Set<String>) { dictionary = dict }

    fun startGame(
        playerNames: List<String>,
        seedWord: String,
        variant: LadderClaimVariant = LadderClaimVariant.CLASSIC,
        aiPlayerIndex: Int? = null
    ) {
        this.variant = variant
        bag = buildBag()
        for (r in 0..14) for (c in 0..14) board[r][c] = null
        words.clear(); placedThisTurn.clear(); players.clear()

        val startCol = 7 - seedWord.length / 2
        seedWord.uppercase().forEachIndexed { i, ch ->
            board[7][startCol + i] = LadderTile(7, startCol + i, ch, ownerId = null)
        }
        words += LadderWord(seedWord.uppercase(), 7, startCol, isHorizontal = true, ownerId = null)

        playerNames.forEachIndexed { idx, name ->
            val rack = mutableListOf<Char>()
            repeat(7) { if (bag.isNotEmpty()) rack.add(bag.removeAt(bag.size - 1)) }
            players.add(LadderPlayer(name, rack, isAi = idx == aiPlayerIndex))
        }
        currentPlayer = 0; turnCount = 0; consecutivePasses = 0; isGameOver = false
    }

    private fun buildBag(): MutableList<Char> {
        val b = mutableListOf<Char>()
        BoardConfig.letterDistribution.forEach { (l, n) -> if (l != '?') repeat(n) { b.add(l) } }
        return b.shuffled().toMutableList()
    }

    fun placeTile(row: Int, col: Int, letter: Char): Boolean {
        if (board[row][col] != null || placedThisTurn.any { it.row == row && it.col == col }) return false
        val player = players[currentPlayer]
        val rack = player.rack.toMutableList()
        val idx = rack.indexOf(letter)
        if (idx == -1) return false
        rack.removeAt(idx)
        players[currentPlayer] = player.copy(rack = rack)
        placedThisTurn.add(PlacedLadderTile(row, col, letter.uppercaseChar()))
        return true
    }

    fun recallTile(row: Int, col: Int) {
        val t = placedThisTurn.find { it.row == row && it.col == col } ?: return
        placedThisTurn.remove(t)
        val player = players[currentPlayer]
        players[currentPlayer] = player.copy(rack = player.rack + t.letter)
    }

    fun recallAll() {
        placedThisTurn.forEach { t ->
            val player = players[currentPlayer]
            players[currentPlayer] = player.copy(rack = player.rack + t.letter)
        }
        placedThisTurn.clear()
    }

    fun wordById(id: String): LadderWord? = words.find { it.id == id }

    /** All words (settled, not this-turn placements) whose span covers this cell — for tap-to-disambiguate. */
    fun wordsThroughCell(row: Int, col: Int): List<LadderWord> = words.filter { w ->
        LadderClaimEngine.cellInWord(w, row, col)
    }

    fun playWord(targetWordId: String?): LadderMoveResult {
        if (placedThisTurn.isEmpty()) return LadderMoveResult.Error("Place some tiles first!")
        if (!LadderClaimEngine.connectsToBoard(board, placedThisTurn)) {
            return LadderMoveResult.Error("Word must connect to existing tiles")
        }

        val rows = placedThisTurn.map { it.row }.distinct()
        val cols = placedThisTurn.map { it.col }.distinct()
        if (rows.size > 1 && cols.size > 1) return LadderMoveResult.Error("Tiles must be in a single row or column")
        val isHorizontal = rows.size == 1
        if (!LadderClaimEngine.isContiguous(board, placedThisTurn, isHorizontal)) {
            return LadderMoveResult.Error("Tiles must form a contiguous word")
        }

        val formed = LadderClaimEngine.collectFormedWords(board, placedThisTurn, isHorizontal)
        val fixedDim = if (isHorizontal) placedThisTurn.first().row else placedThisTurn.first().col
        val mainWord = formed.firstOrNull {
            it.isHorizontal == isHorizontal && (if (isHorizontal) it.startRow == fixedDim else it.startCol == fixedDim)
        } ?: formed.firstOrNull() ?: return LadderMoveResult.Error("No word formed")

        val invalid = formed.filter { it.word.length >= 2 && !dictionary.contains(it.word.uppercase()) }
        if (invalid.isNotEmpty()) return LadderMoveResult.Error("\"${invalid.first().word}\" is not in the dictionary")

        val target = targetWordId?.let { wordById(it) }
        val ownerId = currentPlayer

        val result = when (variant.claimMode) {
            ClaimMode.OWN_TILES -> resolveOwnTilesMode(target, mainWord, formed, isHorizontal, ownerId)
            ClaimMode.TARGET_TILES -> resolveTargetTilesMode(target, mainWord, formed, ownerId)
        }

        drawNewTiles(placedThisTurn.size)
        val playedWord = mainWord.word
        placedThisTurn.clear()
        consecutivePasses = 0
        turnCount++
        advanceTurnOrEnd()
        return LadderMoveResult.Success(result, playedWord)
    }

    // ── Claim mode: original — color your own newly-placed tiles ───────────

    private fun resolveOwnTilesMode(
        target: LadderWord?,
        mainWord: LadderWord,
        formed: List<LadderWord>,
        isHorizontal: Boolean,
        ownerId: Int
    ): LadderPlayResult {
        val (outcome, matchedIndicesInNew) = if (target != null) {
            LadderClaimEngine.classifyOwnTiles(target.word, mainWord.word, variant.fullCreditBar)
        } else ColorOutcome.NEUTRAL to emptySet()

        val coloredCells = mutableSetOf<Pair<Int, Int>>()
        if (outcome != ColorOutcome.NEUTRAL) {
            placedThisTurn.forEachIndexed { i, t ->
                if (outcome == ColorOutcome.FULL || i in matchedIndicesInNew) coloredCells += t.row to t.col
            }
        }

        placedThisTurn.forEach { t ->
            val owner = if ((t.row to t.col) in coloredCells) ownerId else null
            board[t.row][t.col] = LadderTile(t.row, t.col, t.letter, owner, turnClaimed = turnCount)
        }

        formed.forEach { w -> if (words.none { it.id == w.id }) words += w.copy(ownerId = null) }

        var targetConverted = false
        // Bug fix: eligibility is derived from the BOARD's actual per-tile ownership,
        // not the (unreliable, scalar) LadderWord.ownerId field — see
        // LadderClaimEngine.isWordFullyNeutral's kdoc for why the old check
        // (`target.ownerId == null`) allowed re-stealing already-claimed words.
        if (variant.allowNeutralTheft && target != null && outcome != ColorOutcome.NEUTRAL &&
            LadderClaimEngine.isWordFullyNeutral(board, target)
        ) {
            var r = target.startRow; var c = target.startCol
            for (i in target.word.indices) {
                board[r][c]?.let { board[r][c] = it.copy(ownerId = ownerId) }
                r += if (target.isHorizontal) 0 else 1
                c += if (target.isHorizontal) 1 else 0
            }
            val idx = words.indexOfFirst { it.id == target.id }
            if (idx >= 0) words[idx] = words[idx].copy(ownerId = ownerId)
            targetConverted = true
        }

        return LadderPlayResult(target, outcome, matchedIndicesInNew, targetConverted)
    }

    // ── Claim mode: rework — claims land on the TARGET word's cells ────────

    private fun resolveTargetTilesMode(
        target: LadderWord?,
        mainWord: LadderWord,
        formed: List<LadderWord>,
        ownerId: Int
    ): LadderPlayResult {
        // Newly placed tiles are ALWAYS neutral in this mode — ownership only ever
        // transfers onto an existing TARGET word's cells, never onto tiles just placed.
        placedThisTurn.forEach { t ->
            board[t.row][t.col] = LadderTile(t.row, t.col, t.letter, ownerId = null, turnClaimed = turnCount)
        }
        formed.forEach { w -> if (words.none { it.id == w.id }) words += w }

        val (outcome, matchedIndicesInTarget) = if (target != null) {
            LadderClaimEngine.classify(target.word, mainWord.word, variant.fullCreditBar)
        } else ColorOutcome.NEUTRAL to emptySet()

        var claimedCellCount = 0
        if (variant.allowNeutralTheft && target != null && outcome != ColorOutcome.NEUTRAL) {
            var r = target.startRow; var c = target.startCol
            for (i in target.word.indices) {
                if (i in matchedIndicesInTarget) {
                    val existing = board[r][c]
                    when (existing?.ownerId) {
                        null -> {
                            existing?.let {
                                board[r][c] = it.copy(ownerId = ownerId)
                                claimedCellCount++
                            }
                        }
                        ownerId -> { /* already the mover's — no-op */ }
                        else -> {
                            // Opponent-owned cell. Reclaimable ONLY if a word formed this
                            // turn crosses through this exact cell perpendicular to the
                            // target, and that crossing word is not itself solely owned
                            // by the same opponent (an ambiguous/mixed crossing word does
                            // not block the reclaim — see soleOwnerExcluding's kdoc).
                            val crossDir = !target.isHorizontal
                            val crossWord = formed.firstOrNull {
                                it.isHorizontal == crossDir && LadderClaimEngine.cellInWord(it, r, c)
                            }
                            val opponentId = existing.ownerId
                            val crossOwner = crossWord?.let {
                                LadderClaimEngine.soleOwnerExcluding(board, it, excludeCell = r to c)
                            }
                            if (crossWord != null && crossOwner != opponentId) {
                                board[r][c] = existing.copy(ownerId = ownerId)
                                claimedCellCount++
                            }
                        }
                    }
                }
                r += if (target.isHorizontal) 0 else 1
                c += if (target.isHorizontal) 1 else 0
            }
        }

        val targetConverted = target != null && run {
            var r2 = target.startRow; var c2 = target.startCol
            var allMover = true
            for (i in target.word.indices) {
                if (board[r2][c2]?.ownerId != ownerId) allMover = false
                r2 += if (target.isHorizontal) 0 else 1
                c2 += if (target.isHorizontal) 1 else 0
            }
            allMover
        }

        return LadderPlayResult(target, outcome, matchedIndicesInTarget, targetConverted, claimedCellCount)
    }

    // ── Tile economy ────────────────────────────────────────────────────────

    private fun drawNewTiles(count: Int) {
        val player = players[currentPlayer]
        val rack = player.rack.toMutableList()
        repeat(minOf(count, bag.size)) { if (bag.isNotEmpty()) rack.add(bag.removeAt(bag.size - 1)) }
        players[currentPlayer] = player.copy(rack = rack)
    }

    fun skipTurn() {
        recallAll()
        consecutivePasses++
        turnCount++
        if (consecutivePasses >= players.size * 2) isGameOver = true else advanceTurnOrEnd()
    }

    /** Exchange the current player's whole rack for fresh tiles — costs the turn,
     *  same convention as ScrabbleGame.exchangeTiles(). Requires >= 7 tiles left
     *  in the bag so the exchange can't drain it below a normal draw. */
    fun exchangeTiles(): Boolean {
        if (bag.size < 7) return false
        val player = players[currentPlayer]
        recallAll()
        val old = player.rack.toList()
        val newRack = mutableListOf<Char>()
        repeat(7) { if (bag.isNotEmpty()) newRack.add(bag.removeAt(bag.size - 1)) }
        players[currentPlayer] = player.copy(rack = newRack)
        old.forEach { bag.add(it) }
        bag.shuffle()
        turnCount++
        advanceTurnOrEnd()
        return true
    }

    private fun advanceTurnOrEnd() {
        val ranOutOfTurns = turnCount >= maxTurns * players.size
        val bagAndRacksEmpty = bag.isEmpty() && players.any { it.rack.isEmpty() }
        if (ranOutOfTurns || bagAndRacksEmpty) { isGameOver = true; return }
        currentPlayer = (currentPlayer + 1) % players.size
    }

    /** Tiles controlled per player, for the territory meter. */
    fun tallyTerritory(): Map<Int, Int> {
        val tally = mutableMapOf<Int, Int>()
        for (row in board) for (tile in row) {
            val owner = tile?.ownerId ?: continue
            tally[owner] = (tally[owner] ?: 0) + 1
        }
        return tally
    }

    /** Size of the largest 4-directionally-connected group of tiles owned by this player. */
    fun longestOwnedChain(ownerId: Int): Int {
        val visited = Array(15) { BooleanArray(15) }
        var best = 0
        for (r in 0..14) for (c in 0..14) {
            if (visited[r][c]) continue
            val tile = board[r][c]
            if (tile == null || tile.ownerId != ownerId) continue

            var size = 0
            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.addLast(r to c)
            visited[r][c] = true
            while (stack.isNotEmpty()) {
                val (cr, cc) = stack.removeLast()
                size++
                for ((nr, nc) in listOf(cr - 1 to cc, cr + 1 to cc, cr to cc - 1, cr to cc + 1)) {
                    if (nr !in 0..14 || nc !in 0..14 || visited[nr][nc]) continue
                    val nt = board[nr][nc]
                    if (nt == null || nt.ownerId != ownerId) continue
                    visited[nr][nc] = true
                    stack.addLast(nr to nc)
                }
            }
            best = maxOf(best, size)
        }
        return best
    }

    /**
     * Winner per §1.8: most tiles controlled; ties broken by longest owned chain,
     * then by fewest tiles remaining in rack. Returns null only if there are no players
     * (should not happen once a game has started) or a true 3-way-plus tie survives both breaks.
     */
    fun determineWinner(): Int? {
        if (players.isEmpty()) return null
        val tally = tallyTerritory()
        val maxTiles = players.indices.maxOf { tally[it] ?: 0 }
        val byTiles = players.indices.filter { (tally[it] ?: 0) == maxTiles }
        if (byTiles.size == 1) return byTiles.first()

        val chainOf = byTiles.associateWith { longestOwnedChain(it) }
        val maxChain = chainOf.values.max()
        val byChain = byTiles.filter { chainOf[it] == maxChain }
        if (byChain.size == 1) return byChain.first()

        val minRack = byChain.minOf { players[it].rack.size }
        val byRack = byChain.filter { players[it].rack.size == minRack }
        return if (byRack.size == 1) byRack.first() else null   // genuine tie
    }

    fun reset() {
        board.forEach { r -> for (i in r.indices) r[i] = null }
        words.clear(); players.clear(); placedThisTurn.clear()
        currentPlayer = 0; turnCount = 0; consecutivePasses = 0; isGameOver = false; bag.clear()
    }
}
