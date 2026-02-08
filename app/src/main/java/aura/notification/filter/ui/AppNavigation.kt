package aura.notification.filter.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import aura.notification.filter.ui.dashboard.DashboardScreen
import aura.notification.filter.ui.onboarding.OnboardingScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import android.net.Uri
import aura.notification.filter.ui.components.PulsingAuraLoader

@Composable
fun AppNavigation(startDestination: String = "onboarding") {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = { 
            slideInHorizontally(initialOffsetX = { it }) + fadeIn() 
        },
        exitTransition = { 
            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() 
        },
        popEnterTransition = { 
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() 
        },
        popExitTransition = { 
            slideOutHorizontally(targetOffsetX = { it }) + fadeOut() 
        }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                navController = navController,
                onFinish = {
                    // Fallback finish if needed
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(navController = navController)
        }
        composable("digest") {
            aura.notification.filter.ui.digest.DigestScreen(onClose = { navController.popBackStack() })
        }
        composable("settings") {
            aura.notification.filter.ui.settings.SettingsMenuScreen(navController = navController)
        }
        composable("shield_control") {
            aura.notification.filter.ui.settings.ShieldControlScreen(navController = navController)
        }
        composable("profile_detail/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: "STANDARD"
            aura.notification.filter.ui.settings.ProfileDetailScreen(navController, hiltViewModel(), profileId)
        }

        
        // --- NEW V2 APP PICKER (Single Mode) ---
        composable(
            route = "app_picker",
            enterTransition = { 
                androidx.compose.animation.scaleIn(initialScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn() 
            },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { 
                androidx.compose.animation.scaleOut(targetScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut() 
            }
        ) {
            aura.notification.filter.ui.picker.AppPickerScreen(navController = navController, viewModel = hiltViewModel(), initialSelectionMode = false)
        }
        
        // --- BATCH ONBOARDING FLOW ---
        composable(
            route = "onboarding/app_picker",
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            aura.notification.filter.ui.picker.AppPickerScreen(
                navController = navController, 
                viewModel = hiltViewModel(), 
                initialSelectionMode = true
            )
        }
        composable("batch_config/{packageNames}") { backStackEntry ->
            val packageNames = backStackEntry.arguments?.getString("packageNames") ?: ""
            aura.notification.filter.ui.settings.BatchConfigScreen(
                navController = navController,
                packageNames = packageNames,
                mainViewModel = hiltViewModel()
            )
        }
        composable("celebration") {
            aura.notification.filter.ui.onboarding.CelebrationScreen(navController)
        }

        composable("paywall") {
             aura.notification.filter.ui.paywall.PaywallScreen(
                 onPurchaseClick = { /* TODO: Trigger Billing */ },
                 onClose = { navController.popBackStack() }
             )
        }
        composable("app_config/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            val viewModel: aura.notification.filter.ui.MainViewModel = hiltViewModel()
            val activeRules by viewModel.activeRules.collectAsState()
            val isPro by viewModel.isPro.collectAsState()
            
            // Fix: Don't bail if rule doesn't exist. It might be a new app.
            // Fetch AppInfo for label/icon
            val appInfo = remember(packageName) { viewModel.getAppInfo(packageName) }
            
            // Bug Fix: Load rule directly to ensure allowed apps (OPEN) are also loaded
            var rule by remember { mutableStateOf<aura.notification.filter.data.AppRuleEntity?>(null) }
            var isLoading by remember { mutableStateOf(true) } // Fix: Prevent flash of empty state

            LaunchedEffect(packageName) {
                // Add artificial delay for smooth transition if needed, or just fetch
                rule = viewModel.getRule(packageName)
                isLoading = false
            }
            
            // Fix: Use generic clean loader instead of CircularProgressIndicator (Crash prevent)
            if (isLoading) {
                 Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), // Darker background for focus 
                    contentAlignment = Alignment.Center
                 ) {
                     PulsingAuraLoader(color = Color(0xFFDAA520))
                 }
            } else {
                aura.notification.filter.ui.settings.AppConfigSheet(
                    appName = appInfo.label, 
                    packageName = packageName,
                    icon = appInfo.icon,
                    isPro = isPro,
                    // Use rule if exists, otherwise defaults
                    currentShieldLevel = rule?.shieldLevel ?: aura.notification.filter.data.ShieldLevel.SMART,
                    initialCategories = rule?.activeCategories ?: "",
                    keywords = rule?.customKeywords ?: "",
                    onSave = { selectedLevel, selectedCategories, selectedKeywords ->
                        val existingStructure = rule
                        viewModel.updateRule(
                            aura.notification.filter.data.AppRuleEntity(
                                packageName = packageName,
                                // Maintain existing profile ID if editing, else default to FOCUS
                                profileId = existingStructure?.profileId ?: "FOCUS", 
                                shieldLevel = selectedLevel,
                                activeCategories = selectedCategories,
                                customKeywords = selectedKeywords,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                        navController.popBackStack()
                    },
                    onRemove = {
                        // Pass correct argument to removeRule
                        viewModel.removeRule(packageName)
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
        composable("notification_detail/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            aura.notification.filter.ui.notifications.NotificationDetailScreen(
                navController = navController,
                packageName = packageName
            )
        }
    }
}
