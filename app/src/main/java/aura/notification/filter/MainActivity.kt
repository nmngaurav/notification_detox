package aura.notification.filter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import aura.notification.filter.ui.AppNavigation
import aura.notification.filter.ui.theme.AuraTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import aura.notification.filter.ui.splash.SplashScreen
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: aura.notification.filter.data.SettingsDataStore
    @Inject lateinit var configRepository: aura.notification.filter.data.repository.ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Config (Async)
        lifecycleScope.launch {
            configRepository.initialize()
        }
        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isOnboardingCompleted = settingsDataStore.isOnboardingCompleted.collectAsState(initial = null)
                    
                    val showSplash = remember { mutableStateOf(true) }

                    if (showSplash.value) {
                        SplashScreen(onSplashFinished = { showSplash.value = false })
                    } else {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn()
                        ) {
                            if (isOnboardingCompleted.value != null) {
                                AppNavigation(startDestination = if (isOnboardingCompleted.value == true) "dashboard" else "onboarding")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! Aura is starting...",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AuraTheme {
        Greeting("Android")
    }
}
