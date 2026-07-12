package com.enigma.wordnest.games.synthetix

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enigma.wordnest.games.synthetix.data.BoardRepository
import com.enigma.wordnest.games.synthetix.data.TileSetRepository
import com.enigma.wordnest.games.synthetix.data.WordDictionaryManager
import com.enigma.wordnest.games.synthetix.ui.SynthetixViewModel

import com.enigma.wordnest.games.synthetix.network.OnlineMultiplayerManager
import com.enigma.wordnest.games.synthetix.network.WifiMultiplayerManager

class SynthetixViewModelFactory(
    val dictionaryManager: WordDictionaryManager,
    val boardRepo: BoardRepository,
    val tileSetRepo: TileSetRepository,
    val context: Context,
    val wifiManager: WifiMultiplayerManager,
    val onlineManager: OnlineMultiplayerManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SynthetixViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SynthetixViewModel(
                dictionaryManager = dictionaryManager,
                boardRepo         = boardRepo,
                tileSetRepo       = tileSetRepo,
                context           = context,
                wifiManager       = wifiManager,
                onlineManager     = onlineManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
