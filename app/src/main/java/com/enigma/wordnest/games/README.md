# WordNest 💌

A small, standalone Android app with three word games, pulled out of a bigger
project so they can live on their own:

- **Betweenle** — guess a word that falls alphabetically between two boundary words
- **Absurdle** — the adversarial Wordle that dodges your guesses as long as it can
- **ChromaWord** — Wordle with a richer 6-colour feedback system

## Opening the project

1. Unzip this folder anywhere.
2. Open it in **Android Studio** (File → Open → select the `wordnest` folder).
3. Android Studio will offer to generate the Gradle wrapper if it's missing —
   accept that prompt (or in *Settings → Build Tools → Gradle*, set "Use Gradle
   from" to the IDE's bundled Gradle). The `gradlew` script itself wasn't
   included since it ships as a binary jar I can't hand-write — Studio
   regenerates it automatically on first sync.
4. Sync, then run on a device or emulator (minSdk 26 / Android 8.0+).

## About the dictionary

Each game reads its word list from `app/src/main/res/raw/wordlib500.json` —
a single JSON object of `{"A": ["APPLE", "ANT", ...], "B": [...], ...}`.

I generated this one from your system's hunspell English dictionary
(~48,000 words, lengths 3–10, with a basic profanity filter applied) since
the original `wordlib500.json` / `largelib.json` weren't available to me —
they live in the old app's resources and weren't part of the game source
files you shared. **If you have the original `wordlib500.json`, just drop it
into `res/raw/` with the same name to overwrite mine and you'll get back the
exact original word pool.** Any JSON file with that same letter→list-of-words
shape will work.

(Note: the old `largelib.json` / big Scrabble dictionary isn't needed here —
only Lexicon, which you chose to leave out, used it.)

## What changed from the original source

- Everything now lives under the `com.enigma.wordnest` package instead of
  being nested inside the old `fluffyinc` app.
- Each game's `GameScreen` composable was renamed (`BetweenleGameScreen`,
  `AbsurdleGameScreen`, `ChromaWordGameScreen`) so they're unambiguous from
  `MainActivity`.
- `OptimizedWordRepository` dropped the unused "large dictionary" path
  (none of these 3 games used it), so there's only one word-list resource
  to manage.
- Betweenle's theme (previously oddly under `com.betweenle.game.ui.theme`)
  now lives in the same package structure as the other two games'.
- Added a new `HomeScreen` + `MainActivity` with Navigation Compose so you
  land on a menu and pick a game, instead of each game being its own
  activity buried in a bigger app.
- Stats for Betweenle and ChromaWord still persist locally via DataStore,
  same as before — nothing reset.

## Renaming the app / package

If you want a different app name or package id, in Android Studio:
right-click the `com.enigma.wordnest` package → **Refactor → Rename**,
and separately change `app_name` in `res/values/strings.xml` and
`applicationId` in `app/build.gradle.kts`.
