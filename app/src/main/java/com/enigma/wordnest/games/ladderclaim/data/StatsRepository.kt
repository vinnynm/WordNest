package com.enigma.wordnest.games.ladderclaim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.enigma.wordnest.games.ladderclaim.model.LadderClaimStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ladderclaim_stats")

class StatsRepository(private val context: Context) {

    private object Keys {
        val GAMES_PLAYED = intPreferencesKey("games_played")
        val P1_WINS = intPreferencesKey("p1_wins")
        val P2_WINS = intPreferencesKey("p2_wins")
        val TIES = intPreferencesKey("ties")
    }

    val stats: Flow<LadderClaimStats> = context.dataStore.data.map { prefs ->
        LadderClaimStats(
            gamesPlayed = prefs[Keys.GAMES_PLAYED] ?: 0,
            player1Wins = prefs[Keys.P1_WINS] ?: 0,
            player2Wins = prefs[Keys.P2_WINS] ?: 0,
            ties = prefs[Keys.TIES] ?: 0
        )
    }

    /** winnerIndex: 0 = player1, 1 = player2, null = tie */
    suspend fun recordResult(winnerIndex: Int?, current: LadderClaimStats) {
        context.dataStore.edit { p ->
            p[Keys.GAMES_PLAYED] = current.gamesPlayed + 1
            p[Keys.P1_WINS] = current.player1Wins + (if (winnerIndex == 0) 1 else 0)
            p[Keys.P2_WINS] = current.player2Wins + (if (winnerIndex == 1) 1 else 0)
            p[Keys.TIES] = current.ties + (if (winnerIndex == null) 1 else 0)
        }
    }
}