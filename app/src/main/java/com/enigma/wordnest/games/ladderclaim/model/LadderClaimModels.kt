package com.enigma.wordnest.games.ladderclaim.model

enum class ColorOutcome { FULL, PARTIAL, NEUTRAL }

/**
 * How a successful play translates into claimed board tiles.
 *
 *  OWN_TILES    — original design (§1.4/§1.6): the player's OWN newly-placed tiles get
 *                 colored based on the match against the chosen target, and a fully-neutral
 *                 target word can flip entirely to the mover's color ("neutral theft").
 *  TARGET_TILES — reworked flavour: newly placed tiles are ALWAYS neutral. Instead, the
 *                 matching cells on the TARGET word itself are what get claimed. A cell
 *                 already owned by the current mover is a no-op. A cell owned by an
 *                 opponent can only be reclaimed if a word formed THIS turn crosses that
 *                 exact cell perpendicular to the target, and that crossing word is not
 *                 itself solely owned by the same opponent elsewhere.
 */
enum class ClaimMode { OWN_TILES, TARGET_TILES, FAIR_CLAIM }

/**
 * fullCreditBar: matches needed for FULL is (longerLength - fullCreditBar).
 * Default 1 = the doc's "L-1" rule; Generous relaxes it to L-2.
 */
enum class LadderClaimVariant(
    val displayName: String,
    val maxTurns: Int,
    val fullCreditBar: Int,
    val allowNeutralTheft: Boolean,
    val claimMode: ClaimMode = ClaimMode.OWN_TILES
) {
    QUICKFIRE("Quickfire", maxTurns = 10, fullCreditBar = 1, allowNeutralTheft = true, claimMode = ClaimMode.OWN_TILES),
    CLASSIC("Classic",     maxTurns = 20, fullCreditBar = 1, allowNeutralTheft = true, claimMode = ClaimMode.OWN_TILES),
    GENEROUS("Generous",   maxTurns = 20, fullCreditBar = 2, allowNeutralTheft = true, claimMode = ClaimMode.OWN_TILES),
    STRICT("Strict",       maxTurns = 20, fullCreditBar = 1, allowNeutralTheft = false, claimMode = ClaimMode.OWN_TILES),
    TARGET_STRIKE("Target Strike", maxTurns = 20, fullCreditBar = 1, allowNeutralTheft = true, claimMode = ClaimMode.TARGET_TILES),

    /** New mode: matched cells only. Your own newly-placed tiles still color per match,
     *  AND the target's matched cells claim too — but only where those target cells are
     *  currently neutral. No whole-word theft, and opponent-owned cells are never touched. */
    FAIR_CLAIM("Fair Claim", maxTurns = 20, fullCreditBar = 1, allowNeutralTheft = true, claimMode = ClaimMode.FAIR_CLAIM)
}

data class LadderTile(
    val row: Int,
    val col: Int,
    val letter: Char,
    val ownerId: Int? = null,       // null = neutral/seed
    val turnClaimed: Int = 0
)

data class PlacedLadderTile(val row: Int, val col: Int, val letter: Char)

data class LadderWord(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val isHorizontal: Boolean,
    val ownerId: Int? = null
) {
    val id: String get() = "$startRow,$startCol,$isHorizontal"
}

data class LadderPlayResult(
    val targetWord: LadderWord?,
    val outcome: ColorOutcome,
    /** Indices within the TARGET word (TARGET_TILES mode) or within the NEW word
     *  (OWN_TILES mode) that were colored/claimed by this play. */
    val matchedIndices: Set<Int>,
    val targetWordConverted: Boolean,
    /** How many target cells were actually claimed this play (TARGET_TILES mode only;
     *  0 for OWN_TILES, where whole-tile coloring is the unit of account instead). */
    val claimedCellCount: Int = 0
)

data class LadderPlayer(
    val name: String,
    val rack: List<Char>,
    val isAi: Boolean = false
)

sealed class LadderMoveResult {
    data class Success(val result: LadderPlayResult, val playedWord: String) : LadderMoveResult()
    data class Error(val message: String) : LadderMoveResult()
}

data class LadderClaimStats(
    val gamesPlayed: Int = 0,
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val ties: Int = 0
)

data class LadderClaimState(
    val board: Array<Array<LadderTile?>> = Array(15) { arrayOfNulls(15) },
    val words: List<LadderWord> = emptyList(),
    val players: List<LadderPlayer> = emptyList(),
    val currentPlayer: Int = 0,
    val placedThisTurn: List<PlacedLadderTile> = emptyList(),
    val selectedTile: Int? = null,
    val selectedTargetWordId: String? = null,
    /** Non-empty when a tapped cell belongs to 2 words (across + down) and the player must disambiguate. */
    val pendingTargetChoiceIds: List<String> = emptyList(),
    val variant: LadderClaimVariant = LadderClaimVariant.CLASSIC,
    val turnCount: Int = 0,
    val maxTurns: Int = 20,
    val isGameOver: Boolean = false,
    val isGameStarted: Boolean = false,
    val lastMessage: String = "",
    val lastOutcome: ColorOutcome? = null,
    val isVsAi: Boolean = false,
    val isAiThinking: Boolean = false
) {
    val isActive: Boolean get() = isGameStarted && !isGameOver

    // Array has reference equality by default — override so StateFlow's
    // distinctUntilChanged actually compares content (same fix the Lexicon
    // audit flagged GameUiState as needing; applying it here from the start).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LadderClaimState) return false
        return board.contentDeepEquals(other.board) &&
                words == other.words && players == other.players &&
                currentPlayer == other.currentPlayer && placedThisTurn == other.placedThisTurn &&
                selectedTile == other.selectedTile && selectedTargetWordId == other.selectedTargetWordId &&
                pendingTargetChoiceIds == other.pendingTargetChoiceIds && variant == other.variant &&
                turnCount == other.turnCount && maxTurns == other.maxTurns &&
                isGameOver == other.isGameOver && isGameStarted == other.isGameStarted &&
                lastMessage == other.lastMessage && lastOutcome == other.lastOutcome &&
                isVsAi == other.isVsAi && isAiThinking == other.isAiThinking
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + words.hashCode()
        result = 31 * result + players.hashCode()
        result = 31 * result + currentPlayer
        result = 31 * result + placedThisTurn.hashCode()
        result = 31 * result + (selectedTile ?: 0)
        result = 31 * result + (selectedTargetWordId?.hashCode() ?: 0)
        result = 31 * result + pendingTargetChoiceIds.hashCode()
        result = 31 * result + variant.hashCode()
        result = 31 * result + turnCount
        result = 31 * result + maxTurns
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + isGameStarted.hashCode()
        result = 31 * result + lastMessage.hashCode()
        result = 31 * result + (lastOutcome?.hashCode() ?: 0)
        result = 31 * result + isVsAi.hashCode()
        result = 31 * result + isAiThinking.hashCode()
        return result
    }
}
