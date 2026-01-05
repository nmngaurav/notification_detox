package com.aura.ai

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecisionCache @Inject constructor() {

    // Key: "packageName|title" -> Value: "category"
    // We limit to 1000 entries to keep it light
    private val cache = LruCache<String, String>(1000)

    fun getVerdict(packageName: String, title: String): String? {
        // We use title as part of they key because "Mom" vs "PromoBot" from same app matters
        // But for some apps, maybe just package is enough? For now, be specific.
        return cache.get(generateKey(packageName, title))
    }

    fun cacheVerdict(packageName: String, title: String, category: String) {
        cache.put(generateKey(packageName, title), category)
    }

    private fun generateKey(packageName: String, title: String): String {
        // Simple sanitization
        return "$packageName|${title.take(50)}" 
    }
}
