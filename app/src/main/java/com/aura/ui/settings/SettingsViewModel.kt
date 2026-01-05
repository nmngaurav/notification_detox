package com.aura.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.billing.BillingManager
import com.aura.data.AppRuleEntity
import com.aura.data.FilterTemplate
import com.aura.data.NotificationRepository
import com.aura.data.ShieldLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    val billingManager: BillingManager,
    private val appInfoManager: com.aura.util.AppInfoManager
) : ViewModel() {

    // Fetch all rules for STANDARD profile (default for now)
    // TODO: Dynamic switching based on selected profile
    private val _selectedProfile = MutableStateFlow("FOCUS")
    val selectedProfile = _selectedProfile.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val rules = _selectedProfile.flatMapLatest { profile ->
        repository.getRulesForProfile(profile)
            .map { list -> 
                list.sortedBy { rule -> 
                    appInfoManager.getAppInfo(rule.packageName).label.lowercase() 
                } 
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setProfile(profileId: String) {
        _selectedProfile.value = profileId
    }


        
    // Full App List
    private val _installedApps = MutableStateFlow<List<com.aura.util.AppInfoManager.AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = appInfoManager.getInstalledApps()
        }
    }

    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun getAppInfo(packageName: String) = appInfoManager.getAppInfo(packageName)

    fun updateRule(packageName: String, profileId: String, level: ShieldLevel) {
        viewModelScope.launch {
            // Fetch existing rule for the SPECIFIC profile to preserve fields
             val existing = repository.getRule(packageName, profileId)
             repository.updateRule(
                AppRuleEntity(
                    packageName = packageName, 
                    profileId = profileId, 
                    shieldLevel = level, 
                    filterTemplate = existing?.filterTemplate ?: FilterTemplate.NONE, 
                    customKeywords = existing?.customKeywords ?: "",
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateSmartRule(packageName: String, profileId: String, template: FilterTemplate, keywords: String) {
        viewModelScope.launch {
             // Ensure we are updating the rule for the correct profile
             repository.updateRule(
                AppRuleEntity(
                    packageName = packageName, 
                    profileId = profileId, 
                    shieldLevel = ShieldLevel.SMART, // Force smart if configuring smart
                    filterTemplate = template, 
                    customKeywords = keywords,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
}
