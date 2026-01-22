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
        
        val isGroupConversation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)
        } else {
            // Header-based heuristic for older Androids
            title.contains(":") || title.contains("group", ignoreCase = true)
        }

        scope.launch {
            // ... (Ongoing check lines 61-65 skipped in replace, assuming context matches) 
            
            if (sbn.isOngoing || !sbn.isClearable) {
                return@launch
            }

            // 1. Configured Apps Only Logic
            val ruleEntity = repository.getRuleForPackage(packageName)
            if (ruleEntity == null) {
                 Log.v("AuraService", "No rule for $packageName - Allowing passthrough.")
                 return@launch
            }

            val shieldLevel = ruleEntity.shieldLevel
            val activeTags = ruleEntity.activeCategories.split(",").filter { it.isNotEmpty() }.toSet()
            val customRules = ruleEntity.customKeywords.lowercase()
            
            // TIER 1 & 2: Explicit Allowlist (Tags & Keywords)
            val (isAllowedByTag, tagReason) = checkTagsAndRules(title, content, packageName, activeTags, customRules, isGroupConversation)
            
            if (isAllowedByTag) {
                Log.d("AuraService", "Allowed by Tag/Rule: $tagReason")
                return@launch
            }
            
            // If we are here, it is NOT explicitly allowed.
            // DEFAULT: BLOCK.
            // But before blocking, we run TIER 3: AI RESCUE.
            
            // TIER 3: AI Context Rescue (The Safety Net)
            // We ask AI: "Is this CRITICAL?"
            val aiVerdict = classifier.classify(title, content, packageName)
            
            if (aiVerdict == aura.notification.filter.ai.HeuristicEngine.CAT_CRITICAL) {
                // RESCUE!
                triggerRescueNotification(packageName, title) // Optional: Badge it?
                Log.d("AuraService", "RESCUED by AI: $title (Critical)")
                return@launch
            }
            
            // FINAL VERDICT: BLOCK
            // Log it for the user to see in their timeline
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

    // Helper: V8 Checks (Regex/Heuristic)
    private fun checkTagsAndRules(
        title: String, 
        content: String, 
        packageName: String, 
        tags: Set<String>, 
        customRules: String,
        isGroupConversation: Boolean
    ): Pair<Boolean, String> {
        val lowerTitle = title.lowercase()
        val lowerContent = content.lowercase()
        val combined = "$lowerTitle $lowerContent"

        // 1. Custom Keywords (Tier 2 - User Defined)
        if (customRules.isNotEmpty()) {
            val rules = customRules.split(",").map { it.trim() }
            if (rules.any { it.isNotEmpty() && combined.contains(it) }) {
                return true to "Custom Rule"
            }
        }
        
        if (tags.isEmpty()) return false to ""

        // --- SECURITY & CRITICAL ---
        if (tags.contains("OTPs") || tags.contains("Login Codes")) {
            if (combined.contains("otp") || combined.contains("code") || combined.contains("verification") || combined.contains("password") || combined.contains("reset")) return true to "Security"
        }
        if (tags.contains("Calls")) {
            if (combined.contains("call") || combined.contains("voice") || combined.contains("video") || combined.contains("missed")) return true to "Call"
        }
        if (tags.contains("Alarms") || tags.contains("Fraud Alerts")) {
             if (combined.contains("alarm") || combined.contains("alert") || combined.contains("fraud") || combined.contains("suspicious")) return true to "Alarm/Fraud"
        }

        // --- SOCIAL & CHAT (Advanced Group Logic) ---
        // Mentions (@) & Replies work in BOTH DMs and Groups
        if (tags.contains("Mentions") || tags.contains("Replies")) {
             if (combined.contains("@") || combined.contains("replied") || combined.contains("reply")) return true to "Mention/Reply"
        }
        
        // DMs (Strictly 1-on-1)
        if (tags.contains("DMs")) {
            // IF isGroupConversation is FALSE -> It's a DM -> Allow
            // IF isGroupConversation is TRUE -> It's a Group -> Ignore [DM] tag (Must rely on [Group Chats] or [Mentions])
            if (!isGroupConversation) {
                // Heuristic confirmation for API < 28 or raw apps
                val likelyGroup = lowerTitle.contains(":") || lowerTitle.contains("group")
                if (!likelyGroup) return true to "Direct Message"
            }
        }
        
        // Group Chats (Explicit Opt-in)
        if (tags.contains("Group Chats")) {
             if (isGroupConversation || lowerTitle.contains("group") || lowerTitle.contains(":")) return true to "Group Chat"
        }
        
        if (tags.contains("Voice Msgs")) {
            if (combined.contains("voice message") || combined.contains("audio")) return true to "Voice Msg"
        }

        // --- LOGISTICS & TIME ---
        if (tags.contains("Rides") || tags.contains("Traffic")) {
             if (combined.contains("arriving") || combined.contains("driver") || combined.contains("pickup") || combined.contains("traffic") || combined.contains("min away")) return true to "Transport"
        }
        if (tags.contains("Delivery")) {
             if (combined.contains("delivery") || combined.contains("order") || combined.contains("food") || combined.contains("shipped") || combined.contains("out for")) return true to "Delivery"
        }
        if (tags.contains("Reminders") || tags.contains("Calendar")) {
             if (combined.contains("reminder") || combined.contains("event") || combined.contains("meeting") || combined.contains("tomorrow") || combined.contains("starting")) return true to "Schedule"
        }

        // --- MONEY ---
        if (tags.contains("Transaction") || tags.contains("Salary") || tags.contains("Bill Due")) {
             if (combined.contains("spent") || combined.contains("credited") || combined.contains("debited") || combined.contains("salary") || combined.contains("bill") || combined.contains("due")) return true to "Finance"
        }
        if (tags.contains("Offers")) {
             // Dangerous tag! Only allow if explicitly checking for "discount" etc but usually we block this.
             // User explicitly asked for it? If selected, allow.
             if (combined.contains("offer") || combined.contains("discount") || combined.contains("sale")) return true to "Offer"
        }

        // --- SYSTEM ---
        if (tags.contains("Updates") || tags.contains("Downloads") || tags.contains("System")) {
             if (combined.contains("update") || combined.contains("download") || combined.contains("installing") || combined.contains("battery") || combined.contains("usb")) return true to "System"
        }
        if (tags.contains("Reviews")) {
             if (combined.contains("review") || combined.contains("rating") || combined.contains("star") || combined.contains("feedback")) return true to "Review"
        }

        return false to ""
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
