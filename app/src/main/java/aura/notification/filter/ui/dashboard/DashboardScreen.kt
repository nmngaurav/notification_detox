package aura.notification.filter.ui.dashboard

import androidx.compose.animation.*
import aura.notification.filter.util.pulseClickable
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import aura.notification.filter.R
import androidx.compose.foundation.Image
import aura.notification.filter.util.shimmer
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
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
import aura.notification.filter.ui.components.AuraShimmer
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
    val isPro by viewModel.isPro.collectAsState()
    val limit = 6 // Consistent with AppPickerViewModel
    val activeRulesSize = activeRules.size

    // Sync Pulsing (1200ms EaseInOutSine)
    val infiniteTransition = rememberInfiniteTransition(label = "SyncPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
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
        containerColor = Color(0xFF050505),
        modifier = Modifier.statusBarsPadding(),
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
            // --- LIFECYCLE OBSERVER ---
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        viewModel.checkPermissions()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // --- PERMISSION BANNER ---
            val hasNotificationAccess by viewModel.hasNotificationAccess.collectAsState()
            if (!hasNotificationAccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFB00020)) // Error Red
                        .clickable {
                            val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            try {
                                navController.context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                                val intent2 = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                navController.context.startActivity(intent2)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Permission Revoked",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Aura needs access to filter notifications.",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

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
                        // --- Pulsating Green Dot ---
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E676).copy(alpha = pulseAlpha), CircleShape)
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (blockedCount > 0) "$blockedCount silenced" else "Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (blockedCount > 0) accentColor else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isPro) {
                        IconButton(onClick = { navController.navigate("paywall") }) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_premium_crown),
                                contentDescription = "Upgrade",
                                modifier = Modifier
                                    .size(24.dp),
                                colorFilter = ColorFilter.tint(Color(0xFFDAA520))
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                    }
                }
            }


            // --- TIMELINE FEED ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (timeline.isNotEmpty()) {
                    Text(
                        text = "Blocked messages",
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
                        itemsIndexed(timeline, key = { _, burst -> burst.id }) { index, burst ->
                            // Premium Staggered Entrance
                            val state = remember { MutableTransitionState(false) }.apply { targetState = true }
                            AnimatedVisibility(
                                visibleState = state,
                                enter = fadeIn(animationSpec = tween(600, delayMillis = index * 100)) + 
                                        expandVertically(animationSpec = tween(600, delayMillis = index * 100)),
                                exit = fadeOut()
                            ) {
                                BurstCard(burst = burst, viewModel = viewModel, accentColor = accentColor, isPro = isPro, navController = navController)
                            }
                        }
                    }
                } else {
                    // Unified Focal Point for Empty Feed
                    aura.notification.filter.ui.components.AuraLiveFocalPoint(
                        accentColor = accentColor,
                        monitoredAppsCount = activeRules.size
                    )
                }
            }

            // --- ACTIVE MONITORED APPS (FOOTER) ---
            if (activeRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF1E1E1E))) // Divider
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monitored Apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isPro) {
                             Text(
                                text = "$activeRulesSize/$limit used",
                                color = if (activeRulesSize >= limit) Color.Red else Color.Gray,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun BurstCard(
    burst: NotificationBurst,
    viewModel: MainViewModel,
    accentColor: Color,
    isPro: Boolean,
    navController: NavController
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
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
                         .clickable { viewModel.openApp(burst.packageName, context) } // Enhanced Hit Zone
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
            
            // --- ACTION BUTTONS ROW (separate from header) ---
            if (burst.size > 1) {
                AnimatedVisibility(
                    visible = isExpanded || isSummaryMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 52.dp, top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Summary / Close toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSummaryMode) accentColor.copy(alpha = 0.1f) 
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .pulseClickable { 
                                    viewModel.toggleSummaryForPackage(burst.packageName, burst.notifications) 
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSummaryMode) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Summary",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "SUMMARY",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // Clear button
                        if (!isSummaryMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { showClearDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "CLEAR",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(8.dp))
                        }
                        
                        // Open App button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .pulseClickable { viewModel.openApp(burst.packageName, context) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "OPEN",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
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
                        // AI Summary Header
                        Text(
                            text = if (isExpanded) "AI SUMMARY" else "${burst.notifications.size} MESSAGES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                        
                        if (isExpanded) {
                            Spacer(Modifier.height(16.dp))
                            // Styled Summary Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                                    .heightIn(max = 250.dp) // Prevent infinite height
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (summaryText == null || summaryText == "Thinking...") {
                                    Column {
                                        Text(
                                            "Analysing ${burst.size} notifications...",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        AuraShimmer(
                                            modifier = Modifier.fillMaxWidth().height(12.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        AuraShimmer(
                                            modifier = Modifier.fillMaxWidth(0.7f).height(12.dp)
                                        )
                                    }
                                } else {
                                    aura.notification.filter.ui.components.TypewriterText(
                                        text = summaryText ?: "",
                                        color = Color(0xFFEEEEEE),
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        delayMillis = 10
                                    )
                                }
                            }
                        } else {
                            // Collapsed State Preview: Show latest message + count indicator
                            Spacer(Modifier.height(8.dp))
                            val latestNote = burst.notifications.firstOrNull()
                            if (latestNote != null) {
                                Text(
                                    text = latestNote.content,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Raw List View
                        burst.notifications.forEach { note ->
                            key(note.id) { // Important for animations
                                val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.deleteNotification(note.id)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                androidx.compose.material3.SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.targetValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent,
                                            label = "DismissColor"
                                        )
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 6.dp)
                                                .background(color, RoundedCornerShape(8.dp))
                                                .padding(end = 16.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    content = {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .background(Color(0xFF151515)), // Match card bg
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = note.content, 
                                                        color = Color.White, 
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 14.sp, 
                                                        lineHeight = 20.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    
                                                    // Category Tag
                                                    if (note.category.isNotEmpty()) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Surface(
                                                            color = when {
                                                                note.category.contains("Security") || note.category.contains("Emergency") || note.category.contains("Finance") -> Color.Red.copy(alpha = 0.1f)
                                                                note.category.contains("Chats") || note.category.contains("Calls") || note.category.contains("Threads") -> Color.Cyan.copy(alpha = 0.1f)
                                                                note.category.contains("Work") || note.category.contains("Meetings") || note.category.contains("Documents") -> Color.Magenta.copy(alpha = 0.1f)
                                                                note.category.contains("Home") || note.category.contains("Health") || note.category.contains("Transport") -> Color.Green.copy(alpha = 0.1f)
                                                                else -> Color.Gray.copy(alpha = 0.1f)
                                                            },
                                                            shape = RoundedCornerShape(4.dp),
                                                            modifier = Modifier.padding(bottom = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = note.category.uppercase(),
                                                                color = when {
                                                                    note.category.contains("Security") || note.category.contains("Emergency") || note.category.contains("Finance") -> Color.Red
                                                                    note.category.contains("Chats") || note.category.contains("Calls") || note.category.contains("Threads") -> Color.Cyan
                                                                    note.category.contains("Work") || note.category.contains("Meetings") || note.category.contains("Documents") -> Color.Magenta
                                                                    note.category.contains("Home") || note.category.contains("Health") || note.category.contains("Transport") -> Color.Green
                                                                    else -> Color.LightGray
                                                                },
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
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
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = {
                Text(
                    "Clear Notifications",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently delete all blocked notifications for ${appInfo.label}. Do you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAppNotifications(burst.packageName)
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
