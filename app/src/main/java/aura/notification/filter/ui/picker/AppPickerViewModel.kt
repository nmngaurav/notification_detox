package aura.notification.filter.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aura.notification.filter.ai.HeuristicEngine
import aura.notification.filter.util.AppInfoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val appInfoManager: AppInfoManager,
    private val heuristicEngine: HeuristicEngine,
    private val repository: aura.notification.filter.data.NotificationRepository,
    billingManager: aura.notification.filter.billing.BillingManager
) : ViewModel() {

    // Helper to determine if a package belongs to any known category
    private fun getCategory(packageName: String): String? {
        for ((category, packages) in heuristicEngine.knownCategories) {
            if (packages.contains(packageName)) return category
        }
        return null
    }

    // State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTab = MutableStateFlow("All")
    val selectedTab: StateFlow<String> = _selectedTab
    
    // Raw Apps
    private val _allApps = MutableStateFlow<List<AppInfoManager.AppInfo>>(emptyList())

    // Dynamic Tabs: Only show tabs that have at least one app
    val tabs = _allApps.map { apps ->
        val categories = mutableSetOf("All")
        apps.forEach { app ->
            val cat = getCategory(app.packageName)
            if (cat != null) categories.add(cat)
        }
        // Ensure fixed order if present
        val order = listOf("All", "Social", "Entertainment", "Finance", "Shopping", "Productivity")
        order.filter { categories.contains(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("All"))
    
    // Filtered Apps (The result)
    val filteredApps = combine(_allApps, _searchQuery, _selectedTab) { apps, query, tab ->
        filterApps(apps, query, tab)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Suggestions (Top Carousel)
    val suggestedApps = _allApps.map { apps ->
        appInfoManager.getSmartOnboardingApps().take(5) // Reuse the "Smart" logic from onboarding
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Limits & Premium
    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Count currently active rules (across all profiles)
    val activeAppCount = repository.getAllRules()
        .map { rules -> rules.map { it.packageName }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val MAX_FREE_APPS = 6

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages = _selectedPackages.asStateFlow()

    // Bug Fix: Real-time count of (Active + Selected) unique apps
    val totalUniqueApps = combine(repository.getAllRules(), _selectedPackages) { rules, selected ->
        val activePackages = rules.map { it.packageName }.toSet()
        (activePackages + selected).size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun enterSelectionMode() { _isSelectionMode.value = true }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPackages.value = emptySet()
    }
    
    fun toggleSelection(packageName: String) {
        val current = _selectedPackages.value
        if (current.contains(packageName)) {
            _selectedPackages.value = current - packageName
        } else {
            _selectedPackages.value = current + packageName
        }
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        _selectedPackages.value = emptySet()
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                appInfoManager.getInstalledApps()
            }
            _allApps.value = apps
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(tab: String) {
        _selectedTab.value = tab
    }
    
    private fun filterApps(apps: List<AppInfoManager.AppInfo>, query: String, tab: String): List<AppInfoManager.AppInfo> {
        // 1. Filter by Tab
        val tabFiltered = if (tab == "All") {
            apps
        } else {
            val packagesInTab = heuristicEngine.knownCategories[tab] ?: emptySet()
            if (packagesInTab.isNotEmpty()) {
                apps.filter { it.packageName in packagesInTab }
            } else {
                apps // Should not happen if tab logic is correct, but fallback
            }
        }
        
        // 2. Filter by Search
        return if (query.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter { 
                it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
        }
    }
}
