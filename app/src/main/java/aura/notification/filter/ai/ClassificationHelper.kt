package aura.notification.filter.ai

import android.util.Log
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassificationHelper @Inject constructor(
    private val openAIService: OpenAIService,
    private val heuristicEngine: HeuristicEngine,
    private val decisionCache: DecisionCache
) {

    suspend fun classify(title: String, content: String, packageName: String): String {
        // TIER 1: Local Heuristics (<10ms) - SAFETY NET
        // We run this FIRST so that even if "Gaurav" is cached as "social", his "Emergency" message breaks through.
        if (heuristicEngine.isCritical(title, content)) {
             Log.d("ClassificationHelper", "Heuristic Verdict: EMERGENCY for $title")
             return HeuristicEngine.TAG_EMERGENCY
        }

        // TIER 2: Decision Cache (<5ms)
        // If we've seen this sender before and the heuristics didn't flag it as generic, trust the cache.
        decisionCache.getVerdict(packageName, title)?.let {
            // Never trust a cached emergency verdict blindly as content varies.
            if (it == HeuristicEngine.TAG_EMERGENCY) return@let 
            
            Log.d("ClassificationHelper", "Cache Hit: $it for $title")
            return it
        }

        // TIER 3: Asynchronous AI (The Judge) - IMMEDIATE CALL
        // V3 Requirement: If Tier 1 & 2 fail, do API call immediately.
        return try {
            withTimeout(3500) {
                val category = classifyWithAI(title, content)
                
                // Only cache stable categories.
                if (category != HeuristicEngine.TAG_EMERGENCY) {
                    decisionCache.cacheVerdict(packageName, title, category)
                }
                category
            }
        } catch (e: Exception) {
            Log.w("ClassificationHelper", "AI failed, using fallback: ${e.message}")
            // Fallback: Default to Updates to be safe
            HeuristicEngine.TAG_UPDATES
        }
    }

    suspend fun summarize(packageName: String, notifications: List<String>): String {
         return try {
             withTimeout(5000) {
                 val limitedList = if (notifications.size > 30) {
                     notifications.takeLast(30) + " (and ${notifications.size - 30} more...)"
                 } else {
                     notifications
                 }

                 val prompt = """
                     You are a personal concierge. Summarize the SUBSTANCE of these notifications from $packageName.
                     
                     Rules:
                     - Do NOT describe the general kind of talk (e.g., instead of 'They are discussing dinner', say 'Dinner planned at 8 PM at Pizza Hut').
                     - Identify exactly WHAT is happening or being requested.
                     - Use a direct, narrative tone. Be concise but substantive.
                     - Use '⚡' to lead clear ACTION ITEMS.
                     - Use 'URGENT:' only for security, money, or emergency alerts.
                     
                     Notifications:
                     ${limitedList.joinToString("\n") { "- $it" }}
                 """.trimIndent()
                 
                 val request = OpenAIRequest(
                    messages = listOf(
                        Message("system", "You are an expert personal concierge. Be brief, narrative, and highly descriptive."),
                        Message("user", prompt)
                    )
                 )
                 
                 val response = openAIService.chatCompletion(request)
                 val summary = response.choices.firstOrNull()?.message?.content?.trim() ?: "No summary available."
                 
                 if (summary.startsWith("URGENT:", ignoreCase = true)) {
                     Log.e("ClassificationHelper", "URGENT MESSAGE DETECTED IN BLOCK LIST: $summary")
                 }
                 
                 summary
             }
         } catch (e: Exception) {
             Log.w("ClassificationHelper", "AI Summarization failed: ${e.message}")
             val uniqueTitles = notifications.map { it.substringBefore(":").trim() }.distinct()
             if (uniqueTitles.isNotEmpty()) {
                 "Updates from ${uniqueTitles.take(3).joinToString(", ")}" + if (uniqueTitles.size > 3) "..." else ""
             } else {
                 "Multiple notifications received."
             }
         }
    }

    private suspend fun classifyWithAI(title: String, content: String): String {
        val prompt = """
            Classify this notification into EXACTLY ONE of these categories:
            
            1. Safety & Finance: security, finance, emergency, logistics
            2. Personal Inbox: direct_chats, group_threads, mentions, calls
            3. Work & Planning: work, meetings, documents, storage
            4. Activity & Home: smart_home, health, transport, shopping
            5. Content & Awareness: entertainment, headlines, updates, promotions
            
            Rules for specific tags:
            - "security": OTP, 2FA, login codes, password reset.
            - "finance": Bank alerts, card transactions, bills.
            - "emergency": SOS, hospital, safety alerts, family urgency.
            - "logistics": Food delivery, package tracking.
            - "direct_chats": 1-on-1 personal messages.
            - "group_threads": Activity from group chats/channels.
            - "mentions": @mentions, replies, tags.
            - "calls": Voice and video call alerts.
            - "work": Slack, Teams, Jira, work emails.
            - "meetings": Calendar invites, Zoom, Meet.
            - "documents": Comments/edits on docs, sheets, PDFs.
            - "storage": Memory full, cloud backup, sync errors.
            - "smart_home": Doorbell, cameras, IoT sensors.
            - "health": Meditation, step goals, meds, sleep.
            - "transport": Uber, Lyft, flight status, transit.
            - "shopping": Receipts, order confirmations (non-delivery).
            - "entertainment": YouTube, streams, music releases, gaming.
            - "headlines": Breaking news, weather alerts.
            - "updates": System/app software updates.
            - "promotions": Sales, coupons, promo deals.
            
            Notification:
            Title: $title
            Content: $content
            
            Response format: Just the tag name (e.g. "security" or "group_threads").
        """.trimIndent()

        val request = OpenAIRequest(
            messages = listOf(
                Message(role = "system", content = "You are a notification classification assistant. Respond with only the specific tag from the provided list."),
                Message(role = "user", content = prompt)
            ),
            max_tokens = 10
        )

        val response = openAIService.chatCompletion(request)
        val aiTag = response.choices.firstOrNull()?.message?.content?.trim()?.lowercase() ?: HeuristicEngine.TAG_PROMOS
        
        // Map the AI slug back to the display constants used in HeuristicEngine
        return when (aiTag) {
            "security" -> HeuristicEngine.TAG_SECURITY
            "finance" -> HeuristicEngine.TAG_MONEY
            "emergency" -> HeuristicEngine.TAG_EMERGENCY
            "logistics" -> HeuristicEngine.TAG_LOGISTICS
            "direct_chats" -> HeuristicEngine.TAG_MESSAGES
            "group_threads" -> HeuristicEngine.TAG_GROUPS
            "mentions" -> HeuristicEngine.TAG_MENTIONS
            "calls" -> HeuristicEngine.TAG_CALLS
            "work" -> HeuristicEngine.TAG_WORK
            "meetings" -> HeuristicEngine.TAG_SCHEDULE
            "documents" -> HeuristicEngine.TAG_DOCS
            "storage" -> HeuristicEngine.TAG_STORAGE
            "smart_home" -> HeuristicEngine.TAG_IOT
            "health" -> HeuristicEngine.TAG_FITNESS
            "transport" -> HeuristicEngine.TAG_TRAVEL
            "shopping" -> HeuristicEngine.TAG_ORDERS
            "entertainment" -> HeuristicEngine.TAG_ENTERTAINMENT
            "headlines" -> HeuristicEngine.TAG_NEWS
            "updates" -> HeuristicEngine.TAG_UPDATES
            "promotions" -> HeuristicEngine.TAG_PROMOS
            else -> HeuristicEngine.TAG_PROMOS
        }
    }
}
