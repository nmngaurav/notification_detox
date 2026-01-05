package com.aura.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.aura.ui.dashboard.DashboardScreen
import com.aura.ui.onboarding.OnboardingScreen
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
            com.aura.ui.digest.DigestScreen(onClose = { navController.popBackStack() })
        }
        composable("settings") {
            com.aura.ui.settings.SettingsMenuScreen(navController = navController)
        }
        composable("shield_control") {
            com.aura.ui.settings.ShieldControlScreen(navController = navController)
        }
        composable("profile_detail/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: "STANDARD"
            com.aura.ui.settings.ProfileDetailScreen(navController, hiltViewModel(), profileId)
        }
        composable("app_selection/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: "STANDARD"
            com.aura.ui.settings.AppSelectionScreen(navController, hiltViewModel(), profileId)
        }
        composable("paywall") {
             com.aura.ui.paywall.PaywallScreen(
                 onPurchaseClick = { /* TODO: Trigger Billing */ },
                 onClose = { navController.popBackStack() }
             )
        }
    }
}
