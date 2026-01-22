package aura.notification.filter.util

import android.content.Context
import aura.notification.filter.data.AppRuleDao
import aura.notification.filter.data.AppRuleEntity
import aura.notification.filter.data.DetoxCategory
import aura.notification.filter.data.ShieldLevel
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
        "com.instagram.android" to DetoxCategory.SOCIAL,
        "com.facebook.katana" to DetoxCategory.SOCIAL,
        "com.twitter.android" to DetoxCategory.SOCIAL,
        "com.snapchat.android" to DetoxCategory.SOCIAL,
        "com.reddit.frontpage" to DetoxCategory.SOCIAL,
        
        // Messaging
        "com.whatsapp" to DetoxCategory.SOCIAL,
        "org.telegram.messenger" to DetoxCategory.SOCIAL,
        "com.discord" to DetoxCategory.SOCIAL,
        
        // Transactional/Finance
        "com.google.android.gm" to DetoxCategory.FINANCES, // Gmail
        "com.phonepe.app" to DetoxCategory.FINANCES,
        "net.one97.paytm" to DetoxCategory.FINANCES,
        "com.google.android.apps.nbu.paisa.user" to DetoxCategory.FINANCES, // GPay
        
        // Work/Productivity
        "com.slack" to DetoxCategory.SOCIAL,
        "com.microsoft.teams" to DetoxCategory.SOCIAL,
        "com.asana.app" to DetoxCategory.UPDATES,
        "com.trello" to DetoxCategory.UPDATES
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
