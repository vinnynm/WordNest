package com.enigma.wordnest.games.synthetix.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  AiStats — records observed AI performance per difficulty level.
//  Persisted to SharedPreferences so tuning data accumulates across sessions.
// ─────────────────────────────────────────────────────────────────────────────

data class DifficultyStats(
    val difficulty: AiDifficulty,
    val gamesPlayed: Int       = 0,
    val totalAiScore: Int      = 0,
    val totalHumanScore: Int   = 0,
    val totalTurns: Int        = 0,
    val totalAiScorePerTurn: Float = 0f,
    // Running exchange threshold — starts at the enum default, adapts over time
    val currentExchangeThreshold: Int = -1  // -1 = use AiDifficulty.exchangeThreshold default
) {
    val aiWinRate: Float
        get() = if (gamesPlayed == 0) 0.5f else totalAiScore.toFloat() / (totalAiScore + totalHumanScore + 1)

    val avgAiScorePerTurn: Float
        get() = if (totalTurns == 0) 0f else totalAiScorePerTurn / totalTurns

    val effectiveExchangeThreshold: Int
        get() = if (currentExchangeThreshold < 0) difficulty.exchangeThreshold else currentExchangeThreshold
}

data class AiStatsSnapshot(
    val easy: DifficultyStats   = DifficultyStats(AiDifficulty.EASY),
    val medium: DifficultyStats = DifficultyStats(AiDifficulty.MEDIUM),
    val hard: DifficultyStats   = DifficultyStats(AiDifficulty.HARD),
    val expert: DifficultyStats = DifficultyStats(AiDifficulty.EXPERT)
) {
    fun forDifficulty(d: AiDifficulty) = when (d) {
        AiDifficulty.EASY   -> easy
        AiDifficulty.MEDIUM -> medium
        AiDifficulty.HARD   -> hard
        AiDifficulty.EXPERT -> expert
    }

    fun withUpdated(stats: DifficultyStats) = when (stats.difficulty) {
        AiDifficulty.EASY   -> copy(easy   = stats)
        AiDifficulty.MEDIUM -> copy(medium = stats)
        AiDifficulty.HARD   -> copy(hard   = stats)
        AiDifficulty.EXPERT -> copy(expert = stats)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Target win rates per difficulty
//  Easy: AI should win ~30% of the time (mostly loses)
//  Medium: ~45% (slight human advantage)
//  Hard: ~60% (slight AI advantage)
//  Expert: ~75% (strong AI advantage)
// ─────────────────────────────────────────────────────────────────────────────

private val targetWinRates = mapOf(
    AiDifficulty.EASY   to 0.30f,
    AiDifficulty.MEDIUM to 0.45f,
    AiDifficulty.HARD   to 0.60f,
    AiDifficulty.EXPERT to 0.75f
)

// Min games before we trust the stats enough to tune
private const val MIN_GAMES_FOR_TUNING = 3

// ─────────────────────────────────────────────────────────────────────────────
//  AiDifficultyBalancer
//  After each game, records the outcome and optionally nudges the exchange
//  threshold to push win rates toward target values.
// ─────────────────────────────────────────────────────────────────────────────

class AiDifficultyBalancer(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("synthetix_ai_stats", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var _snapshot = load()
    val snapshot: AiStatsSnapshot get() = _snapshot

    // ── Record a finished game ────────────────────────────────────────────────

    /**
     * Call once when an AI game ends.
     * [aiScore] and [humanScore] are final scores.
     * [aiTurns] is how many turns the AI took.
     * [aiTotalPoints] is sum of all points scored per AI move.
     */
    fun recordGame(
        difficulty: AiDifficulty,
        aiScore: Int,
        humanScore: Int,
        aiTurns: Int,
        aiTotalPoints: Int
    ) {
        val current = _snapshot.forDifficulty(difficulty)
        val updated = current.copy(
            gamesPlayed         = current.gamesPlayed + 1,
            totalAiScore        = current.totalAiScore + aiScore,
            totalHumanScore     = current.totalHumanScore + humanScore,
            totalTurns          = current.totalTurns + aiTurns,
            totalAiScorePerTurn = current.totalAiScorePerTurn + aiTotalPoints
        )
        val tuned = tune(updated)
        _snapshot = _snapshot.withUpdated(tuned)
        save()
    }

    /**
     * Returns the (possibly tuned) exchange threshold for a given difficulty.
     * Use this when constructing [AiOpponent] instead of the enum constant.
     */
    fun exchangeThreshold(difficulty: AiDifficulty): Int =
        _snapshot.forDifficulty(difficulty).effectiveExchangeThreshold

    /**
     * Human-readable summary for a debug/settings screen.
     */
    fun summaryLines(): List<String> = AiDifficulty.entries.map { d ->
        val s = _snapshot.forDifficulty(d)
        val target = targetWinRates[d] ?: 0.5f
        buildString {
            append("${d.displayName}: ")
            if (s.gamesPlayed == 0) {
                append("no data yet")
            } else {
                append("${s.gamesPlayed} games, ")
                append("win ${(s.aiWinRate * 100).roundToInt()}% ")
                append("(target ${(target * 100).roundToInt()}%), ")
                append("threshold=${s.effectiveExchangeThreshold}")
            }
        }
    }

    /**
     * Reset all tuning data — useful for testing or user request.
     */
    fun reset() {
        _snapshot = AiStatsSnapshot()
        save()
    }

    // ── Tuning logic ──────────────────────────────────────────────────────────

    /**
     * After MIN_GAMES_FOR_TUNING games, nudge the exchange threshold.
     *
     * Exchange threshold controls how willing the AI is to swap tiles vs play.
     * - AI winning TOO often (above target) → lower the threshold so the AI
     *   accepts lower-scoring plays instead of banking on a better rack →
     *   slightly weaker play.
     * - AI winning TOO RARELY (below target) → raise the threshold so the AI
     *   swaps more aggressively, getting better tiles → slightly stronger play.
     *
     * The nudge is capped at ±2 per game to prevent oscillation.
     */
    private fun tune(stats: DifficultyStats): DifficultyStats {
        if (stats.gamesPlayed < MIN_GAMES_FOR_TUNING) return stats

        val target    = targetWinRates[stats.difficulty] ?: return stats
        val actual    = stats.aiWinRate
        val delta     = actual - target          // positive = AI winning too much
        val threshold = stats.effectiveExchangeThreshold

        // Only adjust if the deviation is significant (>5 pp)
        if (abs(delta) < 0.05f) return stats

        // Nudge: winning too much → lower threshold (harder to get exchange bonus)
        //        winning too little → raise threshold (AI exchanges tiles more)
        val nudge = when {
            delta >  0.15f -> -2   // AI much too strong: lower by 2
            delta >  0.05f -> -1   // AI slightly too strong: lower by 1
            delta < -0.15f ->  2   // AI much too weak: raise by 2
            else           ->  1   // AI slightly too weak: raise by 1
        }

        // Clamp to a reasonable range per difficulty so it can't go wild
        val (minT, maxT) = when (stats.difficulty) {
            AiDifficulty.EASY   ->  2 to 14
            AiDifficulty.MEDIUM ->  6 to 18
            AiDifficulty.HARD   -> 10 to 24
            AiDifficulty.EXPERT -> 16 to 30
        }

        return stats.copy(
            currentExchangeThreshold = (threshold + nudge).coerceIn(minT, maxT)
        )
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun save() {
        prefs.edit { putString("stats", gson.toJson(_snapshot)) }
    }

    private fun load(): AiStatsSnapshot {
        val json = prefs.getString("stats", null) ?: return AiStatsSnapshot()
        return try {
            gson.fromJson(json, AiStatsSnapshot::class.java)
        } catch (e: Exception) {
            AiStatsSnapshot()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  In-game AI score tracker — used to feed data into AiDifficultyBalancer
// ─────────────────────────────────────────────────────────────────────────────

class AiGameTracker {
    var turns: Int = 0
        private set
    var totalPoints: Int = 0
        private set

    fun recordTurn(score: Int) {
        turns++
        totalPoints += score
    }

    fun reset() { turns = 0; totalPoints = 0 }
}
