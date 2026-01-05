package com.aura.util

import android.content.Context
import com.aura.data.AppRuleDao
import com.aura.data.AppRuleEntity
import com.aura.data.FilterTemplate
import com.aura.data.ShieldLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds default Shield rules for popular apps to ensure
 * Focus and Relax modes work out-of-the-box.
 */
@Singleton
class ProfileSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appInfoManager: AppInfoManager,
    private val appRuleDao: AppRuleDao
) {
    private val prefs = context.getSharedPreferences("aura_seeding", Context.MODE_PRIVATE)
    
    private val knownApps = mapOf(
        // Social
        "com.instagram.android" to FilterTemplate.SOCIAL,
        "com.facebook.katana" to FilterTemplate.SOCIAL,
        "com.twitter.android" to FilterTemplate.SOCIAL,
        "com.snapchat.android" to FilterTemplate.SOCIAL,
        "com.reddit.frontpage" to FilterTemplate.SOCIAL,
        
        // Messaging
        "com.whatsapp" to FilterTemplate.MESSAGING,
        "org.telegram.messenger" to FilterTemplate.MESSAGING,
        "com.discord" to FilterTemplate.MESSAGING,
        
        // Transactional/Finance
        "com.google.android.gm" to FilterTemplate.TRANSACTIONAL, // Gmail
        "com.phonepe.app" to FilterTemplate.TRANSACTIONAL,
        "net.one97.paytm" to FilterTemplate.TRANSACTIONAL,
        "com.google.android.apps.nbu.paisa.user" to FilterTemplate.TRANSACTIONAL, // GPay
        
        // Work/Productivity
        "com.slack" to FilterTemplate.WORK,
        "com.microsoft.teams" to FilterTemplate.WORK,
        "com.asana.app" to FilterTemplate.WORK,
        "com.trello" to FilterTemplate.WORK
    )
    
    /**
     * Seeds default rules for installed apps on first run.
     * Should be called from MainViewModel init or Application.onCreate()
     */
    suspend fun seedIfNeeded() {
        if (prefs.getBoolean("seeded_v1", false)) {
            return // Already seeded
        }
        
        withContext(Dispatchers.IO) {
            // Disabled auto-seeding per user request. 
            // Users want total control: "only what were being setup during onboarding".
            // So we do NOTHING here except mark as seeded so we don't try again.
            
            // Mark as seeded
            prefs.edit().putBoolean("seeded_v1", true).apply()
        }
    }
}
