package com.enigma.wordnest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enigma.wordnest.games.lexicon.ScrabbleGameApp
import com.enigma.wordnest.games.lexicon.ui.ScrabbleGameViewModel
import com.enigma.wordnest.ui.HomeScreen
import com.enigma.wordnest.ui.theme.WordNestTheme
import com.enigma.wordnest.games.absurdle.ui.AbsurdleGameScreen
import com.enigma.wordnest.games.betweenle.ui.BetweenleGameScreen
import com.enigma.wordnest.games.chromaword.ui.ChromaWordGameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrabbleGameViewModel = ScrabbleGameViewModel(application)
        enableEdgeToEdge()
        setContent {
            WordNestApp(scrabbleGameViewModel)
        }
    }
}

@Composable
fun WordNestApp(scrabbleGameViewModel: ScrabbleGameViewModel) {
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
        }
    }
}
