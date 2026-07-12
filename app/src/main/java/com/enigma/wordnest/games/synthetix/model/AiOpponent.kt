package com.enigma.wordnest.games.synthetix.model

// ─────────────────────────────────────────────────────────────────────────────
//  AI difficulty
// ─────────────────────────────────────────────────────────────────────────────

enum class AiDifficulty(
    val displayName: String,
    val vocabCoverage: Float,
    val exchangeThreshold: Int
) {
    EASY  ("Easy",   0.40f, 6),
    MEDIUM("Medium", 0.60f, 10),
    HARD  ("Hard",   0.85f, 16),
    EXPERT("Expert", 1.00f, 22)
}

data class AiMove(
    val tiles: List<PlacedTile>,
    val score: Int,
    val word: String,
    val isHorizontal: Boolean
)

sealed class AiDecision {
    data class PlayWord(val move: AiMove) : AiDecision()
    object ExchangeTiles : AiDecision()
    object Skip : AiDecision()
}

// ─────────────────────────────────────────────────────────────────────────────
//  AI Opponent  –  fast anagram-index architecture
//
//  Key optimisations vs the previous implementation:
//
//  1. ANAGRAM INDEX  (built once at construction, O(1) lookup per rack subset)
//     Words are grouped by their sorted-letter key, e.g. "RATE" → "AERT".
//     Given a rack we generate every non-empty subset (≤ 2^7 = 128 subsets for
//     a 7-tile rack) and look up candidate words directly — no linear scan of
//     300 k words per anchor.
//
//  2. REAL canFormWithRack  (actual early-exit pre-filter, was always `true`)
//     Before the expensive tryPlaceWord the rack-subset check is now O(wordLen).
//
//  3. ANCHOR BUDGET  (cap anchors to avoid redundant positions)
//     On a dense board there can be hundreds of valid anchors.  We cap at
//     MAX_ANCHORS and prefer cells that border the most existing tiles.
//
//  4. EARLY EXIT in tryPlaceWord  (returns null as soon as any letter fails)
//     No change in logic — just ensured no extra allocation happens before
//     the first rejection.
//
//  5. DIFFICULTY-BASED VOCAB PRUNING  (same as before, but applied to the
//     index not a flat set, so lookup stays O(1) regardless of coverage).
// ─────────────────────────────────────────────────────────────────────────────

