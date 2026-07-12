package com.enigma.wordnest.games.synthetix.model

// ─────────────────────────────────────────────────────────────────────────────
//  Core data classes
// ─────────────────────────────────────────────────────────────────────────────

data class Tile(
    val letter: Char,
    val points: Int,
    val isBlank: Boolean = false
)

data class PlacedTile(
    val row: Int,
    val col: Int,
    val letter: Char,       // always the display letter (never '?')
    val points: Int,
    val isBlank: Boolean
)

data class Player(
    val name: String,
    val score: Int,
    val rack: List<Char>
)

data class WordScore(
    val word: String,
    val score: Int,
    val startRow: Int,
    val startCol: Int,
    val isHorizontal: Boolean
)

sealed class PlayResult {
    data class Success(val words: List<String>, val score: Int) : PlayResult()
    data class Error(val message: String) : PlayResult()
}

// ─────────────────────────────────────────────────────────────────────────────
//  SynthetixGame
// ─────────────────────────────────────────────────────────────────────────────

class SynthetixGame {

    // ── State ────────────────────────────────────────────────────────────────
    var boardConfig: BoardConfig = BoardConfig.blank()
        private set
    var tileSet: TileSetConfig = TileSetConfig()
        private set

    val boardSize get() = boardConfig.size

    var board: Array<Array<Tile?>> = Array(15) { arrayOfNulls(15) }
        private set

    private val _placedThisTurn = mutableListOf<PlacedTile>()
    val placedThisTurn: List<PlacedTile> get() = _placedThisTurn.toList()

    internal fun patchPlacedTile(index: Int, updated: PlacedTile) {
        if (index in _placedThisTurn.indices) _placedThisTurn[index] = updated
    }

    private val _players = mutableListOf<Player>()
    val players: List<Player> get() = _players.toList()

    private fun setPlayer(index: Int, player: Player) {
        _players[index] = player
    }

    var currentPlayer = 0
        private set
    var isGameOver = false
        private set
    var consecutiveSkips = 0
        private set
    var bag = mutableListOf<Char>()
        private set

    private var dictionary = emptySet<String>()
    private var largeDictionary = emptySet<String>()

    private val letterValues: Map<Char, Int>
        get() = tileSet.letters.associate { it.letter to it.points }

    fun getBagSize() = bag.size

    // ── Initialisation ────────────────────────────────────────────────────────

    fun configure(config: BoardConfig, tiles: TileSetConfig = TileSetConfig()) {
        boardConfig = config
        tileSet = tiles
        board = Array(config.size) { arrayOfNulls(config.size) }
    }

    fun updateDictionary(d: Set<String>) { dictionary = d }
    fun updateLargeDictionary(d: Set<String>) { largeDictionary = d }

    fun startGame(player1Name: String, player2Name: String) {
        board = Array(boardSize) { arrayOfNulls(boardSize) }
        _placedThisTurn.clear()
        bag = tileSet.toBag().toMutableList()

        val p1Rack = mutableListOf<Char>()
        val p2Rack = mutableListOf<Char>()
        val rk = tileSet.rackSize
        repeat(rk) {
            if (bag.isNotEmpty()) p1Rack.add(bag.removeAt(bag.size - 1))
            if (bag.isNotEmpty()) p2Rack.add(bag.removeAt(bag.size - 1))
        }
        _players.clear()
        _players += Player(player1Name, 0, p1Rack)
        _players += Player(player2Name, 0, p2Rack)
        currentPlayer = 0
        consecutiveSkips = 0
        isGameOver = false
    }

    // ── Tile placement ────────────────────────────────────────────────────────

    /**
     * Place a tile. For blank tiles, [blankAs] specifies the letter to display.
     * Returns false if the placement is invalid.
     *
     * ✅ FIX (B2): Validates that blankAs is a letter A–Z before accepting it.
     */
    fun placeTile(row: Int, col: Int, letter: Char, blankAs: Char? = null): Boolean {
        // ✅ Validate blank tile assignment — must be a letter A–Z
        if (letter == '?' && blankAs != null && blankAs !in 'A'..'Z') {
            return false  // Reject invalid blank assignments silently
        }

        val sq = boardConfig.squareAt(row, col)
        if (sq.isObstacle) return false
        if (board[row][col] != null) return false
        if (_placedThisTurn.any { it.row == row && it.col == col }) return false

        val isBlank = letter == '?'
        val displayLetter = if (isBlank && blankAs != null) blankAs.uppercaseChar() else letter
        val pts = if (isBlank) 0 else (letterValues[letter] ?: 0)

        _placedThisTurn += PlacedTile(row, col, displayLetter, pts, isBlank)

        val player = _players[currentPlayer]
        val newRack = player.rack.toMutableList()
        val rackToken = if (isBlank) '?' else letter
        val idx = newRack.indexOf(rackToken)
        if (idx != -1) newRack.removeAt(idx)
        _players[currentPlayer] = player.copy(rack = newRack)
        return true
    }

