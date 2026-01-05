package com.aura.ui.digest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.ai.ClassificationHelper
import com.aura.data.NotificationRepository
import com.aura.data.DigestItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigestViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val classificationHelper: ClassificationHelper,
    private val appInfoManager: com.aura.util.AppInfoManager
) : ViewModel() {

    // Grouping: Package -> List<Notification>
    // Output:  Package -> Summary
    
    // We maintain a simple cache to avoid re-summarizing unchanged lists
    // Map<Package, CachedSummary(hash, text)>
    private val summaryCache = mutableMapOf<String, Pair<Int, String>>()

    val digestItems = repository.getBlockedNotifications()
        .map { notifications ->
            // Group by app
            val grouped = notifications.groupBy { it.packageName }
            
            grouped.map { (pkg, notifs) ->
                // Check cache
                val currentHash = notifs.hashCode() // Simple hash of the list
                val cached = summaryCache[pkg]
                
                // Fetch App Icon & Label
                val appInfo = appInfoManager.getAppInfo(pkg)
                
                // Redesign: We do NOT auto-summarize anymore.
                // We default to showing the list. Summary is available ONLY if cached or requested.
                
                val summaryText = if (cached != null && cached.first == currentHash) {
                    cached.second
                } else {
                    null // No summary yet
                }

                DigestItem(
                    packageName = pkg, 
                    label = appInfo.label,
                    icon = appInfo.icon,
                    summary = summaryText, 
                    count = notifs.size,
                    notifications = notifs.sortedByDescending { it.timestamp } // Show newest first
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _updatedSummaries = MutableStateFlow<Map<String, String>>(emptyMap())
    
    // Combine repo data with async AI results
    val uiState = combine(digestItems, _updatedSummaries) { items, summaries ->
        items.map { item ->
            if (summaries.containsKey(item.packageName)) {
                item.copy(summary = summaries[item.packageName]!!)
            } else {
                item
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val debounceJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // Renamed: requestSummary (User Intent)
    fun requestSummary(packageName: String) { 
        val currentItem = uiState.value.find { it.packageName == packageName } ?: return
        val contents = currentItem.notifications.map { "${it.title}: ${it.content}" }
        val hash = currentItem.notifications.hashCode()

        // Debounce/Queue logic
        debounceJobs[packageName]?.cancel()
        val job = viewModelScope.launch {
            // Immediate feedback: "Analyzing..."
            _updatedSummaries.update { map -> map + (packageName to "Analyzing...") }
            
            // No delay needed for explicit user action, but keeping small buffer if they spam click
            // delay(500) 
            
            val summary = classificationHelper.summarize(packageName, contents)
            summaryCache[packageName] = Pair(hash, summary)
            
            _updatedSummaries.update { map -> map + (packageName to summary) }
            debounceJobs.remove(packageName)
        }
        debounceJobs[packageName] = job
    }
    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllBlocked()
        }
    }
}


