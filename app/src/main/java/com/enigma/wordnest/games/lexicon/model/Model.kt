package com.enigma.wordnest.games.lexicon.model

import com.enigma.wordnest.games.lexicon.model.BoardConfig.letterValues
import java.util.Collections

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

    private val premiumSquares = BoardConfig.buildPremiumMap()
    var bag = mutableListOf<Char>()
    private var largeDictionary = emptySet<String>()
    private var dictionary = emptySet<String>()

    fun getBagSize(): Int = bag.size

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
        BoardConfig.letterDistribution.forEach { (letter, count) -> repeat(count) { newBag.add(letter) } }
        return newBag.shuffled().toMutableList()
    }

    fun placeTile(row: Int, col: Int, letter: Char): Boolean {
        if (board[row][col] != null || placedThisTurn.any { it.row == row && it.col == col }) return false
        val player = players[currentPlayer]
        val newRack = player.rack.toMutableList()
        val index = newRack.indexOf(letter).let { if (it == -1) newRack.indexOf('?') else it }
        if (index == -1) return false          // ← reject instead of silently succeeding
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
        players.forEachIndexed { i, p -> val penalty = p.rack.sumOf { BoardConfig.letterValues[it] ?: 0 }; players[i] = p.copy(score = maxOf(0, p.score - penalty)) }
        isGameOver = true
    }
    fun reset() {
        board.forEach { row -> for (i in row.indices) row[i] = null }
        placedThisTurn.clear(); players.clear(); currentPlayer = 0; consecutiveSkips = 0; isGameOver = false; bag.clear()
    }
}