    fun recallTile(row: Int, col: Int) {
        val tile = _placedThisTurn.find { it.row == row && it.col == col } ?: return
        _placedThisTurn.remove(tile)
        val player = _players[currentPlayer]
        val newRack = player.rack.toMutableList()
        newRack.add(if (tile.isBlank) '?' else tile.letter)
        _players[currentPlayer] = player.copy(rack = newRack)
    }

    fun recallAllTiles() {
        _placedThisTurn.forEach { tile ->
            val player = _players[currentPlayer]
            val newRack = player.rack.toMutableList()
            newRack.add(if (tile.isBlank) '?' else tile.letter)
            _players[currentPlayer] = player.copy(rack = newRack)
        }
        _placedThisTurn.clear()
    }

    // ── Play validation & scoring ─────────────────────────────────────────────

    fun playWord(): PlayResult {
        if (_placedThisTurn.isEmpty()) return PlayResult.Error("Place some tiles first!")

        val boardEmpty = board.all { r -> r.all { it == null } }
        val (anchorRow, anchorCol) = boardConfig.anchorCell()

        if (boardEmpty) {
            if (_placedThisTurn.none { it.row == anchorRow && it.col == anchorCol })
                return PlayResult.Error("First word must cover the start square")
            if (_placedThisTurn.size < 2)
                return PlayResult.Error("First word must be at least 2 letters")
        } else if (!connectsToBoard()) {
            return PlayResult.Error("Word must connect to existing tiles")
        }

        val rows = _placedThisTurn.map { it.row }.distinct()
        val cols = _placedThisTurn.map { it.col }.distinct()
        if (rows.size > 1 && cols.size > 1)
            return PlayResult.Error("Tiles must be in a single row or column")
        val isHorizontal = rows.size == 1
        if (!isContiguous(isHorizontal))
            return PlayResult.Error("Tiles must form a contiguous word")

        val words = collectWords(isHorizontal)
        val invalidWords = words.filter { it.word.length >= 2 && !largeDictionary.contains(it.word.uppercase()) }
        if (invalidWords.isNotEmpty())
            return PlayResult.Error("\"${invalidWords.first().word}\" is not in the dictionary")

        var totalScore = words.filter { it.word.length >= 2 }.sumOf { it.score }
        if (_placedThisTurn.size == tileSet.rackSize) totalScore += tileSet.bingoBonus

        _placedThisTurn.forEach { board[it.row][it.col] = Tile(it.letter, it.points, it.isBlank) }
        val player = _players[currentPlayer]
        _players[currentPlayer] = player.copy(score = player.score + totalScore)
        drawNewTiles(_placedThisTurn.size)
        val playedWords = words.filter { it.word.length >= 2 }.map { it.word }
        _placedThisTurn.clear()
        consecutiveSkips = 0
        if (_players[currentPlayer].rack.isEmpty() && bag.isEmpty()) endGame() else nextTurn()
        return PlayResult.Success(playedWords, totalScore)
    }

    private fun connectsToBoard(): Boolean {
        for (tile in _placedThisTurn) {
            val neighbors = listOf(
                tile.row - 1 to tile.col, tile.row + 1 to tile.col,
                tile.row to tile.col - 1, tile.row to tile.col + 1
            )
            if (neighbors.any { (r, c) ->
                    r in 0 until boardSize && c in 0 until boardSize && board[r][c] != null
                }) return true
        }
        return false
    }

    private fun isContiguous(isHorizontal: Boolean): Boolean {
        if (_placedThisTurn.size == 1) return true
        val sorted = _placedThisTurn.sortedBy { if (isHorizontal) it.col else it.row }
        val fixed = if (isHorizontal) sorted[0].row else sorted[0].col
        for (i in 0 until sorted.size - 1) {
            val cur = if (isHorizontal) sorted[i].col else sorted[i].row
            val next = if (isHorizontal) sorted[i + 1].col else sorted[i + 1].row
            for (pos in cur + 1 until next) {
                val cr = if (isHorizontal) fixed else pos
                val cc = if (isHorizontal) pos else fixed
                if (board[cr][cc] == null && _placedThisTurn.none { it.row == cr && it.col == cc })
                    return false
            }
        }
        return true
    }

