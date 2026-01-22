package aura.notification.filter.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import aura.notification.filter.ui.dashboard.DashboardScreen
import aura.notification.filter.ui.onboarding.OnboardingScreen
import androidx.hilt.navigation.compose.hiltViewModel

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
                onFinish = {
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
        composable("app_selection/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: "STANDARD"
            aura.notification.filter.ui.settings.AppSelectionScreen(navController, hiltViewModel(), profileId)
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
            // We can observe the rule. For now, get from active list or repo (via VM if we add it).
            // Simplest: Find in active list or fetch.
            // Let's grab the current rule from active list flow for instant display.
            val activeRules by viewModel.activeRules.collectAsState()
            val rule = activeRules.find { r -> r.packageName == packageName } 
                ?: return@composable 
            
            aura.notification.filter.ui.settings.AppConfigSheet(
                appName = "App", 
                packageName = rule.packageName,
                currentShieldLevel = rule.shieldLevel,
                initialCategories = rule.activeCategories,
                keywords = rule.customKeywords,
                onSave = { selectedLevel, selectedCategories, selectedKeywords ->
                    viewModel.updateRule(rule.copy(shieldLevel = selectedLevel, activeCategories = selectedCategories, customKeywords = selectedKeywords))
                    navController.popBackStack()
                },
                onRemove = {
                    viewModel.removeRule(rule.packageName)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
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
