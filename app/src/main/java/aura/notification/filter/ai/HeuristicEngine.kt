package aura.notification.filter.ai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicEngine @Inject constructor() {

    // --- UI HELPERS (Package Grouping for Picker) ---
    val knownCategories = mapOf(
        "Social" to setOf("com.whatsapp", "com.instagram.android", "com.facebook.orca", "com.snapchat.android", "org.telegram.messenger", "com.twitter.android", "com.linkedin.android"),
        "Finance" to setOf("com.google.android.apps.nbu.paisa.user", "com.phonepe.app", "net.one97.paytm", "com.sbi.ono", "com.hdfc.mobilebanking"),
        "Shopping" to setOf("com.amazon.mShop.android.shopping", "com.flipkart.android", "com.myntra.android", "com.zomato.android", "com.swiggy.android"),
        "Productivity" to setOf("com.google.android.gm", "com.microsoft.teams", "com.slack", "com.google.android.calendar", "com.google.android.apps.docs"),
        "Entertainment" to setOf("com.google.android.youtube", "com.netflix.mediaclient", "com.spotify.music", "tv.twitch.android")
    )

    // --- SMART TAG CONSTANTS (Premium V2) ---
    companion object {
        // Essentials
        const val TAG_SECURITY = "OTPs & Codes"
        const val TAG_MONEY = "Money"
        const val TAG_UPDATES = "Updates"
        
        // Social
        const val TAG_MESSAGES = "Messages"
        const val TAG_MENTIONS = "Mentions"
        const val TAG_CALLS = "Calls"
        
        // Lifestyle
        const val TAG_ORDERS = "Orders"
        const val TAG_SCHEDULE = "Schedule"
        const val TAG_PROMOS = "Promos"
        const val TAG_NEWS = "News"

        // --- LEGACY CONSTANTS (Required by ClassificationHelper) ---
        const val CAT_CRITICAL = "critical"
        const val CAT_PRODUCTIVITY = "productivity"
        const val CAT_SOCIAL = "social"
        const val CAT_LOGISTICS = "logistics"
        const val CAT_EVENTS = "events"
        const val CAT_GAMIFICATION = "gamification"
        const val CAT_NOISE = "noise"
    }

    /**
     * Safety Net: Checks if content is CRITICAL/SECURITY regardless of user tags.
     * Used by ClassificationHelper as Tier 1 fallback.
     */
    fun isCritical(title: String, content: String): Boolean {
        val combined = "$title $content".lowercase()
        val criticalPatterns = listOf(
            "(?i).*otp.*", "(?i).*verification.*", "(?i).*auth.*", "(?i).*password.*",
            "(?i).*hospital.*", "(?i).*urgent.*", "(?i).*emergency.*", "(?i).*accident.*",
            "(?i).*ambulance.*", "(?i).*police.*", "(?i).*fire.*", "(?i).*sos.*",
            "(?i).*immediate.*", "(?i).*attention.*", "(?i).*bank.*debit.*", "(?i).*card.*lost.*"
        ).map { it.toRegex() }
        
        return criticalPatterns.any { it.containsMatchIn(combined) }
    }

    // --- REGEX PATTERNS (Mapped to Tags) ---
    
    // Tag: Security (OTPs, 2FA)
    private val securityPatterns = listOf(
        "(?i).*otp.*", "(?i).*code.*", "(?i).*verification.*", "(?i).*password.*",
        "(?i).*login.*", "(?i).*auth.*", "(?i).*2fa.*", "(?i).*access.*"
    ).map { it.toRegex() }

    // Tag: Transactions (Money)
    private val transactionPatterns = listOf(
        "(?i).*bank.*", "(?i).*acct.*", "(?i).*debited.*", "(?i).*credited.*", 
        "(?i).*payment.*", "(?i).*card.*", "(?i).*spent.*", "(?i).*balance.*", 
        "(?i).*salary.*", "(?i).*bill.*", "(?i).*due.*", "(?i).*paid.*", "(?i).*txn.*",
        "(?i).*transaction.*"
    ).map { it.toRegex() }

    // Tag: Mentions (Social)
    private val mentionPatterns = listOf(
        "(?i).*@.*", "(?i).*replied.*", "(?i).*reply.*", "(?i).*sent you.*", 
        "(?i).*tagged.*", "(?i).*commented.*", "(?i).*mentioned.*"
    ).map { it.toRegex() }

    // Tag: Direct Msgs (Chat) - *Requires Context (not Group)*
    private val dmPatterns = listOf(
        "(?i).*message.*", "(?i).*chat.*", "(?i).*photo.*", "(?i).*video.*", 
        "(?i).*sticker.*", "(?i).*audio.*", "(?i).*attachment.*"
    ).map { it.toRegex() }

    // Tag: Deliveries
    private val deliveryPatterns = listOf(
        "(?i).*delivery.*", "(?i).*arriving.*", "(?i).*picked up.*", "(?i).*out for.*",
        "(?i).*package.*", "(?i).*track.*", "(?i).*driver.*", "(?i).*order.*", 
        "(?i).*shipment.*", "(?i).*arrived.*", "(?i).*delivered.*"
    ).map { it.toRegex() }

    // Tag: Calls
    private val callPatterns = listOf(
        "(?i).*call.*", "(?i).*voice.*", "(?i).*video.*", "(?i).*missed.*", 
        "(?i).*incoming.*", "(?i).*dialing.*", "(?i).*ringing.*"
    ).map { it.toRegex() }

    // Tag: Events
    private val eventPatterns = listOf(
        "(?i).*remind.*", "(?i).*meeting.*", "(?i).*schedule.*", "(?i).*event.*", 
        "(?i).*start.*", "(?i).*tomorrow.*", "(?i).*calendar.*", "(?i).*today.*",
        "(?i).*zoom.*", "(?i).*teams.*"
    ).map { it.toRegex() }

    // Tag: Offers
    private val offerPatterns = listOf(
        "(?i).*offer.*", "(?i).*sale.*", "(?i).*discount.*", "(?i).*promo.*", 
        "(?i).*deal.*", "(?i).*free.*", "(?i).*off.*", "(?i).*coupon.*",
        "(?i).*cashback.*", "(?i).*reward.*", "(?i).*win.*", "(?i).*gift.*"
    ).map { it.toRegex() }

    /**
     * V2 Classification: Returns TRUE if the notification matches an ACTIVE Smart Tag.
     * Returns FALSE if it should be blocked (unless rescued by AI later).
     * 
     * @param packageName The app origin for context-aware filtering.
     * @param isConversation Whether the OS detected this as a MessagingStyle/Conversation notification.
     */
    fun isAllowedByTags(
        title: String, 
        content: String, 
        activeTagsCSV: String,
        packageName: String = "",
        isConversation: Boolean = false
    ): Pair<Boolean, String> {
        if (activeTagsCSV.isBlank()) return false to ""
        
        val combined = "$title $content"
        val activeTags = activeTagsCSV.split(",").map { it.trim() }.toSet()
        val isSocialApp = knownCategories["Social"]?.contains(packageName) == true

        // 1. Essentials
        if (activeTags.contains(TAG_SECURITY)) {
            if (securityPatterns.any { it.containsMatchIn(combined) }) return true to TAG_SECURITY
        }
        if (activeTags.contains(TAG_MONEY)) {
            if (transactionPatterns.any { it.containsMatchIn(combined) }) return true to TAG_MONEY
        }
        if (activeTags.contains(TAG_UPDATES)) {
             if (Regex("(?i).*(update|download|installing|battery|usb|system).*").containsMatchIn(combined)) return true to TAG_UPDATES
        }

        // 2. Social (Redesigned with Context Awareness)
        if (activeTags.contains(TAG_MESSAGES)) {
             // NOISE REDUCTION: Ignore system notifications from social apps
             val isNoise = Regex("(?i).*(checking for|syncing|connected|status|backup).*").containsMatchIn(combined)
             
             if (!isNoise) {
                 // ALLOW if:
                 // A) Structurally a conversation (MessagingStyle)
                 // B) Keyword match in dmPatterns
                 // C) From a Social App AND has reasonable chat content length (not just a system alert)
                 if (isConversation || 
                     dmPatterns.any { it.containsMatchIn(combined) } || 
                     (isSocialApp && combined.length > 2 && !combined.contains("WhatsApp", ignoreCase = true))
                 ) {
                     return true to TAG_MESSAGES
                 }
             }
        }
        if (activeTags.contains(TAG_MENTIONS)) {
            if (mentionPatterns.any { it.containsMatchIn(combined) }) return true to TAG_MENTIONS
        }
        if (activeTags.contains(TAG_CALLS)) {
            if (callPatterns.any { it.containsMatchIn(combined) }) return true to TAG_CALLS
        }

        // 3. Lifestyle
        if (activeTags.contains(TAG_ORDERS)) {
            if (deliveryPatterns.any { it.containsMatchIn(combined) }) return true to TAG_ORDERS
        }
        if (activeTags.contains(TAG_SCHEDULE)) {
            if (eventPatterns.any { it.containsMatchIn(combined) }) return true to TAG_SCHEDULE
        }
        if (activeTags.contains(TAG_PROMOS)) {
            if (offerPatterns.any { it.containsMatchIn(combined) }) return true to TAG_PROMOS
        }
        if (activeTags.contains(TAG_NEWS)) {
             if (Regex("(?i).*(news|breaking|update|alert).*").containsMatchIn(combined)) return true to TAG_NEWS
        }

        return false to ""
    }
    
    /**
     * Helper to get list of tags for UI
     */
    fun getAllSmartTags(): List<String> = listOf(
        TAG_SECURITY, TAG_MONEY, TAG_UPDATES,
        TAG_MESSAGES, TAG_MENTIONS, TAG_CALLS,
        TAG_ORDERS, TAG_SCHEDULE, TAG_PROMOS, TAG_NEWS
    )

    fun getCategorizedTags(): Map<String, List<TagMetadata>> = mapOf(
        "Essentials" to listOf(
            TagMetadata(TAG_SECURITY, "Security", "OTPs, codes & security alerts"),
            TagMetadata(TAG_MONEY, "Finance", "Bank alerts & transactions"),
            TagMetadata(TAG_UPDATES, "Updates", "System & app updates")
        ),
        "Social" to listOf(
            TagMetadata(TAG_MESSAGES, "Messages", "Direct chats & threads"),
            TagMetadata(TAG_MENTIONS, "Mentions", "@Mentions & replies"),
            TagMetadata(TAG_CALLS, "Calls", "Voice & video calls")
        ),
        "Lifestyle" to listOf(
            TagMetadata(TAG_ORDERS, "Logistics", "Delivery & order updates"),
            TagMetadata(TAG_SCHEDULE, "Events", "Calendar & reminders"),
            TagMetadata(TAG_PROMOS, "Offers", "Sales & promotions"),
            TagMetadata(TAG_NEWS, "News", "Breaking news & global alerts")
        )
    )

    data class TagMetadata(val id: String, val label: String, val description: String)
}
