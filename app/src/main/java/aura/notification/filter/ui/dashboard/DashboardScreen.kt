package aura.notification.filter.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import aura.notification.filter.ui.components.ZenEmptyState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import aura.notification.filter.ui.MainViewModel
import aura.notification.filter.ui.components.LiquidCard
import aura.notification.filter.util.AppInfoManager
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val timeline by viewModel.timelineState.collectAsState()
    val blockedCount by viewModel.totalBlockedCount.collectAsState()
    val activeRules by viewModel.activeRules.collectAsState()
    
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    // Theme
    val accentColor = Color(0xFFDAA520)

    Scaffold(
        containerColor = Color(0xFF050505),
        floatingActionButton = {
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            FloatingActionButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.navigate("app_picker") 
                },
                containerColor = accentColor,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add App")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ... (Header skipped, kept same) ...
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accentColor, CircleShape) // Pulsing active dot
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (blockedCount > 0) "$blockedCount Notifications Filtered" else "Aura Active • Total Peace",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                }
            }


            // --- TIMELINE FEED ---
            Column(
                modifier = Modifier
                    .weight(1f) // Push Monitor to bottom
                    .fillMaxWidth()
            ) {
                if (activeRules.isEmpty()) {
                    // Empty State (No Apps Configured)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp).background(Color(0xFF1E1E1E), CircleShape).padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Start Your Detox", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add apps to filter their notifications.\nCritical alerts will still get through.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("app_picker") },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                        ) {
                            Text("Add App")
                        }
                    }
                } else {
                    // --- TIMELINE (Only show if data exists) ---
                    if (timeline.isNotEmpty()) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(timeline, key = { it.id }) { burst ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            viewModel.clearActivityForPackage(burst.packageName)
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val colorAlpha by androidx.compose.animation.core.animateFloatAsState(
                                            if (dismissState.dismissDirection != null) 0.6f else 0.2f, label = "swipeAlpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                        colors = listOf(Color.Transparent, Color.Red.copy(alpha = colorAlpha))
                                                    )
                                                ),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(end = 32.dp), 
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Clear", color = Color.Red.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                Spacer(Modifier.width(10.dp))
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    BurstCard(burst = burst, viewModel = viewModel, accentColor = accentColor)
                                }
                            }
                        }
                    } else {
                        // User Feedback Fix: Monitoring State (Active but no blocks yet)
                        ZenEmptyState(accentColor = accentColor)
                    }
                }
            }

            // --- ACTIVE MONITORED APPS (FOOTER) ---
            if (activeRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF1E1E1E))) // Divider
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A)) // Ensure background for footer
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Monitored Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(start = 24.dp, end = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(activeRules) { rule ->
                            val appInfo = viewModel.getAppInfo(rule.packageName)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(64.dp) // More compact
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp) // Smaller icon for footer
                                        .clip(CircleShape)
                                        .background(Color(0xFF222222)) // Added Background
                                        .clickable { navController.navigate("app_config/${Uri.encode(rule.packageName)}") }
                                ) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { ctx -> 
                                            android.widget.ImageView(ctx).apply { 
                                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER 
                                            } 
                                        },
                                        update = { view ->
                                            if (appInfo.icon != null) {
                                                view.setImageDrawable(appInfo.icon)
                                            } else {
                                                view.setImageDrawable(null)
                                            }
                                        }
                                    )
                                    // Status Indicator
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(12.dp)
                                            .background(
                                                if (rule.shieldLevel == aura.notification.filter.data.ShieldLevel.FORTRESS) Color.Red else Color(0xFFDAA520),
                                                CircleShape
                                            )
                                            .border(2.dp, Color(0xFF050505), CircleShape)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = appInfo.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp, // Smaller font for footer
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        // --- ADD BUTTON REMOVED (Redundant with FAB) ---
                    }
                }
            }
        }
    }
}

@Composable
fun BurstCard(
    burst: NotificationBurst,
    viewModel: MainViewModel,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    val appInfo = remember(burst.packageName) { viewModel.getAppInfo(burst.packageName) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    // Summary State
    val isSummaryMode by remember(burst.packageName, viewModel.perAppViewMode) { 
        derivedStateOf { viewModel.perAppViewMode[burst.packageName] == true } 
    }
    val summaryText by remember(burst.packageName, viewModel.summaries) { 
        derivedStateOf { viewModel.summaries[burst.packageName] } 
    }

    aura.notification.filter.ui.components.PremiumMagneticCard(
        modifier = Modifier.fillMaxWidth(),
        isExpanded = isExpanded || isSummaryMode,
        accentColor = accentColor,
        backgroundColor = if (isExpanded) Color(0xFF151515) else Color(0xFF0F0F0F)
    ) {
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            // --- HEADER ROW ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon
                Box(
                    modifier = Modifier
                         .size(40.dp)
                         .clip(CircleShape)
                         .background(Color(0xFF222222))
                ) {
                     androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx -> 
                            android.widget.ImageView(ctx).apply { 
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER 
                            } 
                        },
                        update = { view ->
                             if (appInfo.icon != null) {
                                view.setImageDrawable(appInfo.icon)
                             } else {
                                view.setImageDrawable(null)
                             }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appInfo.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        // Velocity Indicator (New)
                        if (System.currentTimeMillis() - burst.timestamp < 10 * 60 * 1000L) {
                             Spacer(Modifier.width(8.dp))
                             Box(Modifier.size(6.dp).background(accentColor, CircleShape))
                        }
                    }
                    Text(
                        text = "Latest ${timeFormat.format(Date(burst.timestamp))}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                // ACTION: Summary Toggle
                if (burst.size > 1) {
                      Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSummaryMode) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewModel.toggleSummaryForPackage(burst.packageName, burst.notifications) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSummaryMode) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = "Close",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isSummaryMode) "Close" else "Summarize",
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                // Expand Icon / Count Badge
                if (!isExpanded && !isSummaryMode) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${burst.size}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- EXPANDED CONTENT ---
            AnimatedVisibility(
                visible = isExpanded || isSummaryMode, 
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    
                    if (isSummaryMode) {
                        // PREMIUM AI Summary View
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AURA AI INSIGHT", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        if (summaryText == null || summaryText == "Thinking...") {
                            aura.notification.filter.ui.components.AuraShimmer(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            aura.notification.filter.ui.components.TypewriterText(
                                text = summaryText!!,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                delayMillis = 20
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Generated from ${burst.size} messages",
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        // Raw List View
                        burst.notifications.forEach { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Timeline indicator
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.content, 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp, 
                                        lineHeight = 20.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (note.title.isNotEmpty()) {
                                            Text(
                                                text = note.title, 
                                                color = Color.Gray, 
                                                fontSize = 11.sp, 
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = timeFormat.format(Date(note.timestamp)), 
                                            color = Color.DarkGray, 
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
