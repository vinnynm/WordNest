package com.enigma.wordnest.games.absurdauction.model

import com.enigma.wordnest.games.lexicon.model.BoardConfig
import com.enigma.wordnest.games.lexicon.model.BoardConfig.letterValues
import com.enigma.wordnest.games.lexicon.model.PlacedTile
import com.enigma.wordnest.games.lexicon.model.PlayResult
import com.enigma.wordnest.games.lexicon.model.Player
import com.enigma.wordnest.games.lexicon.model.Tile
import com.enigma.wordnest.games.lexicon.model.WordScore
import kotlin.random.Random

/**
 * AuctionGame — a fork of ScrabbleGame (Lexicon) whose tile draws are resolved by
 * [BankerEngine] instead of a uniform-random bag pop. Board/scoring/dictionary
 * legality, premium squares, and word-collection logic are copied verbatim from
 * ScrabbleGame — see the audit's note that these three concepts (letter values,
 * premium squares, connects-to-board) are already duplicated across the codebase;
 * this is a deliberate 4th copy rather than a shared-base refactor, to keep this
 * game's build independent of a larger `BoardGameEngine` extraction.
 *
 * Only the tile-acquisition path (bag build + draws) differs from ScrabbleGame:
 * every draw goes through [BankerEngine.decide] with the current score
 * differential, tracked pity-timer streaks, and the active [BankerStance].
 */
class AuctionGame {
    val board = Array(15) { arrayOfNulls<Tile>(15) }
    val players = mutableListOf<Player>()
    var currentPlayer = 0
    val placedThisTurn = mutableListOf<PlacedTile>()
    var isGameOver = false
    var consecutiveSkips = 0

    private val premiumSquares = BoardConfig.buildPremiumMap()
    var pool = mutableListOf<Char>()
    private var dictionary = emptySet<String>()
    private var largeDictionary = emptySet<String>()

    var stance: BankerStance = BankerStance.EQUALIZING
    private var consecutiveVowelless = mutableMapOf<Int, Int>()
    private var consecutiveConsonantless = mutableMapOf<Int, Int>()

    /** Populated after every draw so the ViewModel can render the tension bar. */
    var lastDecisions: MutableList<BankerDecision> = mutableListOf()
        private set

    fun updateDictionary(newDict: Set<String>) { dictionary = newDict }
    fun updateLargeDictionary(newDict: Set<String>) { largeDictionary = newDict }

    fun getPoolSize(): Int = pool.size

    fun startGame(player1Name: String, player2Name: String, stance: BankerStance) {
        this.stance = stance
        pool = buildPool()
        for (i in 0 until 15) for (j in 0 until 15) board[i][j] = null
        placedThisTurn.clear()
        consecutiveVowelless = mutableMapOf(0 to 0, 1 to 0)
        consecutiveConsonantless = mutableMapOf(0 to 0, 1 to 0)
        lastDecisions = mutableListOf()

        players.clear()
        players.add(Player(player1Name, 0, emptyList()))
        players.add(Player(player2Name, 0, emptyList()))
        currentPlayer = 0; consecutiveSkips = 0; isGameOver = false

        // Opening racks: both players draw via the engine's isOpeningDraw path
        // (Perfect Median rack, §2.4 item 5), not the adversarial path.
        for (idx in players.indices) drawTiles(idx, 7, isOpening = true)
    }

    private fun buildPool(): MutableList<Char> {
        val newPool = mutableListOf<Char>()
        BoardConfig.letterDistribution.forEach { (letter, count) -> repeat(count) { newPool.add(letter) } }
        return newPool // no shuffle: BankerEngine controls ordering via selection, not pre-shuffling
    }

    /** Draws [count] tiles for player [playerIdx] via the Banker, appending to their rack. */
    private fun drawTiles(playerIdx: Int, count: Int, isOpening: Boolean = false) {
        val actualCount = minOf(count, pool.size)
        if (actualCount <= 0) return

        val opponentIdx = if (players.size > 1) 1 - playerIdx else playerIdx
        val ctx = BankerEngine.DrawContext(
            remainingPool = pool,
            drawCount = actualCount,
            drawingPlayerScore = players.getOrNull(playerIdx)?.score ?: 0,
            opponentScore = players.getOrNull(opponentIdx)?.score ?: 0,
            stance = stance,
            consecutiveVowelless = consecutiveVowelless[playerIdx] ?: 0,
            consecutiveConsonantless = consecutiveConsonantless[playerIdx] ?: 0,
            isOpeningDraw = isOpening,
            random = Random.Default
        )
        val outcome = BankerEngine.decide(ctx)
        pool = outcome.newPool.toMutableList()
        consecutiveVowelless[playerIdx] = outcome.newConsecutiveVowelless
        consecutiveConsonantless[playerIdx] = outcome.newConsecutiveConsonantless
        lastDecisions.add(outcome.decision)

        val player = players[playerIdx]
        players[playerIdx] = player.copy(rack = player.rack + outcome.decision.chosenTiles)
    }

