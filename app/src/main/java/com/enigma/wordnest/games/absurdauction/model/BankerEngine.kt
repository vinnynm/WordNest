package com.enigma.wordnest.games.absurdauction.model

import com.enigma.wordnest.games.lexicon.model.BoardConfig
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * BankerEngine — the adversarial draw-selection brain for Absurd Auction.
 *
 * Directly modeled on AbsurdleEngine.process/chooseBucket: instead of bucketing
 * candidates by a Wordle-style pattern and picking the worst bucket for the
 * player, this buckets candidate *draws* by heuristics and picks the draw
 * that's worst for a leading player / best for a trailing one.
 *
 * IMPORTANT DISTINCTION (learned from playtesting): "rack quality" (how pleasant
 * / synergistic a rack feels to play) and "scoring potential" (how many points
 * it's actually likely to yield) are NOT the same axis. A rack of four different
 * common low-value letters (E, A, I, T) is high quality/synergy but low
 * scoring potential. A rack with an isolated Q or X is low quality but, if it
 * gets played at all, high scoring potential. Starving/favoring must target
 * SCORING POTENTIAL, not synergy — synergy is retained only as a tie-break and
 * for the hard-ratio/pity guardrails, which are about playability, not score.
 *
 * Phase 1 scope: BankerStance.NEUTRAL and EQUALIZING only. CHAOTIC and the
 * >2-player field-average comparison are not implemented yet — [decide] falls
 * back to a two-player leader/trailer comparison and treats CHAOTIC the same
 * as EQUALIZING for now (flagged below), pending a follow-up pass.
 */
object BankerEngine {

    private const val SAMPLE_COUNT = 16
    private const val LEAD_THRESHOLD = 30
    private const val PITY_DROUGHT_LIMIT = 2 // consecutive draws w/o a vowel or consonant

    /** Letters the design doc asked for explicitly: decent value (2-4pts), pairs well with
     * vowels, not intimidating like Q/X/Z/J. Used to sweeten a favored rack without
     * making it toothless (an all-1-point rack is still a bad time even when "easy"). */
    private val GENTLE_SCORERS = setOf('V', 'D', 'C', 'M')

    data class DrawContext(
        val remainingPool: List<Char>,
        val drawCount: Int,
        val drawingPlayerScore: Int,
        val opponentScore: Int,
        val stance: BankerStance,
        val consecutiveVowelless: Int,
        val consecutiveConsonantless: Int,
        val isOpeningDraw: Boolean,
        val random: Random = Random.Default
    )

    data class DrawOutcome(
        val decision: BankerDecision,
        val newPool: List<Char>,
        val newConsecutiveVowelless: Int,
        val newConsecutiveConsonantless: Int
    )

    /**
     * Resolves a single draw of [DrawContext.drawCount] tiles.
     */
    fun decide(ctx: DrawContext): DrawOutcome {
        require(ctx.remainingPool.size >= ctx.drawCount) { "Not enough tiles left in the pool" }

        // Opening draw: skip adversarial selection entirely, deal a fixed Perfect
        // Median rack (§2.4 item 5) so every player's first hand is neutral.
        if (ctx.isOpeningDraw) {
            val tiles = perfectMedianDraw(ctx.remainingPool, ctx.drawCount)
            val newPool = removeAll(ctx.remainingPool, tiles)
            return DrawOutcome(
                decision = BankerDecision(
                    tiles, rejectedAlternativesCount = 0, favoredPlayer = null,
                    role = DrawRole.OPENING, potentialDelta = 0
                ),
                newPool = newPool,
                newConsecutiveVowelless = updateStreak(tiles, ctx.consecutiveVowelless, LetterSynergy::isVowel),
                newConsecutiveConsonantless = updateStreak(tiles, ctx.consecutiveConsonantless) { !LetterSynergy.isVowel(it) }
            )
        }

        if (ctx.stance == BankerStance.NEUTRAL) {
            val tiles = sampleUniform(ctx.remainingPool, ctx.drawCount, ctx.random)
            val newPool = removeAll(ctx.remainingPool, tiles)
            return DrawOutcome(
                decision = BankerDecision(
                    tiles, rejectedAlternativesCount = 0, favoredPlayer = null,
                    role = DrawRole.UNIFORM_RANDOM, potentialDelta = 0
                ),
                newPool = newPool,
                newConsecutiveVowelless = updateStreak(tiles, ctx.consecutiveVowelless, LetterSynergy::isVowel),
                newConsecutiveConsonantless = updateStreak(tiles, ctx.consecutiveConsonantless) { !LetterSynergy.isVowel(it) }
            )
        }

        // 1. Sample candidate draws (weighted so rare letters aren't systematically excluded).
        var candidates = (0 until SAMPLE_COUNT).map {
            buildCandidate(sampleWeighted(ctx.remainingPool, ctx.drawCount, ctx.random))
        }

        // Hard ratio cap: discard degenerate all-vowel / all-consonant candidates outright.
        candidates = candidates.filterNot { isDegenerate(it) }.ifEmpty {
            // If everything got filtered (tiny endgame pool), fall back to the raw samples.
            (0 until SAMPLE_COUNT).map { buildCandidate(sampleWeighted(ctx.remainingPool, ctx.drawCount, ctx.random)) }
        }

        val averagePotential = candidates.map { it.scoringPotential }.average()

        // Pity timer: force a vowel/consonant after a drought, overriding stance for this draw.
        var pityApplied = false
        if (ctx.consecutiveVowelless >= PITY_DROUGHT_LIMIT) {
            val withVowel = candidates.filter { it.vowelCount > 0 }
            if (withVowel.isNotEmpty()) { candidates = withVowel; pityApplied = true }
        }
        if (ctx.consecutiveConsonantless >= PITY_DROUGHT_LIMIT) {
            val withConsonant = candidates.filter { it.consonantCount > 0 }
            if (withConsonant.isNotEmpty()) { candidates = withConsonant; pityApplied = true }
        }

        val chosen: DrawCandidate
        val role: DrawRole

        if (pityApplied) {
            // Pity override: pick close to median rather than continuing to punish/favor.
            chosen = candidates.sortedBy { it.scoringPotential }[candidates.size / 2]
            role = DrawRole.PITY_OVERRIDE
        } else {
            val diff = ctx.drawingPlayerScore - ctx.opponentScore
            // Primary sort key is SCORING POTENTIAL (fixes the leader-scores-more-anyway
            // bug); rack quality/synergy is only a tie-break so a "worst potential" pick
            // doesn't happen to also be a duplicate-letter mess that reads as obviously rigged.
            val sorted = candidates.sortedWith(
                compareBy<DrawCandidate> { it.scoringPotential }
                    .thenBy { it.rackQualityScore }
                    .thenBy { synergySignature(it) }
            )
            chosen = when {
                diff > LEAD_THRESHOLD -> sorted.first()   // leading -> lowest scoring potential
                diff < -LEAD_THRESHOLD -> sorted.last()   // trailing -> highest scoring potential
                else -> sorted[sorted.size / 2]           // close game -> avoid whiplash
            }
            role = when {
                diff > LEAD_THRESHOLD -> DrawRole.STARVED
                diff < -LEAD_THRESHOLD -> DrawRole.FAVORED
                else -> DrawRole.MEDIAN
            }
        }

        val newPool = removeAll(ctx.remainingPool, chosen.tiles)
        return DrawOutcome(
            decision = BankerDecision(
                chosenTiles = chosen.tiles,
                rejectedAlternativesCount = candidates.size - 1,
                favoredPlayer = null,
                pityOverrideApplied = pityApplied,
                role = role,
                potentialDelta = (chosen.scoringPotential - averagePotential).let { Math.round(it).toInt() }
            ),
            newPool = newPool,
            newConsecutiveVowelless = updateStreak(chosen.tiles, ctx.consecutiveVowelless, LetterSynergy::isVowel),
            newConsecutiveConsonantless = updateStreak(chosen.tiles, ctx.consecutiveConsonantless) { !LetterSynergy.isVowel(it) }
        )
    }

    // ── Rack Quality Scorer (§2.4 item 2) — playability, NOT scoring potential ──

    private fun buildCandidate(tiles: List<Char>): DrawCandidate {
        val upper = tiles.map { it.uppercaseChar() }
        val vowels = upper.count { LetterSynergy.isVowel(it) }
        val consonants = upper.size - vowels
        return DrawCandidate(
            tiles = tiles,
            rackQualityScore = scoreRack(upper, vowels, consonants),
            vowelCount = vowels,
            consonantCount = consonants,
            scoringPotential = scoringPotential(upper)
        )
    }

    private fun scoreRack(upper: List<Char>, vowels: Int, consonants: Int): Int {
        var score = 0
        val total = upper.size.coerceAtLeast(1)

        // Vowel/consonant ratio: penalize outside a healthy 40-60% vowel range.
        val vowelRatio = vowels.toDouble() / total
        score -= when {
            vowelRatio in 0.4..0.6 -> 0
            vowelRatio !in 0.2..0.8 -> 8
            else -> 3
        }

        // Duplicate-letter penalty.
        val freq = upper.groupingBy { it }.eachCount()
        freq.values.forEach { count -> if (count > 1) score -= (count - 1) * 3 }

        // Blend bonus for adjacent-pair synergy (order-independent, all pairs checked).
        for (i in upper.indices) for (j in i + 1 until upper.size) {
            score += LetterSynergy.blendBonus(upper[i], upper[j])
        }

        // Isolate penalty for high-value letters without a connector present.
        val upperSet = upper.toSet()
        upper.distinct().forEach { letter ->
            if (LetterSynergy.isIsolated(letter, upperSet)) score -= 5
        }

        return score
    }

    /**
     * Expected point yield of a rack — the metric that actually drives starve/favor
     * selection. Unlike [scoreRack] this is NOT about how pleasant the rack feels;
     * it's a rough estimate of how many points it can realistically produce:
     *
     *  - A letter's raw point value counts in full if it's a vowel or has a usable
     *    connector present elsewhere in the rack (i.e. it's likely to actually get
     *    played, not just sit dead in the rack).
     *  - An isolated high-point letter (no connector) counts at a steep discount —
     *    it LOOKS scary but is unlikely to be placed, so it shouldn't inflate the
     *    "starve the leader" target and accidentally reward them with a high-value
     *    rack anyway.
     *  - A small bonus applies for gentle mid-value scorers (V/D/C/M) so a
     *    "favored" rack isn't just an all-1-point vowel/common-consonant soup —
     *    it should feel winnable, not just harmless.
     */
    private fun scoringPotential(upper: List<Char>): Int {
        val upperSet = upper.toSet()
        var potential = 0.0
        for (ch in upper) {
            val value = BoardConfig.letterValues[ch] ?: 0
            val usable = LetterSynergy.isVowel(ch) || !LetterSynergy.isIsolated(ch, upperSet)
            if (usable) potential+=value else potential+=(value * 0.3)
        }
        potential += upper.count { it in GENTLE_SCORERS } * 2
        return potential.roundToInt()
    }

    private fun synergySignature(c: DrawCandidate) = c.tiles.sorted().joinToString("")

    private fun isDegenerate(c: DrawCandidate): Boolean {
        val total = c.tiles.size
        if (total < 5) return false // too small a rack for the ratio rule to be meaningful
        return c.vowelCount == total || c.consonantCount == total
    }

    // ── Sampling ────────────────────────────────────────────────────────────────

    private fun sampleUniform(pool: List<Char>, count: Int, random: Random): List<Char> =
        pool.shuffled(random).take(count)

    /**
     * Weighted sampling: shuffles the pool (preserving natural letter-frequency
     * proportions, since we sample from the real remaining multiset) rather than
     * applying any artificial letter-rarity weighting — availability stays fair
     * long-run; only which combination gets chosen among samples is adversarial.
     */
    private fun sampleWeighted(pool: List<Char>, count: Int, random: Random): List<Char> =
        pool.shuffled(random).take(count)

    /** Fixed, balanced opening rack: prefers a mid-vowel-ratio, low-duplicate, low-value-outlier draw. */
    private fun perfectMedianDraw(pool: List<Char>, count: Int): List<Char> {
        val attempts = (0 until 40).map { sampleUniform(pool, count, Random.Default) }
        return attempts.minByOrNull { candidate ->
            val upper = candidate.map { it.uppercaseChar() }
            val vowels = upper.count { LetterSynergy.isVowel(it) }
            val ratio = vowels.toDouble() / count.coerceAtLeast(1)
            val dupPenalty = upper.groupingBy { it }.eachCount().values.sumOf { if (it > 1) it - 1 else 0 }
            kotlin.math.abs(ratio - 0.5) * 10 + dupPenalty
        } ?: sampleUniform(pool, count, Random.Default)
    }

    private fun removeAll(pool: List<Char>, tiles: List<Char>): List<Char> {
        val remaining = pool.toMutableList()
        for (t in tiles) remaining.remove(t)
        return remaining
    }

    private fun updateStreak(tiles: List<Char>, current: Int, matches: (Char) -> Boolean): Int =
        if (tiles.any { matches(it.uppercaseChar()) }) 0 else current + 1
}
