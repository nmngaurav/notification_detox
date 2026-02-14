package aura.notification.filter.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import aura.notification.filter.ai.HeuristicEngine
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.ui.MainViewModel
import aura.notification.filter.ui.components.GlassCard
// PREMIUM COMPONENTS
import aura.notification.filter.ui.components.*
import aura.notification.filter.service.BlockerNotificationService

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BatchConfigScreen(
    navController: NavController,
    packageNames: String, // Comma separated
    mainViewModel: MainViewModel = hiltViewModel(),
    isOnboarding: Boolean = false
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val packages = remember(packageNames) { packageNames.split(",").filter { it.isNotEmpty() } }
    
    // Internal States
    var selectedLevel by remember { mutableStateOf(ShieldLevel.SMART) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var customKeywords by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    val isPro by mainViewModel.isPro.collectAsState()
    
    // Animation/UI States
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    
    // Categorized Tags (Fixed)
    val heuristicEngine = remember { HeuristicEngine() }
    val categorizedTags = remember { heuristicEngine.getCategorizedTags() }

    AuraBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Batch Mode",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        AuraConfigHero(
                            subtitle = "Batch setup for multiple apps",
                            shieldCount = packages.size
                        )
                    }

                    item {
                        AuraPrecisionSection(
                            selectedLevel = selectedLevel,
                            onLevelSelected = { selectedLevel = it }
                        )
                    }

                    if (selectedLevel == ShieldLevel.SMART) {
                        item {
                            AuraFilterHierarchy(
                                categorizedTags = categorizedTags,
                                selectedTags = selectedTags,
                                expandedCategories = expandedCategories,
                                onTagToggle = { id ->
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    selectedTags = if (selectedTags.contains(id)) selectedTags - id else selectedTags + id
                                },
                                onCategoryToggle = { category, tagIds, state ->
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    selectedTags = if (state == androidx.compose.ui.state.ToggleableState.On) {
                                        selectedTags - tagIds.toSet()
                                    } else {
                                        selectedTags + tagIds.toSet()
                                    }
                                },
                                onExpandToggle = { category ->
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    expandedCategories[category] = !(expandedCategories[category] ?: false)
                                }
                            )
                        }
                    }

                    item {
                        if (isPro) {
                            AuraCustomRulesSection(
                                keywords = customKeywords,
                                onKeywordsChange = { customKeywords = it }
                            )
                        } else {
                            ProLockedKeywords(
                                onProClick = { navController.navigate("paywall") },
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                        AuraConfigFooter(
                            primaryActionLabel = "APPLY TO ALL",
                            isLoading = isSaving,
                            onPrimaryAction = {
                                isSaving = true
                                packages.forEach { pkg ->
                                    mainViewModel.updateRule(
                                        aura.notification.filter.data.AppRuleEntity(
                                            packageName = pkg,
                                            profileId = "FOCUS",
                                            shieldLevel = selectedLevel,
                                            activeCategories = selectedTags.joinToString(","),
                                            customKeywords = customKeywords,
                                            lastUpdated = System.currentTimeMillis()
                                        )
                                    )
                                }
                                if (isOnboarding) {
                                    navController.navigate("celebration") {
                                        popUpTo("onboarding/app_picker") { inclusive = true }
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onReset = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                selectedLevel = ShieldLevel.SMART
                                selectedTags = setOf(HeuristicEngine.TAG_SECURITY)
                                customKeywords = ""
                            }
                        )
                    }
                }
            }
        }
    }
}
