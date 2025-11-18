package fr.univcotedazur.crosswordsai

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.WebAuthProvider
import fr.univcotedazur.crosswordsai.ui.theme.CrossWordsAITheme
import fr.univcotedazur.crosswordsai.ui.views.HomeView
import fr.univcotedazur.crosswordsai.ui.views.PictureView
import fr.univcotedazur.crosswordsai.ui.views.SettingsView
import kotlinx.coroutines.launch

/*
Application entry point
 */
class MainActivity : ComponentActivity() {
    private lateinit var auth0: Auth0
    private val sessionState = mutableStateOf(UserSession())

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Use all screen view

        Log.d("AUTH", "TEST")

        // If loading data
        // splashScreen.setKeepOnScreenCondition { condition HERE }
        auth0 = Auth0.getInstance(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )

        setContent {
            // Set UI Theme
            CrossWordsAITheme {
                // Inside, UI content
                CrossWordsAIApp(
                    session = sessionState.value,
                    onLogin = { login() },
                    onLogout = { logout() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (WebAuthProvider.resume(intent)) {
            Log.d("AUTH", "Auth0 a capturé l'intent !")
            return
        }
    }

    private fun login() {
        lifecycleScope.launch {
            Log.d("AUTH", "LOGIN STARTED")
            try {
                val credentials = WebAuthProvider.login(auth0)
                    .withScheme(getString(R.string.com_auth0_scheme))
                    .withScope("openid profile email offline_access")
                    .await(this@MainActivity)

                sessionState.value = sessionState.value.copy(
                    isAuthenticated = true,
                    userName = credentials.user.givenName ?: credentials.user.nickname ?: credentials.user.email,
                    accessToken = credentials.accessToken
                )
            } catch (exception: AuthenticationException) {
                Log.e("AUTH", "LOGIN ERROR", exception)
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                WebAuthProvider.logout(auth0)
                    .withScheme(getString(R.string.com_auth0_scheme))
                    .await(this@MainActivity)

                sessionState.value = sessionState.value.copy(
                    isAuthenticated = false,
                    userName = null,
                    accessToken = null
                )

                Log.d("AUTH", "LOGOUT SUCCESS")
            } catch (exception: AuthenticationException) {
                Log.d("AUTH", "LOGOUT ERROR", exception)
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun CrossWordsAIApp(
    session: UserSession = UserSession(),
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
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
                AppDestinations.HOME -> HomeView(Modifier.padding(innerPadding), session.userName)
                AppDestinations.FAVORITES -> PictureView(Modifier.padding(innerPadding))
                AppDestinations.PROFILE -> SettingsView(
                    modifier = Modifier.padding(innerPadding),
                    isLoggedIn = session.isAuthenticated,
                    onLogin = onLogin,
                    onLogout = onLogout
                )
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

data class UserSession(
    val isAuthenticated: Boolean = false,
    val userName: String? = null,
    val accessToken: String? = null
)