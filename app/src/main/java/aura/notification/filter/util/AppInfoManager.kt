package aura.notification.filter.util

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager
    private val cache = mutableMapOf<String, AppInfo>()

    data class AppInfo(
        val label: String,
        val icon: Drawable?,
        val packageName: String = ""
    )

    fun getAppInfo(packageName: String): AppInfo {
        if (cache.containsKey(packageName)) {
            return cache[packageName]!!
        }

        val info = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            AppInfo(label, icon)
        } catch (e: Exception) {
            // Fallback if app is uninstalled or not found
            AppInfo(packageName, null)
        }

        cache[packageName] = info
        return info
    }

    fun getInstalledApps(): List<AppInfo> {
        try {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            
            val result = apps.mapNotNull { appInfo ->
                val packageName = appInfo.packageName
                
                // Skip self
                if (packageName == context.packageName) {
                    return@mapNotNull null
                }
                
                // Skip true system services (GMS, providers, etc.)
                if (isSystemApp(packageName)) {
                    return@mapNotNull null
                }
                
                // Check if app has FLAG_SYSTEM set
                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                
                // Get installer package
                val installer = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        packageManager.getInstallSourceInfo(packageName).installingPackageName
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getInstallerPackageName(packageName)
                    }
                } catch (e: Exception) {
                    null
                }
                

                
                // Known user-facing notification apps (can be system apps)
                val knownNotificationApps = listOf(
                    "com.google.android.gm", // Gmail
                    "com.android.chrome",
                    "com.google.android.apps.messaging", // Messages
                    "com.whatsapp",
                    "com.facebook",
                    "com.instagram.android",
                    "com.google.android.apps.docs", // Google Drive
                    "com.google.android.apps.photos", // Google Photos
                    "com.google.android.calendar", // Google Calendar
                    "com.google.android.youtube" // YouTube
                )
                val isKnownApp = knownNotificationApps.any { packageName.startsWith(it) }
                
                // Include if:
                // 1. Installed from Play Store (and not a system app, unless it's a known app)
                val isFromPlayStore = installer == "com.android.vending"
                
                // 2. User-sideloaded (no installer and not a system app)
                val isSideloaded = installer == null && !isSystem
                
                // 3. Known notification apps (allowed even if system)
                
                // Final decision: include only if one of the criteria is met
                val shouldInclude = when {
                    isKnownApp -> true // Always include known apps
                    isSystem && !isKnownApp -> false // Exclude system apps unless they're known
                    isFromPlayStore -> true // Include Play Store apps
                    isSideloaded -> true // Include sideloaded apps
                    else -> false // Exclude everything else
                }
                
                // User Feedback Fix: Ensure app is launchable (hides background services like Key Verifier)
                val isLaunchable = packageManager.getLaunchIntentForPackage(packageName) != null
                if (!shouldInclude || !isLaunchable) {
                    return@mapNotNull null
                }
                
                if (!shouldInclude) {
                    return@mapNotNull null
                }
                
                try {
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    
                    // Cache it
                    cache[packageName] = AppInfo(label, icon)
                    AppInfo(label, icon, packageName)
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.label }
            
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun isSystemApp(packageName: String): Boolean {
        // ONLY filter out internal system services that NEVER send user notifications
        // Allow ALL user-facing apps (Gmail, Chrome, WhatsApp, etc.)
        
        val trueSystemServices = listOf(
            // Core Android internals
            "com.android.providers",
            "com.android.server",
            "com.android.systemui",
            "com.android.inputmethod",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.documentsui",
            "com.android.externalstorage",
            "com.android.htmlviewer",
            "com.android.mms.service",
            "com.android.printspooler",
            "com.android.proxyhandler",
            "com.android.wallpaper",
            "com.android.shell",
            "com.android.sharedstoragebackup",
            "com.android.statementservice",
            "com.android.vpndialogs",
            "com.android.keychain",
            "com.android.location.fused",
            "com.android.certinstaller",
            
            // Google Play Services (backend only)
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.ext.services",
            "com.google.android.configupdater",
            "com.google.android.webview",
            "com.google.android.backuptransport",
            "com.google.android.feedback",
            "com.google.android.onetimeinitializer",
            "com.google.android.partnersetup",
            "com.google.android.setupwizard",
            "com.google.android.syncadapters",
            
            // Package installer
            "com.google.android.packageinstaller",
            
            // Google system modules (even if from Play Store)
            "com.google.android.modulemetadata", // "Main components"
            "com.google.android.networkstack", // "Network manager"
            "com.google.mainline.telemetry", // "Support components"
            "com.google.mainline.adservices", // "Main components"
            "com.google.android.apps.wellbeing", // "Digital Wellbeing"
            "com.google.android.marvin.talkback", // "Android Accessibility Suite"
            "com.google.android.tts", // "Speech Recognition"
            "com.google.android.overlay",
            "com.google.android.permissioncontroller",
            
            // OEM system processes
            "com.motorola.actions",
            "com.samsung.android.app.settings",
            "com.samsung.android.app",
            "com.miui.system",
            "com.coloros",
            "com.oppo.launcher"
        )
        
        // ONLY block if it starts with one of these patterns
        return trueSystemServices.any { packageName.startsWith(it) }
    }

    /**
     * Checks if an app has notification permission enabled.
     * Uses AppOpsManager to check notification permission for the given package.
     * For Android M (API 23) and above, checks the actual permission state.
     * For older versions, assumes enabled if app exists (pre-API 23 behavior).
     */
    fun hasNotificationPermission(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val uid = packageManager.getPackageUid(packageName, 0)
                
                // Use string-based check for API 29+, reflection for API 23-28
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29+: Use string-based check
                    appOpsManager.checkOpNoThrow(
                        "android:post_notification",
                        uid,
                        packageName
                    )
                } else {
                    // API 23-28: Use reflection to call the int-based method
                    // OP_POST_NOTIFICATION = 11
                    try {
                        val method = AppOpsManager::class.java.getMethod(
                            "checkOpNoThrow",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            String::class.java
                        )
                        @Suppress("DEPRECATION")
                        method.invoke(appOpsManager, 11, uid, packageName) as Int
                    } catch (e: Exception) {
                        // If reflection fails, exclude the app (return MODE_IGNORED)
                        // This ensures we only show apps we can verify have notifications enabled
                        return false
                    }
                }
                // MODE_ALLOWED = 0, MODE_IGNORED = 1, MODE_ERRORED = 2
                // Only return true if mode is explicitly MODE_ALLOWED
                mode == AppOpsManager.MODE_ALLOWED
            } else {
                // For older versions (pre-API 23), notifications were always enabled by default
                true
            }
        } catch (e: Exception) {
            // If we can't check (e.g., app not found), exclude the app to be safe
            false
        }
    }

    /**
     * Gets a filtered list of installed apps that have notification permissions enabled.
     * This is useful for showing only apps that can actually send notifications.
     */
    fun getAppsWithNotificationPermission(): List<AppInfo> {
        val allApps = getInstalledApps()
        return allApps.filter { app ->
            hasNotificationPermission(app.packageName)
        }.sortedBy { it.label }
    }

    // V2: Smart App List for Onboarding - Only show High Noise apps (Social, Messaging)
    fun getSmartOnboardingApps(): List<AppInfo> {
        val allApps = getInstalledApps()
        
        // Key categories to show initially
        val highNoisePackages = listOf(
            "com.whatsapp", "com.instagram", "com.facebook", "com.snapchat", 
            "com.twitter", "com.reddit", "com.discord", "org.telegram",
            "com.google.android.apps.messaging", "com.google.android.gm",
            "com.slack", "com.microsoft.teams"
        )
        
        val smartList = allApps.filter { app ->
            highNoisePackages.any { app.packageName.contains(it, ignoreCase = true) } ||
            app.label.contains("Gram", true) || // Instagram, Telegram
            app.label.contains("Chat", true) || // WeChat, Snapchat
            app.label.contains("Msg", true) ||  // Messenger
            app.label.contains("Mail", true)    // Gmail, Hotmail
        }
        
        // If list is too small, fallback to top 8 installed apps
        return if (smartList.size >= 4) smartList else allApps.take(10)
    }

    // V2: Generic Urgent App Detection (Banks, Payments, Security)
    fun findUrgentApps(): List<AppInfo> {
        val allApps = getInstalledApps()
        
        val urgentKeywords = listOf(
            "bank", "pay", "upi", "wallet", "money", "finance", 
            "auth", "secure", "security", "authenticator", "card"
        )
        
        val urgentPackages = listOf(
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.phonepe", "net.one97.paytm", "com.paypal", 
            "com.sbi", "com.hdfc", "com.icici" // Indian Banks (examples)
        )

        return allApps.filter { app ->
            val labelLower = app.label.lowercase()
            val pkgLower = app.packageName.lowercase()
            
            urgentKeywords.any { labelLower.contains(it) } || 
            urgentPackages.any { pkgLower.contains(it) }
        }
    }
}
