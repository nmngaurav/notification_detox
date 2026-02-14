package aura.notification.filter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aura.notification.filter.billing.BillingManager
import aura.notification.filter.data.AppRuleEntity
import aura.notification.filter.data.DetoxCategory
import aura.notification.filter.data.NotificationRepository
import aura.notification.filter.data.ShieldLevel
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
    private val appInfoManager: aura.notification.filter.util.AppInfoManager,
    val analyticsManager: aura.notification.filter.util.AnalyticsManager
) : ViewModel() {

    // Fetch all rules for STANDARD profile (default for now)
    // TODO: Dynamic switching based on selected profile
    private val _selectedProfile = MutableStateFlow("FOCUS")
    val selectedProfile = _selectedProfile.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val rules = repository.getRulesForProfile("FOCUS")
        .map { list -> 
            list.sortedBy { rule -> 
                appInfoManager.getAppInfo(rule.packageName).label.lowercase() 
            } 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setProfile(profileId: String) {
        // No-op V3
    }


        
    // Full App List
    private val _installedApps = MutableStateFlow<List<aura.notification.filter.util.AppInfoManager.AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = appInfoManager.getAppsWithNotificationPermission()
        }
    }

    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun getAppInfo(packageName: String) = appInfoManager.getAppInfo(packageName)

    fun updateRule(packageName: String, profileId: String, level: ShieldLevel, keywords: String = "") {
        viewModelScope.launch {
            // Fetch existing to preserve other fields if needed, or overwrite
             val existing = repository.getRule(packageName, profileId)
             repository.updateRule(
                AppRuleEntity(
                    packageName = packageName, 
                    profileId = profileId, 
                    shieldLevel = level, 
                    filterTemplate = existing?.filterTemplate ?: DetoxCategory.SOCIAL, 
                    activeCategories = existing?.activeCategories ?: "",
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateRule(packageName: String, profileId: String, level: ShieldLevel) {
        updateRule(packageName, profileId, level, "")
    }

    fun updateSmartRule(packageName: String, profileId: String, categories: String, keywords: String) {
        viewModelScope.launch {
             // Ensure we are updating the rule for the correct profile
             repository.updateRule(
                AppRuleEntity(
                    packageName = packageName, 
                    profileId = profileId, 
                    shieldLevel = ShieldLevel.SMART, // Force smart if configuring smart
                    activeCategories = categories, 
                    customKeywords = keywords,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteRule(packageName: String, profileId: String) {
        viewModelScope.launch {
            repository.deleteRule(packageName, profileId)
        }
    }
    
    /**
     * Bulk Config: Apply the same configuration to multiple apps at once.
     * This is a premium feature that can be locked behind a paywall.
     * 
     * @param packageNames List of package names to update
     * @param profileId The current focus profile ID
     * @param shieldLevel The shield level to apply (OPEN, SMART, FORTRESS)
     * @param categories Comma-separated category tags to allow
     * @param keywords Comma-separated custom keywords to allow
     */
    fun applyBulkConfig(
        packageNames: List<String>,
        profileId: String,
        shieldLevel: ShieldLevel,
        categories: String,
        keywords: String
    ) {
        viewModelScope.launch {
            packageNames.forEach { packageName ->
                repository.updateRule(
                    AppRuleEntity(
                        packageName = packageName,
                        profileId = profileId,
                        shieldLevel = shieldLevel,
                        activeCategories = categories,
                        customKeywords = keywords,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
