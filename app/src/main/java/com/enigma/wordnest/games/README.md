# WordNest 💌

A standalone Android app featuring a collection of seven curated word games, unified under a modern Compose-based architecture.

## The Games

- **Betweenle** — Guess a word that falls alphabetically between two shifting boundary words.
- **Absurdle** — The adversarial Wordle. It doesn't have a secret word; it actively dodges your guesses by shrinking its word pool to keep you guessing as long as possible.
- **ChromaWord** — Wordle with a rich 6-color feedback system that provides granular hints about character proximity and position.
- **Lexicon** — A deep, two-player word strategy game (Scrabble-like) featuring a challenging AI opponent and local pass-and-play.
- **WordLadder** — A classic puzzle where you transform one word into another by changing exactly one letter at a time.
- **Hangman** — (Absurdman) An adversarial take on the classic letter-guessing game.
- **Ladder Claim** — A strategic territory control game where you "claim" board space by building valid word ladders.

## Opening the Project

1. Open the root folder in **Android Studio**.
2. Android Studio will offer to generate the Gradle wrapper or sync the project — accept the prompt.
3. Once synced, run on a device or emulator (**minSdk 26 / Android 8.0+**).

## About the Dictionaries

The app uses an optimized repository system to manage multiple word libraries:
- `wordlib500.json`: The primary library used for most games, indexed for fast length and letter-based lookups.
- `largelib_gb_augmented.json`: A massive dictionary used by **Lexicon** for comprehensive word-legality checks.

These files are located in `app/src/main/res/raw/`. Any JSON file following the `{"LETTER": ["WORD1", "WORD2"]}` structure can be used to customize the game's vocabulary.

## Technical Highlights

- **Unified Architecture**: All games follow a strict MVVM pattern using Kotlin StateFlow and Jetpack Compose.
- **Shared Components**: Unified UI infrastructure, including a generic `GameKeyboard` and `GameOverTemplate` to ensure a consistent look and feel.
- **Hard Mode**: Absurdle features a fully enforced Hard Mode where players must respect all revealed hints while the engine actively avoids committing to a winning word.
- **Persistence**: Statistics for all games (streaks, win rates, etc.) are persisted locally using **Jetpack DataStore**.
- **Modern Package Structure**: Everything is organized under the `com.enigma.wordnest` namespace for clean modularity.

## Renaming the App / Package

To change the app name or package ID in Android Studio:
1. Right-click the `com.enigma.wordnest` package → **Refactor → Rename**.
2. Update `app_name` in `res/values/strings.xml`.
3. Update `applicationId` in `app/build.gradle.kts`.
