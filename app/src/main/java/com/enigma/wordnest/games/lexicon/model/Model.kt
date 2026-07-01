package com.enigma.wordnest.games.lexicon.model

data class Tile(val letter: Char, val points: Int, val isBlank: Boolean = false)

data class PlacedTile(val row: Int, val col: Int, val letter: Char, val points: Int, val isBlank: Boolean)

data class Player(val name: String, val score: Int, val rack: List<Char>)

data class WordScore(
    val word: String, val score: Int,
    val startRow: Int, val startCol: Int, val isHorizontal: Boolean
)

sealed class PlayResult {
    data class Success(val words: List<String>, val score: Int) : PlayResult()
    data class Error(val message: String) : PlayResult()
}

class ScrabbleGame {
    val board = Array(15) { arrayOfNulls<Tile>(15) }
    val players = mutableListOf<Player>()
    var currentPlayer = 0
    val placedThisTurn = mutableListOf<PlacedTile>()
    var isGameOver = false
    var consecutiveSkips = 0

    private val letterDistribution = mapOf(
        'A' to 9, 'B' to 2, 'C' to 2, 'D' to 4, 'E' to 12, 'F' to 2, 'G' to 3,
        'H' to 2, 'I' to 9, 'J' to 1, 'K' to 1, 'L' to 4, 'M' to 2, 'N' to 6,
        'O' to 8, 'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 4, 'T' to 6, 'U' to 4,
        'V' to 2, 'W' to 2, 'X' to 1, 'Y' to 2, 'Z' to 1, '?' to 2
    )

    private val letterValues = mapOf(
        'A' to 1, 'B' to 3, 'C' to 3, 'D' to 2, 'E' to 1, 'F' to 4, 'G' to 2,
        'H' to 4, 'I' to 1, 'J' to 8, 'K' to 5, 'L' to 1, 'M' to 3, 'N' to 1,
        'O' to 1, 'P' to 3, 'Q' to 10, 'R' to 1, 'S' to 1, 'T' to 1, 'U' to 1,
        'V' to 4, 'W' to 4, 'X' to 8, 'Y' to 4, 'Z' to 10, '?' to 0
    )

    private val premiumSquares = mutableMapOf<String, String>()
    var bag = mutableListOf<Char>()
    private var largeDictionary = emptySet<String>()
    private var dictionary = emptySet<String>()

    init { initPremiumSquares() }

    fun getBagSize(): Int = bag.size

    private fun initPremiumSquares() {
        listOf(0 to 0, 0 to 7, 0 to 14, 7 to 0, 7 to 14, 14 to 0, 14 to 7, 14 to 14)
            .forEach { premiumSquares["${it.first},${it.second}"] = "TW" }
        listOf(1 to 1, 2 to 2, 3 to 3, 4 to 4, 10 to 10, 11 to 11, 12 to 12, 13 to 13,
            1 to 13, 2 to 12, 3 to 11, 4 to 10, 10 to 4, 11 to 3, 12 to 2, 13 to 1)
            .forEach { premiumSquares["${it.first},${it.second}"] = "DW" }
        listOf(1 to 5, 1 to 9, 5 to 1, 5 to 5, 5 to 9, 5 to 13, 9 to 1, 9 to 5, 9 to 9, 9 to 13, 13 to 5, 13 to 9)
            .forEach { premiumSquares["${it.first},${it.second}"] = "TL" }
        listOf(0 to 3, 0 to 11, 2 to 6, 2 to 8, 3 to 0, 3 to 7, 3 to 14, 6 to 2, 6 to 6, 6 to 8,
            6 to 12, 7 to 3, 7 to 11, 8 to 2, 8 to 6, 8 to 8, 8 to 12, 11 to 0, 11 to 7,
            11 to 14, 12 to 6, 12 to 8, 14 to 3, 14 to 11)
            .forEach { premiumSquares["${it.first},${it.second}"] = "DL" }
    }

    fun updateDictionary(newDict: Set<String>) { dictionary = newDict }
    fun updateLargeDictionary(newDict: Set<String>) { largeDictionary = newDict }

    fun startGame(player1Name: String, player2Name: String) {
        bag = buildBag()
        for (i in 0 until 15) for (j in 0 until 15) board[i][j] = null
        placedThisTurn.clear()
        val p1Rack = mutableListOf<Char>(); val p2Rack = mutableListOf<Char>()
        repeat(7) {
            if (bag.isNotEmpty()) p1Rack.add(bag.removeAt(bag.size - 1))
            if (bag.isNotEmpty()) p2Rack.add(bag.removeAt(bag.size - 1))
        }
        players.clear(); players.add(Player(player1Name, 0, p1Rack)); players.add(Player(player2Name, 0, p2Rack))
        currentPlayer = 0; consecutiveSkips = 0; isGameOver = false
    }

    private fun buildBag(): MutableList<Char> {
        val newBag = mutableListOf<Char>()
        letterDistribution.forEach { (letter, count) -> repeat(count) { newBag.add(letter) } }
        return newBag.shuffled().toMutableList()
    }

    fun placeTile(row: Int, col: Int, letter: Char): Boolean {
        if (board[row][col] != null || placedThisTurn.any { it.row == row && it.col == col }) return false
        val points = letterValues[letter] ?: 0
        placedThisTurn.add(PlacedTile(row, col, letter, points, letter == '?'))
        val player = players[currentPlayer]; val newRack = player.rack.toMutableList()
        val index = newRack.indexOf(letter)
        if (index != -1) { newRack.removeAt(index); players[currentPlayer] = player.copy(rack = newRack) }
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
        drawNewTiles(placedThisTurn.size)
        val playedWords = words.filter { it.word.length >= 2 }.map { it.word }
        placedThisTurn.clear(); consecutiveSkips = 0
        if (players[currentPlayer].rack.isEmpty() && bag.isEmpty()) endGame() else nextTurn()
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

    private fun drawNewTiles(count: Int) {
        val player = players[currentPlayer]; val newRack = player.rack.toMutableList()
        repeat(minOf(count, bag.size)) { if (bag.isNotEmpty()) newRack.add(bag.removeAt(bag.size - 1)) }
        players[currentPlayer] = player.copy(rack = newRack)
    }

    fun skipTurn() { recallAllTiles(); consecutiveSkips++; if (consecutiveSkips >= 4) endGame() else nextTurn() }

    fun exchangeTiles(): Boolean {
        if (bag.size < 7) return false
        recallAllTiles(); val p = players[currentPlayer]; val old = p.rack.toList(); val nr = mutableListOf<Char>()
        repeat(7) { if (bag.isNotEmpty()) nr.add(bag.removeAt(bag.size - 1)) }
        players[currentPlayer] = p.copy(rack = nr); old.forEach { bag.add(it) }; bag.shuffle(); consecutiveSkips++; nextTurn()
        return true
    }

    fun shuffleRack() { val p = players[currentPlayer]; players[currentPlayer] = p.copy(rack = p.rack.shuffled()) }
    private fun nextTurn() { currentPlayer = 1 - currentPlayer }
    private fun endGame() {
        players.forEachIndexed { i, p -> val penalty = p.rack.sumOf { letterValues[it] ?: 0 }; players[i] = p.copy(score = maxOf(0, p.score - penalty)) }
        isGameOver = true
    }
    fun reset() {
        board.forEach { row -> for (i in row.indices) row[i] = null }
        placedThisTurn.clear(); players.clear(); currentPlayer = 0; consecutiveSkips = 0; isGameOver = false; bag.clear()
    }
}