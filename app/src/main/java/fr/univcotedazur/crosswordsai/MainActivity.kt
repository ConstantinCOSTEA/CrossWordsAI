package fr.univcotedazur.crosswordsai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.univcotedazur.crosswordsai.ui.theme.CrossWordsAITheme
import fr.univcotedazur.crosswordsai.ui.views.HomeView
import fr.univcotedazur.crosswordsai.ui.views.PictureView
import fr.univcotedazur.crosswordsai.ui.views.SettingsView

/*
Application entry point
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Use all screen view

        // If loading data
        // splashScreen.setKeepOnScreenCondition { condition HERE }
        //TODO : Auth0

        setContent {
            // Set UI Theme
            CrossWordsAITheme {
                // Inside, UI content
                CrossWordsAIApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun CrossWordsAIApp() {
    // Navigation memory item
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // Default navigation bar function
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            // For each navigation item
            AppDestinations.entries.forEach {
                item(
                    // Create an icon (ex: Home Icon)
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    // Button label (ex: Home)
                    label = { Text(it.label) },

                    // Selected item if the Navigation memory item equals
                    selected = it == currentDestination,

                    // Click navigation : notify property changed of 'currentDestination'
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        // Main screen content (above navigation bar)
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeView(Modifier.padding(innerPadding))
                AppDestinations.FAVORITES -> PictureView(Modifier.padding(innerPadding))
                AppDestinations.PROFILE -> SettingsView(Modifier.padding(innerPadding))
            }
        }
    }
}

// Every navigation items
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Camera", Icons.Default.Add),
    PROFILE("Settings", Icons.Default.Settings),
}