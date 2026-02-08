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
             Log.d("ClassificationHelper", "Heuristic Verdict: CRITICAL for $title")
             return HeuristicEngine.CAT_CRITICAL
        }

        // TIER 2: Decision Cache (<5ms)
        // If we've seen this sender before and the heuristics didn't flag it as generic, trust the cache.
        decisionCache.getVerdict(packageName, title)?.let {
            // Never trust a cached "critical" verdict blindly as content varies.
            if (it == HeuristicEngine.CAT_CRITICAL) return@let 
            
            Log.d("ClassificationHelper", "Cache Hit: $it for $title")
            return it
        }

        // TIER 3: Asynchronous AI (The Judge) - IMMEDIATE CALL
        // V3 Requirement: If Tier 1 & 2 fail, do API call immediately.
        return try {
            withTimeout(3500) {
                val category = classifyWithAI(title, content)
                
                // Only cache stable categories.
                if (category != HeuristicEngine.CAT_CRITICAL) {
                    decisionCache.cacheVerdict(packageName, title, category)
                }
                category
            }
        } catch (e: Exception) {
            Log.w("ClassificationHelper", "AI failed, using fallback: ${e.message}")
            // Fallback: Default to NOISE to be safe (over-blocking is better than under-blocking in V3)
            HeuristicEngine.CAT_NOISE
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
            Classify this notification into ONE category: critical, productivity, social, logistics, events, gamification, or noise.
            
            Title: $title
            Content: $content
            
            Rules:
            - "critical": OTPs, codes, security, emergency (hospital, accident, safety), bank debit, call from family.
            - "productivity": Work docs, teams, slack, calendar invites, system info.
            - "logistics": Deliveries, food arrival, shipping updates, tracking info.
            - "events": Concerts, starting news, live streams, tickets, gym reminders.
            - "social": Direct messages, personal chats, @mentions.
            - "gamification": Streaks, daily rewards, game invites, points, levels.
            - "noise": Sales, marketing, group chat reactions, likes, generic feed updates.
            
            IMPORTANT: If the message implies ANY physical or financial danger, safety emergency, or immediate family urgency, ALWAYS use "critical".
            
            Response format: Just the category name in lowercase.
        """.trimIndent()

        val request = OpenAIRequest(
            messages = listOf(
                Message(role = "system", content = "You are a notification classification assistant. Respond with only one word."),
                Message(role = "user", content = prompt)
            ),
            max_tokens = 10
        )

        val response = openAIService.chatCompletion(request)
        val category = response.choices.firstOrNull()?.message?.content?.trim()?.lowercase() ?: HeuristicEngine.CAT_NOISE
        
        return when (category) {
            HeuristicEngine.CAT_CRITICAL, 
            HeuristicEngine.CAT_PRODUCTIVITY, 
            HeuristicEngine.CAT_SOCIAL, 
            HeuristicEngine.CAT_LOGISTICS, 
            HeuristicEngine.CAT_EVENTS, 
            HeuristicEngine.CAT_GAMIFICATION, 
            HeuristicEngine.CAT_NOISE -> category
            else -> HeuristicEngine.CAT_PRODUCTIVITY // Safe default
        }
    }
}
