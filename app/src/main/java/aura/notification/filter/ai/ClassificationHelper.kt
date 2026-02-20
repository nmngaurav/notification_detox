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

    suspend fun classify(title: String, content: String, packageName: String, skipCache: Boolean = false): String {
        // TIER 1: Local Heuristics (<10ms) - SAFETY NET
        // We run this FIRST so that even if "Gaurav" is cached as "social", his "Emergency" message breaks through.
        if (heuristicEngine.isCritical(title, content)) {
             Log.d("ClassificationHelper", "Heuristic Verdict: EMERGENCY for $title")
             return HeuristicEngine.TAG_EMERGENCY
        }

        // TIER 2: Decision Cache (<5ms) — skip when called for rescue to get fresh AI verdict
        if (!skipCache) {
            decisionCache.getVerdict(packageName, title)?.let {
                // Never trust a cached emergency verdict blindly as content varies.
                if (it == HeuristicEngine.TAG_EMERGENCY) return@let 
                
                Log.d("ClassificationHelper", "Cache Hit: $it for $title")
                return it
            }
        }

        // TIER 3: Asynchronous AI (The Judge) - IMMEDIATE CALL
        // V3 Requirement: If Tier 1 & 2 fail, do API call immediately.
        return try {
            withTimeout(3500) {
                val category = classifyWithAI(title, content, packageName)
                
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
             withTimeout(8000) { // Increased timeout for longer context
                 val limitedList = if (notifications.size > 50) {
                     notifications.takeLast(50) + " (and ${notifications.size - 50} more...)"
                 } else {
                     notifications
                 }

                 val prompt = """
                     You are an expert personal executive assistant. These are raw notification texts from the app "$packageName".
                     Your job is to synthesize them into a single, high-value "Briefing" for the user.

                     Input Data:
                     ${limitedList.joinToString("\n") { "- $it" }}

                     Instructions:
                     1. **Context Awareness**: Understand that these are APP NOTIFICATIONS. Fragmented texts like "Hey" + "How are you?" from the same person should be grouped (e.g., "John sent 2 messages involving specific questions").
                     2. **Relevance**: Ignore generic noise. Focus on WHO, WHAT, and ACTION.
                     3. **Style**: Use a professional, crisp, energetic tone. No "Here is a summary". Just the intel.
                     4. **Formatting**:
                        - Use bolding (e.g. **OTP 1234**) for key data.
                        - Use ⚡ for Action Items.
                        - Use ⚠️ for Urgent alerts.
                     5. **Output Length**: Keep it under 60 words unless there is complex data.
                 """.trimIndent()
                 
                 val request = OpenAIRequest(
                    messages = listOf(
                        Message("system", "You are an elite executive assistant application. Output concise, actionable intelligence."),
                        Message("user", prompt)
                    ),
                    max_tokens = 300 // Explicitly increased to prevent cut-offs
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

    private suspend fun classifyWithAI(title: String, content: String, packageName: String = ""): String {
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
            App: $packageName
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
