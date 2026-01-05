package com.aura.ai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicEngine @Inject constructor() {

    // --- KNOWN PACKAGES (Zero Latency) ---
    private val socialPackages = setOf(
        "com.whatsapp",
        "com.facebook.orca", // Messenger
        "com.instagram.android",
        "com.snapchat.android",
        "com.twitter.android",
        "com.x.android",
        "org.telegram.messenger",
        "com.discord",
        "com.slack",
        "com.linkedin.android"
    )

    private val entertainmentPackages = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.spotify.music",
        "com.ZHILIAOAPP.musically", // TikTok
        "tv.twitch.android",
        "com.amazon.avod.thirdpartyclient" // Prime Video
    )

    private val promoPackages = setOf(
        "com.amazon.mShop.android.shopping",
        "com.alibaba.aliexpresshd",
        "com.ubercab.eats"
    )

    // --- REGEX PATTERNS (Low Latency) ---
    private val urgentPatterns = listOf(
        "(?i).*otp.*",
        "(?i).*verification code.*",
        "(?i).*emergency.*",
        "(?i).*alert.*",
        "(?i).*call from.*",
        "(?i).*missed call.*",
        "(?i).*security code.*",
        "(?i).*login.*",
        "(?i).*bank.*",
        "(?i).*transaction.*",
        "(?i).*debited.*",
        "(?i).*credited.*"
    ).map { it.toRegex() }

    private val spamPatterns = listOf(
        "(?i).*sale.*",
        "(?i).*offer.*",
        "(?i).*discount.*",
        "(?i).*subscribe.*",
        "(?i).*promo.*",
        "(?i).*buy now.*",
        "(?i).*limited time.*",
        "(?i).*coupon.*"
    ).map { it.toRegex() }

    private val socialPatterns = listOf(
        "(?i).*message.*",
        "(?i).*reply.*",
        "(?i).*chat.*",
        "(?i).*friend.*",
        "(?i).*group.*",
        "(?i).*dm.*",
        "(?i).*sent you.*"
    ).map { it.toRegex() }

    private val entertainmentPatterns = listOf(
        "(?i).*video.*",
        "(?i).*stream.*",
        "(?i).*live.*",
        "(?i).*game.*",
        "(?i).*playing.*",
        "(?i).*watch.*"
    ).map { it.toRegex() }

    /**
     * Fast local classification. Returns category or null if unsure.
     */
    fun fastClassify(packageName: String, title: String, content: String): String? {
        val combined = "$title $content"
        
        // 1. Urgent Check (HIGHEST PRIORITY - Content is King)
        // Check this BEFORE package names, so "Emergency" from WhatsApp breaks through.
        if (urgentPatterns.any { it.containsMatchIn(combined) }) {
            return "urgent"
        }
        
        // 2. Package Check (fastest)
        if (packageName in socialPackages) return "social"
        if (packageName in entertainmentPackages) return "entertainment"
        if (packageName in promoPackages) return "promotional"

        // 3. Regex Pattern Matching
        if (spamPatterns.any { it.containsMatchIn(combined) }) return "promotional"
        if (socialPatterns.any { it.containsMatchIn(combined) }) return "social"
        if (entertainmentPatterns.any { it.containsMatchIn(combined) }) return "entertainment"

        // 4. Fallback (Unsure -> AI)
        return null 
    }
}
