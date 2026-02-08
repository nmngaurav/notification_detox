package aura.notification.filter.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import aura.notification.filter.ai.ClassificationHelper
import aura.notification.filter.data.NotificationEntity
import aura.notification.filter.data.NotificationRepository

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuraNotificationService : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository


    @Inject lateinit var classifier: ClassificationHelper
    @Inject lateinit var heuristicEngine: aura.notification.filter.ai.HeuristicEngine
    @Inject lateinit var focusModeManager: aura.notification.filter.data.FocusModeManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        // Skip self
        if (packageName == "aura.notification.filter") return
        
        val extras = sbn.notification.extras
        // SAFE EXTRACTION: Use getCharSequence() to prevent ClassCastException on Spannable text
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val mainText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isConversation = extras.containsKey(Notification.EXTRA_MESSAGING_PERSON) || 
                           extras.containsKey("android.isConversation") ||
                           sbn.notification.getShortcutId() != null

        val content = if (isGroupSummary) {
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            val latestLine = lines?.lastOrNull()?.toString() ?: ""
            "$mainText $latestLine"
        } else {
            mainText
        }
        
        scope.launch {
            if (sbn.isOngoing || !sbn.isClearable) {
                return@launch
            }

            // 1. Configured Apps Only Logic
            val ruleEntity = repository.getRuleForPackage(packageName) ?: return@launch

            val customRules = ruleEntity.customKeywords.lowercase()
            
            // TIER 1 & 2: Explicit Allowlist (Tags & Keywords)
            val combinedText = "$title $content".lowercase()
            val (isAllowedByTag, tagReason) = if (customRules.isNotEmpty() && customRules.split(",").any { it.trim().isNotEmpty() && combinedText.contains(it.trim()) }) {
                 true to "Custom Rule"
            } else {
                 heuristicEngine.isAllowedByTags(
                    title = title, 
                    content = content, 
                    activeTagsCSV = ruleEntity.activeCategories,
                    packageName = packageName,
                    isConversation = isConversation
                 )
            }
            
            if (isAllowedByTag) {
                Log.d("AuraService", "Allowed by Tag/Rule: $tagReason")
                return@launch
            }
            
            // ... (AI Rescue same as before)
            val aiVerdict = classifier.classify(title, content, packageName)
            
            if (aiVerdict == aura.notification.filter.ai.HeuristicEngine.CAT_CRITICAL) {
                triggerRescueNotification(packageName, title)
                Log.d("AuraService", "RESCUED by AI: $title (Critical)")
                return@launch
            }
            
            Log.d("AuraService", "Blocked by Default: $title (AI Verdict: $aiVerdict)")
            
            if (!isGroupSummary) {
                repository.logNotification(
                    NotificationEntity(
                        packageName = packageName,
                        title = title,
                        content = content,
                        timestamp = System.currentTimeMillis(),
                        category = aiVerdict,
                        isBlocked = true
                    )
                )
            }
            cancelNotification(sbn.key)
        }
    }


    
    // ... rescue notification helper remains ...
    private fun triggerRescueNotification(originPackage: String, originTitle: String) {
        // Implementation kept as is
        val channelId = "aura_rescue"
        val manager = getSystemService(android.app.NotificationManager::class.java)
        // ... (rest of logic same as before, just ensuring we don't delete it unintentionally)
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
            .setSmallIcon(android.R.drawable.ic_dialog_alert) 
            .setContentTitle("Rescue: $originTitle")
            .setContentText("Aura blocked this, but AI safely rescued it.")
            .setAutoCancel(true)
            .build()
            
        manager.notify(originPackage.hashCode(), notification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
    
    // Removed old performSmartCheck which is now obsolete
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
