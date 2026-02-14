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

    // --- SMART TAG CONSTANTS (Refined V4) ---
    companion object {
        // Category: Safety & Finance
        const val TAG_SECURITY = "Security"
        const val TAG_MONEY = "Finance"
        const val TAG_EMERGENCY = "Emergency"
        const val TAG_LOGISTICS = "Logistics"
        
        // Category: Personal Inbox
        const val TAG_MESSAGES = "Direct Chats"
        const val TAG_GROUPS = "Group Threads"
        const val TAG_MENTIONS = "Mentions"
        const val TAG_CALLS = "Calls"
        
        // Category: Work & Planning
        const val TAG_WORK = "Work"
        const val TAG_SCHEDULE = "Meetings"
        const val TAG_DOCS = "Documents"
        const val TAG_STORAGE = "Storage"

        // Category: Activity & Home
        const val TAG_IOT = "Smart Home"
        const val TAG_FITNESS = "Health"
        const val TAG_TRAVEL = "Transport"
        const val TAG_ORDERS = "Shopping"

        // Category: Content & Awareness
        const val TAG_ENTERTAINMENT = "Entertainment"
        const val TAG_NEWS = "Headlines"
        const val TAG_UPDATES = "Updates"
        const val TAG_PROMOS = "Promotions"

    }

    /**
     * Safety Net: Checks if content is CRITICAL/SECURITY regardless of user tags.
     */
    fun isCritical(title: String, content: String): Boolean {
        val combined = "$title $content".lowercase()
        val criticalPatterns = listOf(
            "otp", "verification", "auth", "password",
            "hospital", "urgent", "emergency", "accident",
            "ambulance", "police", "fire", "sos",
            "immediate", "attention", "bank.*debit", "card.*lost"
        ).map { it.toRegex(RegexOption.IGNORE_CASE) }
        
        return criticalPatterns.any { it.containsMatchIn(combined) }
    }

    // --- REGEX PATTERNS (Refined V4) ---
    
    // Tag: Security
    private val securityPatterns = listOf("otp", "code", "verification", "password", "login", "auth", "2fa", "access").map { "(?i).*$it.*".toRegex() }

    // Tag: Finance
    private val financePatterns = listOf("bank", "acct", "debited", "credited", "payment", "card", "spent", "balance", "salary", "bill", "due", "paid", "txn", "transaction").map { "(?i).*$it.*".toRegex() }

    // Tag: Emergency
    private val emergencyPatterns = listOf("emergency", "sos", "hospital", "police", "fire", "accident", "ambulance", "safety", "urgent").map { "(?i).*$it.*".toRegex() }

    // Tag: Logistics (Deliveries)
    private val logisticsPatterns = listOf("delivery", "arriving", "picked up", "out for", "package", "track", "driver", "order", "shipment", "arrived", "delivered", "food", "zomato", "swiggy", "ubereats", "door dash").map { "(?i).*$it.*".toRegex() }

    // Tag: Direct Chats (1-on-1)
    private val dmPatterns = listOf("message", "photo", "video", "sticker", "audio", "attachment", "sent you").map { "(?i).*$it.*".toRegex() }

    // Tag: Group Threads
    private val groupPatterns = listOf("group", "thread", "everyone", "added you", "channel").map { "(?i).*$it.*".toRegex() }

    // Tag: Mentions
    private val mentionPatterns = listOf("@", "replied", "commented", "mentioned", "liked", "followed", "reply").map { "(?i).*$it.*".toRegex() }

    // Tag: Calls
    private val callPatterns = listOf("call", "voice", "video", "missed", "incoming", "dialing", "ringing").map { "(?i).*$it.*".toRegex() }

    // Tag: Work
    private val workPatterns = listOf("slack", "teams", "jira", "assigned", "collab", "mention", "workspace", "meeting", "approver", "ticket").map { "(?i).*$it.*".toRegex() }

    // Tag: Meetings
    private val meetingPatterns = listOf("remind", "meeting", "schedule", "event", "start", "tomorrow", "calendar", "today", "zoom", "meet", "appointment").map { "(?i).*$it.*".toRegex() }

    // Tag: Documents
    private val docPatterns = listOf("document", "doc", "pdf", "sheet", "xlsx", "csv", "share", "comment", "editor", "shared with you").map { "(?i).*$it.*".toRegex() }

    // Tag: Storage
    private val storagePatterns = listOf("storage", "memory", "full", "backup", "sync", "cleaning", "disk", "cloud").map { "(?i).*$it.*".toRegex() }

    // Tag: Smart Home
    private val iotPatterns = listOf("doorbell", "motion", "camera", "light", "thermostat", "unlocked", "front door", "ring", "nest").map { "(?i).*$it.*".toRegex() }

    // Tag: Health
    private val healthPatterns = listOf("medication", "pill", "meds", "step", "goal", "workout", "meditation", "sleep", "calories", "heart rate").map { "(?i).*$it.*".toRegex() }

    // Tag: Transport
    private val transportPatterns = listOf("flight", "gate", "boarding", "uber", "lyft", "ride", "driver", "transit", "train", "ticket", "hotel").map { "(?i).*$it.*".toRegex() }

    // Tag: Shopping (Non-Logistics)
    private val shoppingPatterns = listOf("receipt", "purchase", "order confirmed", "shopping", "cart", "wishlist", "amazon", "ebay").map { "(?i).*$it.*".toRegex() }

    // Tag: Entertainment
    private val entertainmentPatterns = listOf("video", "upload", "live", "stream", "spotify", "music", "release", "gaming", "play", "match", "twitch", "youtube").map { "(?i).*$it.*".toRegex() }

    // Tag: News
    private val newsPatterns = listOf("news", "breaking", "headlines", "alert", "report", "weather", "forecast").map { "(?i).*$it.*".toRegex() }

    // Tag: Updates
    private val updatePatterns = listOf("update", "download", "installing", "battery", "usb", "system").map { "(?i).*$it.*".toRegex() }

    // Tag: Promotions
    private val promoPatterns = listOf("offer", "sale", "discount", "promo", "deal", "free", "off", "coupon", "cashback", "reward", "win", "gift").map { "(?i).*$it.*".toRegex() }


    /**
     * V4 Classification logic
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
        val isWorkApp = packageName.contains("teams") || packageName.contains("slack") || packageName.contains("gmail") || packageName.contains("outlook")

        // Helper to check if a tag is active and its patterns match
        fun matches(tag: String, patterns: List<Regex>) = activeTags.contains(tag) && patterns.any { it.containsMatchIn(combined) }

        // Category 1: Safety & Finance
        if (matches(TAG_SECURITY, securityPatterns)) return true to TAG_SECURITY
        if (matches(TAG_MONEY, financePatterns)) return true to TAG_MONEY
        if (matches(TAG_EMERGENCY, emergencyPatterns)) return true to TAG_EMERGENCY
        if (matches(TAG_LOGISTICS, logisticsPatterns)) return true to TAG_LOGISTICS

        // Category 2: Personal Inbox
        if (activeTags.contains(TAG_MESSAGES)) {
             val isNoise = Regex("(?i).*(checking for|syncing|connected|status|backup).*").containsMatchIn(combined)
             if (!isNoise) {
                 if (isConversation || (!groupPatterns.any { it.containsMatchIn(combined) } && (dmPatterns.any { it.containsMatchIn(combined) } || (isSocialApp && combined.length > 2)))) {
                     return true to TAG_MESSAGES
                 }
             }
        }
        if (matches(TAG_GROUPS, groupPatterns)) return true to TAG_GROUPS
        if (matches(TAG_MENTIONS, mentionPatterns)) return true to TAG_MENTIONS
        if (matches(TAG_CALLS, callPatterns)) return true to TAG_CALLS

        // Category 3: Work & Planning
        if (activeTags.contains(TAG_WORK) && (isWorkApp || workPatterns.any { it.containsMatchIn(combined) })) return true to TAG_WORK
        if (matches(TAG_SCHEDULE, meetingPatterns)) return true to TAG_SCHEDULE
        if (matches(TAG_DOCS, docPatterns)) return true to TAG_DOCS
        if (matches(TAG_STORAGE, storagePatterns)) return true to TAG_STORAGE

        // Category 4: Activity & Home
        if (matches(TAG_IOT, iotPatterns)) return true to TAG_IOT
        if (matches(TAG_FITNESS, healthPatterns)) return true to TAG_FITNESS
        if (matches(TAG_TRAVEL, transportPatterns)) return true to TAG_TRAVEL
        if (matches(TAG_ORDERS, shoppingPatterns)) return true to TAG_ORDERS

        // Category 5: Content & Awareness
        if (matches(TAG_ENTERTAINMENT, entertainmentPatterns)) return true to TAG_ENTERTAINMENT
        if (matches(TAG_NEWS, newsPatterns)) return true to TAG_NEWS
        if (matches(TAG_UPDATES, updatePatterns)) return true to TAG_UPDATES
        if (matches(TAG_PROMOS, promoPatterns)) return true to TAG_PROMOS

        return false to ""
    }
    
    fun getAllSmartTags(): List<String> = listOf(
        TAG_SECURITY, TAG_MONEY, TAG_EMERGENCY, TAG_LOGISTICS,
        TAG_MESSAGES, TAG_GROUPS, TAG_MENTIONS, TAG_CALLS,
        TAG_WORK, TAG_SCHEDULE, TAG_DOCS, TAG_STORAGE,
        TAG_IOT, TAG_FITNESS, TAG_TRAVEL, TAG_ORDERS,
        TAG_ENTERTAINMENT, TAG_NEWS, TAG_UPDATES, TAG_PROMOS
    )

    fun getCategorizedTags(): Map<String, List<TagMetadata>> = mapOf(
        "Safety & Finance" to listOf(
            TagMetadata(TAG_SECURITY, "OTPs & Security", "Banking codes & critical login alerts"),
            TagMetadata(TAG_MONEY, "Finance & Cards", "Bank alerts & card transactions"),
            TagMetadata(TAG_EMERGENCY, "Urgent", "SOS & health alerts"),
            TagMetadata(TAG_LOGISTICS, "Orders & Deliveries", "Food delivery & package tracking")
        ),
        "Personal" to listOf(
            TagMetadata(TAG_MESSAGES, "Direct Messages", "Personal 1-on-1 chats"),
            TagMetadata(TAG_GROUPS, "Group Threads", "Group chats & channels"),
            TagMetadata(TAG_MENTIONS, "Mentions", "Replies & @mentions"),
            TagMetadata(TAG_CALLS, "Calls", "Voice & video calls")
        ),
        "Work" to listOf(
            TagMetadata(TAG_WORK, "Work Chat", "Professional apps & chats"),
            TagMetadata(TAG_SCHEDULE, "Meetings", "Calendar & video events"),
            TagMetadata(TAG_DOCS, "Documents", "Shared files & edits"),
            TagMetadata(TAG_STORAGE, "Cloud & Storage", "Memory & backup alerts")
        ),
        "Home & Activity" to listOf(
            TagMetadata(TAG_IOT, "Smart Home", "Doorbell & camera alerts"),
            TagMetadata(TAG_FITNESS, "Health", "Fitness & medication goals"),
            TagMetadata(TAG_TRAVEL, "Travel", "Rides, flights & hotels"),
            TagMetadata(TAG_ORDERS, "Shopping", "Order status & receipts")
        ),
        "Discover" to listOf(
            TagMetadata(TAG_ENTERTAINMENT, "Media", "Videos, music & gaming"),
            TagMetadata(TAG_NEWS, "News", "Local & breaking headlines"),
            TagMetadata(TAG_UPDATES, "Apps", "System & app updates"),
            TagMetadata(TAG_PROMOS, "Deals", "Sales, offers & coupons")
        )
    )

    data class TagMetadata(val id: String, val label: String, val description: String)
}
