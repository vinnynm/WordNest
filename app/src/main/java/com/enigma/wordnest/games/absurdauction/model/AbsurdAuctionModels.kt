package com.enigma.wordnest.games.absurdauction.model

import com.enigma.wordnest.games.lexicon.model.PlacedTile
import com.enigma.wordnest.games.lexicon.model.Player
import com.enigma.wordnest.games.lexicon.model.Tile

/**
 * Absurd Auction — design doc §2. Standard Lexicon board/scoring/dictionary, but
 * draws are resolved by an adversarial "Banker" instead of a uniform-random bag.
 *
 * Phase 1 scope (per current build plan): [BankerStance.NEUTRAL] and
 * [BankerStance.EQUALIZING] only. CHAOTIC and the >2-player field-average
 * comparison are deferred to a later pass — this file leaves room for them
 * (the enum entry and state shape already exist) without wiring their logic yet.
 */
enum class BankerStance { NEUTRAL, EQUALIZING, CHAOTIC }

/** What the Banker was doing on a given draw — surfaced directly in the UI so
 * "is it helping or hurting right now" is never a mystery, not just a one-off toast. */
enum class DrawRole {
    OPENING,          // fixed Perfect Median rack, pre-adversarial
    UNIFORM_RANDOM,   // BankerStance.NEUTRAL — true random, engine disabled
    STARVED,          // drawing player was leading — given lowest scoring-potential rack
    FAVORED,          // drawing player was trailing — given highest scoring-potential rack
    MEDIAN,           // scores were close — picked near the middle to avoid whiplash
    PITY_OVERRIDE     // vowel/consonant drought forced a correction, overriding stance
}

data class DrawCandidate(
    val tiles: List<Char>,
    /** Heuristic score in place of a full move-search oracle — see LetterSynergy + RackScorer. */
    val rackQualityScore: Int,
    val vowelCount: Int,
    val consonantCount: Int,
    /** Expected point yield — the metric that actually drives starve/favor selection.
     * Distinct from [rackQualityScore]: a "high quality" rack can have LOW scoring
     * potential (e.g. four different common 1-point letters) and vice versa. */
    val scoringPotential: Int = 0
)

data class BankerDecision(
    val chosenTiles: List<Char>,
    val rejectedAlternativesCount: Int,
    val favoredPlayer: Int?,
    val pityOverrideApplied: Boolean = false,
    val role: DrawRole = DrawRole.MEDIAN,
    /** Chosen rack's scoringPotential minus the average across sampled candidates —
     * a signed magnitude for "how hard did the Banker lean this draw." */
    val potentialDelta: Int = 0
)

data class AuctionGameState(
    val board: Array<Array<Tile?>> = Array(15) { arrayOfNulls(15) },
    val players: List<Player> = emptyList(),
    val remainingPool: List<Char> = emptyList(),
    val currentPlayer: Int = 0,
    /** Tiles placed on the board THIS turn but not yet submitted via playWord() —
     * must be rendered by the board UI immediately on placement, not only after
     * commit, or placed tiles appear to vanish until the play is submitted. */
    val placedThisTurn: List<PlacedTile> = emptyList(),
    val bankerStance: BankerStance = BankerStance.EQUALIZING,
    val scoreDifferentialHistory: List<Int> = emptyList(),
    val consecutiveVowellessDraws: Map<Int, Int> = emptyMap(),
    val consecutiveConsonantlessDraws: Map<Int, Int> = emptyMap(),
    val lastDecision: BankerDecision? = null,
    /** Who the last decision was drawn for — needed since BankerDecision itself
     * doesn't know which player it applied to. */
    val lastDecisionPlayerIndex: Int? = null,
    /** Running total of potentialDelta per player index — "the Banker has net
     * helped/hurt this player by N points of scoring potential so far." Answers
     * "was it actually helping" without requiring the player to track it mentally. */
    val cumulativeImpact: Map<Int, Int> = emptyMap(),
    val isBankerThinking: Boolean = false,
    val isGameStarted: Boolean = false,
    val isGameOver: Boolean = false
) {
    // Array has reference equality by default; override so StateFlow's
    // distinctUntilChanged compares content (same fix applied to LadderClaimState).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuctionGameState) return false
        return board.contentDeepEquals(other.board) &&
            players == other.players &&
            remainingPool == other.remainingPool &&
            currentPlayer == other.currentPlayer &&
            placedThisTurn == other.placedThisTurn &&
            bankerStance == other.bankerStance &&
            scoreDifferentialHistory == other.scoreDifferentialHistory &&
            consecutiveVowellessDraws == other.consecutiveVowellessDraws &&
            consecutiveConsonantlessDraws == other.consecutiveConsonantlessDraws &&
            lastDecision == other.lastDecision &&
            lastDecisionPlayerIndex == other.lastDecisionPlayerIndex &&
            cumulativeImpact == other.cumulativeImpact &&
            isBankerThinking == other.isBankerThinking &&
            isGameStarted == other.isGameStarted &&
            isGameOver == other.isGameOver
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + players.hashCode()
        result = 31 * result + remainingPool.hashCode()
        result = 31 * result + currentPlayer
        result = 31 * result + placedThisTurn.hashCode()
        result = 31 * result + bankerStance.hashCode()
        result = 31 * result + scoreDifferentialHistory.hashCode()
        result = 31 * result + consecutiveVowellessDraws.hashCode()
        result = 31 * result + consecutiveConsonantlessDraws.hashCode()
        result = 31 * result + (lastDecision?.hashCode() ?: 0)
        result = 31 * result + (lastDecisionPlayerIndex ?: -1)
        result = 31 * result + cumulativeImpact.hashCode()
        result = 31 * result + isBankerThinking.hashCode()
        result = 31 * result + isGameStarted.hashCode()
        result = 31 * result + isGameOver.hashCode()
        return result
    }
}
