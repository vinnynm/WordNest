# WordNest Project Audit

**Project Name:** WordNest  
**Platform:** Android (Min SDK 26, Target SDK 35)  
**Package:** `com.enigma.wordnest`  
**Architecture:** MVVM with Jetpack Compose

---

## 1. Executive Summary
WordNest is a standalone Android collection containing three distinct word games: **Absurdle**, **Betweenle**, and **ChromaWord**. Originally extracted from a larger suite, the project has been modernized with a central navigation hub, unified word repository management, and persistent local statistics.

---

## 2. Game Modules

### 🎮 Absurdle (Adversarial Wordle)
*   **Concept:** A "menacing" version of Wordle where the game doesn't have a fixed target word. Instead, it prunes its word list to keep the candidate pool as large as possible based on the user's guesses.
*   **Key Features:**
    *   **Adversarial Engine:** Uses `AbsurdleEngine` to calculate the worst-case response for the player.
    *   **Dynamic UI:** Includes a `CandidateCountBar` that visually represents the remaining pool size.
    *   **Customization:** Supports variable word lengths (3–10) and a "Hard Mode".
    *   **Visual Style:** Dark purple "menacing" theme.

### 🎮 Betweenle (Alphabetical Boundary Game)
*   **Concept:** Players must guess a hidden word that falls alphabetically between two boundary words.
*   **Key Features:**
    *   **Dynamic Boundaries:** The bounds narrow down with every guess (e.g., guessing "MAN" between "APPLE" and "ZOO" might move the lower bound to "MAN").
    *   **Proximity Feedback:** Provides "Distance Hints" (Burning, Hot, Warm, Far) based on the number of words between the guess and the target in the dictionary.
    *   **Stats Tracking:** Persists win/loss records and guess distributions.

### 🎮 ChromaWord (6-Color Wordle)
*   **Concept:** An advanced Wordle variant using a 6-color feedback system for more granular clues.
*   **Key Features:**
    *   **Rich Feedback:** Colors indicate not just "Correct/Present/Absent", but also "Close in Alphabet", "Extra letters", and "Relative position".
    *   **Difficulty Tiers:** Easy (Infinite), Medium (8 guesses), and Hard (6 guesses).
    *   **Flexible Length:** Supports 4 to 8 letter words.
    *   **Legend:** Built-in color legend to help players learn the 6-color system.

---

## 3. Technical Infrastructure

### 📚 Lexicon & Word Processing
*   **Central Repository:** `wordlib500.json` (stored in `res/raw`) acts as the single source of truth for all games.
*   **Optimized Loading:** Words are indexed by starting letter and length to allow fast lookups and random selection.
*   **Word Validity:** All games share a validation logic ensuring guesses exist in the provided dictionary.

### 🧠 State Management
*   **Modern Stack:** Built entirely on **Kotlin Coroutines** and **StateFlow**.
*   **ViewModel Driven:** Each game has a dedicated ViewModel (`AbsurdleViewModel`, `GameViewModel`) that manages game lifecycle, input validation, and result calculation.
*   **Unidirectional Data Flow:** UI observes immutable state objects, ensuring consistency during recompositions.

### 💾 Persistence
*   **DataStore:** Uses `androidx.datastore:datastore-preferences` for lightweight, asynchronous storage of game statistics (streaks, win rates, etc.).

### 🎨 User Interface
*   **Jetpack Compose:** 100% declarative UI.
*   **Responsive Design:** Keyboard components (`AbsurdleKeyboard`, `ChromaKeyboard`) calculate font sizes and key widths dynamically based on screen density.
*   **Unified Navigation:** Uses `navigation-compose` to switch between the Home Menu and individual game screens.

---

## 4. External Dependencies
| Library | Version | Purpose |
| :--- | :--- | :--- |
| **Compose BOM** | 2025.06.00 | Unified Compose versioning |
| **Activity Compose** | 1.10.1 | Integration with Android Activity |
| **Lifecycle** | 2.9.1 | ViewModel and Compose lifecycle support |
| **Navigation** | 2.9.0 | App navigation |
| **DataStore** | 1.1.7 | Local statistics persistence |
| **Gson** | 2.13.1 | JSON parsing for the word library |
| **Kotlin** | 2.1.21 | Language version |

---

## 5. Recent Audit Fixes
*   **Type Safety:** Fixed `TextUnit` coercion errors in keyboard rendering.
*   **Logic Cleanup:** Corrected heuristic calculations in AI opponent modules.
*   **Namespace Alignment:** Standardized all package declarations to `com.enigma.wordnest` to match the project structure.

---
**Audit performed by AI Assistant.**
