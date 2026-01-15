package fr.miage.m1.crosswordsai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.miage.m1.crosswordsai.ui.theme.CrossWordsAITheme
import fr.miage.m1.crosswordsai.ui.views.CrossedWordsView
import fr.miage.m1.crosswordsai.ui.views.HistoryView
import fr.miage.m1.crosswordsai.ui.views.HomeView
import fr.miage.m1.crosswordsai.ui.views.PictureView
import fr.miage.m1.crosswordsai.viewmodel.CrosswordViewModel

/**
 * Application entry point
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CrossWordsAITheme {
                CrossWordsAIApp()
            }
        }
    }
}

/**
 * Navigation screens
 */
sealed class Screen {
    object Home : Screen()
    object Solving : Screen()
    object Grid : Screen()
    object History : Screen()
}

@PreviewScreenSizes
@Composable
fun CrossWordsAIApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val crosswordViewModel: CrosswordViewModel = viewModel()

    // Handle back button
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            Screen.Solving -> {
                crosswordViewModel.reset()
                currentScreen = Screen.Home
            }
            Screen.Grid -> currentScreen = Screen.Home
            Screen.History -> currentScreen = Screen.Home
            else -> {}
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            Screen.Home -> HomeView(
                modifier = Modifier.padding(innerPadding),
                onSolveClick = { currentScreen = Screen.Solving },
                onHistoryClick = { currentScreen = Screen.History }
            )
            Screen.Solving -> PictureView(
                modifier = Modifier.padding(innerPadding),
                viewModel = crosswordViewModel,
                onNavigateToGrid = { currentScreen = Screen.Grid },
                onBack = {
                    crosswordViewModel.reset()
                    currentScreen = Screen.Home
                }
            )
            Screen.Grid -> CrossedWordsView(
                modifier = Modifier.padding(innerPadding),
                viewModel = crosswordViewModel,
                onBack = { currentScreen = Screen.Home }
            )
            Screen.History -> HistoryView(
                modifier = Modifier.padding(innerPadding),
                onBack = { currentScreen = Screen.Home },
                onSelectPuzzle = { gridData ->
                    crosswordViewModel.loadFromHistory(gridData)
                    currentScreen = Screen.Grid
                }
            )
        }
    }
}
