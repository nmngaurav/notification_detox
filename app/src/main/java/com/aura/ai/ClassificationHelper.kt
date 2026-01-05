package com.aura.ai

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
        // TIER 1: Local Heuristics (<10ms) - CONTENT IS KING
        // Check for urgent keywords (OTP, etc) or obvious spam.
        // We run this FIRST so that even if "Gaurav" is cached as "social", his "Emergency" message breaks through.
        heuristicEngine.fastClassify(packageName, title, content)?.let {
            Log.d("ClassificationHelper", "Heuristic Check: $it for $title")
            return it
        }

        // TIER 2: Decision Cache (<5ms)
        // If we've seen this sender before and the heuristics didn't flag it as urgent, trust the cache.
        decisionCache.getVerdict(packageName, title)?.let {
            // CRITICAL FIX: Never trust a cached "urgent" verdict.
            // Urgency depends on content ("Emergency" vs "lol"), but cache key is only (Package + Title).
            // If we blindly return cached "urgent", then "lol" from the same person will bypass the shield.
            if (it == "urgent") return@let 
            
            Log.d("ClassificationHelper", "Cache Hit: $it for $title")
            return it
        }

        // TIER 3: Asynchronous AI (The Judge)
        // If we differ to AI, we must accept latency. 
        // In the future, we could "hold" the notification, but for now we block/allow based on risk.
        return try {
            withTimeout(3000) {
                val category = classifyWithAI(title, content)
                
                // Only cache stable categories. "Urgent" is often content-dependent (e.g. "Pick up phone!").
                // "Social", "Promotional", "Update" are usually stable per sender.
                if (category != "urgent") {
                    decisionCache.cacheVerdict(packageName, title, category)
                }
                category
            }
        } catch (e: Exception) {
            Log.w("ClassificationHelper", "AI failed, using fallback: ${e.message}")
            // Fallback to strict heuristics or default to 'update' to be safe
            classifyWithHeuristics(packageName, title, content).also {
                // Don't cache fallbacks, retry next time
            }
        }
    }

    suspend fun summarize(packageName: String, notifications: List<String>): String {
         return try {
             withTimeout(5000) {
                 // Optimize for scale: Take last 30 messages
                 val limitedList = if (notifications.size > 30) {
                     notifications.takeLast(30) + " (and ${notifications.size - 30} more...)"
                 } else {
                     notifications
                 }

                 val prompt = """
                     Summarize these notifications from $packageName into ONE concise sentence.
                     If any message seems URGENT (emergency, money lost, otp), start the summary with "URGENT:".
                     
                     Notifications:
                     ${limitedList.joinToString("\n") { "- $it" }}
                 """.trimIndent()
                 
                 val request = OpenAIRequest(
                    // Use default model (gpt-4o-mini)
                    messages = listOf(
                        Message("system", "You are a helpful assistant. Be concise."),
                        Message("user", prompt)
                    )
                 )
                 
                 val response = openAIService.chatCompletion(request)
                 val summary = response.choices.firstOrNull()?.message?.content?.trim() ?: "No summary available."
                 
                 // URGENT RESCUE
                 if (summary.startsWith("URGENT:", ignoreCase = true)) {
                     // TODO: Trigger Rescue Notification (Callback or Event)
                     Log.e("ClassificationHelper", "URGENT MESSAGE DETECTED IN BLOCK LIST: $summary")
                 }
                 
                 summary
             }
         } catch (e: Exception) {
             Log.w("ClassificationHelper", "AI Summarization failed: ${e.message}")
             // Smart Fallback: Just list the unique titles
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
            Classify this notification into ONE category: urgent, social, promotional, or update.
            
            Title: $title
            Content: $content
            
            Rules:
            - "urgent": OTPs, verification codes, security alerts, critical account issues, delivery updates

            - "promotional": Sales, discounts, marketing, newsletters
            - "update": App updates, system notifications, news
            
            Response format: Just the category name in lowercase, nothing else.
        """.trimIndent()

        val request = OpenAIRequest(
            // Use default model (gpt-4o-mini)
            messages = listOf(
                Message(role = "system", content = "You are a notification classification assistant. Respond with only one word: urgent, social, promotional, or update."),
                Message(role = "user", content = prompt)
            ),
            max_tokens = 10
        )

        val response = openAIService.chatCompletion(request)
        val category = response.choices.firstOrNull()?.message?.content?.trim()?.lowercase() ?: "social"
        
        return when (category) {
            "urgent", "social", "promotional", "update" -> category
            else -> "social" // Default if AI returns unexpected value
        }
    }

    private fun classifyWithHeuristics(packageName: String, title: String, content: String): String {
        // Use the HeuristicEngine which now handles package + regex
        return heuristicEngine.fastClassify(packageName, title, content) ?: "social"
    }
}
