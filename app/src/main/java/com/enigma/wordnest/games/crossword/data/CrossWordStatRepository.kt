package com.enigma.wordnest.games.crossword.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.crosswordDataStore: DataStore<Preferences> by preferencesDataStore(name = "crossword_stats")

data class CrosswordStats(
    val puzzlesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    /** Best (lowest) completion time across all solved puzzles, in seconds. */
    val bestTimeSeconds: Int? = null
)

/**
 * Persists Crossword completion stats (DataStore, same pattern as
 * betweenle/chromaword's StatsRepository) plus a single in-progress puzzle
 * save slot (SharedPreferences + Gson, same pattern as Lexicon's
 * ScrabbleGameViewModel.saveGame/resumeGame) so a killed process or a
 * navigation-away doesn't lose an unfinished grid.
 */
class CrosswordStatsRepository(private val context: Context) {

    private object Keys {
        val COMPLETED    = intPreferencesKey("puzzles_completed")
        val STREAK       = intPreferencesKey("current_streak")
        val MAX_STREAK   = intPreferencesKey("max_streak")
        val BEST_TIME    = intPreferencesKey("best_time_seconds")
    }

    val stats: Flow<CrosswordStats> = context.crosswordDataStore.data.map { prefs ->
        CrosswordStats(
            puzzlesCompleted = prefs[Keys.COMPLETED] ?: 0,
            currentStreak    = prefs[Keys.STREAK] ?: 0,
            maxStreak        = prefs[Keys.MAX_STREAK] ?: 0,
            bestTimeSeconds  = prefs[Keys.BEST_TIME]
        )
    }

    suspend fun recordCompletion(elapsedSeconds: Int, current: CrosswordStats) {
        val newStreak = current.currentStreak + 1
        val newBest = current.bestTimeSeconds?.let { minOf(it, elapsedSeconds) } ?: elapsedSeconds
        context.crosswordDataStore.edit { p ->
            p[Keys.COMPLETED]  = current.puzzlesCompleted + 1
            p[Keys.STREAK]     = newStreak
            p[Keys.MAX_STREAK] = maxOf(current.maxStreak, newStreak)
            p[Keys.BEST_TIME]  = newBest
        }
    }

    /** Called if a puzzle is abandoned (new game started without finishing) — resets streak. */
    suspend fun recordAbandon(current: CrosswordStats) {
        context.crosswordDataStore.edit { p -> p[Keys.STREAK] = 0 }
    }
}