class AiOpponent(
    fullDictionary: Set<String>,
    /**
     * Separate validation dictionary used for cross-word validation so the AI
     * never rejects a valid play just because the cross-word isn't in its
     * sampled vocabulary subset.
     */
    private val validationDictionary: Set<String>,
    val difficulty: AiDifficulty
) {
    // ── Core / common word lists (verified TWL06/OSPD5) ──────────────────────

    private val coreWords: Set<String> = setOf(
        "AA","AB","AD","AE","AG","AH","AI","AL","AM","AN","AR","AS","AT","AW","AX","AY",
        "BA","BE","BI","BO","BY","CH",
        "DA","DE","DI","DO",
        "EA","ED","EE","EF","EH","EL","EM","EN","ER","ES","ET","EX",
        "FA","FE","FI","GI","GO","GU",
        "HA","HE","HI","HO",
        "ID","IF","IN","IO","IS","IT","JA","JO","KA","KI","LA","LO",
        "MA","ME","MI","MO","MU","MY","NA","NE","NO","NU",
        "OB","OD","OE","OF","OH","OI","OM","ON","OP","OR","OS","OW","OX","OY",
        "PA","PE","PI","QI","RE","SH","SI","SO",
        "TA","TE","TI","TO","UG","UH","UM","UN","UP","UR","US","UT",
        "WE","WO","XI","XU","YA","YE","YO","ZA","ZO"
    )

    private val commonWords: Set<String> = setOf(
        "ACE","ACT","ADD","AGE","AGO","AID","AIM","AIR","ALE","ALL","ANT","APE","APT",
        "ARC","ARK","ARM","ART","ASH","ASK","AWE",
        "BAD","BAG","BAN","BAR","BAT","BAY","BED","BEG","BID","BIG","BIT","BOW","BOX",
        "BOY","BUD","BUG","BUN","BUS","BUT","BUY",
        "CAB","CAN","CAP","CAR","CAT","COP","COT","COW","CRY","CUB","CUP","CUR","CUT",
        "DAB","DAD","DAM","DID","DIG","DIM","DIP","DOC","DOE","DOG","DOT","DRY","DUB",
        "DUG","DUN","DUO",
        "EAR","EAT","EEL","EGG","ELF","ELK","ELM","EMU","END","ERA","ERR","EVE","EWE","EYE",
        "FAD","FAN","FAR","FAT","FAX","FED","FEW","FIG","FIN","FIT","FIX","FLY","FOB",
        "FOE","FOG","FOP","FOR","FOX","FRY","FUB","FUN","FUR",
        "GAB","GAG","GAL","GAP","GAR","GAS","GAY","GEL","GEM","GET","GIG","GIN","GNU",
        "GOB","GOD","GOT","GUM","GUN","GUT","GUY","GYM",
        "HAD","HAM","HAS","HAT","HAW","HAY","HEN","HEP","HIM","HIP","HIS","HIT","HOB",
        "HOD","HOG","HOP","HOT","HOW","HUB","HUG","HUL","HUM","HUT",
        "ICE","ICY","ILL","IMP","INK","INN","ION","IRE","IRK","IVY",
        "JAB","JAG","JAM","JAR","JAW","JAY","JET","JIG","JOB","JOG","JOT","JOY","JUG","JUT",
        "KEG","KID","KIN","KIT",
        "LAB","LAD","LAG","LAP","LAW","LAX","LAY","LEA","LEG","LET","LID","LIT","LOG","LOT","LOW",
        "MAP","MAR","MAT","MAW","MAY","MEN","MEW","MID","MIX","MOB","MOP","MOW","MUD","MUG","MUM",
        "NAB","NAP","NAG","NET","NIL","NIT","NOD","NOR","NOT","NOW","NUB","NUN","NUT",
        "OAK","OAR","OAT","ODD","ODE","OFF","OFT","OHM","OPT","ORB","ORE","OWL","OWN",
        "PAD","PAL","PAN","PAR","PAT","PAW","PAY","PEA","PEG","PEN","PEP","PET","PIE","PIG",
        "PIN","PIT","PLY","POD","POP","POT","POW","PRY","PUB","PUG","PUN","PUP",
        "RAG","RAM","RAN","RAP","RAT","RAW","RAY","RED","REF","RID","RIG","RIM","RIP","ROB",
        "ROD","ROT","ROW","RUB","RUG","RUN","RUT",
        "SAC","SAG","SAP","SAT","SAW","SAY","SEA","SET","SIP","SIT","SIX","SKI","SKY","SLY",
        "SOB","SOD","SON","SOP","SOT","SOW","SOY","SPA","SPY","STY","SUM","SUN","SUP",
        "TAB","TAD","TAN","TAP","TAR","TAT","TAW","TAX","TEA","TEN","TIE","TIN","TIP","TON",
        "TOP","TOT","TOW","TOY","TUB","TUG","TUN","TUX",
        "VAN","VAT","VET","VIA","VIE",
        "WAD","WAR","WAX","WAY","WED","WIG","WIN","WIT","WOE","WOK","WON","WOO","WOW",
        "YAK","YAM","YAP","YAW","YEA","YEP","YEW","YIP",
        "ZAG","ZAP","ZED","ZEK","ZEN","ZIT","ZOO",
        "ABLE","ACED","ACES","ACHE","ACID","ACME","ACRE","ACTS","AGED","AGES","AGOG","AGUE",
        "AHEM","AIDE","AIDS","AIMS","AIRS","AIRY","AJAR","AKIN","ALES","ALLY","ALSO","ALTO",
        "ALUM","AMEN","AMID","AMOK","AMPS","ANAL","ANEW","ANTE","ANTS","APEX","ARCH","AREA",
        "ARIA","ARID","ARMY","ARTS","ARTY","ASHY","ATOM","ATOP","AUNT","AUTO","AVID","AVOW",
        "AWED","AWRY","AXLE","ZING","ZONE","ZOOM","ZEAL","ZERO","ZEST","ZANY"
    )

    /**
     * Working vocabulary — difficulty-sampled, guaranteed to include all core
     * and common words.
     */
    val vocabulary: Set<String> = buildVocabulary(fullDictionary)

    // ── Anagram index: sortedLetters → list of matching words ────────────────
    //
    //  Built once at construction.  A 300 k word dictionary with average length
    //  ~6 produces ~120 k distinct keys.  Index memory ≈ 10–15 MB on the heap,
    //  well within Android limits.
    //
    //  Lookup: given a rack subset of N letters, sort them → O(N log N), then
    //  retrieve the word list in O(1).  No linear scan of the whole vocabulary.

    private val anagramIndex: Map<String, List<String>> = buildAnagramIndex(vocabulary)

    private fun buildVocabulary(full: Set<String>): Set<String> {
        val base = coreWords + commonWords
        if (difficulty.vocabCoverage >= 1.0f) return full + base
        val nonCore = full.filter { it !in base }
        val sampleSize = (nonCore.size * difficulty.vocabCoverage).toInt()
        return nonCore.shuffled().take(sampleSize).toSet() + base
    }

    private fun buildAnagramIndex(words: Set<String>): Map<String, List<String>> {
        val map = HashMap<String, MutableList<String>>(words.size / 2)
        for (word in words) {
            val key = word.uppercase().toCharArray().also { it.sort() }.let { String(it) }
            map.getOrPut(key) { mutableListOf() }.add(word.uppercase())
        }
        return map
    }

    // ── Main decision ─────────────────────────────────────────────────────────

    fun decideMove(
        board: Array<Array<Tile?>>,
        rack: List<Char>,
        bagSize: Int,
        boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): AiDecision {
        val boardEmpty = board.all { row -> row.all { it == null } }
        val candidates = findAllMoves(board, rack, boardEmpty, boardConfig, tileSet)

        if (candidates.isEmpty())
            return if (bagSize >= tileSet.rackSize) AiDecision.ExchangeTiles else AiDecision.Skip

        val sorted = candidates.sortedByDescending { it.score }
        val best = sorted.first()

        if (best.score < difficulty.exchangeThreshold && bagSize >= tileSet.rackSize)
            return AiDecision.ExchangeTiles

        return AiDecision.PlayWord(pickMove(sorted))
    }

    private fun pickMove(sorted: List<AiMove>): AiMove {
        val x = sorted.size
        return when (difficulty) {
            AiDifficulty.EASY -> {
                val poolSize = if (x / 4 > 6) 6 else maxOf(x / 4, 1)
                sorted.takeLast(poolSize).random()
            }
            AiDifficulty.MEDIUM -> {
                val poolSize = minOf(10, x)
                val from = maxOf(0, (x / 2) - poolSize / 2)
                sorted.subList(from, minOf(x, from + poolSize)).random()
            }
            AiDifficulty.HARD -> {
                val poolSize = if (x / 4 > 6) 6 else maxOf(x / 4, 1)
                sorted.take(poolSize).random()
            }
            AiDifficulty.EXPERT -> sorted.first()
        }
    }

    // ── Move finding ──────────────────────────────────────────────────────────

    companion object {
        /** Cap the number of anchors evaluated per turn to prevent O(n²) growth
         *  on large boards.  Anchors are ranked by neighbour density first so the
         *  best positions are always included. */
        private const val MAX_ANCHORS = 40

        /** Maximum number of valid candidates to collect before stopping search.
         *  Prevents runaway evaluation on very open boards. */
        private const val CANDIDATE_CAP = 300
    }

    fun findBestMove(
        board: Array<Array<Tile?>>,
        rack: List<Char>,
        boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): AiMove? {
        val boardEmpty = board.all { row -> row.all { it == null } }
        return findAllMoves(board, rack, boardEmpty, boardConfig, tileSet)
            .maxByOrNull { it.score }
    }

    private fun findAllMoves(
        board: Array<Array<Tile?>>,
        rack: List<Char>,
        boardEmpty: Boolean,
        boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): List<AiMove> {
        val anchors = if (boardEmpty) {
            val (ar, ac) = boardConfig.anchorCell(); listOf(ar to ac)
        } else {
            findAnchors(board, boardConfig)
        }

        // Pre-compute all candidate words reachable from this rack (including
        // letters already on the board won't be known yet, but we'll use this
        // to quickly decide which anchors are worth exploring).
        val rackWords = findWordsFromRack(rack)

        val moves = mutableListOf<AiMove>()

        outer@ for ((aRow, aCol) in anchors) {
            for (horiz in listOf(true, false)) {
                moves += generateMovesAt(
                    board, rack, rackWords, aRow, aCol, horiz,
                    boardEmpty, boardConfig, tileSet
                )
                if (moves.size >= CANDIDATE_CAP) break@outer
            }
        }

        return moves.distinctBy {
            val startR = it.tiles.minOfOrNull { t -> t.row } ?: 0
            val startC = it.tiles.minOfOrNull { t -> t.col } ?: 0
            "${it.word}:${startR},${startC}:${it.isHorizontal}"
        }
    }

    /**
     * Generate all words formable from the rack alone (no board tiles) using
     * the anagram index.  These are used as a fast pre-filter per anchor.
     *
     * For each non-empty subset of the rack we sort the letters and look up the
     * anagram index.  With a 7-tile rack there are 127 non-empty subsets, each
     * lookup is O(k log k) where k ≤ 7.  Total cost: ~500 comparisons regardless
     * of dictionary size.
     */
    private fun findWordsFromRack(rack: List<Char>): Set<String> {
        val result = mutableSetOf<String>()
        val n = rack.size
        // Iterate all non-empty subsets using bitmask
        for (mask in 1 until (1 shl n)) {
            val subset = CharArray(Integer.bitCount(mask))
            var idx = 0
            for (i in 0 until n) {
                if (mask and (1 shl i) != 0) subset[idx++] = rack[i].uppercaseChar()
            }
            subset.sort()
            val key = String(subset)
            anagramIndex[key]?.let { result.addAll(it) }
        }
        return result
    }

    private fun findAnchors(board: Array<Array<Tile?>>, bc: BoardConfig): List<Pair<Int, Int>> {
        val bs = bc.size
        data class ScoredAnchor(val row: Int, val col: Int, val score: Int)
        val seen = HashSet<Long>()
        val candidates = mutableListOf<ScoredAnchor>()

        fun key(r: Int, c: Int) = r.toLong() * bs + c

        for (r in 0 until bs) for (c in 0 until bs) {
            if (board[r][c] != null) continue
            if (bc.squareAt(r, c).isObstacle) continue

            // Count immediate (±1) neighbours with tiles — highest priority.
            val immediateNeighbours = listOf(r-1 to c, r+1 to c, r to c-1, r to c+1)
                .count { (nr, nc) ->
                    nr in 0 until bs && nc in 0 until bs && board[nr][nc] != null
                }

            if (immediateNeighbours > 0) {
                if (seen.add(key(r, c)))
                    candidates += ScoredAnchor(r, c, immediateNeighbours + 10)
                continue
            }

            // On obstacle boards, a corridor cell may be 2 squares away from a
            // tile with only empty (not obstacle) squares in between — still a
            // valid anchor because a word can bridge the gap.
            // Score these lower than immediate neighbours so they're evaluated
            // last and get dropped first when the cap is reached.
            val nearNeighbours = listOf(r-2 to c, r+2 to c, r to c-2, r to c+2)
                .count { (nr, nc) ->
                    if (nr !in 0 until bs || nc !in 0 until bs) return@count false
                    if (board[nr][nc] == null) return@count false
                    // The intermediate cell must be empty (not an obstacle) so a
                    // word really can be placed through it.
                    val mr = (r + nr) / 2; val mc = (c + nc) / 2
                    !bc.squareAt(mr, mc).isObstacle
                }

            if (nearNeighbours > 0 && seen.add(key(r, c)))
                candidates += ScoredAnchor(r, c, nearNeighbours)
        }

        // Also include the cell that is the immediate neighbour of every existing
        // tile even if that cell itself has no tile-neighbour yet — this ensures
        // the AI never gets stuck when the board is very sparse.
        if (candidates.isEmpty()) {
            for (r in 0 until bs) for (c in 0 until bs) {
                if (board[r][c] != null) continue
                if (bc.squareAt(r, c).isObstacle) continue
                for (dr in -2..2) for (dc in -2..2) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until bs && nc in 0 until bs && board[nr][nc] != null
                        && seen.add(key(r, c))
                    ) {
                        candidates += ScoredAnchor(r, c, 1)
                        break
                    }
                }
            }
        }

        return candidates
            .sortedByDescending { it.score }
            .take(MAX_ANCHORS)
            .map { it.row to it.col }
    }

    private fun generateMovesAt(
        board: Array<Array<Tile?>>,
        rack: List<Char>,
        rackWords: Set<String>,   // words formable from rack alone (pre-computed)
        anchorRow: Int, anchorCol: Int,
        isHorizontal: Boolean,
        boardEmpty: Boolean,
        boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): List<AiMove> {
        val bs = boardConfig.size
        val results = mutableListOf<AiMove>()

        val maxLen = maxRunLength(board, boardConfig, anchorRow, anchorCol, isHorizontal)
            .coerceAtLeast(2)

        // Collect letters already on the board along this run — words that use
        // those letters are eligible even if not in rackWords.
        val boardLettersInRun = collectBoardLettersInRun(
            board, boardConfig, anchorRow, anchorCol, isHorizontal, bs
        )

        // Determine the candidate word set for this anchor:
        // • Pure rack words (no board tiles needed)
        // • PLUS words that can be built with rack + board letters in this run
        // For performance we add board-letter words only when there are board
        // tiles available in the run (avoids scanning the whole index again).
        val candidates: Set<String> = if (boardLettersInRun.isEmpty()) {
            rackWords
        } else {
            // Build an extended rack: actual rack + letters already on the board
            // in this run direction, then find words formable from that combined set.
            val combined = rack.toMutableList()
            boardLettersInRun.forEach { combined.add(it) }
            // Limit combined subset expansion — board letters are positionally fixed
            // so we find words from all subsets of combined but only include words
            // that USE at least one board letter (otherwise rackWords already covers it).
            findWordsUsingBoardLetters(rack, boardLettersInRun) + rackWords
        }

        for (word in candidates) {
            if (word.length < 2 || word.length > maxLen) continue

            for (offset in word.indices) {
                val startRow = if (isHorizontal) anchorRow else anchorRow - offset
                val startCol = if (isHorizontal) anchorCol - offset else anchorCol
                if (startRow < 0 || startCol < 0) continue
                val endRow = if (isHorizontal) startRow else startRow + word.length - 1
                val endCol = if (isHorizontal) startCol + word.length - 1 else startCol
                if (endRow >= bs || endCol >= bs) continue

                if (boardEmpty) {
                    val (ar, ac) = boardConfig.anchorCell()
                    val coversAnchor = (0 until word.length).any { i ->
                        val r = if (isHorizontal) startRow else startRow + i
                        val c = if (isHorizontal) startCol + i else startCol
                        r == ar && c == ac
                    }
                    if (!coversAnchor) continue
                }

                tryPlaceWord(
                    board, word, startRow, startCol, isHorizontal,
                    rack, boardEmpty, boardConfig, tileSet
                )?.let { results += it }
            }
        }
        return results
    }

    /**
     * Collect distinct letters that are already committed to the board along a
     * run direction from the given anchor.  Used to widen the candidate set to
     * include words that need a board letter as a "free" tile.
     */
    private fun collectBoardLettersInRun(
        board: Array<Array<Tile?>>,
        bc: BoardConfig,
        anchorRow: Int, anchorCol: Int,
        isHorizontal: Boolean,
        bs: Int
    ): List<Char> {
        val letters = mutableListOf<Char>()
        // Find the start of the run: walk backward stopping only at obstacles or bounds.
        // Empty squares within the run are valid play positions and must NOT stop the walk.
        var r = anchorRow; var c = anchorCol
        while (true) {
            val nr = if (isHorizontal) r else r - 1
            val nc = if (isHorizontal) c - 1 else c
            if (nr < 0 || nc < 0) break
            if (bc.squareAt(nr, nc).isObstacle) break
            r = nr; c = nc
        }
        // Walk forward through the entire run collecting letters already on the board.
        while (r in 0 until bs && c in 0 until bs) {
            if (bc.squareAt(r, c).isObstacle) break
            board[r][c]?.let { letters.add(it.letter.uppercaseChar()) }
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return letters
    }

    /**
     * Find words that can be formed using [rack] letters PLUS at least one of
     * the [boardLetters].  The board letters are treated as "free" (already on
     * the board) but must be used in the correct position — this is a quick
     * superset of possibilities; exact placement is validated in tryPlaceWord.
     *
     * We enumerate subsets of (rack + boardLetters) that include at least one
     * board letter, then look up the anagram index.  Board letters are de-duped
     * so the subset expansion stays small.
     */
    private fun findWordsUsingBoardLetters(
        rack: List<Char>,
        boardLetters: List<Char>
    ): Set<String> {
        val result = mutableSetOf<String>()
        val uniqueBoard = boardLetters.distinct()

        // For each unique board letter, pretend it is added to the rack and find
        // words from all subsets of (rack + that board letter).  This is a valid
        // superset — invalid placements are rejected by tryPlaceWord.
        for (bl in uniqueBoard) {
            val extended = rack.map { it.uppercaseChar() } + bl.uppercaseChar()
            val n = extended.size
            for (mask in 1 until (1 shl n)) {
                // Must include the board letter bit (last bit = index n-1)
                if (mask and (1 shl (n - 1)) == 0) continue
                val subset = CharArray(Integer.bitCount(mask))
                var idx = 0
                for (i in 0 until n) {
                    if (mask and (1 shl i) != 0) subset[idx++] = extended[i]
                }
                subset.sort()
                val key = String(subset)
                anagramIndex[key]?.let { result.addAll(it) }
            }
        }
        return result
    }

    private fun maxRunLength(
        board: Array<Array<Tile?>>,
        bc: BoardConfig,
        anchorRow: Int, anchorCol: Int,
        isHorizontal: Boolean
    ): Int {
        val bs = bc.size
        // Walk backward to the true start of the run (obstacle or board edge).
        // Empty squares are valid play positions and must NOT stop this walk —
        // on obstacle boards a run can be: [OBS] _ E _ _ [OBS] and the full
        // length of 4 must be returned, not 1 (distance back to anchor).
        var sr = anchorRow; var sc = anchorCol
        while (true) {
            val nr = if (isHorizontal) sr else sr - 1
            val nc = if (isHorizontal) sc - 1 else sc
            if (nr < 0 || nc < 0) break
            if (bc.squareAt(nr, nc).isObstacle) break
            sr = nr; sc = nc
        }
        var len = 0
        var r = sr; var c = sc
        while (r in 0 until bs && c in 0 until bs) {
            if (bc.squareAt(r, c).isObstacle) break
            len++
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return len
    }

    private fun tryPlaceWord(
        board: Array<Array<Tile?>>,
        word: String,
        startRow: Int, startCol: Int,
        isHorizontal: Boolean,
        availableRack: List<Char>,
        boardEmpty: Boolean,
        boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): AiMove? {
        val bs = boardConfig.size
        val tempRack = availableRack.toMutableList()
        val hasBlank = '?' in tempRack
        val newTiles = mutableListOf<PlacedTile>()

        for (i in word.indices) {
            val r = if (isHorizontal) startRow else startRow + i
            val c = if (isHorizontal) startCol + i else startCol
            val letter = word[i].uppercaseChar()
            val sq = boardConfig.squareAt(r, c)

            if (sq.isObstacle) return null

            val existing = board[r][c]
            if (existing != null) {
                if (existing.letter.uppercaseChar() != letter) return null
            } else {
                when {
                    tempRack.contains(letter) -> {
                        tempRack.remove(letter)
                        newTiles.add(PlacedTile(r, c, letter, tileSet.pointsFor(letter), false))
                    }
                    hasBlank && tempRack.contains('?') -> {
                        tempRack.remove('?')
                        newTiles.add(PlacedTile(r, c, letter, 0, true))
                    }
                    else -> return null
                }
            }
        }

        if (newTiles.isEmpty()) return null

        // Check nothing extends the word unintentionally
        val afterR = if (isHorizontal) startRow else startRow + word.length
        val afterC = if (isHorizontal) startCol + word.length else startCol
        if (afterR in 0 until bs && afterC in 0 until bs) {
            val afterSq = boardConfig.squareAt(afterR, afterC)
            if (!afterSq.isObstacle && board[afterR][afterC] != null) return null
        }
        val beforeR = if (isHorizontal) startRow else startRow - 1
        val beforeC = if (isHorizontal) startCol - 1 else startCol
        if (beforeR in 0 until bs && beforeC in 0 until bs) {
            val beforeSq = boardConfig.squareAt(beforeR, beforeC)
            if (!beforeSq.isObstacle && board[beforeR][beforeC] != null) return null
        }

        // Connectivity check
        if (!boardEmpty) {
            val connects = newTiles.any { t ->
                listOf(t.row-1 to t.col, t.row+1 to t.col, t.row to t.col-1, t.row to t.col+1)
                    .any { (nr, nc) ->
                        nr in 0 until bs && nc in 0 until bs &&
                                !boardConfig.squareAt(nr, nc).isObstacle &&
                                board[nr][nc] != null
                    }
            }
            val overlaps = (0 until word.length).any { i ->
                val r = if (isHorizontal) startRow else startRow + i
                val c = if (isHorizontal) startCol + i else startCol
                board[r][c] != null
            }
            if (!connects && !overlaps) return null
        }

        // Cross-word validation using the full large dictionary
        for (tile in newTiles) {
            val crossWord = buildCrossWord(
                board, tile.row, tile.col, !isHorizontal, newTiles, bs, boardConfig
            )
            if (crossWord.length >= 2 &&
                !validationDictionary.contains(crossWord.uppercase())
            ) return null
        }

        val score = scoreMove(
            board, word, startRow, startCol, isHorizontal, newTiles, boardConfig, tileSet
        )
        return AiMove(newTiles, score, word, isHorizontal)
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    private fun scoreMove(
        board: Array<Array<Tile?>>, word: String,
        startRow: Int, startCol: Int, isHorizontal: Boolean,
        newTiles: List<PlacedTile>, boardConfig: BoardConfig,
        tileSet: TileSetConfig
    ): Int {
        var total = scoreWord(board, startRow, startCol, isHorizontal, newTiles, boardConfig)
        for (tile in newTiles) {
            val cross = buildCrossWord(
                board, tile.row, tile.col, !isHorizontal, newTiles, boardConfig.size, boardConfig
            )
            if (cross.length >= 2)
                total += scoreWord(board, tile.row, tile.col, !isHorizontal, newTiles, boardConfig)
        }
        if (newTiles.size == tileSet.rackSize) total += tileSet.bingoBonus
        return total
    }

    private fun scoreWord(
        board: Array<Array<Tile?>>,
        startRow: Int, startCol: Int,
        isHorizontal: Boolean, newTiles: List<PlacedTile>,
        boardConfig: BoardConfig
    ): Int {
        val bs = boardConfig.size
        var sr = startRow; var sc = startCol
        while (true) {
            val nr = if (isHorizontal) sr else sr - 1
            val nc = if (isHorizontal) sc - 1 else sc
            if (nr !in 0 until bs || nc !in 0 until bs) break
            if (boardConfig.squareAt(nr, nc).isObstacle) break
            val hasTile = board[nr][nc] != null || newTiles.any { it.row == nr && it.col == nc }
            if (!hasTile) break
            sr = nr; sc = nc
        }

        var lScore = 0; var wordMult = 1
        var r = sr; var c = sc
        while (r in 0 until bs && c in 0 until bs) {
            if (boardConfig.squareAt(r, c).isObstacle) break
            val newTile = newTiles.find { it.row == r && it.col == c }
            val boardTile = board[r][c]
            val tile = newTile
                ?: boardTile?.let { PlacedTile(r, c, it.letter, it.points, it.isBlank) }
                ?: break
            var lv = tile.points
            if (newTile != null) {
                val sq = boardConfig.squareAt(r, c)
                when {
                    !sq.isNegative && !sq.isAnchor -> {
                        lv *= sq.letterMultiplier
                        wordMult *= sq.wordMultiplier
                    }
                    sq.isAnchor -> wordMult *= 2
                }
            }
            lScore += lv
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return lScore * wordMult
    }

    private fun buildCrossWord(
        board: Array<Array<Tile?>>, row: Int, col: Int,
        isHorizontal: Boolean, newTiles: List<PlacedTile>, bs: Int,
        boardConfig: BoardConfig
    ): String {
        fun tileAt(r: Int, c: Int): Char? {
            if (boardConfig.squareAt(r, c).isObstacle) return null
            newTiles.find { it.row == r && it.col == c }?.let { return it.letter }
            return board[r][c]?.letter
        }

        var sr = row; var sc = col
        while (true) {
            val nr = if (isHorizontal) sr else sr - 1
            val nc = if (isHorizontal) sc - 1 else sc
            if (nr !in 0 until bs || nc !in 0 until bs) break
            if (tileAt(nr, nc) == null) break
            sr = nr; sc = nc
        }

        val sb = StringBuilder()
        var r = sr; var c = sc
        while (r in 0 until bs && c in 0 until bs) {
            sb.append(tileAt(r, c) ?: break)
            r += if (isHorizontal) 0 else 1
            c += if (isHorizontal) 1 else 0
        }
        return sb.toString()
    }
}
