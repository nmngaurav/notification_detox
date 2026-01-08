package com.aura.ai


import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicEngine @Inject constructor() {

    // --- CATEGORY CONSTANTS ---
    companion object {
        const val CAT_CRITICAL = "critical"
        const val CAT_PRODUCTIVITY = "productivity"
        const val CAT_SOCIAL = "social"
        const val CAT_FINANCES = "finances"
        const val CAT_LOGISTICS = "logistics"
        const val CAT_EVENTS = "events"
        const val CAT_GAMIFICATION = "gamification"
        const val CAT_PROMOS = "promos"
        const val CAT_UPDATES = "updates"
        const val CAT_NOISE = "noise" // Legacy/Spam
    }

    // --- KNOWN PACKAGES (Zero Latency) ---
    private val productivityPackages = setOf(
        "com.google.android.gm", // Gmail
        "com.microsoft.office.outlook",
        "com.google.android.calendar",
        "com.microsoft.teams",
        "com.slack",
        "com.linkedin.android",
        "com.google.android.apps.tasks",
        "com.todoist"
    )

    private val socialPackages = setOf(
        "com.whatsapp",
        "com.facebook.orca", // Messenger
        "com.instagram.android",
        "com.snapchat.android",
        "com.twitter.android",
        "com.x.android",
        "org.telegram.messenger",
        "com.discord"
    )

    private val logisticsPackages = setOf(
        "com.ubercab", "com.ubercab.eats", "com.grubhub.android", 
        "com.door dash.door dash", "com.postmates.android", 
        "com.instacart.client", "com.fedex.mobile", "com.ups.mobile.android",
        "com.dhl.express.mobile", "com.zomato.android", "com.swiggy.android"
    )

    private val eventPackages = setOf(
        "com.eventbrite.organiser", "com.ticketmaster.mobile.android",
        "com.meetup", "com.accuweather.android", "com.google.android.apps.fitness"
    )

    private val gamificationPackages = setOf(
        "com.duolingo", "com.supercell.clashofclans", "com.king.candycrushsaga",
        "com.activision.callofduty.shooter", "com.roblox.client", 
        "com.nianticlabs.pokemongo", "com.discord" // Discord is social/game hybrid
    )

    private val noisePackages = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.spotify.music",
        "com.ZHILIAOAPP.musically", // TikTok
        "tv.twitch.android",
        "com.amazon.avod.thirdpartyclient",
        "com.amazon.mShop.android.shopping",
        "com.alibaba.aliexpresshd"
    )

    // --- REGEX PATTERNS (Low Latency) ---
    // TIER 1: CRITICAL (Always Allow - Buzz)
    private val criticalPatterns = listOf(
        // Security / OTP
        "(?i).*otp.*", "(?i).*code.*", "(?i).*verification.*", "(?i).*password.*",
        "(?i).*login.*", "(?i).*security.*", "(?i).*auth.*", "(?i).*2fa.*",
        // Financial
        "(?i).*bank.*", "(?i).*acct.*", "(?i).*transaction.*", "(?i).*debited.*",
        "(?i).*credited.*", "(?i).*payment.*", "(?i).*card.*", "(?i).*spent.*",
        "(?i).*balance.*", "(?i).*withdrawn.*", "(?i).*stmt.*",
        // Emergency / Vital
        "(?i).*emergency.*", "(?i).*alert.*", "(?i).*call from.*", "(?i).*missed call.*",
        "(?i).*urgent.*", "(?i).*help.*", "(?i).*found.*", "(?i).*important.*",
        "(?i).*pickup.*", "(?i).*delivery.*", "(?i).*arriving.*", "(?i).*reached.*"
    ).map { it.toRegex() }

    // TIER 2: PRODUCTIVITY (Work/Updates - Buzz if Work Mode)
    private val productivityPatterns = listOf(
        "(?i).*meeting.*", "(?i).*standup.*", "(?i).*calendar.*", "(?i).*schedule.*",
        "(?i).*zoom.*", "(?i).*link.*", "(?i).*doc.*", "(?i).*sheet.*", "(?i).*pdf.*",
        "(?i).*email.*", "(?i).*sent.*", "(?i).*task.*", "(?i).*project.*",
        "(?i).*deadline.*", "(?i).*submitted.*"
    ).map { it.toRegex() }

    // TIER 3: SOCIAL (DMs - Block if Focus Mode)
    private val socialPatterns = listOf(
        "(?i).*message.*", "(?i).*reply.*", "(?i).*chat.*", "(?i).*sent you.*",
        "(?i).*typing.*", "(?i).*sticker.*", "(?i).*photo.*", "(?i).*video.*",
        "(?i).*voice.*", "(?i).*audio.*", "(?i).*friend.*", "(?i).*mention.*"
    ).map { it.toRegex() }

    // TIER 3.1: LOGISTICS (Deliveries)
    private val logisticsPatterns = listOf(
        "(?i).*delivery.*", "(?i).*arriving.*", "(?i).*picked up.*", "(?i).*out for delivery.*",
        "(?i).*reached.*", "(?i).*shipment.*", "(?i).*package.*", "(?i).*track.*",
        "(?i).*on its way.*"
    ).map { it.toRegex() }

    // TIER 3.2: EVENTS (Calendar/Live)
    private val eventPatterns = listOf(
        "(?i).*starting.*", "(?i).*live now.*", "(?i).*reminder.*", "(?i).*ticket.*",
        "(?i).*rsvp.*", "(?i).*confirmed.*", "(?i).*event.*", "(?i).*webinar.*"
    ).map { it.toRegex() }

    // TIER 3.3: GAMIFICATION (Streaks/Rewards)
    private val gamificationPatterns = listOf(
        "(?i).*streak.*", "(?i).*reward.*", "(?i).*claim.*", "(?i).*points.*",
        "(?i).*level.*", "(?i).*achievement.*", "(?i).*daily.*", "(?i).*free gift.*",
        "(?i).*bonus.*", "(?i).*unlocked.*"
    ).map { it.toRegex() }

    // TIER 4: NOISE (Spam/Promos/Ent - Always Block)
    private val noisePatterns = listOf(
        "(?i).*sale.*", "(?i).*offer.*", "(?i).*discount.*", "(?i).*deal.*",
        "(?i).*promo.*", "(?i).*buy.*", "(?i).*limited.*",
        "(?i).*subscribe.*", "(?i).*stream.*", "(?i).*game.*",
        "(?i).*play.*", "(?i).*win.*", "(?i).*free.*", "(?i).*coupon.*",
        "(?i).*news.*", "(?i).*headline.*", "(?i).*story.*", "(?i).*trending.*",
        "(?i).*recommended.*", "(?i).*vlog.*"
    ).map { it.toRegex() }

    /**
     * Fast local classification. Returns category or null if unsure.
     */
    fun fastClassify(packageName: String, title: String, content: String): String? {
        val combined = "$title $content"
        
        // 1. Critical Check (HIGHEST PRIORITY - Content is King)
        if (criticalPatterns.any { it.containsMatchIn(combined) }) {
            return CAT_CRITICAL
        }

        // 2. Package Check
        if (packageName in productivityPackages) return CAT_PRODUCTIVITY
        if (packageName in socialPackages) return CAT_SOCIAL
        if (packageName in logisticsPackages) return CAT_LOGISTICS
        if (packageName in eventPackages) return CAT_EVENTS
        if (packageName in gamificationPackages) return CAT_GAMIFICATION
        if (packageName in noisePackages) return CAT_NOISE

        // 3. Regex Pattern Matching
        if (productivityPatterns.any { it.containsMatchIn(combined) }) return CAT_PRODUCTIVITY
        if (socialPatterns.any { it.containsMatchIn(combined) }) return CAT_SOCIAL
        if (logisticsPatterns.any { it.containsMatchIn(combined) }) return CAT_LOGISTICS
        if (eventPatterns.any { it.containsMatchIn(combined) }) return CAT_EVENTS
        if (gamificationPatterns.any { it.containsMatchIn(combined) }) return CAT_GAMIFICATION
        if (noisePatterns.any { it.containsMatchIn(combined) }) return CAT_NOISE

        // 4. Fallback (Unsure -> AI)
        return null 
    }
}
