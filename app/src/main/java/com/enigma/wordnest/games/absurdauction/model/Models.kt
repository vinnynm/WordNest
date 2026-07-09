package com.enigma.wordnest.games.absurdauction.model

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

data class DrawCandidate(
    val tiles: List<Char>,
    /** Heuristic score in place of a full move-search oracle — see LetterSynergy + RackScorer. */
    val rackQualityScore: Int,
    val vowelCount: Int,
    val consonantCount: Int
)

data class BankerDecision(
    val chosenTiles: List<Char>,
    val rejectedAlternativesCount: Int,
    val favoredPlayer: Int?,
    val pityOverrideApplied: Boolean = false
)

data class AuctionGameState(
    val board: Array<Array<Tile?>> = Array(15) { arrayOfNulls(15) },
    val players: List<Player> = emptyList(),
    val remainingPool: List<Char> = emptyList(),
    val currentPlayer: Int = 0,
    val bankerStance: BankerStance = BankerStance.EQUALIZING,
    val scoreDifferentialHistory: List<Int> = emptyList(),
    val consecutiveVowellessDraws: Map<Int, Int> = emptyMap(),
    val consecutiveConsonantlessDraws: Map<Int, Int> = emptyMap(),
    val lastDecision: BankerDecision? = null,
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
                bankerStance == other.bankerStance &&
                scoreDifferentialHistory == other.scoreDifferentialHistory &&
                consecutiveVowellessDraws == other.consecutiveVowellessDraws &&
                consecutiveConsonantlessDraws == other.consecutiveConsonantlessDraws &&
                lastDecision == other.lastDecision &&
                isBankerThinking == other.isBankerThinking &&
                isGameStarted == other.isGameStarted &&
                isGameOver == other.isGameOver
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + players.hashCode()
        result = 31 * result + remainingPool.hashCode()
        result = 31 * result + currentPlayer
        result = 31 * result + bankerStance.hashCode()
        result = 31 * result + scoreDifferentialHistory.hashCode()
        result = 31 * result + consecutiveVowellessDraws.hashCode()
        result = 31 * result + consecutiveConsonantlessDraws.hashCode()
        result = 31 * result + (lastDecision?.hashCode() ?: 0)
        result = 31 * result + isBankerThinking.hashCode()
        result = 31 * result + isGameStarted.hashCode()
        result = 31 * result + isGameOver.hashCode()
        return result
    }
}