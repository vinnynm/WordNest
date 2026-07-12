package com.enigma.wordnest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enigma.wordnest.games.absurdauction.ui.AbsurdAuctionGameScreen
import com.enigma.wordnest.games.lexicon.ScrabbleGameApp
import com.enigma.wordnest.games.lexicon.ui.ScrabbleGameViewModel
import com.enigma.wordnest.ui.HomeScreen
import com.enigma.wordnest.ui.theme.WordNestTheme
import com.enigma.wordnest.games.absurdle.ui.AbsurdleGameScreen
import com.enigma.wordnest.games.absurdman.ui.AbsurdmanGameScreen
import com.enigma.wordnest.games.betweenle.ui.BetweenleGameScreen
import com.enigma.wordnest.games.chromaword.ui.ChromaWordGameScreen
import com.enigma.wordnest.games.codeword.ui.CodewordGameScreen
import com.enigma.wordnest.games.crossword.ui.CrosswordGameScreen
import com.enigma.wordnest.games.fragment.ui.FragmentGameScreen
import com.enigma.wordnest.games.ladderclaim.ui.LadderClaimGameScreen
import com.enigma.wordnest.games.synthetix.SynthetixApp
import com.enigma.wordnest.games.synthetix.SynthetixViewModelFactory
import com.enigma.wordnest.games.synthetix.data.BoardRepository
import com.enigma.wordnest.games.synthetix.data.TileSetRepository
import com.enigma.wordnest.games.synthetix.data.WordDictionaryManager
import com.enigma.wordnest.games.synthetix.network.OnlineMultiplayerManager
import com.enigma.wordnest.games.synthetix.network.WifiMultiplayerManager
import com.enigma.wordnest.games.synthetix.ui.SynthetixViewModel
import com.enigma.wordnest.games.synthetix.ui.theme.ThemeManager
import com.enigma.wordnest.games.wordladder.ui.WordLadderGameScreen
import com.enigma.wordnest.ui.AboutScreen
import kotlin.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrabbleGameViewModel = ScrabbleGameViewModel(application)
        val themeManager by lazy { ThemeManager(applicationContext) }



        val wifiManager by lazy { WifiMultiplayerManager(applicationContext) }
        val onlineManager by lazy { OnlineMultiplayerManager() }

        val viewModel: SynthetixViewModel by viewModels {
            SynthetixViewModelFactory(
                dictionaryManager = WordDictionaryManager(applicationContext),
                boardRepo         = BoardRepository(applicationContext),
                tileSetRepo       = TileSetRepository(applicationContext),
                context           = applicationContext,
                wifiManager       = wifiManager,
                onlineManager     = onlineManager
            )
        }
        enableEdgeToEdge()
        setContent {

            WordNestApp(
                scrabbleGameViewModel,
                SynthetixApp(
                    viewModel = viewModel,
                    themeManager = themeManager,
                    activity = this
                )
            )
        }
    }
}

@Composable
fun WordNestApp(
    scrabbleGameViewModel: ScrabbleGameViewModel,
     app:  @Composable Unit
) {
    val navController = rememberNavController()




    WordNestTheme {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable("betweenle") { BetweenleGameScreen() }
            composable("absurdle") { AbsurdleGameScreen() }
            composable("chromaword") { ChromaWordGameScreen() }
            composable("lexicon") { ScrabbleGameApp(viewModel = scrabbleGameViewModel) }
            composable("Hangman") { AbsurdmanGameScreen() }
            composable("WordLadder") { WordLadderGameScreen() }
            composable("ladder_claim") { LadderClaimGameScreen() }
            composable("crossword") { CrosswordGameScreen() }
            composable("codeword") { CodewordGameScreen() }
            composable("fragment") { FragmentGameScreen() }
            composable("synth") {
                app
            }
            composable("absurd_auction") { AbsurdAuctionGameScreen() }
            composable("about") { AboutScreen(onBack = navController::popBackStack) }
        }
    }
}
