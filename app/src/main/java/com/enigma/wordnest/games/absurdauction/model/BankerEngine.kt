package com.enigma.wordnest.games.absurdauction.model

import kotlin.random.Random

/**
 * BankerEngine — the adversarial draw-selection brain for Absurd Auction.
 *
 * Directly modeled on AbsurdleEngine.process/chooseBucket: instead of bucketing
 * candidates by a Wordle-style pattern and picking the worst bucket for the
 * player, this buckets candidate *draws* by a cheap rack-quality heuristic and
 * picks the draw that's worst for a leading player / best for a trailing one.
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
                decision = BankerDecision(tiles, rejectedAlternativesCount = 0, favoredPlayer = null),
                newPool = newPool,
                newConsecutiveVowelless = updateStreak(tiles, ctx.consecutiveVowelless, LetterSynergy::isVowel),
                newConsecutiveConsonantless = updateStreak(tiles, ctx.consecutiveConsonantless) { !LetterSynergy.isVowel(it) }
            )
        }

        if (ctx.stance == BankerStance.NEUTRAL) {
            val tiles = sampleUniform(ctx.remainingPool, ctx.drawCount, ctx.random)
            val newPool = removeAll(ctx.remainingPool, tiles)
            return DrawOutcome(
                decision = BankerDecision(tiles, rejectedAlternativesCount = 0, favoredPlayer = null),
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
        val favoredPlayer: Int?

        if (pityApplied) {
            // Pity override: pick close to median quality rather than continuing to punish/favor.
            chosen = candidates.sortedBy { it.rackQualityScore }[candidates.size / 2]
            favoredPlayer = null
        } else {
            val diff = ctx.drawingPlayerScore - ctx.opponentScore
            val sorted = candidates.sortedWith(
                compareBy<DrawCandidate> { it.rackQualityScore }.thenBy { synergySignature(it) }
            )
            chosen = when {
                diff > LEAD_THRESHOLD -> sorted.first()   // leading -> worst rack
                diff < -LEAD_THRESHOLD -> sorted.last()   // trailing -> best rack
                else -> sorted[sorted.size / 2]           // close game -> avoid whiplash
            }
            favoredPlayer = when {
                diff > LEAD_THRESHOLD -> null       // starved, not favored
                diff < -LEAD_THRESHOLD -> null       // favored player is the drawer themself; caller knows who's drawing
                else -> null
            }
        }

        val newPool = removeAll(ctx.remainingPool, chosen.tiles)
        return DrawOutcome(
            decision = BankerDecision(
                chosenTiles = chosen.tiles,
                rejectedAlternativesCount = candidates.size - 1,
                favoredPlayer = favoredPlayer,
                pityOverrideApplied = pityApplied
            ),
            newPool = newPool,
            newConsecutiveVowelless = updateStreak(chosen.tiles, ctx.consecutiveVowelless, LetterSynergy::isVowel),
            newConsecutiveConsonantless = updateStreak(chosen.tiles, ctx.consecutiveConsonantless) { !LetterSynergy.isVowel(it) }
        )
    }

    // ── Rack Quality Scorer (§2.4 item 2) ──────────────────────────────────────

    private fun buildCandidate(tiles: List<Char>): DrawCandidate {
        val upper = tiles.map { it.uppercaseChar() }
        val vowels = upper.count { LetterSynergy.isVowel(it) }
        val consonants = upper.size - vowels
        return DrawCandidate(
            tiles = tiles,
            rackQualityScore = scoreRack(upper, vowels, consonants),
            vowelCount = vowels,
            consonantCount = consonants
        )
    }

    private fun scoreRack(upper: List<Char>, vowels: Int, consonants: Int): Int {
        var score = 0
        val total = upper.size.coerceAtLeast(1)

        // Vowel/consonant ratio: penalize outside a healthy 40-60% vowel range.
        val vowelRatio = vowels.toDouble() / total
        score -= when {
            vowelRatio in 0.4..0.6 -> 0
            vowelRatio < 0.2 || vowelRatio > 0.8 -> 8
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