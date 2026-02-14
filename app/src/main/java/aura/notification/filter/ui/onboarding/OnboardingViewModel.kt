package aura.notification.filter.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: aura.notification.filter.data.SettingsDataStore,
    val appInfoManager: aura.notification.filter.util.AppInfoManager,
    private val repository: aura.notification.filter.data.NotificationRepository,
    private val analyticsManager: aura.notification.filter.util.AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
            analyticsManager.logOnboardingComplete()
        }
    }

    fun startOnboarding() {
        analyticsManager.logOnboardingStart()
    }
}

data class OnboardingUiState(
    val isLoading: Boolean = false
)
