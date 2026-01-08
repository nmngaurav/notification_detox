package com.aura.ui.onboarding

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
    private val settingsDataStore: com.aura.data.SettingsDataStore,
    val appInfoManager: com.aura.util.AppInfoManager,
    private val repository: com.aura.data.NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
        }
    }
}

data class OnboardingUiState(
    val isLoading: Boolean = false
)