    // ── Everything below mirrors ScrabbleGame exactly (board legality, scoring) ──

    fun placeTile(row: Int, col: Int, letter: Char): Boolean {
        if (board[row][col] != null || placedThisTurn.any { it.row == row && it.col == col }) return false
        val player = players[currentPlayer]
        val newRack = player.rack.toMutableList()
        val index = newRack.indexOf(letter).let { if (it == -1) newRack.indexOf('?') else it }
        if (index == -1) return false
        val isBlank = newRack[index] == '?'
        newRack.removeAt(index)
        players[currentPlayer] = player.copy(rack = newRack)
        placedThisTurn.add(PlacedTile(row, col, letter, if (isBlank) 0 else letterValues[letter] ?: 0, isBlank))
        return true
    }

    fun recallTile(row: Int, col: Int) {
        val tile = placedThisTurn.find { it.row == row && it.col == col } ?: return
        placedThisTurn.remove(tile)
        val player = players[currentPlayer]; val newRack = player.rack.toMutableList()
        newRack.add(if (tile.isBlank) '?' else tile.letter)
        players[currentPlayer] = player.copy(rack = newRack)
    }

    fun recallAllTiles() {
        placedThisTurn.forEach { tile ->
            val player = players[currentPlayer]; val newRack = player.rack.toMutableList()
            newRack.add(if (tile.isBlank) '?' else tile.letter)
            players[currentPlayer] = player.copy(rack = newRack)
        }
        placedThisTurn.clear()
    }

    fun playWord(): PlayResult {
        if (placedThisTurn.isEmpty()) return PlayResult.Error("Place some tiles first!")
        val boardEmpty = board.all { r -> r.all { it == null } }
        if (boardEmpty) {
            if (placedThisTurn.none { it.row == 7 && it.col == 7 }) return PlayResult.Error("First word must cover the center star")
            if (placedThisTurn.size < 2) return PlayResult.Error("First word must be at least 2 letters")
        } else if (!connectsToBoard()) return PlayResult.Error("Word must connect to existing tiles")

        val rows = placedThisTurn.map { it.row }.distinct(); val cols = placedThisTurn.map { it.col }.distinct()
        if (rows.size > 1 && cols.size > 1) return PlayResult.Error("Tiles must be in a single row or column")
        val isHorizontal = rows.size == 1
        if (!isContiguous(isHorizontal)) return PlayResult.Error("Tiles must form a contiguous word")

        val words = collectWords(isHorizontal)
        val invalidWords = words.filter { it.word.length >= 2 && !largeDictionary.contains(it.word.uppercase()) }
        if (invalidWords.isNotEmpty()) return PlayResult.Error("\"${invalidWords.first().word}\" is not in the dictionary")

        var totalScore = words.filter { it.word.length >= 2 }.sumOf { it.score }
        if (placedThisTurn.size == 7) totalScore += 50
        placedThisTurn.forEach { board[it.row][it.col] = Tile(it.letter, it.points, it.isBlank) }
        val player = players[currentPlayer]; players[currentPlayer] = player.copy(score = player.score + totalScore)
        val drawCount = placedThisTurn.size
        val playedWords = words.filter { it.word.length >= 2 }.map { it.word }
        placedThisTurn.clear(); consecutiveSkips = 0
        drawTiles(currentPlayer, drawCount)
        if (players[currentPlayer].rack.isEmpty() && pool.isEmpty()) endGame() else nextTurn()
        return PlayResult.Success(playedWords, totalScore)
    }

