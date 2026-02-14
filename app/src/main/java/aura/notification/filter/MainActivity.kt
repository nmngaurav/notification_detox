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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import aura.notification.filter.ui.splash.SplashScreen
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import aura.notification.filter.util.UpdateManager
import aura.notification.filter.util.UpdateStatus
import aura.notification.filter.ui.components.UpdateDialog
import com.google.android.play.core.appupdate.AppUpdateInfo
import android.content.Intent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: aura.notification.filter.data.SettingsDataStore
    @Inject lateinit var configRepository: aura.notification.filter.data.repository.ConfigRepository
    @Inject lateinit var billingManager: aura.notification.filter.billing.BillingManager
    @Inject lateinit var analyticsManager: aura.notification.filter.util.AnalyticsManager

    private lateinit var updateManager: UpdateManager
    private var appUpdateInfo = mutableStateOf<AppUpdateInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Config (Async)
        lifecycleScope.launch {
            configRepository.initialize()
        }

        // Initialize Update Manager
        updateManager = UpdateManager(this)
        lifecycleScope.launch {
            appUpdateInfo.value = updateManager.checkForUpdate()
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
                                AppNavigation(
                                    startDestination = if (isOnboardingCompleted.value == true) "dashboard" else "onboarding",
                                    billingManager = billingManager,
                                    analyticsManager = analyticsManager
                                )
                            }
                        }
                    }

                    // Update Dialog Integration
                    val updateStatus by updateManager.status.collectAsState()
                    val updateProgress by updateManager.progress.collectAsState()
                    val showUpdateDialog = remember { mutableStateOf(true) }

                    if ((updateStatus == UpdateStatus.AVAILABLE || updateStatus == UpdateStatus.DOWNLOADING || updateStatus == UpdateStatus.DOWNLOADED) && showUpdateDialog.value) {
                        LaunchedEffect(updateStatus) {
                             if (updateStatus == UpdateStatus.AVAILABLE) {
                                 analyticsManager.logUpdatePopupView("LEGACY", "NEW") // Version fetching is complex, using placeholders
                             }
                        }
                        UpdateDialog(
                            status = updateStatus,
                            progress = updateProgress,
                            onUpdate = {
                                analyticsManager.logUpdateAction("update")
                                if (updateStatus == UpdateStatus.DOWNLOADED) {
                                    updateManager.completeUpdate()
                                } else {
                                    appUpdateInfo.value?.let { 
                                        updateManager.startFlexibleUpdate(it)
                                    }
                                }
                            },
                            onDismiss = {
                                analyticsManager.logUpdateAction("later")
                                showUpdateDialog.value = false
                            },
                            onBackground = {
                                analyticsManager.logUpdateAction("background")
                                showUpdateDialog.value = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (::updateManager.isInitialized && updateManager.isUpdateInProgress()) {
                // Update downloaded while app was in background
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::updateManager.isInitialized) {
            updateManager.unregister()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UpdateManager.UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                android.util.Log.e("MainActivity", "Update flow failed! Result code: $resultCode")
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
