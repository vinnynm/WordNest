# WordNest — App Audit

_Scope: Absurdle, ChromaWord, Betweenle, Lexicon (Scrabble-like), shared word-repo/processors layer._

## 1. Overall impression

The four games are well-separated by package, share a sane `OptimizedWordRepository` loader, and each has its own theme/viewmodel/state pattern (StateFlow + Compose). Code quality is generally good — readable, commented where it matters (esp. `AbsurdleEngine`, `GuessEvaluator`). The issues below are mostly small correctness bugs, duplication, and a few unfinished features, not architectural problems.

---

## 2. Bugs / correctness issues

| # | Location | Issue |
|---|----------|-------|
| 1 | `lexicon/ui/ViewModel.kt` — `downloadUpdate()` | `TODO("Not yet implemented")` — the "Word Library Update" button in `ScrabbleGameApp.kt` calls this and will **crash** (throws `NotImplementedError`) if tapped. `uiState.updateAvailable` also appears to always be non-null-checked against a `String?`/`Boolean` mismatch (`if (uiState.updateAvailable != null && ...)` next to `val updateAvailable: Boolean = false` in `GameUiState`) — that's a type inconsistency; `updateAvailable` is declared `Boolean` but used as if nullable/String elsewhere (`"Version ${uiState.updateAvailable}"`). This won't compile as-is unless `updateAvailable` is actually a nullable String somewhere else — worth double-checking the real type. |
| 2 | `lexicon/ui/ViewModel.kt` — `GameUiState` | Same class has `isDownloading: Boolean =false` (missing space, harmless) but combined with #1, the whole "update available" feature looks stubbed-in but not wired to a real source. Either finish it or strip it out — right now it's dead UI that can crash. |
| 3 | `AbsurdleViewModel.submitGuess()` | `revealedWord` is only set `if (result.isWon)` or when candidates collapse to 1 **and not won**. But `GameResultCard` reads `revealedWord` for the "give up" path too — that's fine — however note `isActive` in `AbsurdleState` is `!isWon && revealedWord == null`. If candidates collapse to exactly 1 word on a *non-winning* guess (pattern isn't all green but only 1 candidate remains — shouldn't normally happen since 1 candidate means the guess would be scored against itself as all-green, but if the guess ≠ that candidate this is reachable), `revealedWord` gets set and the game becomes inactive without `isWon` being true and without it being a real "give up" — the UI would show `GameResultCard` in the "Gave up" branch even though the player didn't give up. Worth a state field like `GameOverReason` instead of overloading `revealedWord`. |
| 4 | `AbsurdleEngine.chooseBucket` hard mode | Hard mode filters out the all-green bucket only when `buckets.size > 1`. If the *only* bucket happens to be all-green (i.e., the guess is correct and it's the last possible word), that's correctly unavoidable — fine. But hard mode as implemented doesn't actually enforce "must use revealed hints" (per its own dialog text: "Any revealed hints must be used") — it currently only delays wins, it doesn't filter candidates that contradict previously-shown green/yellow letters. The label may overpromise. |
| 5 | `WordRepository` (betweenle) — `randomBoundaryPair` | `require(sortedWords.size > maxGap + 2)`. With default `minGap=20, maxGap=500` (as called in `GameViewModel.startNewGame`), this requires 500+ words in the dictionary just to boot the game. If someone swaps in a small `wordlib500.json` (per the README, ~48k words expected, but a custom list could be much smaller), this throws and crashes on `startNewGame()`. No try/catch around it in the ViewModel. |
| 6 | `AiOpponent.pickMove` (`lexicon`) | `EASY`/`HARD` pools: `if (x / 4 > 6) 6 else maxOf(x / 4, 1)`. When `x` (candidate count) is small (e.g., 1–3), `x/4` = 0, `maxOf(0,1)=1`, fine. But `sorted.takeLast(1).random()` / `.take(1).random()` — `random()` on a single-element list is fine, just noting it's a needless `.random()` call. Not a bug, just noise.
| 7 | `ScrabbleGameViewModel.resumeGame()` | Restores `game.board`/`players`/`bag` but never restores `game.placedThisTurn` (fine, that's transient) — however it also never re-validates `aiOpponent`'s `vocabulary` against the *current* dictionary if the dictionary/version changed between saves. Minor, low risk. |
| 8 | `GameUiState.equals()` (Lexicon) | Custom `equals()` omits `dictionarySize`, `bagSize` is actually included, but `isDownloading` and `updateAvailable` are **not** compared. Since `hashCode()` is `javaClass.hashCode()` (constant) for **all** instances, this whole class already breaks the hashCode/equals contract for use in hash-based collections (not used that way today, but a landmine for future code, e.g. `distinctUntilChanged()` on the StateFlow relies on `equals`, and the missing fields mean UI won't recompose on `isDownloading`/`updateAvailable` changes alone). |
| 9 | `AbsurdleKeyboard.kt` / `ChromaKeyboard.kt` | Near-identical composables duplicated verbatim in two packages (see "Duplication" below) — not a bug, but the `letterKeyW` calc has an unused local `co` (`val co = constraints.maxWidth`) in `AbsurdleKeyboard.kt` — dead code. |
| 10 | `PhysicalKeyboardInput.kt` | Docstring literally says "Package is under `absurdle.ui` but also imported directly by `chromaword.ui`" — this is a known cross-game dependency the author flagged themselves. Works, but it's an architecture smell (see below). |

---

## 3. Duplication (candidates for extraction into a shared module)

- **Keyboard widgets**: `AbsurdleKeyboard.kt` and `ChromaKeyboard.kt` are ~95% identical (row layout math, key sizing, action keys). Only the color-lookup function differs (`keyBackground` vs `letterColor`). Could be unified into one `GameKeyboard(keyStates: Map<Char, T>, colorOf: (T) -> Color, ...)`.
- **`PhysicalKeyboardInput`**: already shared (good!) but lives oddly under `absurdle.ui` and is cross-imported by `chromaword`. Should move to a neutral shared package (e.g. `games.common.ui`).
- **`WordRepository`**: Absurdle, ChromaWord, and Betweenle each have their own near-duplicate `WordRepository` wrapping `OptimizedWordRepository` (load, lowercase, dedupe, group-by-length). Betweenle's additionally sorts + binary-searches. These three could share a common base with game-specific extensions.
- **`HowToPlayDialog`, `StatsDialog`, `GameResultCard`/`GameOverCard`/`GameOverBanner`**: same visual pattern (icon/title, share+new-game buttons) reimplemented three times with different color constants. A shared `ResultCard(title, subtitle, accentColor, onShare, onNewGame)` composable would cut a lot of code.
- **Letter-point tables**: `getLetterPoints()` (top-level in `ScrabbleGameApp.kt`) and `letterValues` map (in both `Model.kt`'s `ScrabbleGame` and `AiOpponent.kt`) define the same Scrabble point values three separate times. Should be a single source of truth.
- **Premium-square maps**: `buildPremiumMap()` in `AiOpponent.kt` and `initPremiumSquares()` in `Model.kt`'s `ScrabbleGame` build the identical TW/DW/TL/DL board layout independently. Same for the `premiumType` `when` block duplicated again in `Board.kt`. Three copies of the same 15×15 board layout is a maintenance risk — a single typo fix would need to land in three places.

---

## 4. Design/UX notes

- **Hard mode wording mismatch** (Absurdle): dialog claims "Any revealed hints must be used" but engine only delays all-green wins — consider updating copy or engine.
- **Give-up vs. loss framing** (Absurdle/#3 above): worth a dedicated `enum GameEndReason { WON, GAVE_UP, FORCED_REVEAL }` so the result card never misrepresents how the game ended.
- **Update-checker UI is inert**: `MenuScreen`'s "Word Library Update Available" button and dialog have no real backing data source — either wire it to something or remove it to avoid a crash path (`TODO()`).
- **AI "vocabulary" sampling is non-deterministic per game start** (`AiOpponent.buildVocabulary` uses `.shuffled().take(...)`), so `EASY`/`MEDIUM` AI difficulty will vary in strength run-to-run for the same seed rack — intentional randomness, but if determinism/testability matters, consider seeding.

---

## 5. Suggestions for additional simple word games

All of these can reuse the existing `OptimizedWordRepository` + `WordRepository`-per-game pattern, StateFlow ViewModel structure, and the shared keyboard/result-card components (especially if you do the dedup refactor above first — new games become much cheaper to add).

1. **Anagram Sprint** — show a shuffled word, player has N seconds to type any valid anagram (or the exact original). Very little new engine work: shuffle a word from `byLength`, check `contains()`. Good for a fast, low-friction "daily quickie" alongside the puzzle-style games.

2. **Ladder (Word Ladder)** — transform word A into word B one letter at a time, each step must be a valid dictionary word. Fits your `wordsBetween`/binary-search infra loosely; mostly needs a BFS/graph solver over same-length words differing by 1 letter — reuse `AbsurdleEngine.scorePattern`-style comparison logic to check "differs by exactly one letter."

3. **Hangman, but adversarial** ("Absurdman"?) — pairs naturally with your existing `AbsurdleEngine` philosophy: instead of committing to a word, the engine dodges by always picking the letter-guess bucket with the most surviving candidates, exactly like Absurdle's `chooseBucket`. You could literally reuse `AbsurdleEngine.process`-style bucketing logic with a "letter present/absent" pattern instead of position-based G/Y/X.

4. **Spelling Bee clone** ("Hive" or similar) — given 7 letters (1 mandatory center letter), find all valid words ≥4 letters using only those letters, center letter required. Needs a new scoring/pangram bonus system but reuses the dictionary loader; good "endless single session" game type distinct from your guess-based games.

5. **Connections-style word grouping** — present 16 words, player groups them into 4 hidden categories. This is content-authoring heavy (needs curated category data, not just a flat word list) — bigger lift than the others, mention only if you're open to hand-authoring puzzles or sourcing a puzzle dataset.

6. **Boggle / Word Search grid** — given an NxM letter grid, find all valid dictionary words along adjacent-cell paths. Reuses `contains()` heavily; the new piece is a grid generator + DFS path validator. Great "endless, no daily-limit" companion to your guess-limited games.

7. **Typing-speed word race** — words scroll/appear, player types them before time runs out; score = words-per-minute. Almost zero dictionary logic needed (just a source of words by difficulty/length), mostly a UI/timer exercise — a nice low-effort addition if you want variety without much new engine code.

My pick if you want the smallest lift with the most thematic fit: **#3 (adversarial Hangman)**, since it can literally reuse `AbsurdleEngine`'s bucket-selection logic almost unchanged, and **#1 (Anagram Sprint)** for something quick and different in feel.