    private fun connectsToBoard(): Boolean {
        for (tile in placedThisTurn) {
            val neighbors = listOf(tile.row - 1 to tile.col, tile.row + 1 to tile.col, tile.row to tile.col - 1, tile.row to tile.col + 1)
            for ((r, c) in neighbors) if (r in 0..14 && c in 0..14 && board[r][c] != null) return true
        }
        return false
    }

    private fun isContiguous(isHorizontal: Boolean): Boolean {
        if (placedThisTurn.size == 1) return true
        val sorted = placedThisTurn.sortedBy { if (isHorizontal) it.col else it.row }
        val fixedDim = if (isHorizontal) sorted[0].row else sorted[0].col
        for (i in 0 until sorted.size - 1) {
            val cur = if (isHorizontal) sorted[i].col else sorted[i].row
            val next = if (isHorizontal) sorted[i + 1].col else sorted[i + 1].row
            for (pos in cur + 1 until next) {
                val cr = if (isHorizontal) fixedDim else pos; val cc = if (isHorizontal) pos else fixedDim
                if (board[cr][cc] == null && placedThisTurn.none { it.row == cr && it.col == cc }) return false
            }
        }
        return true
    }

    private fun getTileAt(row: Int, col: Int): Tile? {
        placedThisTurn.find { it.row == row && it.col == col }?.let { return Tile(it.letter, it.points, it.isBlank) }
        return board[row][col]
    }

    private fun buildWord(startRow: Int, startCol: Int, isHorizontal: Boolean): WordScore {
        var sr = startRow; var sc = startCol
        while (true) {
            val nr = sr - if (isHorizontal) 0 else 1; val nc = sc - if (isHorizontal) 1 else 0
            if (nr in 0..14 && nc in 0..14 && getTileAt(nr, nc) != null) { sr = nr; sc = nc } else break
        }
        var word = ""; var score = 0; var wordMultiplier = 1; var cr = sr; var cc = sc
        while (cr in 0..14 && cc in 0..14) {
            val tile = getTileAt(cr, cc) ?: break
            word += tile.letter; var letterValue = tile.points
            val isNew = placedThisTurn.any { it.row == cr && it.col == cc }
            if (isNew) {
                when (premiumSquares["$cr,$cc"]) {
                    "DL" -> letterValue *= 2; "TL" -> letterValue *= 3
                    "DW" -> wordMultiplier *= 2; "TW" -> wordMultiplier *= 3
                }
                if (cr == 7 && cc == 7) wordMultiplier *= 2
            }
            score += letterValue; cr += if (isHorizontal) 0 else 1; cc += if (isHorizontal) 1 else 0
        }
        return WordScore(word, score * wordMultiplier, sr, sc, isHorizontal)
    }

    private fun collectWords(isHorizontal: Boolean): List<WordScore> {
        val words = mutableListOf<WordScore>()
        val first = placedThisTurn.first(); val main = buildWord(first.row, first.col, isHorizontal)
        if (main.word.length >= 2) words.add(main)
        placedThisTurn.forEach { t -> val cross = buildWord(t.row, t.col, !isHorizontal); if (cross.word.length >= 2) words.add(cross) }
        return words.distinctBy { "${it.word}:${it.startRow},${it.startCol}" }
    }

    fun skipTurn() { recallAllTiles(); consecutiveSkips++; if (consecutiveSkips >= 4) endGame() else nextTurn() }

    fun exchangeTiles(): Boolean {
        if (pool.size < 7) return false
        recallAllTiles()
        val p = players[currentPlayer]; val old = p.rack.toList()
        players[currentPlayer] = p.copy(rack = emptyList())
        old.forEach { pool.add(it) }
        drawTiles(currentPlayer, 7)
        consecutiveSkips++; nextTurn()
        return true
    }

    fun shuffleRack() { val p = players[currentPlayer]; players[currentPlayer] = p.copy(rack = p.rack.shuffled()) }
    private fun nextTurn() { currentPlayer = 1 - currentPlayer }
    private fun endGame() {
        players.forEachIndexed { i, p -> val penalty = p.rack.sumOf { BoardConfig.letterValues[it] ?: 0 }; players[i] = p.copy(score = maxOf(0, p.score - penalty)) }
        isGameOver = true
    }

    fun reset() {
        board.forEach { row -> for (i in row.indices) row[i] = null }
        placedThisTurn.clear(); players.clear(); currentPlayer = 0; consecutiveSkips = 0; isGameOver = false; pool.clear()
        lastDecisions = mutableListOf()
    }
}