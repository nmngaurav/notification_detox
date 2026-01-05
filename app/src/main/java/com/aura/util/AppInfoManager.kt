package com.aura.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
                
                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                

                
                // Include if:
                // 1. Installed from Play Store
                val isFromPlayStore = installer == "com.android.vending"
                
                // 2. User-sideloaded (no installer)
                val isSideloaded = installer == null && !isSystem
                
                // 3. Known notification apps (even if system)
                val knownNotificationApps = listOf(
                    "com.google.android.gm", // Gmail
                    "com.android.chrome",
                    "com.google.android.apps.messaging", // Messages
                    "com.whatsapp",
                    "com.facebook",
                    "com.instagram.android"
                )
                val isKnownApp = knownNotificationApps.any { packageName.startsWith(it) }
                
                if (!isFromPlayStore && !isSideloaded && !isKnownApp) {
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
            
            // Google Play Services (backend only)
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.ext.services",
            "com.google.android.configupdater",
            
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
            
            // OEM system processes
            "com.motorola.actions",
            "com.samsung.android.app.settings",
            "com.miui.system"
        )
        
        // ONLY block if it starts with one of these patterns
        return trueSystemServices.any { packageName.startsWith(it) }
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
