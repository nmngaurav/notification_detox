package aura.notification.filter.ui.picker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import aura.notification.filter.ui.components.LiquidCard
import aura.notification.filter.util.AppInfoManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    navController: NavController,
    viewModel: AppPickerViewModel = hiltViewModel(),
    initialSelectionMode: Boolean = false
) {
    LaunchedEffect(initialSelectionMode) {
        if (initialSelectionMode) viewModel.enterSelectionMode()
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    val suggestions by viewModel.suggestedApps.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    
    // --- Multi-Select State ---
    val isSelectionMode = viewModel.isSelectionMode.collectAsState().value
    val selectedPackages = viewModel.selectedPackages.collectAsState().value
    
    val isPro by viewModel.isPro.collectAsState()
    val activeAppCount by viewModel.activeAppCount.collectAsState()
    val totalUniqueApps by viewModel.totalUniqueApps.collectAsState()
    val limit = viewModel.MAX_FREE_APPS
    
    // Bug Fix: Check against Total Unique (Active + Selected)
    val isLimitReached = !isPro && totalUniqueApps >= limit

    val context = androidx.compose.ui.platform.LocalContext.current

    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            aura.notification.filter.ui.components.PulsingAuraLoader(color = Color(0xFFDAA520))
            Spacer(modifier = Modifier.height(100.dp))
            Text("Scanning Apps...", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 80.dp))
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        floatingActionButton = {
            if (isSelectionMode && selectedPackages.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { 
                        val pkgString = selectedPackages.joinToString(",")
                        navController.navigate("batch_config/${android.net.Uri.encode(pkgString)}") 
                    },
                    containerColor = Color(0xFFDAA520),
                    contentColor = Color.Black
                ) {
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        Text("Configure (${selectedPackages.size})", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // --- HEADER: Search + Command Island ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
                    .padding(bottom = 8.dp)
            ) {
                // Command Island (Replaces Standard Top Bar when Selecting)
                if (isSelectionMode) {
                    aura.notification.filter.ui.components.CommandIsland(
                        count = selectedPackages.size,
                        onClose = { viewModel.exitSelectionMode() },
                        onDone = { 
                            if (selectedPackages.isNotEmpty()) {
                                val pkgString = selectedPackages.joinToString(",")
                                navController.navigate("batch_config/${android.net.Uri.encode(pkgString)}") 
                            }
                        }
                    )
                } else {
                    // Standard Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                       Column {
                           Text(
                                "Select Apps", 
                                color = Color.White, 
                                style = MaterialTheme.typography.headlineMedium, 
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "to filter notifications", 
                                color = Color.Gray, 
                                style = MaterialTheme.typography.bodyMedium
                            )
                       }
                        
                        // Premium Batch Mode Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E1E))
                                .clickable { viewModel.enterSelectionMode() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Batch Mode", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search apps...", color = Color(0xFFCCCCCC)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFCCCCCC)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFDAA520)
                    )
                )

                // Suggestions Carousel
                // Only show if searching blank and "All" tab
                if (suggestions.isNotEmpty() && searchQuery.isBlank() && selectedTab == "All") {
                    // Check logic: If strictly limited and NOT selection mode, maybe hide?
                    // But if selection mode, we want to allow selecting them.
                     if (!isLimitReached || isSelectionMode) {
                        Text(
                            text = "Suggested for Detox",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(suggestions) { app ->
                                val isSelected = selectedPackages.contains(app.packageName)
                                SuggestionItem(app, isSelectionMode, isSelected) {
                                    if (isSelectionMode) {
                                        if (!isPro && selectedPackages.size >= limit && !isSelected) {
                                            navController.navigate("paywall")
                                        } else {
                                            viewModel.toggleSelection(app.packageName)
                                        }
                                    } else {
                                        if (isLimitReached) navController.navigate("paywall") else navController.navigate("app_config/${android.net.Uri.encode(app.packageName)}")
                                    }
                                }
                            }
                        }
                    }
                }

                // LIMIT BANNER
                if (!isPro) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLimitReached && !isSelectionMode) Color(0xFF3E2723) else Color(0xFF1E1E1E)) 
                            .border(1.dp, if (isLimitReached && !isSelectionMode) Color.Red.copy(alpha=0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLimitReached) Icons.Default.Lock else Icons.Default.Info, 
                                contentDescription = null,
                                tint = if (isLimitReached) Color.Red else Color(0xFFDAA520),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val count = if (isSelectionMode) totalUniqueApps else activeAppCount
                                Text(
                                    text = if (count >= limit) "Limit Reached ($count/$limit)" else "Free Plan: $count/$limit Active",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (count >= limit) "Upgrade to add more." else "Select up to ${limit - count} more.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (isLimitReached && !isSelectionMode) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "UPGRADE",
                                    color = Color(0xFFDAA520),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { navController.navigate("paywall") }
                                )
                            }
                        }
                    }
                }
            }

            // --- TABS ---
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = Color(0xFF0A0A0A),
                contentColor = Color(0xFFDAA520),
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { 
                            Text(
                                text = tab, 
                                fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                                color = if (tab == selectedTab) Color(0xFFDAA520) else Color.Gray
                            ) 
                        }
                    )
                }
            }

            // --- PREMIUM APP LIST ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp, start = 16.dp, end = 16.dp), 
                verticalArrangement = Arrangement.spacedBy(8.dp) // Compact spacing
            ) {
                // Bug Fix: Add key to maintain state across tab switches
                items(
                    count = apps.size,
                    key = { index -> apps[index].packageName },
                    contentType = { "app_item" }
                ) { index ->
                    val app = apps[index]
                    val isSelected = selectedPackages.contains(app.packageName)
                    
                    aura.notification.filter.ui.components.AliveCard(
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                // Bug Fix: Check if adding this specific app would exceed total limit
                                if (!isPro && !isSelected && totalUniqueApps >= limit) {
                                     navController.navigate("paywall")
                                } else {
                                    viewModel.toggleSelection(app.packageName)
                                }
                            } else {
                                if (isLimitReached) navController.navigate("paywall") else navController.navigate("app_config/${android.net.Uri.encode(app.packageName)}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(72.dp) // Compact height
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Icon
                             Box(
                                modifier = Modifier
                                    .size(40.dp) // Smaller icon
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx -> 
                                        android.widget.ImageView(ctx).apply { 
                                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER 
                                        } 
                                    },
                                    update = { view ->
                                        if (app.icon != null) view.setImageDrawable(app.icon) else view.setImageDrawable(null)
                                    }
                                )
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            // Text Info
                            Column {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) Color(0xFFDAA520) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp // Slightly smaller
                                )
                                Text(
                                    text = if(isSelected) "Selected" else "Tap to configure",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            
                            Spacer(Modifier.weight(1f))
                            
                            // Status Indicator
                            if (isSelectionMode) {
                                // Gold Check Circle
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if(isSelected) Color(0xFFDAA520) else Color.Transparent,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(2.dp, if(isSelected) Color(0xFFDAA520) else Color.Gray, CircleShape)
                                        .padding(4.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight, 
                                    contentDescription = "Configure",
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(app: AppInfoManager.AppInfo, isSelectionMode: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFFDAA520).copy(alpha = 0.2f) else Color(0xFF1E1E1E))
                .border(2.dp, if (isSelected) Color(0xFFDAA520) else Color.Transparent, CircleShape) // Added Border
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                 androidx.compose.ui.viewinterop.AndroidView(
                     modifier = Modifier.fillMaxSize(),
                     factory = { ctx -> 
                         android.widget.ImageView(ctx).apply { 
                             scaleType = android.widget.ImageView.ScaleType.FIT_CENTER 
                         } 
                     },
                     update = { view ->
                         view.setImageDrawable(app.icon)
                         // Removed color filter to keep icon original
                         if (isSelected) view.alpha = 0.5f else view.alpha = 1f
                     }
                 )
            }
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = Color(0xFFDAA520), modifier = Modifier.size(24.dp).align(Alignment.Center))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label.take(8),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFFDAA520) else Color.White,
            maxLines = 1
        )
    }
}

@Composable
fun AppListItem(app: AppInfoManager.AppInfo, isSelectionMode: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    LiquidCard(
        modifier = Modifier.clickable(onClick = onClick),
        backgroundColor = if (isSelected) Color(0xFFDAA520).copy(alpha = 0.1f) else Color(0xFF121212)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> 
                        android.widget.ImageView(ctx).apply { 
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER 
                        } 
                    },
                    update = { view ->
                        if (app.icon != null) {
                            view.setImageDrawable(app.icon)
                        } else {
                            view.setImageDrawable(null) 
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) Color(0xFFDAA520) else Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFDAA520),
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.Black
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight, 
                    contentDescription = "Configure",
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}
