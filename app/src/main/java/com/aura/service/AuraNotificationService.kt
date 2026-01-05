package com.aura.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aura.ai.ClassificationHelper
import com.aura.data.NotificationEntity
import com.aura.data.NotificationRepository
import com.aura.util.ContactUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuraNotificationService : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository
    @Inject lateinit var contactUtils: ContactUtils
    @Inject lateinit var classifier: ClassificationHelper
    @Inject lateinit var heuristicEngine: com.aura.ai.HeuristicEngine
    @Inject lateinit var focusModeManager: com.aura.data.FocusModeManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        // Skip self
        if (packageName == "com.aura") return
        
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val mainText = extras.getString(Notification.EXTRA_TEXT) ?: ""
        
        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        val content = if (isGroupSummary) {
            // Group Summary Logic:
            // Inspect ONLY the LATEST line from history to avoid "Sticky Urgency" (old urgent messages).
            // But still catch the "Current Urgency" if the newest message is critical.
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            val latestLine = lines?.lastOrNull()?.toString() ?: ""
            "$mainText $latestLine"
        } else {
            // Individual Child Notification: Use its own text.
            extras.getString(Notification.EXTRA_TEXT) ?: ""
        }
        
        scope.launch {
            // 0. Check Significant Contacts (System-level VIPs)
            if (contactUtils.isSignificantContact(title)) {
                 return@launch
            }

            // 1. Configured Apps Only Logic
            // If no rule exists for this app in the active profile (or fallback), we Allow it (OPEN).
            // This ensures only "configured" apps are touched by the shield.
            val ruleEntity = repository.getRuleForPackage(packageName)
            if (ruleEntity == null) {
                 Log.v("AuraService", "No rule for $packageName - Allowing passthrough.")
                 return@launch
            }

            val shieldLevel = ruleEntity.shieldLevel
            val filterTemplate = ruleEntity.filterTemplate
            val customKeywords = ruleEntity.customKeywords
            val activeMode = focusModeManager.getMode()
            
            // 2. Shield Logic
            val shouldBlock = when (shieldLevel) {
                com.aura.data.ShieldLevel.OPEN -> false // Always Allow
                com.aura.data.ShieldLevel.FORTRESS -> true // Always Block
                com.aura.data.ShieldLevel.SMART -> {
                     // 2.1 Custom Keyword Override (Super Priority)
                     // If user explicitly said "Allow 'salary'" or "Block 'sale'", respect it instantly.
                     if (customKeywords.isNotEmpty()) {
                         val lowerTitle = title.lowercase()
                         val lowerContent = content.lowercase()
                         val keywords = customKeywords.lowercase().split(",").map { it.trim() }
                         
                         // Check for ALLOW matches (e.g. "urgent, otp")
                         // TODO: We need a way to distinguish Allow vs Block keywords. 
                         // For now, let's assume custom keywords are "Always Allow" overrides for Smart Mode.
                         if (keywords.any { it.isNotEmpty() && (lowerTitle.contains(it) || lowerContent.contains(it)) }) {
                             false // Allow
                         } else {
                             // Proceed to AI
                             performSmartCheck(title, content, packageName, filterTemplate, activeMode)
                         }
                     } else {
                         performSmartCheck(title, content, packageName, filterTemplate, activeMode)
                     }
                }
                com.aura.data.ShieldLevel.NONE -> false
            }

            // Save to DB ONLY if blocked (Smart Logging)
            if (shouldBlock) {
                 // URGENT RESCUE CHECK
                 // Even if blocked by rule, if it looks critical, we alert the user via Aura.
                 val isUrgentInfo = heuristicEngine.fastClassify(packageName, title, content)
                 if (isUrgentInfo == "urgent") {
                     triggerRescueNotification(packageName, title)
                 }
                
                 // FIX: Only log individual notifications to avoid double counting (Summary + Child).
                 // We still cancel/block the summary so it doesn't leak.
                 if (!isGroupSummary) {
                     repository.logNotification(
                        NotificationEntity(
                            packageName = packageName,
                            title = title,
                            content = content,
                            timestamp = System.currentTimeMillis(),
                            category = classifier.classify(title, content, packageName),
                            isBlocked = true
                        )
                    )
                 }
                cancelNotification(sbn.key)
                Log.d("AuraService", "Blocked notification from $packageName: $title")
            } else {
                // If allowed, we do NOT log it to the Aura DB. 
                // It appears in the system tray naturally.
                Log.d("AuraService", "Allowed notification from $packageName")
            }
        }
    }

    private fun triggerRescueNotification(originPackage: String, originTitle: String) {
        val channelId = "aura_rescue"
        val manager = getSystemService(android.app.NotificationManager::class.java)
        
        // Ensure channel exists
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = android.app.NotificationChannel(
                channelId, 
                "Urgent Rescue", 
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for blocked messages that appear urgent."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = android.app.Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // TODO: Use Aura icon
            .setContentTitle("Rescue: $originTitle")
            .setContentText("Aura blocked this from $originPackage, but it looks urgent.")
            .setAutoCancel(true)
            .build()
            
        manager.notify(originPackage.hashCode(), notification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
    
    private suspend fun performSmartCheck(
        title: String, 
        content: String, 
        packageName: String, 
        filterTemplate: com.aura.data.FilterTemplate, 
        activeMode: com.aura.data.FocusMode
    ): Boolean {
         val category = classifier.classify(title, content, packageName)
         
         if (filterTemplate != com.aura.data.FilterTemplate.NONE) {
             return when (filterTemplate) {
                 com.aura.data.FilterTemplate.TRANSACTIONAL -> {
                     category == "social" || category == "entertainment" || category == "promotional"
                 }
                 com.aura.data.FilterTemplate.MESSAGING -> {
                     category == "promotional" || category == "entertainment"
                 }
                 com.aura.data.FilterTemplate.SOCIAL -> {
                     category == "promotional"
                 }
                 com.aura.data.FilterTemplate.WORK -> {
                     category == "social" || category == "entertainment" || category == "promotional"
                 }
                 else -> false
             }
         } else {
             return when (activeMode) {
                com.aura.data.FocusMode.FOCUS -> {
                     category == "social" || category == "promotional" || category == "entertainment"
                }
                com.aura.data.FocusMode.RELAX -> {
                     category == "promotional"
                }
             }
         }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
