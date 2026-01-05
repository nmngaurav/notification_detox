package com.aura.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.data.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.aura.billing.BillingManager
import com.aura.util.AppInfoManager
import com.aura.data.AppRuleEntity
import com.aura.data.ShieldLevel
import com.aura.data.FilterTemplate
import com.aura.data.NotificationRepository

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NotificationRepository,
    billingManager: BillingManager,
    private val appInfoManager: AppInfoManager,
    private val focusModeManager: com.aura.data.FocusModeManager,
    private val classifier: com.aura.ai.ClassificationHelper,
    private val profileSeeder: com.aura.util.ProfileSeeder
) : ViewModel() {

    init {
        // Seed default rules on first run
        viewModelScope.launch {
            profileSeeder.seedIfNeeded()
        }
    }

    private val _activeMode = MutableStateFlow(focusModeManager.getMode())
    val activeMode = _activeMode.asStateFlow()

    fun setFocusMode(mode: com.aura.data.FocusMode) {
        focusModeManager.setMode(mode)
        _activeMode.value = mode
    }

    fun getAppInfo(packageName: String) = appInfoManager.getAppInfo(packageName)
    fun isSystemApp(packageName: String) = appInfoManager.isSystemApp(packageName)

    val isPro = billingManager.isPro
        .map { true } // Force TRUE for User Testing/Demo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _currentMode = MutableStateFlow(focusModeManager.getMode())
    val currentMode = _currentMode.asStateFlow()

    fun setMode(mode: com.aura.data.FocusMode) {
        focusModeManager.setMode(mode)
        _currentMode.value = mode
    }

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
                    filterTemplate = FilterTemplate.NONE,
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
                    filterTemplate = FilterTemplate.NONE,
                    profileId = _currentMode.value.name,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    private val _summaries = mutableStateMapOf<String, String>()
    val summaries: Map<String, String> get() = _summaries

    fun generateSummaryForPackage(packageName: String, notifications: List<NotificationEntity>) {
        if (_summaries.containsKey(packageName)) return // Already summarized
        
        viewModelScope.launch {
            _summaries[packageName] = "Thinking..."
            val contentList = notifications.map { "${it.title}: ${it.content}" }
            val summary = classifier.summarize(packageName, contentList)
            _summaries[packageName] = summary
        }
    }

    fun allowConversation(packageName: String) {
        viewModelScope.launch {
            repository.updateRule(
                AppRuleEntity(
                    packageName = packageName,
                    profileId = "STANDARD",
                    shieldLevel = ShieldLevel.OPEN,
                    filterTemplate = FilterTemplate.NONE,
                    lastUpdated = System.currentTimeMillis()
                )
            )
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

    // In a real app we would inject BillingManager here or use a shared UseCase
    // For MVP, we'll assume the repository or a separate manager holds this state
    // We will pass it from UI for now or inject BillingManager
}
