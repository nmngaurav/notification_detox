package com.aura.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.hilt.navigation.compose.hiltViewModel
import com.aura.ui.MainViewModel
import com.aura.ui.components.GlassCard
import androidx.navigation.NavController
import androidx.compose.foundation.Canvas
import android.widget.ImageView
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val blockedCount by viewModel.totalBlockedCount.collectAsState()
    val activeRules by viewModel.activeRules.collectAsState()
    val activeMode by viewModel.currentMode.collectAsState()
    val isSummarizeMode by viewModel.isSummarizeMode.collectAsState()
    val groupedNotifications by viewModel.groupedNotifications.collectAsState()
    val summaries = viewModel.summaries
    val isPro by viewModel.isPro.collectAsState()

    
    // Overlay State
    var selectedRule by remember { mutableStateOf<com.aura.data.AppRuleEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingPackage by remember { mutableStateOf<String?>(null) }
    
    // Bulk Selection State
    var isBulkSelectMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var showBulkConfigSheet by remember { mutableStateOf(false) }
    
    // Premium gate for bulk actions (set to true to lock behind premium in future)
    val isBulkActionPremiumLocked = false // Change to !isPro when ready to monetize
    
    // Particle Background State
    val particles = remember { List(20) { Particle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "Particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    // Gold Theme
    val accentColor = Color(0xFFDAA520) 

    // OVERLAY: Config Sheet
    if (selectedRule != null) {
        val appInfo = remember(selectedRule) { viewModel.getAppInfo(selectedRule!!.packageName) }
        
        com.aura.ui.settings.AppConfigSheet(
            appName = appInfo.label,
            packageName = selectedRule!!.packageName,
            icon = appInfo.icon,
            currentShieldLevel = selectedRule!!.shieldLevel,
            initialCategories = selectedRule!!.activeCategories,
            keywords = selectedRule!!.customKeywords,
            onSave = { level, categories, keywords ->
                viewModel.updateSmartRule(selectedRule!!.packageName, activeMode.name, categories, keywords)
                selectedRule = null
            },
            onRemove = {
                viewModel.removeRule(selectedRule!!.packageName)
                selectedRule = null
            },
            onDismiss = { selectedRule = null }
        )
    }

    // DELETE DIALOG
    if (showDeleteDialog && deletingPackage != null) {
        val appInfo = remember(deletingPackage) { viewModel.getAppInfo(deletingPackage!!) }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Stop Digital Detox?", color = Color.White) },
            text = { Text("Aura will stop filtering notifications for ${appInfo.label}. You can re-enable it anytime.", color = Color.Gray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeRule(deletingPackage!!)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Stop Detox", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
    
    // BULK CONFIG SHEET
    if (showBulkConfigSheet && selectedPackages.isNotEmpty()) {
        BulkConfigSheet(
            selectedPackages = selectedPackages.toList(),
            viewModel = viewModel,
            isPremiumLocked = isBulkActionPremiumLocked,
            onApply = { shieldLevel, categories, keywords ->
                viewModel.applyBulkConfig(
                    packageNames = selectedPackages.toList(),
                    profileId = activeMode.name,
                    shieldLevel = shieldLevel,
                    categories = categories,
                    keywords = keywords
                )
                showBulkConfigSheet = false
                isBulkSelectMode = false
                selectedPackages = emptySet()
            },
            onDismiss = {
                showBulkConfigSheet = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Premium Particle Background
        Canvas(
             modifier = Modifier
                 .fillMaxSize()
                 .then(
                     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                         Modifier.blur(60.dp)
                     } else {
                         Modifier
                     }
                 )
        ) {
            val width = size.width
            val height = size.height
            
            // Draw gradient mesh
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.3f),
                    radius = width * 0.7f
                )
            )
            
            // Draw drifting particles
            particles.forEachIndexed { index, particle ->
                val x = (particle.initialX + cos(time * 6.28 + index).toFloat() * 50f) % width
                val y = (particle.initialY + sin(time * 6.28 + index).toFloat() * 100f + time * 200f) % height
                
                // Wrap around
                val drawX = if (x < 0) width + x else x
                val drawY = if (y < 0) height + y else y
                
                drawCircle(
                    color = accentColor.copy(alpha = particle.alpha * 0.6f),
                    radius = particle.size,
                    center = androidx.compose.ui.geometry.Offset(drawX, drawY)
                )
            }
        }
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aura",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (blockedCount > 0) {
                        Text(
                            text = "⚡ $blockedCount notifications blocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    } else if (activeRules.isNotEmpty()) {
                        Text(
                            text = "Detox Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                }
                
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.List, contentDescription = "Menu", tint = Color.Gray)
                }
            }
            

                
                // --- VERTICAL NOTIFICATION FEED ---
                if (blockedCount > 0) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(groupedNotifications.keys.toList()) { pkg ->
                            AppActionCard(
                                pkg = pkg,
                                viewModel = viewModel,
                                accentColor = accentColor,
                                summaries = summaries,
                                notifications = groupedNotifications[pkg] ?: emptyList(),
                                navController = navController
                            )
                        }
                    }
                } else if (activeRules.isEmpty()) {
                    // EMPTY STATE: NO APPS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1A1A))
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                                    .clickable { navController.navigate("app_selection/${activeMode.name}") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add First App",
                                    tint = accentColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Your Notification Detox is Ready",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                            Text(
                                text = "Select apps to filter out noise.\nOnly important alerts will get through.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    // QUIET STATE: APPS EXIST, NO NOTIFICATIONS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    )
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape)
                                    .clickable { navController.navigate("shield_control") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Smart Filter",
                                    tint = accentColor,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Notification Detox Active",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No notifications blocked",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Spacer for visual breathing room before dock
                Spacer(modifier = Modifier.height(16.dp))

            // --- APPS DOCK (Bottom) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isBulkSelectMode) 8.dp else 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBulkSelectMode) "SELECT APPS (${selectedPackages.size})" else "DETOX FILTERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBulkSelectMode) accentColor else Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Bulk Select Toggle / Exit Button
                    if (activeRules.size >= 2) {
                        if (isBulkSelectMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Select All Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222222))
                                        .clickable {
                                            selectedPackages = if (selectedPackages.size == activeRules.size) {
                                                emptySet()
                                            } else {
                                                activeRules.map { it.packageName }.toSet()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (selectedPackages.size == activeRules.size) "Deselect All" else "Select All",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                
                                // Cancel Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222222))
                                        .clickable {
                                            isBulkSelectMode = false
                                            selectedPackages = emptySet()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        } else {
                            // Bulk Edit Button (Entry point)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .border(
                                        width = 1.dp,
                                        color = accentColor.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { 
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        isBulkSelectMode = true 
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Bulk Edit",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bulk Edit",
                                        color = accentColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                     // 1. SMART FILTER CENTER (Conditional) - Hide in bulk select mode
                     if (!isBulkSelectMode && activeRules.isNotEmpty() && (activeRules.isEmpty() || blockedCount > 0)) {
                         item {
                             Box(
                                 modifier = Modifier
                                     .size(60.dp)
                                     .clip(CircleShape)
                                     .background(
                                         brush = Brush.linearGradient(
                                             colors = listOf(accentColor.copy(alpha = 0.3f), Color(0xFF1A1A1A))
                                         )
                                     )
                                     .border(1.dp, accentColor.copy(alpha = 0.2f), CircleShape)
                                     .clickable { navController.navigate("shield_control") },
                                 contentAlignment = Alignment.Center
                             ) {
                                 Icon(
                                     imageVector = Icons.Default.Star,
                                     contentDescription = "Smart Filter Center",
                                     tint = accentColor,
                                     modifier = Modifier.size(28.dp)
                                 )
                             }
                         }
                     }

                         // 2. ADD BUTTON - Hide in bulk select mode
                        if (!isBulkSelectMode) {
                            item {
                                 Box(
                                     modifier = Modifier
                                         .size(60.dp)
                                         .clip(CircleShape)
                                         .background(Color(0xFF1A1A1A))
                                         .border(1.dp, Color.DarkGray.copy(alpha = 0.3f), CircleShape)
                                         .clickable { navController.navigate("app_selection/${activeMode.name}") },
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Icon(
                                         imageVector = Icons.Default.Add,
                                         contentDescription = "Add App",
                                         tint = Color.Gray,
                                         modifier = Modifier.size(28.dp)
                                     )
                                 }
                            }
                        }
                        
                        // 3. CONFIGURED APPS (with bulk select support)
                        items(items = activeRules) { rule ->
                            val info = remember(rule) { viewModel.getAppInfo(rule.packageName) }
                            val isSelected = selectedPackages.contains(rule.packageName)
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                   modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.2f) 
                                            else Color(0xFF111111)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) accentColor else accentColor.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .pointerInput(isBulkSelectMode) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    if (!isBulkSelectMode) {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        isBulkSelectMode = true
                                                        selectedPackages = setOf(rule.packageName)
                                                    }
                                                },
                                                onTap = {
                                                    if (isBulkSelectMode) {
                                                        // Toggle selection
                                                        selectedPackages = if (isSelected) {
                                                            selectedPackages - rule.packageName
                                                        } else {
                                                            selectedPackages + rule.packageName
                                                        }
                                                    } else {
                                                        // Normal click -> Open config
                                                        selectedRule = rule
                                                    }
                                                }
                                            )
                                        },
                                   contentAlignment = Alignment.Center
                                ) {
                                     if (info.icon != null) {
                                         AppIcon(
                                             info.icon, 
                                             modifier = Modifier
                                                 .size(40.dp)
                                                 .alpha(if (isBulkSelectMode && !isSelected) 0.5f else 1f)
                                         )
                                     } else {
                                         Text(
                                             text = info.label.take(1).uppercase(),
                                             color = Color.White,
                                             style = MaterialTheme.typography.titleLarge,
                                             fontWeight = FontWeight.Bold
                                         )
                                     }
                                     
                                     // Selection Checkmark Overlay
                                     if (isBulkSelectMode && isSelected) {
                                         Box(
                                             modifier = Modifier
                                                 .size(60.dp)
                                                 .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.Check,
                                                 contentDescription = "Selected",
                                                 tint = accentColor,
                                                 modifier = Modifier.size(28.dp)
                                             )
                                         }
                                     }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.label,
                                    color = if (isSelected) accentColor else Color.Gray,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            
            // --- BULK ACTION BAR (Floating at bottom when in bulk mode) ---
            AnimatedVisibility(
                visible = isBulkSelectMode && selectedPackages.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedPackages.size} apps selected",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Apply settings to all",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Button(
                            onClick = { 
                                if (!isBulkActionPremiumLocked) {
                                    showBulkConfigSheet = true
                                } else {
                                    // TODO: Show premium upsell dialog
                                    navController.navigate("premium")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isBulkActionPremiumLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Premium",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "Apply Config",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Particle Helper
class Particle {
    val initialX = Random.nextFloat() * 1080f
    val initialY = Random.nextFloat() * 2400f
    val size = Random.nextFloat() * 4f + 2f
    val alpha = Random.nextFloat() * 0.5f + 0.1f
}

@Composable
fun AppActionCard(
    pkg: String,
    viewModel: MainViewModel,
    accentColor: Color,
    summaries: Map<String, String>,
    notifications: List<com.aura.data.NotificationEntity>,
    navController: NavController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appInfo = remember(pkg) { viewModel.getAppInfo(pkg) }
    val summary = summaries[pkg]
    // Use the per-app mode if available, otherwise check if global mode is on.
    // However, user requested default is RAW list, summary only on tap.
    // So we primarily rely on `summary != null` to show summary view if generated.
    // Or we can use `perAppViewMode`. Let's assume `generateSummary` toggles the view.
    val isSummaryMode = viewModel.perAppViewMode[pkg] == true
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App Identity & Open App Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (appInfo.icon != null) {
                        AppIcon(appInfo.icon, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = appInfo.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                // Actions Row
                Row {
            // ACTION: SUMMARIZE TOGGLE (Clickable Area)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                            .clickable { viewModel.toggleSummaryForPackage(pkg, notifications) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSummaryMode) "✨ Done" else "✨ Summarize",
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    // ACTION: OPEN APP
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .clickable {
                                 val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                 intent?.let { context.startActivity(it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Open App",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // CONTENT AREA: Raw Notifications OR Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (isSummaryMode && summary != null) {
                    Text(
                        text = summary,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                } else {
                    // Raw List (Top 3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        notifications.take(3).forEach { note ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(4.dp)
                                        .background(accentColor.copy(alpha = 0.7f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    if (note.title.isNotEmpty()) {
                                        Text(
                                           text = note.title,
                                           color = Color.LightGray,
                                           style = MaterialTheme.typography.labelMedium,
                                           fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = note.content,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        if (notifications.size > 3) {
                            Text(
                                text = "+ ${notifications.size - 3} more",
                                color = accentColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(start = 12.dp, top = 4.dp)
                                    .clickable { 
                                        navController.navigate("notification_detail/$pkg")
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(drawable: android.graphics.drawable.Drawable, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                setImageDrawable(drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
                // Add tiny padding to prevent touching edges if adaptive icon background is messy
                setPadding(2, 2, 2, 2)
            }
        },
        update = { it.setImageDrawable(drawable) }
    )
}

// ============================================================================
// BULK CONFIG SHEET - Apply settings to multiple apps at once
// ============================================================================

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BulkConfigSheet(
    selectedPackages: List<String>,
    viewModel: MainViewModel,
    isPremiumLocked: Boolean,
    onApply: (com.aura.data.ShieldLevel, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = Color(0xFFDAA520)
    
    var selectedLevel by remember { mutableStateOf(com.aura.data.ShieldLevel.SMART) }
    var selectedTags by remember { mutableStateOf(setOf("OTPs", "Login Codes", "Fraud Alerts", "Calls")) }
    var currentKeywords by remember { mutableStateOf("") }
    
    // Tag sections for bulk config (simplified universal set)
    val tagSections = remember {
        listOf(
            BulkTagSection("Critical & Security", Icons.Default.Lock, listOf("OTPs", "Login Codes", "Fraud Alerts", "Calls", "Alarms")),
            BulkTagSection("Social & Chat", Icons.Default.Email, listOf("DMs", "Group Chats", "Mentions", "Replies", "Voice Msgs")),
            BulkTagSection("Logistics & Time", Icons.Default.ShoppingCart, listOf("Rides", "Delivery", "Reminders", "Calendar", "Traffic")),
            BulkTagSection("Money", Icons.Default.Info, listOf("Transaction", "Bill Due", "Salary", "Offers")),
            BulkTagSection("System", Icons.Default.Settings, listOf("Updates", "Downloads", "Reviews", "System"))
        )
    }
    
    val isSmartMode = selectedLevel == com.aura.data.ShieldLevel.SMART
    val hasRules = selectedTags.isNotEmpty() || currentKeywords.isNotEmpty()
    val canSave = !isSmartMode || hasRules

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // --- HEADER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accentColor.copy(alpha = 0.3f), Color(0xFF222222))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Bulk Edit",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Bulk Configuration",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedPackages.size} apps selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor
                    )
                }
            }
            
            // --- SELECTED APPS PREVIEW ---
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(selectedPackages.take(6)) { pkg ->
                    val info = remember(pkg) { viewModel.getAppInfo(pkg) }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (info.icon != null) {
                            AppIcon(info.icon, modifier = Modifier.size(28.dp))
                        } else {
                            Text(
                                text = info.label.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (selectedPackages.size > 6) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${selectedPackages.size - 6}",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
            
            // --- MODE SELECTOR ---
            com.aura.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Apply Mode to All",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Option 1: Allow All
                    BulkModeOption(
                        title = "Allow All",
                        description = "Let everything through for all apps",
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color.Green,
                        selected = selectedLevel == com.aura.data.ShieldLevel.OPEN,
                        onClick = { selectedLevel = com.aura.data.ShieldLevel.OPEN }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 2: Smart Filter
                    BulkModeOption(
                        title = "Smart Filter",
                        description = "Only allow selected categories",
                        icon = Icons.Default.Done,
                        iconTint = accentColor,
                        selected = selectedLevel == com.aura.data.ShieldLevel.SMART,
                        onClick = { selectedLevel = com.aura.data.ShieldLevel.SMART }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 3: Block All
                    BulkModeOption(
                        title = "Block All",
                        description = "Complete silence for all apps",
                        icon = Icons.Default.Lock,
                        iconTint = Color.Red,
                        selected = selectedLevel == com.aura.data.ShieldLevel.FORTRESS,
                        onClick = { selectedLevel = com.aura.data.ShieldLevel.FORTRESS }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- SMART FILTER OPTIONS (Scrollable) ---
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedLevel == com.aura.data.ShieldLevel.SMART) {
                    item {
                        Text(
                            text = "ALLOW THESE CATEGORIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "These filters will be applied to all ${selectedPackages.size} selected apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    item {
                        com.aura.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                tagSections.forEachIndexed { index, section ->
                                    if (index > 0) {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = Color.White.copy(alpha = 0.1f)
                                        )
                                    }
                                    
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(28.dp)
                                                .background(Color(0xFF222222), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                section.icon,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column {
                                            Text(
                                                text = section.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            
                                            BulkFlowRow(horizontalGap = 6.dp, verticalGap = 6.dp) {
                                                section.tags.forEach { tag ->
                                                    val isSelected = selectedTags.contains(tag)
                                                    BulkFilterChip(
                                                        label = tag,
                                                        selected = isSelected,
                                                        onClick = {
                                                            selectedTags = if (isSelected) {
                                                                selectedTags - tag
                                                            } else {
                                                                selectedTags + tag
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Quick Actions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222222))
                                    .clickable { 
                                        selectedTags = tagSections.flatMap { it.tags }.toSet()
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select All",
                                    color = accentColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222222))
                                    .clickable { selectedTags = emptySet() }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // --- FOOTER ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 56.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222222),
                        contentColor = Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = {
                        if (canSave) {
                            onApply(selectedLevel, selectedTags.joinToString(","), currentKeywords)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSave) accentColor else Color(0xFF222222),
                        contentColor = if (canSave) Color.Black else Color.Gray
                    ),
                    modifier = Modifier.weight(2f),
                    enabled = canSave
                ) {
                    Text(
                        text = if (canSave) "Apply to ${selectedPackages.size} Apps" else "Select Categories",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class BulkTagSection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tags: List<String>
)

@Composable
fun BulkModeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) Color(0xFF222222) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) iconTint.copy(alpha = 0.5f) else Color(0xFF333333),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) iconTint else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) Color.White else Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                color = if (selected) iconTint.copy(alpha = 0.8f) else Color.DarkGray,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = iconTint,
                unselectedColor = Color.Gray
            )
        )
    }
}

@Composable
fun BulkFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFFDAA520)
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (selected) accentColor.copy(alpha = 0.5f) else Color.Transparent,
                CircleShape
            )
            .background(
                if (selected) accentColor.copy(alpha = 0.2f) else Color(0xFF222222),
                CircleShape
            )
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Text("+ ", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(label, color = if (selected) Color.White else Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun BulkFlowRow(
    modifier: Modifier = Modifier,
    horizontalGap: androidx.compose.ui.unit.Dp = 0.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = horizontalGap.roundToPx()
        val vGapPx = verticalGap.roundToPx()
        
        var currentRowWidth = 0
        var nextRowWidth = 0
        var totalHeight = 0
        
        val rowHeights = mutableListOf<Int>()
        val rowWidths = mutableListOf<Int>()
        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(constraints)
            val widthWithGap = placeable.width + hGapPx
            
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rowWidths.add(currentRowWidth - hGapPx)
                rowHeights.add(nextRowWidth)
                totalHeight += nextRowWidth + vGapPx
                currentRowWidth = widthWithGap
                nextRowWidth = placeable.height
            } else {
                currentRowWidth += widthWithGap
                nextRowWidth = maxOf(nextRowWidth, placeable.height)
            }
            placeable
        }
        
        if (rowWidths.size < rowHeights.size + 1) {
            rowWidths.add(currentRowWidth - hGapPx)
            rowHeights.add(nextRowWidth)
            totalHeight += nextRowWidth
        }
        
        layout(width = constraints.maxWidth, height = totalHeight) {
            var yOffset = 0
            var xOffset = 0
            var rowIndex = 0
            var rowMaxHeight = rowHeights.getOrElse(0) { 0 }
            
            placeables.forEachIndexed { index, placeable ->
                if (xOffset + placeable.width > constraints.maxWidth) {
                    xOffset = 0
                    yOffset += rowMaxHeight + vGapPx
                    rowIndex++
                    rowMaxHeight = rowHeights.getOrElse(rowIndex) { 0 }
                }
                placeable.placeRelative(x = xOffset, y = yOffset)
                xOffset += placeable.width + hGapPx
            }
        }
    }
}