    private fun getTileAt(row: Int, col: Int): Tile? {
        _placedThisTurn.find { it.row == row && it.col == col }?.let {
            return Tile(it.letter, it.points, it.isBlank)
        }
        return board[row][col]
    }

    private fun buildWord(startRow: Int, startCol: Int, isHorizontal: Boolean): WordScore {
        var sr = startRow; var sc = startCol
        while (true) {
            val nr = sr - if (isHorizontal) 0 else 1
            val nc = sc - if (isHorizontal) 1 else 0
            if (nr in 0 until boardSize && nc in 0 until boardSize && getTileAt(nr, nc) != null)
            { sr = nr; sc = nc } else break
        }

        var word = ""; var score = 0; var wordMult = 1
        var cr = sr; var cc = sc
        var isNegativeWord = false
        while (cr in 0 until boardSize && cc in 0 until boardSize) {
            val tile = getTileAt(cr, cc) ?: break
            word += tile.letter
            var lv = tile.points
            val isNew = _placedThisTurn.any { it.row == cr && it.col == cc }
            if (isNew) {
                val sq = boardConfig.squareAt(cr, cc)
                when {
                    sq.isNegative -> isNegativeWord = true
                    sq.isAnchor   -> wordMult *= 2
                    else          -> { lv *= sq.letterMultiplier; wordMult *= sq.wordMultiplier }
                }
            }
            score += lv
            cr += if (isHorizontal) 0 else 1
            cc += if (isHorizontal) 1 else 0
        }
        val finalScore = score * wordMult
        return WordScore(word, if (isNegativeWord) -finalScore else finalScore, sr, sc, isHorizontal)
    }

    private fun collectWords(isHorizontal: Boolean): List<WordScore> {
        val words = mutableListOf<WordScore>()
        val first = _placedThisTurn.first()
        val main = buildWord(first.row, first.col, isHorizontal)
        if (main.word.length >= 2) words += main
        _placedThisTurn.forEach { t ->
            val cross = buildWord(t.row, t.col, !isHorizontal)
            if (cross.word.length >= 2) words += cross
        }
        return words.distinctBy { "${it.word}:${it.startRow},${it.startCol}" }
    }

    private fun drawNewTiles(count: Int) {
        val player = _players[currentPlayer]
        val newRack = player.rack.toMutableList()
        repeat(minOf(count, bag.size)) { if (bag.isNotEmpty()) newRack.add(bag.removeAt(bag.size - 1)) }
        _players[currentPlayer] = player.copy(rack = newRack)
    }

    fun skipTurn() {
        recallAllTiles()
        consecutiveSkips++
        if (consecutiveSkips >= 4) endGame() else nextTurn()
    }

    fun exchangeTiles(): Boolean {
        if (bag.size < tileSet.rackSize) return false
        recallAllTiles()
        val p = _players[currentPlayer]; val old = p.rack.toList()
        val nr = mutableListOf<Char>()
        repeat(tileSet.rackSize) { if (bag.isNotEmpty()) nr.add(bag.removeAt(bag.size - 1)) }
        _players[currentPlayer] = p.copy(rack = nr)
        old.forEach { bag.add(it) }
        bag.shuffle()
        consecutiveSkips++
        nextTurn()
        return true
    }

    fun shuffleRack() {
        val p = _players[currentPlayer]
        _players[currentPlayer] = p.copy(rack = p.rack.shuffled())
    }

    private fun nextTurn() { currentPlayer = 1 - currentPlayer }

    private fun endGame() {
        _players.forEachIndexed { i, p ->
            val penalty = p.rack.sumOf { letterValues[it] ?: 0 }
            _players[i] = p.copy(score = maxOf(0, p.score - penalty))
        }
        isGameOver = true
    }

    fun reset() {
        board = Array(boardConfig.size) { arrayOfNulls(boardConfig.size) }
        _placedThisTurn.clear()
        _players.clear()
        currentPlayer = 0; consecutiveSkips = 0; isGameOver = false; bag.clear()
    }

    // ── Restore helpers used by ViewModel during game resume ──────────────────

    fun restoreBoard(saved: List<List<Tile?>>) {
        saved.forEachIndexed { r, row -> row.forEachIndexed { c, tile -> board[r][c] = tile } }
    }

    fun restorePlayers(saved: List<Player>) {
        _players.clear(); _players.addAll(saved)
    }

    fun restoreBag(saved: List<Char>) {
        bag.clear(); bag.addAll(saved)
    }

    fun restoreCurrentPlayer(cp: Int) { currentPlayer = cp }
    fun restoreConsecutiveSkips(cs: Int) { consecutiveSkips = cs }
}
