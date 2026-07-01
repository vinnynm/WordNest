package com.enigma.wordnest.games.chromaword.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.enigma.wordnest.games.chromaword.model.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chromaword_stats")

class StatsRepository(private val context: Context) {

    private object Keys {
        val GAMES_PLAYED   = intPreferencesKey("games_played")
        val GAMES_WON      = intPreferencesKey("games_won")
        val CUR_STREAK     = intPreferencesKey("cur_streak")
        val MAX_STREAK     = intPreferencesKey("max_streak")
        val GUESS_DIST     = stringPreferencesKey("guess_dist")
    }

    val stats: Flow<Stats> = context.dataStore.data.map { prefs ->
        val distStr = prefs[Keys.GUESS_DIST] ?: List(10) { "0" }.joinToString(",")
        val dist = distStr.split(",").map { it.toIntOrNull() ?: 0 }.padTo(10)
        Stats(
            gamesPlayed = prefs[Keys.GAMES_PLAYED] ?: 0,
            gamesWon = prefs[Keys.GAMES_WON] ?: 0,
            currentStreak = prefs[Keys.CUR_STREAK] ?: 0,
            maxStreak = prefs[Keys.MAX_STREAK] ?: 0,
            guessDistribution = dist
        )
    }

    suspend fun recordResult(won: Boolean, guessCount: Int, cur: Stats) {
        val dist = cur.guessDistribution.toMutableList().padTo(10)
        if (won && guessCount in 1..10) dist[guessCount - 1]++
        val newStreak = if (won) cur.currentStreak + 1 else 0

        context.dataStore.edit { p ->
            p[Keys.GAMES_PLAYED] = cur.gamesPlayed + 1
            p[Keys.GAMES_WON]    = cur.gamesWon + (if (won) 1 else 0)
            p[Keys.CUR_STREAK]   = newStreak
            p[Keys.MAX_STREAK]   = maxOf(cur.maxStreak, newStreak)
            p[Keys.GUESS_DIST]   = dist.joinToString(",")
        }
    }

    private fun List<Int>.padTo(size: Int): MutableList<Int> {
        val r = toMutableList()
        while (r.size < size) r.add(0)
        return r.subList(0, size)
    }
}
