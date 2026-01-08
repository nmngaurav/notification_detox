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

    
    // Overlay State
    var selectedRule by remember { mutableStateOf<com.aura.data.AppRuleEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingPackage by remember { mutableStateOf<String?>(null) }
    
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
                                notifications = groupedNotifications[pkg] ?: emptyList()
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
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "DETOX FILTERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                     // 1. SMART FILTER CENTER (Conditional)
                     // HIDE if activeRules is empty (as per user request: "remove star icon on bottom left if there are not apps configured")
                     if (activeRules.isNotEmpty() && (activeRules.isEmpty() || blockedCount > 0)) {
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

                         // 2. ADD BUTTON
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
                        
                        // 3. CONFIGURED APPS
                        items(items = activeRules) { rule ->
                            val info = remember(rule) { viewModel.getAppInfo(rule.packageName) }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                   modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF111111))
                                        .border(1.dp, accentColor.copy(alpha = 0.2f), CircleShape)
                                        .clickable { 
                                            // Click -> Open Overlay
                                            selectedRule = rule
                                        },
                                   contentAlignment = Alignment.Center
                                ) {
                                     if (info.icon != null) {
                                         AppIcon(info.icon, modifier = Modifier.size(40.dp))
                                     } else {
                                         Text(
                                             text = info.label.take(1).uppercase(),
                                             color = Color.White,
                                             style = MaterialTheme.typography.titleLarge,
                                             fontWeight = FontWeight.Bold
                                         )
                                     }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.label,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    notifications: List<com.aura.data.NotificationEntity>
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
                                color = accentColor.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
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
