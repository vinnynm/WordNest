package com.enigma.wordnest.games.fragment.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.enigma.wordnest.games.fragment.model.FragmentStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fragment_stats")

class StatsRepository(private val context: Context) {
    private object Keys {
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val GAMES_WON = intPreferencesKey("games_won")
        val CUR_STREAK = intPreferencesKey("cur_streak")
        val MAX_STREAK = intPreferencesKey("max_streak")
    }

    val stats: Flow<FragmentStats> = context.dataStore.data.map { p ->
        FragmentStats(
            gamesPlayed = p[Keys.GAMES_PLAYED] ?: 0,
            gamesWon = p[Keys.GAMES_WON] ?: 0,
            currentStreak = p[Keys.CUR_STREAK] ?: 0,
            maxStreak = p[Keys.MAX_STREAK] ?: 0
        )
    }

    suspend fun recordResult(won: Boolean, cur: FragmentStats) {
        val newStreak = if (won) cur.currentStreak + 1 else 0
        context.dataStore.edit { p ->
            p[Keys.GAMES_PLAYED] = cur.gamesPlayed + 1
            p[Keys.GAMES_WON] = cur.gamesWon + (if (won) 1 else 0)
            p[Keys.CUR_STREAK] = newStreak
            p[Keys.MAX_STREAK] = maxOf(cur.maxStreak, newStreak)
        }
    }
}