package com.enigma.wordnest.games.betweenle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.enigma.wordnest.games.betweenle.model.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.text.get

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "betweenle_stats")

class StatsRepository(private val context: Context) {

    private object Keys {
        val GAMES_PLAYED     = intPreferencesKey("games_played")
        val GAMES_WON        = intPreferencesKey("games_won")
        val CURRENT_STREAK   = intPreferencesKey("current_streak")
        val MAX_STREAK       = intPreferencesKey("max_streak")
        // Store distribution as comma-separated ints
        val GUESS_DIST       = stringPreferencesKey("guess_distribution")
    }

    val stats: Flow<Stats> = context.dataStore.data.map { prefs ->
        val distStr = prefs[Keys.GUESS_DIST] ?: "0,0,0,0,0,0,0,0,0,0"
        val dist = distStr.split(",").map { it.trim().toIntOrNull() ?: 0 }
        Stats(
            gamesPlayed       = prefs[Keys.GAMES_PLAYED] ?: 0,
            gamesWon          = prefs[Keys.GAMES_WON] ?: 0,
            currentStreak     = prefs[Keys.CURRENT_STREAK] ?: 0,
            maxStreak         = prefs[Keys.MAX_STREAK] ?: 0,
            guessDistribution = dist.padOrTruncate(10)
        )
    }

    suspend fun recordResult(won: Boolean, guessCount: Int, currentStats: Stats) {
        val newDist = currentStats.guessDistribution.toMutableList().padOrTruncate(10)
        if (won && guessCount in 1..10) {
            newDist[guessCount - 1] = newDist[guessCount - 1] + 1
        }
        val newStreak = if (won) currentStats.currentStreak + 1 else 0
        val newMaxStreak = maxOf(currentStats.maxStreak, newStreak)

        context.dataStore.edit { prefs ->
            prefs[Keys.GAMES_PLAYED]   = currentStats.gamesPlayed + 1
            prefs[Keys.GAMES_WON]      = currentStats.gamesWon + (if (won) 1 else 0)
            prefs[Keys.CURRENT_STREAK] = newStreak
            prefs[Keys.MAX_STREAK]     = newMaxStreak
            prefs[Keys.GUESS_DIST]     = newDist.joinToString(",")
        }
    }

    private fun List<Int>.padOrTruncate(size: Int): MutableList<Int> {
        val result = this.toMutableList()
        while (result.size < size) result.add(0)
        return result.subList(0, size)
    }
}
