package aura.notification.filter.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import aura.notification.filter.R
import androidx.compose.foundation.Image
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.state.ToggleableState
import aura.notification.filter.ai.HeuristicEngine
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    appName: String,
    packageName: String,
    icon: android.graphics.drawable.Drawable? = null,
    isPro: Boolean,
    currentShieldLevel: aura.notification.filter.data.ShieldLevel,
    initialCategories: String = "",
    keywords: String,
    onSave: (aura.notification.filter.data.ShieldLevel, String, String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    onProClick: () -> Unit = {},
    analyticsManager: aura.notification.filter.util.AnalyticsManager
) {
    var selectedLevel by remember(currentShieldLevel) { mutableStateOf(currentShieldLevel) }
    var currentKeywords by remember(keywords) { mutableStateOf(keywords) }
    
    val heuristicEngine = remember { HeuristicEngine() }
    val categorizedTags = remember { heuristicEngine.getCategorizedTags() }

    // Initial Selection
    var selectedTags by remember { 
        mutableStateOf(
            if (initialCategories.isNotEmpty()) {
                initialCategories.split(",").filter { it.isNotEmpty() }.toSet()
            } else {
                setOf(HeuristicEngine.TAG_SECURITY) // Default safe
            }
        )
    }

    // Expandable State
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>().apply { 
        categorizedTags.keys.forEach { put(it, it == "Safety & Finance") } 
    } }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val accentColor = Color(0xFFDAA520)
    val isSmartMode = selectedLevel == ShieldLevel.SMART
    val hasRules = selectedTags.isNotEmpty() || currentKeywords.isNotEmpty()
    val canSave = !isSmartMode || hasRules

    // Unified Sync Pulsing (1200ms EaseInOutSine)
    val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_premium_crown),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("App Control", color = Color.White, fontWeight = FontWeight.Black)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = Color.Gray)
                    }
                },
                actions = {
                    TextButton(onClick = onRemove) {
                        Text("REMOVE", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                AuraConfigHero(
                    title = appName,
                    subtitle = "Aura Notification Blocker Active",
                    accentColor = when(selectedLevel) {
                        ShieldLevel.OPEN -> Color.Gray
                        ShieldLevel.SMART -> accentColor
                        ShieldLevel.FORTRESS -> Color(0xFFFF4D4D)
                        else -> accentColor
                    },
                    icon = {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSmartMode || selectedLevel == ShieldLevel.FORTRESS) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(pulseScale)
                                        .background(
                                            (if(selectedLevel == ShieldLevel.FORTRESS) Color(0xFFFF4D4D) else accentColor).copy(alpha = pulseAlpha), 
                                            CircleShape
                                        )
                                        .border(
                                            1.dp, 
                                            (if(selectedLevel == ShieldLevel.FORTRESS) Color(0xFFFF4D4D) else accentColor).copy(alpha = pulseAlpha), 
                                            CircleShape
                                        )
                                )
                            }
                            if (icon != null) {
                                AndroidView(
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                                    factory = { ctx -> android.widget.ImageView(ctx).apply { setImageDrawable(icon); scaleType = android.widget.ImageView.ScaleType.FIT_CENTER } }
                                )
                            } else {
                                Box(Modifier.size(56.dp).background(Color(0xFF222222), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Text(appName.take(1), color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                )
                
                Spacer(Modifier.height(8.dp))
            }

            item {
                AuraPrecisionSection(
                    selectedLevel = selectedLevel,
                    onLevelSelected = { 
                        selectedLevel = it 
                        val bundle = android.os.Bundle().apply {
                            putString(aura.notification.filter.util.AnalyticsConstants.PARAM_PACKAGE_NAME, packageName)
                            putString(aura.notification.filter.util.AnalyticsConstants.PARAM_LEVEL, it.name)
                        }
                        analyticsManager.logEvent(aura.notification.filter.util.AnalyticsConstants.EVENT_SHIELD_LEVEL_CHANGE, bundle)
                    }
                )
            }

            if (isSmartMode) {
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
                            selectedTags = if (state == ToggleableState.On) {
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
                        keywords = currentKeywords,
                        onKeywordsChange = { currentKeywords = it }
                    )
                } else {
                    ProLockedKeywords(onProClick = onProClick, modifier = Modifier.padding(top = 16.dp))
                }

                AuraConfigFooter(
                    primaryActionLabel = "SAVE CHANGES",
                    onPrimaryAction = { 
                        if (canSave) onSave(selectedLevel, selectedTags.joinToString(","), currentKeywords) 
                    },
                    onReset = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        selectedLevel = ShieldLevel.SMART
                        selectedTags = setOf(HeuristicEngine.TAG_SECURITY)
                        currentKeywords = ""
                    }
                )
            }
        }
    }
}
