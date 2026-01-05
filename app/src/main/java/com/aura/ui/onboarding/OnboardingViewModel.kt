package com.aura.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.util.ContactUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactUtils: ContactUtils,
    private val settingsDataStore: com.aura.data.SettingsDataStore,
    val appInfoManager: com.aura.util.AppInfoManager,
    private val repository: com.aura.data.NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(contactPermissionGranted = isGranted)
        if (isGranted) {
            loadContacts()
        }
    }

    private fun loadContacts() {
        // Mock loading or real loading
        // In a real app we would load contacts here
        _uiState.value = _uiState.value.copy(
            contacts = listOf("Mom", "Partner", "Boss") // Placeholder
        )
    }

    fun toggleContact(contact: String) {
        val current = _uiState.value.selectedContacts.toMutableSet()
        if (current.contains(contact)) {
            current.remove(contact)
        } else {
            current.add(contact)
        }
        _uiState.value = _uiState.value.copy(selectedContacts = current)
    }
    
    fun saveInitialShieldConfigs(configs: List<com.aura.ui.onboarding.AppShieldConfig>) {
        viewModelScope.launch {
            // Save each app's shield config to database
            // Save to both FOCUS and RELAX profiles so user has a starting point for both
            val profiles = listOf("FOCUS", "RELAX")
            
            profiles.forEach { profile ->
                configs.forEach { config ->
                    val rule = com.aura.data.AppRuleEntity(
                        packageName = config.packageName,
                        profileId = profile, 
                        shieldLevel = config.shieldLevel,
                        filterTemplate = config.suggestedTemplate,
                        customKeywords = config.customKeywords,
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.updateRule(rule)
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
        }
    }
}

data class OnboardingUiState(
    val contactPermissionGranted: Boolean = false,
    val contacts: List<String> = emptyList(),
    val selectedContacts: Set<String> = emptySet()
)
