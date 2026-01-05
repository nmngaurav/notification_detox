package com.aura

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
import com.aura.ui.AppNavigation
import com.aura.ui.theme.AuraTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import com.aura.ui.splash.SplashScreen
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: com.aura.data.SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
