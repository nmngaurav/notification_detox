package aura.notification.filter.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aura.notification.filter.data.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import aura.notification.filter.ui.dashboard.NotificationBurst

import aura.notification.filter.billing.BillingManager
import aura.notification.filter.util.AppInfoManager
import aura.notification.filter.data.AppRuleEntity
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.data.DetoxCategory
import aura.notification.filter.data.NotificationRepository
import aura.notification.filter.data.FocusMode

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NotificationRepository,
    billingManager: BillingManager,
    private val appInfoManager: AppInfoManager,
    private val focusModeManager: aura.notification.filter.data.FocusModeManager,
    private val classifier: aura.notification.filter.ai.ClassificationHelper,
    private val profileSeeder: aura.notification.filter.util.ProfileSeeder
) : ViewModel() {

    init {
        // Seed default rules on first run
        viewModelScope.launch {
            profileSeeder.seedIfNeeded()
            
            // Trigger 24h retention policy
            val threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            repository.pruneOldNotifications(threshold)
        }
    }

    private val _currentMode = MutableStateFlow(FocusMode.FOCUS)
    val currentMode = _currentMode.asStateFlow()

    private val _isSummarizeMode = MutableStateFlow(true)
    val isSummarizeMode = _isSummarizeMode.asStateFlow()

    fun setMode(mode: aura.notification.filter.data.FocusMode) {
        // No-op V3
    }

    fun toggleSummarizeMode() {
        _isSummarizeMode.value = !_isSummarizeMode.value
    }

    // Per-app View Toggle (Summary vs Detail)
    private val _perAppViewMode = mutableStateMapOf<String, Boolean>() // pkg to isSummarize
    val perAppViewMode: Map<String, Boolean> get() = _perAppViewMode

    fun toggleAppViewMode(packageName: String) {
        val current = _perAppViewMode[packageName] ?: _isSummarizeMode.value
        _perAppViewMode[packageName] = !current
    }

    fun getAppInfo(packageName: String) = appInfoManager.getAppInfo(packageName)
    fun isSystemApp(packageName: String) = appInfoManager.isSystemApp(packageName)

    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)



    fun clearNotificationsForPackage(packageName: String) {
        viewModelScope.launch {
            // In a real implementation, we would mark them as 'read' or delete them
            // repository.deleteNotificationsForPackage(packageName)
        }
    }

    fun muteAppForever(packageName: String) {
        viewModelScope.launch {
            repository.updateRule(
                AppRuleEntity(
                    packageName = packageName,
                    shieldLevel = ShieldLevel.FORTRESS,
                    filterTemplate = DetoxCategory.SOCIAL, // Legacy fallback
                    profileId = _currentMode.value.name,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun allowAppAlways(packageName: String) {
        viewModelScope.launch {
            repository.updateRule(
                AppRuleEntity(
                    packageName = packageName,
                    shieldLevel = ShieldLevel.OPEN,
                    filterTemplate = DetoxCategory.SOCIAL, // Legacy fallback
                    profileId = _currentMode.value.name,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    private val _summaries = mutableStateMapOf<String, String>()
    val summaries: Map<String, String> get() = _summaries

    private val _lastSummaryCounts = mutableMapOf<String, Int>()

    fun toggleSummaryForPackage(packageName: String, notifications: List<NotificationEntity>) {
        val currentlyShowingSummary = _perAppViewMode[packageName] == true
        
        if (currentlyShowingSummary) {
            // If already showing, toggle OFF (back to raw list)
            _perAppViewMode[packageName] = false
        } else {
            // If showing List, toggle ON (to summary)
            _perAppViewMode[packageName] = true
            
            // Generate if needed
            val currentCount = notifications.size
            val lastCount = _lastSummaryCounts[packageName] ?: 0
            
            if (!_summaries.containsKey(packageName) || currentCount > lastCount) {
                viewModelScope.launch {
                    _summaries[packageName] = "Thinking..."
                    _lastSummaryCounts[packageName] = currentCount
                    
                    val contentList = notifications.map { "${it.title}: ${it.content}" }
                    val summary = classifier.summarize(packageName, contentList)
                    _summaries[packageName] = summary
                }
            }
        }
    }



    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Filter out system apps from the UI list
    // Filter out system apps from the UI list
    val blockedNotifications = repository.getBlockedNotifications()
        .map { list -> 
            list.filter { !appInfoManager.isSystemApp(it.packageName) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBlockedCount = blockedNotifications.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val focusTimeSaved = blockedNotifications.map { list ->
        // Weighted algorithm based on category
        list.sumOf { notification ->
            when (notification.category.lowercase()) {
                "social" -> 5      // Social media = 5 min average distraction
                "promotional" -> 1 // Promo emails = 1 min
                "update" -> 2      // App updates = 2 min
                "urgent" -> 0      // These shouldn't be blocked, but just in case
                else -> 2          // Default
            }.toLong() // Explicit cast to avoid ambiguity
        }.toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mostDistractingApp = blockedNotifications.map { list ->
        list.groupBy { it.packageName }
            .maxByOrNull { it.value.size }?.key ?: "None"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "None")

    val groupedNotifications = repository.getBlockedNotifications()
        .map { notifications ->
            notifications.groupBy { it.packageName }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- V2 SMART TIMELINE (Persistent Stacking: 1 App = 1 Card) ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val timelineState = blockedNotifications.map { notifications ->
        notifications.groupBy { it.packageName }
            .map { (packageName, list) ->
                // Sort notifications within stack by newest first
                val sortedNotes = list.sortedByDescending { it.timestamp }
                val latestTimestamp = sortedNotes.first().timestamp
                
                NotificationBurst(
                    id = "stack_$packageName", 
                    packageName = packageName,
                    timestamp = latestTimestamp,
                    notifications = sortedNotes
                )
            }
            .sortedByDescending { it.timestamp } // Global sort: App with newest message at top
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose active rules for Dashboard "Active Filters" list
    // Expose active rules for Dashboard "Active Filters" list (Profile Aware)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeRules = _currentMode.flatMapLatest { mode ->
        repository.getRulesForProfile(mode.name)
    }.map { rules -> 
        rules.filter { it.shieldLevel == ShieldLevel.SMART || it.shieldLevel == ShieldLevel.FORTRESS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // In a real app we would inject BillingManager here or use a shared UseCase
    // For MVP, we'll assume the repository or a separate manager holds this state
    // We will pass it from UI for now or inject BillingManager
    
    fun updateRule(rule: AppRuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun updateSmartRule(packageName: String, profileId: String, categories: String, keywords: String) {
        viewModelScope.launch {
            repository.updateRule(
                AppRuleEntity(
                    packageName = packageName,
                    profileId = profileId,
                    shieldLevel = ShieldLevel.SMART,
                    activeCategories = categories,
                    customKeywords = keywords,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    // FIX: Expose rule fetching for AppNavigation (Keywords Persistence Bug)
    suspend fun getRule(packageName: String): AppRuleEntity? {
        return repository.getRuleForPackage(packageName)
    }

    fun clearActivityForPackage(packageName: String) {
        viewModelScope.launch {
            // Log Fix: Only clear logs, leave rules intact
            repository.clearNotificationsForPackage(packageName)
            
            // Clear local caches for this app's logs
            _summaries.remove(packageName)
            _perAppViewMode.remove(packageName)
            _lastSummaryCounts.remove(packageName)
        }
    }

    fun removeRule(packageName: String) {
        viewModelScope.launch {
            val mode = _currentMode.value.name
            
            // 1. Remove Rule from DB
            repository.deleteRule(packageName, mode)
            
            // 2. Clear Blocked Logs (Bug Fix)
            repository.clearNotificationsForPackage(packageName)
            
            // 3. Clear Local Caches
            _summaries.remove(packageName)
            _perAppViewMode.remove(packageName)
            _lastSummaryCounts.remove(packageName)
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
