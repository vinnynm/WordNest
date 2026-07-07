# WordNest — Hybrid Game Concepts: Design Document

_Five original word-game concepts (plus one repositioned as a challenge-mode add-on), each blending existing WordNest mechanics or extending its dictionary infrastructure into something new. Written as a starting spec for implementation — architecture follows the established WordNest pattern (per-game `WordRepository` + `AndroidViewModel`/`StateFlow` + Compose UI split)._

---

## Table of Contents

1. [Ladder Claim](#1-ladder-claim) — Word Ladder + Lexicon (territory control)
2. [Absurd Auction](#2-absurd-auction) — Absurdle + Lexicon (adversarial tile economy)
3. [Category Lexicon](#3-category-lexicon) — Connections + Lexicon (hidden-grouping challenge mode)
4. [Crossword](#4-crossword) — dictionary-backed generated crossword puzzles
5. [Codeword](#5-codeword) — number-substitution decoding puzzle
6. [Shared Infrastructure Notes](#6-shared-infrastructure-notes)

---

## 1. Ladder Claim

**Elevator pitch:** Scrabble-style tile placement — any valid dictionary word connecting to the board is a legal play, so the game never stalls. But if your word is also an edit-distance-legal "ladder move" against a board word you point at, your new tiles convert to your color. The closer your play is to a true one-edit ladder move, the more of your tiles change color — down to zero for a word that shares nothing with its target. This keeps the tile-scarcity tension of Scrabble while layering the ladder concept on as a reward, not a requirement.

### 1.1 Core Loop

1. Board starts with one **seed word** placed in the center (e.g., a random 4-letter word), colored neutral/gray.
2. On your turn:
    - Place tiles from your rack to form **any valid dictionary word** that legally connects to the board (standard Scrabble/Lexicon adjacency rules — no ladder requirement to make a legal move at all).
    - **Select one existing board word as your target** (tap/point to it in the UI — see 1.5). This is mandatory; it determines how your new tiles are colored.
3. The engine compares your new word against the target word (see 1.4 for the exact rule) and colors your newly placed tiles accordingly — anywhere from full color to fully neutral.
4. If your play matches the target closely enough to earn at least partial color, and the target word is currently **neutral** (not owned by any player), the *entire target word* also converts to your color (see 1.6).
5. Game ends when the bag is empty and no player can make a legal move, or after a fixed round count (configurable, default 20 turns/player).
6. **Winner:** player controlling the most claimed tiles on the board.

### 1.2 Why this is different

Standard Scrabble scores letters. Word Ladder scores shortest-path cleverness. Neither is spatial or adversarial about territory. Ladder Claim turns the ladder relationship into a **graded, spatial reward layer sitting on top of ordinary Scrabble legality** — you always have a legal move if a Scrabble player would, but *how much territory you earn* depends on how cleverly you can land a near-ladder-legal word against a target of your choosing. A "safe" neutral play is a real tactical option (dump bad tiles, bait a target for later, deny an opponent's setup) rather than a failure state.

### 1.3 Components / Data Model

```kotlin
enum class ColorOutcome { FULL, PARTIAL, NEUTRAL }

data class LadderTile(
    val row: Int,
    val col: Int,
    val letter: Char,
    val ownerId: Int?,          // null = neutral/seed, else player index
    val turnClaimed: Int        // for tie-break / animation ordering
)

data class LadderWord(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val isHorizontal: Boolean,
    val ownerId: Int?           // null = neutral (unclaimed / seed / left-over from a neutral play)
)

data class LadderPlayResult(
    val targetWord: LadderWord,
    val outcome: ColorOutcome,
    val matchingTileCount: Int,      // for PARTIAL, how many newly-placed tiles get colored
    val targetWordConverted: Boolean // true if a neutral target flips entirely to the player's color
)

data class LadderClaimState(
    val board: Array<Array<LadderTile?>>,      // 15x15, same convention as Lexicon
    val words: List<LadderWord>,                // all words ever placed, for target selection + lookups
    val players: List<LadderPlayer>,
    val currentPlayer: Int,
    val bag: MutableList<Char>,
    val turnCount: Int,
    val maxTurns: Int = 20,
    val isGameOver: Boolean = false
)

data class LadderPlayer(
    val name: String,
    val rack: List<Char>,
    val tilesControlled: Int = 0   // derived/cached from board scan
)
```

### 1.4 Move Legality & Coloring Rule

**Legality (does this play happen at all):** identical to standard Lexicon/Scrabble — the new word must be in the dictionary and must connect to the board (share/extend from at least one existing tile, standard `connectsToBoard()` check). There is **no ladder requirement to make a legal move.** This guarantees a legal move always exists whenever ordinary Scrabble would allow one — the dead-turn problem from the original design is fully resolved.

**Coloring (how much of the new word gets your color):** the player selects **one existing board word** as the target for this play. Let `L` be the length of the *longer* of the two words (target vs. new word). Compare:

| Relationship between new word and target | Outcome |
|---|---|
| New word is a **true single-edit-distance move** from the target — i.e. **same length with exactly one substituted letter (matches `L-1` positions)**, or **one letter longer/shorter via a single insertion/deletion (`L+1`/`L-1` relationship)** | **FULL** — all newly placed tiles are colored |
| New word is the same length as the target but matches **fewer than `L-1`** positions, or is longer/shorter by exactly one letter but the "best alignment" (see below) matches fewer than `L-1` shared letters | **PARTIAL** — only the newly-placed tiles that land in a matching position are colored; the rest of the newly placed tiles stay neutral. Existing board tiles are never recolored either way. |
| New word shares **zero** matching letters with the target in any alignment | **NEUTRAL** — no new tiles are colored, no target conversion (see 1.6) |

**Alignment rule for length-differing words:** when the new word is one letter longer or shorter than the target, test every possible insertion/deletion offset and use whichever alignment yields the **most matching positions** — always give the player the most generous valid reading. Use this same best-alignment result to decide whether the `L-1`-match bar (for FULL) is met.

**Worked examples (target: MANE):**
- Play **MATE** → same length, 3 of 4 positions match (`M_A_?_E` mismatch only at position 3) → `L-1 = 3` met → **FULL**, all 4 new tiles colored.
- Play **MAST** → same length, matches only M and A (2 of 4) → below `L-1` → **PARTIAL**, only the M and A tiles color.
- Play **MEAL** → same length, matches only M (1 of 4) → **PARTIAL**, only the M tile colors.
- Play **MANES** → one letter longer; best alignment matches all 4 original letters (`L-1 = 4` of the 5-length word) → **FULL**.
- Play **MANTLE** → one letter longer by more than 1 net edit relative to MANE isn't a valid comparison target at all here (length differs by 2) — such a play simply can't reference MANE as a target; the player would need to pick a different, closer-length target word, or accept **NEUTRAL** by targeting MANE anyway with zero credit.
- Play **CRISP** vs target MANE → zero shared-position letters → **NEUTRAL**.

### 1.5 Target Selection (UI)

Since coloring now depends on a manually chosen comparison word, the player must explicitly indicate which board word they're playing against:

- After placing tiles (before submitting), the player taps an existing board word to highlight it as their **target**.
- Only one target may be selected per turn.
- If no target is selected, the play defaults to **NEUTRAL** (legal, scores no territory) — useful for a pure "dump tiles" turn.
- The UI should preview the outcome (FULL/PARTIAL/NEUTRAL and which tiles would color) before the player commits, so this is a visible, informed choice rather than a guess.

### 1.6 Neutral-Tile Theft

To add a late-game "juicy target" layer: if a play earns **FULL or PARTIAL** color (i.e., at least one matching tile) **and the target word is currently neutral** (unowned — includes the original seed word or any prior NEUTRAL play sitting on the board), the **entire target word converts to the playing player's color**, not just the newly placed tiles.

- This does **not** apply to a target word already owned by another player — claimed territory is protected and can't be stolen this way (no separate "attack" mechanic on owned words in v1).
- This makes a deliberately NEUTRAL play from an earlier turn genuine bait: it sits on the board unclaimed until someone lands even a single matching letter against it, at which point it flips entirely.
- Because the theft requires only "at least one matching tile," this is easy to trigger — intentional, since it rewards paying attention to neutral words on the board rather than only chasing high-value ladder chains.

### 1.7 Turn Structure

1. Player selects tiles from rack, places on board (same UI interaction as Lexicon: select tile, tap cell, recall, etc.).
2. Player taps a target board word (optional — omitting it guarantees a NEUTRAL outcome).
3. UI previews the coloring outcome.
4. On submit:
    - Validate dictionary word + board connection (standard Scrabble legality).
    - If invalid: show specific reason ("Doesn't connect to the board", "Not in dictionary").
    - If valid: commit tiles, compute FULL/PARTIAL/NEUTRAL outcome against the chosen target, apply tile coloring + neutral-theft conversion if applicable, draw replacement tiles from bag.
5. Turn passes. Track consecutive-pass count for stalemate detection (same 4-pass-ends-game rule as Lexicon).

### 1.8 Scoring / Win Condition

- No letter-point scoring at all (a deliberate departure from Scrabble-style value).
- End-of-game tally: count board tiles by `ownerId`.
- Tiebreaker 1: player with the longest single unbroken owned chain (tiles of their color that are board-adjacent to each other).
- Tiebreaker 2: player with fewest tiles remaining in rack (rewards efficient play).

### 1.9 Difficulty / Variants

| Variant | Change |
|---|---|
| **Quickfire** | maxTurns = 10 (faster games, same coloring rules) |
| **Classic** | maxTurns = 20 (default) |
| **Generous** | Lower the FULL-credit bar from `L-1` to `L-2` matches — easier to earn full color, faster territory swings |
| **Strict** | Disable neutral-tile theft entirely — territory can only be earned via direct new-tile coloring, never by flipping a whole existing word |

### 1.10 Reused Components

| Component | Source | Reuse |
|---|---|---|
| Board grid rendering, premium squares (optional, could reuse or strip) | `lexicon/ui/components/Board.kt` | Board UI skeleton, extended with tap-to-target selection |
| Rack UI, tile drag/select | `lexicon/ui/components/Rack.kt` | Rack interaction |
| Bag build/draw, tile distribution, connects-to-board check | `ScrabbleGame` `buildBag()`/`drawNewTiles()`/`connectsToBoard()` | Tile economy + legality, unmodified |
| Dictionary loading | `OptimizedWordRepository` | Word validity checks |
| Edit-distance / position-matching logic | Adapted from `AbsurdleEngine.scorePattern`'s position-by-position comparison approach | Same-length matching-position count |

### 1.11 New Work Required

- Ownership-aware board renderer (color-tint tiles by `ownerId` instead of / in addition to premium-square coloring), plus a target-word highlight/selection affordance.
- Coloring engine: same-length position-matching, best-alignment search for length-differing words, FULL/PARTIAL/NEUTRAL classification per the `L-1` rule.
- Neutral-tile theft conversion (flip entire target word's tiles when a neutral target is matched).
- Outcome preview UI (show projected coloring before submit).
- New win-condition/scoring UI (territory bar instead of point totals — visually similar to `CandidateCountBar` from Absurdle, repurposed as a two/four-color territory meter).

**Retired from earlier drafts:** the redirect-radius / chain-tracing mechanic (BFS through `parentWord` links) is no longer needed now that target selection is manual and per-word rather than inferred through a chain — it's superseded by direct target selection plus neutral-tile theft.

---

## 2. Absurd Auction

**Elevator pitch:** Scrabble-style tile placement, but there's no fixed random bag. An adversarial "Banker" AI — built on the exact same bucket-selection logic as `AbsurdleEngine` — decides what tiles each player draws, specifically to keep the game as close as possible. Whoever is ahead gets starved of useful letters; whoever is behind gets a assist. Players can see the shrinking "favorable pool" and must adapt in real time.

### 2.1 Core Loop

1. Standard Lexicon setup: 15×15 board, 7-tile racks, dictionary-backed word validation, standard letter-point scoring.
2. **The twist:** instead of drawing uniformly at random from a shuffled bag, every draw is resolved by the **Banker**:
    - The Banker maintains the same remaining-tile multiset as a normal bag (letter frequencies unchanged — this is not adding/removing tiles, only **choosing which tile you get, in what order**).
    - When a player is due to draw `n` tiles, the Banker evaluates a small set of candidate draws (sampled subsets of the remaining pool) and — mirroring `AbsurdleEngine.chooseBucket` — picks the draw that is **worst for the currently-leading player** or **best for the currently-trailing player**, using score-differential as the bucket metric instead of candidate-set size.
    - Concretely: bucket candidate draws by a cheap **rack-quality heuristic** (vowel/consonant balance, duplicate-letter penalty, letter-synergy lookup — see 2.4, not a live board search), and pick the bucket that minimizes the leader's rack quality / maximizes the trailer's, subject to a fairness cap (see 2.4) so it never feels rigged beyond plausibility.
3. Players always see a **live "Draw Pool Tension" meter** (reusing the visual language of `CandidateCountBar`) showing how skewed the Banker's recent draws have been — full transparency that the game is adversarial, which is the whole appeal (same as Absurdle telling you the candidate count).

### 2.2 Why this is different

Lexicon/Scrabble's biggest "luck" complaint is bag variance — a lucky QU-heavy draw or a dry stretch of vowels can decide a game regardless of skill. Absurd Auction makes that variance **intentional and adversarial** rather than random, converting bag luck into a visible, game-theoretic mechanic: players start bluffing about their own rack strength, holding back big plays to avoid signaling a big lead (since a visible lead triggers Banker starvation), or intentionally underplaying (see 2.5).

### 2.3 Data Model

```kotlin
enum class BankerStance { NEUTRAL, EQUALIZING, CHAOTIC }

data class DrawCandidate(
    val tiles: List<Char>,
    val rackQualityScore: Int,       // heuristic score: vowel/consonant ratio, duplicate penalty, letter-synergy lookup — NOT a full board move-search
    val vowelCount: Int,              // tracked explicitly for pity-timer / hard-ratio-cap checks
    val consonantCount: Int
)

data class BankerDecision(
    val chosenTiles: List<Char>,
    val rejectedAlternativesCount: Int,   // for the tension meter / transparency
    val favoredPlayer: Int?,              // which player this draw was "aimed at helping", null if neutral
    val pityOverrideApplied: Boolean = false  // true if the pity timer forced this draw, overriding the active stance
)

data class AuctionGameState(
    val board: Array<Array<Tile?>>,
    val players: List<Player>,             // reuse Lexicon's Player model
    val remainingPool: MutableList<Char>,  // same multiset as a normal bag, order not fixed
    val currentPlayer: Int,
    val bankerStance: BankerStance = BankerStance.EQUALIZING,
    val scoreDifferentialHistory: List<Int>, // for the tension meter
    val consecutiveVowellessDraws: Map<Int, Int> = emptyMap(),   // playerId -> streak, for pity timer
    val consecutiveConsonantlessDraws: Map<Int, Int> = emptyMap(),
    val isGameOver: Boolean = false
)
```

### 2.4 Banker Algorithm (detailed)

Directly modeled on `AbsurdleEngine.process` / `chooseBucket`:

1. **Bucket generation:** sample `k` (e.g. 12–20) candidate draws of the required tile count from the remaining pool (weighted sampling so rare letters aren't systematically excluded — this must remain statistically fair over a full game, only *ordering* is adversarial, not *availability*).
2. **Bucket scoring — Heuristic Rack Scorer (replaces full move-search):** running `AiOpponent.findAllMoves` 12–20 times per draw, every draw, every turn, is far too heavy for on-device performance. Instead, score each candidate draw with a cheap, board-independent heuristic combining:
    - **Vowel/consonant ratio** — penalize racks outside a healthy 40–60% vowel range.
    - **Duplicate-letter penalty** — too many copies of one letter lowers the score.
    - **Precomputed letter-synergy table** — static lookup scoring common useful pairings higher (e.g., "Q" without "U" is heavily penalized; common blends like "ST", "ER", "ING" fragments score well) and awkward high-point isolates lower (e.g., a lone "J" or "X" with no low-point connectors).
    - This produces a `rackQualityScore` per candidate in place of `bestAchievableScore` — same role in the bucket-selection logic (2.4.3), far cheaper to compute.
    - **Run asynchronously:** dispatch the Banker's evaluation on a background coroutine (matching the existing `viewModelScope.launch` + `Dispatchers.Default` pattern already used for `AiOpponent.decideMove`); the UI observes the result via `StateFlow` and can show a brief "Banker is thinking…" state while it resolves, which also gives the taunting UI (2.6) a natural trigger point.
3. **Selection rule:**
    - If `bankerStance == EQUALIZING` (default): if the drawing player currently leads by more than a threshold (default 30 points), pick the candidate with the **lowest** `rackQualityScore`. If they trail, pick the **highest**. If roughly even, pick close to the median (avoid whiplash).
    - If `bankerStance == CHAOTIC`: pick whichever candidate maximizes `abs(rackQualityScore - currentAverage)` — favors volatility over fairness, a harder/sillier mode.
    - If `bankerStance == NEUTRAL`: true uniform random draw (Banker disabled — acts as a normal bag; useful as a fallback/control mode or for players who just want vanilla Lexicon rules on this board variant).
4. **Fairness cap — Pity Timer + Hard Ratio Caps (replaces the old "±% of pool proportion" rule):** a percentage-deviation tolerance breaks down for rare letters (a ±15% band around "expect ~0.1 Qs per window" is meaningless), so the guardrail is built from simple counting rules instead:
    - **Pity timer:** track consecutive draws-without-a-vowel (and separately, draws-without-a-consonant) per player. If a player goes 2 consecutive draws with zero vowels, the Banker's next candidate pool is filtered to only buckets containing at least one vowel, **overriding** the active stance for that one draw. Same rule mirrored for consonant droughts.
    - **Hard ratio cap:** no single draw may ever result in a rack of 7 vowels or 7 consonants (or similarly degenerate all-one-type compositions) — candidates violating this are discarded from the bucket pool before selection even runs.
    - Together these guarantee the Banker can reorder *which* combinations you get and *when*, but can never produce a genuinely unplayable rack — this is the guardrail against "the Banker just never gives me an E" complaints.
5. **Opening draw (no board exists yet):** `rackQualityScore` has nothing to evaluate against on turn 1, so the Banker skips adversarial selection entirely for every player's first draw and instead deals a **Perfect Median rack** — a fixed, balanced composition (e.g., exactly 3 vowels, 4 consonants, mid-tier total letter value, no duplicate high-point tiles) for *every* player's opening hand, not just the first player's. This guarantees a neutral, symmetric starting baseline before the adversarial mechanics engage on draw 2 onward.
6. **Multiplayer (3+ players) — Field Average:** the leader/trailer framing only works for 2 players. For 3+, the Banker instead compares each drawing player's score to the **median score of all active players**:
    - Above median → treated as "leading" (starved, per stance rules).
    - Below median → treated as "trailing" (favored).
    - Within a small band of the median → neutral draw.
    - **Kingmaker guard:** apply a diminishing-returns curve to how much a trailing player is favored as their score *approaches* the median — this prevents the Banker from over-correcting a last-place player straight into first in one or two lucky draws, which would read as whiplash rather than fair balancing.
7. Same **tie-break discipline as `AbsurdleEngine.chooseBucket`**: deterministic secondary/tertiary tie-breaks so behavior is reproducible for a given seed (useful for daily-challenge modes).

### 2.5 Strategic Layer This Creates

- **Sandbagging:** a leading player may deliberately play a lower-scoring word (or exchange tiles) to avoid triggering visible-lead starvation — a genuine, novel risk/reward layer standard Scrabble doesn't have.
- **Bluff via exchange:** exchanging tiles is now also a signaling move — the Banker's view of your "rack synergy" resets partially, which savvy players can exploit to reset an unfavorable starvation streak.
- **Comeback design:** because trailing players get systematically better draws, blowout games become self-correcting — likely improves replayability/casual retention versus vanilla Lexicon, where a bad early bag can snowball.

### 2.5a Meanness Multiplier (revenge scoring layer)

**Status: design in progress — the shape below is the current working direction, not finalized. Flagged for a follow-up pass.**

Rather than the Banker's "meanness" being pure punishment, a player who successfully plays a strong word *despite* an unfavorable Banker draw earns a scoring multiplier — and passes a version of it to their opponent's next play, creating a pendulum rather than a one-way penalty.

**Formula (tile-progress based):**

```
gapRatio  = (leaderScore - trailerScore) / leaderScore     // 0 if leaderScore == 0
progress  = 1 - pctTilesRemaining                           // 0 at game start, → 1 as bag empties

rawMultiplier = 1 + gapRatio * progress

finalMultiplier =
    if rawMultiplier < 1.2  → 1.2   (Normal mode floor — standard baseline "meanness" even in close games)
    else                    → min(rawMultiplier, cap)
                                cap = 3.0   (Normal mode)
                                cap = ∞     (Demon mode — no ceiling)
```

- **Why progress multiplies the gap rather than dividing it:** meanness is meant to *build* over the course of the game, not fade — early leads (mostly noise) shouldn't trigger heavy multipliers, but a large gap that persists late into the game should apply real pressure to the leader. Progress starts at 0 and grows to 1, so the multiplier is intentionally weak early and strongest late.
- **Floor exists so close games still feel like something is happening:** without it, an evenly-matched game produces a near-1.0 multiplier that reads as "the mechanic isn't doing anything." The 1.2 floor guarantees a baseline "meanness" is always live.
- **`leaderScore == 0` edge case:** while both players are scoreless (start of game), `gapRatio` is undefined — treat as 0, so the floor multiplier (1.2) applies rather than a crash or a spurious huge number.

**Use-it-or-lose-it, with decay (replaces full expiry):** the multiplier applies to the affected player's very next play. If unused by the start of their following turn, it doesn't vanish outright — it **decays by 50%** and carries forward one more turn before finally expiring if still unused. This keeps the "press your advantage or risk losing it" tension from the original design while softening the punishment for a player who simply didn't have a playable rack that turn, which is a real possibility given the Banker is actively working against them.

**Pendulum handoff with recipient kicker:** when the **originator** (the player who played through an unfavorable Banker draw) successfully plays a word, two things happen:

1. The originator scores their word at `base × M`, where `M` is the multiplier computed from 2.5a's core formula.
2. `M` is handed to the **recipient** (the originator's opponent) for the recipient's own next play — but the recipient's cash-in gets an additional kicker on top, so a successful "pay it forward" play is worth more to the recipient than the plain `M` alone:

```
recipientMultiplier = M + (1 + gapRatio_atCashIn)
```

where `gapRatio_atCashIn` is recalculated fresh at the moment the recipient plays (using the score gap *as it stands then*, not the gap from when `M` was originally generated). This deliberately replaces an earlier draft that scaled the kicker by game-progress (`1 + progress`) — that version made the kicker *largest* right at the end of the bag, which directly worked against the "no dramatic endgame miracle" goal by concentrating the biggest possible swing into the final few tiles. Tying the kicker to the live score gap instead means it stays proportional to how much ground is actually left to make up, at any point in the game, rather than to how little of the bag is left.

**Propagation cap (prevents runaway):** only the Banker's freshly-recalculated base `M` (2.5a's core formula, recomputed each turn from the *current* score gap and tile progress) is passed forward as the multiplier for the next handoff. The `(1 + gapRatio)` kicker is a **one-time bonus applied only at the moment of cash-in** — it is never itself inherited or compounded into the next player's `M`. Without this rule, each successful handoff would hand forward an ever-larger combined value with no ceiling, regardless of the stated Normal-mode cap; with this rule, `M` is capped at 3 (Normal) / ∞ (Demon) on every single handoff, and the kicker resets and is freshly computed each time rather than accumulating.

**Worked example:** Banker draw yields `M = 2` for the originator. Originator plays a 30-point word → scores `30 × 2 = 60`. Multiplier `M = 2` passes to the recipient. Recipient's turn arrives; the live gap at that moment gives `gapRatio = 0.5`. Recipient plays a 30-point word → scores `30 × 2 + 30 × (1 + 0.5) = 60 + 45 = 105`. Whatever `M` gets computed fresh for the *next* handoff after this is drawn from the Banker's standard formula again — it does not inherit the 105 or the recipient's kicker in any way.

**Premium squares / "Absolute Meanness" toggle:** by default (Normal mode), the meanness multiplier (and its kicker) applies to the word's **base letter score only**, calculated *before* premium square (double/triple word or letter) bonuses are applied — this keeps a lucky triple-word play from compounding into an extreme outlier. An opt-in **"Absolute Meanness"** toggle (off by default) allows the multiplier to apply after premium squares as well, for players who want maximum chaos.

**Casual alternative — the Defiance Bonus:** the full pendulum/gap-ratio/decay system above is mechanically rich but asks a casual player to track a fair amount of running state in their head. As a simpler difficulty option, **Casual mode** replaces the entire multiplier system with a flat rule: if the Banker hands out a rack scoring below a set `rackQualityScore` threshold (2.4) and the player still manages to score above a set point threshold that turn, they earn a flat **Defiance Bonus** (default +15 points), no gap math, no handoff, no decay. This preserves the emotional payoff ("I beat a bad draw") without any of the runaway-multiplier risk the full system needed three rounds of hardening to avoid. Normal and Demon modes keep the full proportional system; Casual mode uses the Defiance Bonus instead — a difficulty-tier choice, not a replacement.

**Active catch-up option — Banker's Favor:** independent of the multiplier system (works alongside either version above), a trailing player may spend a turn (or sacrifice a fixed point amount) to force the Banker into `NEUTRAL` stance for their **next two draws** — a player-initiated, deliberate catch-up tool rather than a purely passive one triggered by the algorithm. This gives a trailing player agency: instead of only waiting for the pendulum to swing their way, they can choose to cash in a turn for a guaranteed window of fair, unbiased draws when they most need a specific tile.

**Resolved from the earlier open-questions list:**
- The >2-player field-average comparison (previously open) is now resolved — see 2.4, item 6.
- Multiplier decay-instead-of-full-expiry (previously open) is now resolved above.
- The multiplier remains a separate scoring layer on top of the draw mechanic (2.4) rather than sharing inputs directly — kept deliberately decoupled so each system can be tuned independently without destabilizing the other.

### 2.6 UI Additions

- **Draw Pool Tension bar** — the primary, always-on visual: a Clinical Readout style meter (monospace, minimal, green→purple gradient reusing `CandidateCountBar`'s styling) showing the Banker's recent skew as plain data, e.g. `SYSTEM STANCE: EQUALIZING` / `DRAW SKEW: LDR -4.2 | TRL +2.1` / `REJECTED ALTERNATIVES: 14`. This is the default for all players — transparent, unemotional, and fits a sleek/modern aesthetic without committing to a tone that might not land for every player.
- **Taunting Banker mode (toggleable, off by default)** — an optional personality layer for players who want it. When enabled, an exceptionally skewed draw (gapRatio crossing a high threshold) triggers a brief overlay/snackbar with in-character copy instead of (or alongside) the clinical readout — e.g., *"Hope you like the letter W."* to a starved leader, or *"Don't say I never did anything for you — take these vowels."* to a favored trailer. Kept as a toggle rather than baked-in behavior because it's genuinely fun but has a real "gimmick fatigue" risk over repeated games — players who want the personality can opt in, players who find it grating or tilting can leave it off without losing any of the actual mechanical transparency (the Clinical Readout keeps running underneath regardless of this toggle).
- **Banker stance indicator**: small badge/icon showing current stance (Equalizing / Chaotic / Neutral) — set at game start as a difficulty option, like Absurdle's Hard Mode toggle.
- **"Banker is thinking…" state**: while the async heuristic evaluation (2.4) resolves, the tension bar shows a brief pending/loading state rather than snapping instantly — masks any processing latency and, when Taunting mode is on, is also where a taunt line can be queued to display the moment the draw resolves.
- **Post-game breakdown**: "The Banker helped you catch up by ~14 points over the game" summary card, similar spirit to Absurdle's `GameResultCard` guess-count commentary.

### 2.7 Reused Components

| Component | Source | Reuse |
|---|---|---|
| Bucket-and-choose-worst-for-player logic | `AbsurdleEngine.chooseBucket` | Core Banker decision algorithm, retargeted from letter-patterns to score-differential |
| Async AI-turn pattern (`viewModelScope.launch` + `Dispatchers.Default`, `isAiThinking` flag) | `ScrabbleGameViewModel.maybeRunAiTurn` | Async Banker evaluation + "Banker is thinking…" UI state |
| Board, rack, scoring, dictionary validation | `ScrabbleGame`, `Board.kt`, `Rack.kt` | Entire base game layer, unmodified |
| Candidate-pool tension visualization pattern | `CandidateCountBar` | Draw Pool Tension bar (Clinical Readout variant) |

### 2.8 New Work Required

- Banker decision engine (new class, e.g. `BankerEngine`, mirroring `AbsurdleEngine`'s object structure), using the Heuristic Rack Scorer rather than a full move-search oracle.
- Precomputed letter-synergy lookup table (static data, e.g. common digraphs/blends scored favorably, awkward high-point isolates penalized) feeding the heuristic.
- Weighted-sampling draw candidate generator respecting long-run letter-frequency fairness, plus the pity-timer/hard-ratio-cap guardrail (2.4).
- Score-differential history tracking + tension-bar UI (Clinical Readout default, Taunting Banker as an optional overlay).
- Settings for Banker stance (Neutral/Equalizing/Chaotic), Casual/Normal/Demon multiplier mode, and the Taunting Banker toggle, alongside the existing AI difficulty picker.
- Meanness Multiplier tracking (active multiplier per player, decay-then-expire logic, pendulum handoff with recipient kicker) per 2.5a, plus the simpler Defiance Bonus path for Casual mode.
- Banker's Favor mechanic (spend a turn / sacrifice points to force two guaranteed-neutral draws).

### 2.9 Resolution Log

All items previously flagged as open in this section have been resolved and folded into 2.4–2.6 above:

- **Fairness cap** — replaced the underspecified "roughly matches pool proportions" rule with a concrete pity-timer (forces a vowel/consonant after 2 consecutive draws without one) plus hard ratio caps (no rack ever ends up all-vowel or all-consonant). See 2.4, item 4.
- **Performance cost** — the full `AiOpponent.findAllMoves` oracle is replaced with a cheap Heuristic Rack Scorer (vowel/consonant ratio, duplicate penalty, letter-synergy lookup), run asynchronously. See 2.4, item 2.
- **Opening draw** — resolved via a fixed "Perfect Median" balanced rack dealt to every player's first hand, skipping adversarial selection entirely until draw 2. See 2.4, item 5.
- **>2-player framing** — resolved via field-average comparison (median score) with a diminishing-returns "Kingmaker guard" to prevent overcorrection. See 2.4, item 6.
- **Transparency vs. player experience** — resolved as a Clinical Readout default (always transparent) with an optional, toggleable Taunting Banker personality layer rather than a binary transparent/opaque choice — keeps the strategic information visible either way while letting tone be a player preference. See 2.6.

---

## 3. Category Lexicon (Challenge Mode Add-On)

**Status: not a standalone game.** Following a stress test of this concept, the core problem surfaced clearly: the deduction layer (the thing that would make this distinct from ordinary Lexicon) has no mechanical consequence as designed — players earn the bonus just by incidentally playing into a hidden category, whether or not they were deducing anything, which makes this "ordinary Lexicon with an occasional surprise bonus," not the Connections-style deduction game the pitch promised. Rather than force a mechanical fix that risks over-complicating standard Lexicon, this concept is repositioned as a **daily/weekly challenge add-on layered on top of standard Lexicon**, not a fifth game in its own right.

**Revised elevator pitch:** a daily or weekly challenge mode for standard Lexicon where 4 hidden categories are seeded into that day's game. Matching a hidden category doesn't change how you play — it's a bonus layer that rewards attentive players with hints, cosmetic recognition, or challenge-specific rewards (e.g., streak credit, a bonus reveal in tomorrow's puzzle) rather than being core to how the match is won. This keeps standard Lexicon's ruleset completely untouched while giving daily-challenge players a reason to look closer at their plays and come back tomorrow.

### 3.1 Core Loop (Challenge Mode)

1. Each day/week, the server (or a seeded local generator, for offline play) selects **4 hidden categories** for that challenge instance, using the same curated category dataset described in 3.4.
2. Players play **standard, unmodified Lexicon** — same scoring, same board, same AI opponent if applicable.
3. **Bonus resolution:** whenever a played word matches one of the 4 hidden categories, the player earns a **reward token** (not a score multiplier) — see 3.5 for what tokens do.
4. **Hints:** same cadence as originally designed (every 3rd global play reveals one example word, not the category name) — but now purely in service of the challenge-completion goal, not competitive scoring.
5. At game end, categories are revealed along with which plays matched them, and any earned tokens are credited to the player's daily/weekly challenge progress.

### 3.2 Why this repositioning works better

Daily/weekly challenge modes (Wordle's whole genre) succeed on **shared context and low competitive stakes** — everyone gets the same categories that day, and the reward is bragging rights / streak-keeping, not winning a head-to-head match. That's a much better fit for a deduction layer that doesn't need to be perfectly balanced against real-money-equivalent competitive scoring — it only needs to be *fun to notice*. This also sidesteps essentially every fairness problem raised in the original stress test (category difficulty variance, AI-opponent oracle knowledge, multi-word/premium-square interactions) because none of those questions matter once the categories aren't feeding into who wins a competitive match.

### 3.3 Data Model

```kotlin
data class WordCategory(
    val id: String,
    val displayName: String,        // revealed only at challenge end
    val matchWords: Set<String>     // pre-filtered against active dictionary
)

data class CategoryMatch(
    val category: WordCategory,
    val matchedWord: String,
    val playerId: Int,
    val turnNumber: Int
)

data class ChallengeRewardState(
    val challengeDate: String,                      // e.g. "2026-07-05" for daily seeding
    val hiddenCategories: List<WordCategory>,        // not shown to players until challenge end
    val revealedHints: List<String>,                 // example words shown so far, no category label attached
    val matchesThisChallenge: List<CategoryMatch>,
    val tokensEarned: Int,
    val streakCount: Int                             // consecutive days/weeks with at least 1 category matched
)
```

### 3.4 Category Dataset — Content Requirements

This remains the one piece that needs real content authoring rather than pure code, same as the original stress test noted:

- **Bootstrap approach:** tag a subset of your existing `large_wordlib.json` against broad WordNet-style semantic categories (birds, colors, professions, weather terms, tools, emotions, etc.) using an offline script — same pattern as your existing `add_missing_words.py`/`remove_invalid_words.py` tooling (dual console+log output, no input overwriting). WordNet's synonym-set structure (see the crossword dictionary discussion, section 4) is directly useful here too — it already groups words by sense, which is most of the category-tagging work done for you.
- **Category pool size guidance:** each category needs at least ~15-20 valid dictionary words at commonly-playable lengths (3-9 letters) so a category doesn't run dry mid-game.
- **Since this is now a non-competitive challenge layer, category difficulty variance (originally stress-test finding #2) is far less risky** — an unusually hard or easy category just changes how satisfying that day's challenge feels, it doesn't create a competitive fairness complaint the way it would in a scored head-to-head match.

### 3.5 Reward Tokens (replaces the old score multiplier)

| Match | Reward |
|---|---|
| 1st category matched that challenge | Reveal one hint early (skip ahead in the hint cadence) |
| 2nd category matched | Small cosmetic reward (tile skin, board theme, etc. — reuse whatever cosmetic system, if any, exists elsewhere in WordNest) |
| 3rd category matched | Bonus streak credit (counts double toward streak-keeping) |
| All 4 categories matched ("Category Sweep") | Badge/achievement + guaranteed easier category next challenge (a soft rubber-band for engagement, not competitive balance) |

### 3.6 Reused Components

| Component | Source | Reuse |
|---|---|---|
| Entire base scoring game (board, rack, bag, AI opponent) | `ScrabbleGame`, `AiOpponent`, `Board.kt`, `Rack.kt` | Unmodified — this is standard Lexicon underneath |
| Dictionary loading/validation | `OptimizedWordRepository` | Word legality (unchanged) + category-word cross-validation |
| Snackbar-style transient message pattern | `ScrabbleGameApp.kt`'s `lastPlayMessage` card | Hint reveal + token-earned notifications |
| Stats/streak persistence pattern | `betweenle/data/StatsRepository.kt` / `chromaword/data/StatsRepository.kt` (DataStore-backed) | Challenge streak tracking |

### 3.7 New Work Required

- Category dataset creation/tagging pipeline (unchanged from original — still the biggest lift).
- Daily/weekly seed generation (deterministic seeding by date, so all players get the same categories that day — same principle as any daily-challenge word game).
- `CategoryEngine`: match-checking, hint-reveal scheduling, token awarding (simplified from the original multiplier-balancing work, since tokens don't need score-fairness tuning).
- Challenge streak persistence + end-of-challenge recap screen.

---

## 4. Crossword

**Elevator pitch:** A single-player, pre-generated crossword puzzle built from your existing dictionary infrastructure plus a definitions source (WordNet — see below). The grid-fill complexity that made a *live, real-time* crossword-style Scrabble variant impractical (per the earlier "too complex/taxing" assessment) disappears once generation happens offline/on a loading screen instead of turn-by-turn against a waiting opponent — this is a fundamentally easier engineering problem once there's no live opponent and no time pressure on the generation step itself.

### 4.1 Dictionary Requirement

Crossword clues need **definitions**, which your existing word-validity lists (`large_wordlib.json`, TWL06/SOWPODS) don't provide — those only answer "is this a legal word," not "what does it mean."

**Recommended source: WordNet (Princeton).** Its license explicitly permits commercial use royalty-free, with the only requirement being that the copyright notice ships with any distributed copies — this is meaningfully cleaner than the alternatives:
- **GCIDE** has richer, more natural clue prose, but mixed public-domain/GPL licensing per-entry depending on which contributor added the definition — usable, but requires filtering to base-Webster entries to avoid GPL copyleft obligations bleeding into the app.
- **Wiktionary** is the most complete and current, but is licensed **CC BY-SA (share-alike)** — a real licensing question (share-alike can require derivative works to carry a compatible license) worth resolving with whoever handles the app's licensing before use, not just a technical integration detail.

WordNet also groups words by sense with synonym sets already built in, which is directly reusable for clue-writing (multiple candidate definitions per word) and for the Category Lexicon challenge-mode dataset (section 3.4) — one dictionary integration serving two features.

### 4.2 Data Model

```kotlin
data class CrosswordCell(
    val row: Int,
    val col: Int,
    val isBlocked: Boolean,          // true = black square, no letter
    val letter: Char?,               // the solution letter, null if blocked
    val clueNumberAcross: Int?,      // non-null if this cell starts an Across entry
    val clueNumberDown: Int?         // non-null if this cell starts a Down entry
)

data class CrosswordClue(
    val number: Int,
    val direction: ClueDirection,    // ACROSS or DOWN
    val word: String,
    val clueText: String,            // pulled from WordNet definition, lightly filtered/edited
    val startRow: Int,
    val startCol: Int
)

data class CrosswordPuzzle(
    val grid: Array<Array<CrosswordCell>>,
    val clues: List<CrosswordClue>,
    val gridSize: Int,                // e.g. 15x15 daily, smaller for quick mode
    val difficulty: CrosswordDifficulty
)

data class CrosswordPlayerState(
    val filledLetters: Map<Pair<Int, Int>, Char>,   // player's current guesses, sparse map
    val revealedCells: Set<Pair<Int, Int>>,          // cells revealed via hint
    val checkedCells: Set<Pair<Int, Int>>,           // cells the player has checked (right/wrong marker shown)
    val isComplete: Boolean,
    val elapsedSeconds: Int
)
```

### 4.3 Generation Algorithm

1. **Choose a block pattern** for the grid (predefined symmetric block layouts, similar to how published crosswords use a small library of standard patterns rather than fully custom layouts each time — start with a handful of hand-designed 15×15 patterns to avoid needing a pattern-generator as a separate project).
2. **Fill longest slots first:** identify all across/down slots (runs of open cells) sorted by length descending, and attempt to fill using your existing word list, preferring higher-frequency/less-obscure words first if using a scored list like `spread the word(list)`'s data.
3. **Constraint propagation + backtrack:** each fill choice constrains its crossing slots (shared letter positions) — standard crossword constructor approach is a backtracking search that retries a different word choice for a slot when a crossing slot can no longer be filled. Cap backtrack depth/attempts and regenerate the block pattern if a fill can't complete within a reasonable attempt budget, rather than allowing unbounded search time.
4. **Attach clues:** for each placed word, look up a WordNet definition; if multiple senses exist, prefer the shortest clean definition; if no definition exists at all (proper nouns, rare word-list entries), either substitute a synonym-based clue (via WordNet's synonym sets) or exclude that word from the fill candidates entirely and re-attempt that slot.
5. **Difficulty tuning:** vary via (a) grid size (5×5 quick mode vs. 15×15 daily), (b) word obscurity bias (favor common words for Easy, allow rarer entries for Hard), (c) clue style (more literal for Easy, more wordplay/oblique for Hard — oblique clue-writing is a content task, likely a v2 feature rather than launch scope).

### 4.4 UI / Interaction

- Standard crossword grid rendering, numbered cells, Across/Down clue lists (tap a clue to highlight its cells; tap a cell to see its clue).
- Physical-keyboard-driven letter entry (reuse `PhysicalKeyboardInput`'s existing pattern, adapted for cell-by-cell navigation rather than a single input row).
- Check/Reveal affordances: check a single cell, check the whole grid, reveal a cell, reveal a whole word — standard crossword-app conventions, gated behind a hint budget if you want a scoring/rating dimension (e.g., "solved with 2 reveals" as a completion badge tier).

### 4.5 Reused Components

| Component | Source | Reuse |
|---|---|---|
| Dictionary loading | `OptimizedWordRepository` | Word-fill candidate source |
| Scored word-list concept (obscurity tiers) | `spread the word(list)` data pattern (external) | Difficulty-tuned fill preference |
| Keyboard input pattern | `absurdle/ui/PhysicalKeyboardInput.kt` | Adapted for grid-cell entry |
| Stats/streak persistence pattern | `betweenle/data/StatsRepository.kt` | Daily-solve streak tracking |

### 4.6 New Work Required

- WordNet integration + parsing pipeline (new — first definitions-capable dictionary source in the app).
- Grid-fill generator (constraint-propagation + backtracking) — the single biggest new algorithmic component across all five concepts in this document.
- A small library of hand-designed block patterns to seed generation from.
- Crossword grid UI (cell-by-cell navigation, clue list, check/reveal affordances) — entirely new UI, no existing WordNest screen is close to this shape.
- Clue-quality filtering/editing pass over raw WordNet definitions (some will read awkwardly verbatim and need light polish, likely an ongoing content task rather than a one-time job).

---

## 5. Codeword

**Elevator pitch:** The same generated crossword grid engine from section 4, but instead of clues, every letter of the alphabet is replaced by a number (A=one specific number, B=another, consistently across the whole grid). The player decodes the grid by figuring out which number maps to which letter, using word-pattern and letter-frequency reasoning — classic Codeword/Kriss Kross puzzle mechanics. Your proposed simplification — blurring rather than fully hiding a small set of starter letters — gives new players a foothold instead of the intimidating fully-blank grid Codeword traditionally opens with.

### 5.1 Why this pairs naturally with Crossword

Codeword needs **no definitions at all** — it's a pure letter-pattern/frequency deduction puzzle — so it sidesteps the WordNet integration and clue-quality-editing work entirely. Given both puzzle types share the same underlying grid-fill generator (section 4.3), Codeword is close to a "reskin" of the Crossword generator once that engine exists: generate the grid and word list exactly the same way, then simply substitute a consistent letter→number mapping for the clue-list UI instead of attaching definitions.

### 5.2 Core Loop

1. Generate a grid using the **same fill algorithm as Crossword** (section 4.3), but skip the clue-attachment step (4.3, step 4) entirely — no definitions needed.
2. Assign a **random 1-to-1 mapping** of letters A-Z to numbers 1-26 (only numbers actually used in the generated grid need a mapping; unused letters can be omitted from the number pool for a smaller/cleaner number range if the grid doesn't use all 26 letters).
3. Render the grid with **numbers instead of letters** in every cell.
4. **Starter letters (your blur mechanic):** select a small number of cells (e.g., 2-4, scaled to grid size) to display **with their letter visible but visually blurred/obscured** (e.g., low-opacity or frosted-glass style rendering) rather than fully blank. This tells the player "this letter is revealed, but you still have to squint/tap to confirm it" — giving a genuine foothold (a known starting letter-number pair) while preserving a light decode step even for the freebie cells, so it doesn't feel like the puzzle is just handing over free answers.
5. Player fills in letters by number: tapping any cell showing number `N` and entering a letter fills **every cell showing `N` on the entire grid simultaneously** (the core Codeword mechanic — you're solving the mapping, not individual cells).
6. Standard check/reveal affordances, same as Crossword.
7. Completion: full grid correctly filled; a "reveal mapping" screen at the end shows the solved number-to-letter key.

### 5.3 Data Model

```kotlin
data class CodewordPuzzle(
    val grid: Array<Array<CodewordCell>>,   // reuses CrosswordCell's row/col/isBlocked shape conceptually
    val letterToNumber: Map<Char, Int>,     // the solution mapping, hidden from the player until solved/revealed
    val blurredStarterCells: Set<Pair<Int, Int>>,
    val gridSize: Int
)

data class CodewordCell(
    val row: Int,
    val col: Int,
    val isBlocked: Boolean,
    val number: Int?,                // null if blocked; the number shown to the player
    val solutionLetter: Char?        // null if blocked; not shown to the player except via blur/reveal
)

data class CodewordPlayerState(
    val numberToPlayerLetter: Map<Int, Char>,   // player's current guessed mapping, sparse
    val revealedNumbers: Set<Int>,               // numbers the player has revealed via hint
    val isComplete: Boolean,
    val elapsedSeconds: Int
)
```

### 5.4 Difficulty / Variants

| Variant | Change |
|---|---|
| **Gentle** (default, your proposal) | 3-4 blurred starter cells given |
| **Classic** | 1-2 blurred starter cells — closer to traditional Codeword's minimal-foothold difficulty |
| **Blank Start** | Zero starter cells — full traditional Codeword, no assists, for players who've outgrown Gentle/Classic |

### 5.5 Reused Components

| Component | Source | Reuse |
|---|---|---|
| Grid-fill generator | Crossword's engine (section 4.3), minus the clue-attachment step | Entire puzzle generation, shared wholesale |
| Grid rendering / cell navigation UI | Crossword's grid UI (section 4.4) | Adapted to show numbers instead of letters, plus blur rendering for starter cells |
| Dictionary loading | `OptimizedWordRepository` | Word-fill candidate source (no definitions needed here) |

### 5.6 New Work Required

- Letter-to-number mapping assignment + the "fill one cell, fill all matching-number cells" interaction (the one genuinely new interaction model versus Crossword).
- Blur-rendering treatment for starter cells (a CSS/Compose visual effect — frosted/low-opacity text over the revealed letter).
- Difficulty settings for starter-cell count (Gentle/Classic/Blank Start).
- Because this reuses Crossword's generator almost entirely, this should be a **fast follow after Crossword ships**, not a parallel build — most of the heavy lifting (grid generation) is already amortized.

---

## 6. Shared Infrastructure Notes

Cutting across all three concepts, and worth doing **before** or **alongside** whichever one you build first, per the existing audit's duplication findings:

- **Letter values & premium-square layout** are currently defined three times (`Model.kt`, `AiOpponent.kt`, `Board.kt`). Any of these three new games will need a fourth (or fifth/sixth) copy unless this gets extracted into a single `ScrabbleConstants` object first. Strongly recommend doing this extraction before starting Ladder Claim or Absurd Auction, both of which reuse the board/scoring layer heavily.
- **A shared `BoardGameEngine` base** (bag build/draw, tile racks, premium squares, connects-to-board / contiguous-tiles checks) would let all three concepts (plus vanilla Lexicon) inherit one implementation instead of forking `ScrabbleGame`. Given Ladder Claim strips scoring entirely and Absurd Auction only changes *draw* logic, both are strong candidates for "extends base, overrides one seam" rather than copy-paste.
- **Result-card / how-to-play / stats-dialog patterns** are already duplicated three times per the audit; a fourth-through-sixth duplication is the natural next pressure point to finally justify the shared `ResultCard(title, subtitle, accentColor, onShare, onNewGame)` composable the audit already recommended.
- **Suggested build order:** Ladder Claim first (lowest net-new logic — mostly wiring already-built solvers together), then Crossword (biggest standalone value and no live-opponent complexity now that generation is offline, but the single largest new algorithmic component — the grid-fill generator), then Codeword as a fast follow immediately after Crossword (reuses its generator almost entirely), then Absurd Auction (moderate — new Banker engine, but the AI move-scoring oracle already exists, and the Meanness Multiplier sub-mechanic still has open balancing questions per section 2.9 worth resolving before implementation), then the Category Lexicon challenge-mode add-on last (lowest engineering risk since it sits on top of unmodified Lexicon, but gated on the WordNet integration from Crossword existing first, and on having a content-authoring pass for the category dataset).

---

_End of document. Each section above is meant to be a standalone starting spec — happy to expand any one into full Kotlin class skeletons, ViewModel state machines, or a content-authoring script for the category dataset once you pick a build order._
