package aura.notification.filter.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.navigation.NavController
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
    profileId: String
) {
    val haptic = LocalHapticFeedback.current
    val installedApps by viewModel.installedApps.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { 
            it.label.contains(searchQuery, ignoreCase = true) || 
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Config Sheet State
    var selectedForConfig by remember { mutableStateOf<aura.notification.filter.util.AppInfoManager.AppInfo?>(null) }
    
    // Bulk Selection State
    var isBulkSelectMode by remember { mutableStateOf(false) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var showBulkConfigSheet by remember { mutableStateOf(false) }
    
    // Premium gate for bulk actions (set to true to lock behind premium in future)
    val isBulkActionPremiumLocked = false // Change to !isPro when ready to monetize
    
    val accentColor = Color(0xFFDAA520)

    // Custom Rule Config Sheet
    if (selectedForConfig != null) {
        val app = selectedForConfig!!
        
        // Use default SMART level for new apps
        aura.notification.filter.ui.settings.AppConfigSheet(
            appName = app.label,
            packageName = app.packageName,
            icon = app.icon,
            currentShieldLevel = ShieldLevel.SMART,
            initialCategories = "",
            keywords = "", 
            onSave = { level, categories, keywords ->
                viewModel.updateSmartRule(app.packageName, profileId, categories, keywords)
                selectedForConfig = null
                navController.popBackStack()
            },
            onRemove = {
                viewModel.deleteRule(app.packageName, profileId)
                selectedForConfig = null
            },
            onDismiss = { selectedForConfig = null }
        )
    }
    
    // Bulk Config Sheet
    if (showBulkConfigSheet && selectedPackages.isNotEmpty()) {
        BulkConfigSheetForSelection(
            selectedPackages = selectedPackages.toList(),
            viewModel = viewModel,
            profileId = profileId,
            isPremiumLocked = isBulkActionPremiumLocked,
            onApply = { shieldLevel, categories, keywords ->
                viewModel.applyBulkConfig(
                    packageNames = selectedPackages.toList(),
                    profileId = profileId,
                    shieldLevel = shieldLevel,
                    categories = categories,
                    keywords = keywords
                )
                showBulkConfigSheet = false
                isBulkSelectMode = false
                selectedPackages = emptySet()
                navController.popBackStack()
            },
            onDismiss = {
                showBulkConfigSheet = false
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0A0A0A))) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = if (isBulkSelectMode) "Select Apps (${selectedPackages.size})" else "Add App Filter",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isBulkSelectMode) accentColor else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Bulk Edit Toggle / Exit Button
                    if (filteredApps.size >= 2) {
                        if (isBulkSelectMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Select All Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222222))
                                        .clickable {
                                            selectedPackages = if (selectedPackages.size == filteredApps.size) {
                                                emptySet()
                                            } else {
                                                filteredApps.map { it.packageName }.toSet()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (selectedPackages.size == filteredApps.size) "Deselect All" else "Select All",
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search 100+ apps...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (isBulkSelectMode && selectedPackages.isNotEmpty()) 100.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                val isSelected = selectedPackages.contains(app.packageName)
                
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .pointerInput(isBulkSelectMode) {
                            detectTapGestures(
                                onLongPress = {
                                    if (!isBulkSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isBulkSelectMode = true
                                        selectedPackages = setOf(app.packageName)
                                    }
                                },
                                onTap = {
                                    if (isBulkSelectMode) {
                                        // Toggle selection
                                        selectedPackages = if (isSelected) {
                                            selectedPackages - app.packageName
                                        } else {
                                            selectedPackages + app.packageName
                                        }
                                    } else {
                                        // Normal click -> Open config
                                        selectedForConfig = app
                                    }
                                }
                            )
                        },
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .alpha(if (isBulkSelectMode && !isSelected) 0.5f else 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selection Checkbox (when in bulk mode)
                        if (isBulkSelectMode) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) accentColor else Color(0xFF333333)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) accentColor else Color.Gray,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        if (app.icon != null) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { context ->
                                    android.widget.ImageView(context).apply {
                                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                update = { view ->
                                    view.setImageDrawable(app.icon)
                                },
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF333333), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    app.label.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) accentColor else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Bold
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Add Button Visual (hide in bulk mode)
                        if (!isBulkSelectMode) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF222222), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                 Icon(
                                     imageVector = Icons.Default.Add,
                                     contentDescription = "Add",
                                     tint = accentColor,
                                     modifier = Modifier.size(16.dp)
                                 )
                            }
                        }
                    }
                }
            }
            }
            
            // --- BULK ACTION BAR (Floating at bottom when in bulk mode) ---
            AnimatedVisibility(
                visible = isBulkSelectMode && selectedPackages.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0A0A0A))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (isBulkActionPremiumLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Premium",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Configure ${selectedPackages.size} Apps",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// BULK CONFIG SHEET FOR APP SELECTION - Apply settings to multiple apps at once
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkConfigSheetForSelection(
    selectedPackages: List<String>,
    viewModel: SettingsViewModel,
    profileId: String,
    isPremiumLocked: Boolean,
    onApply: (ShieldLevel, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = Color(0xFFDAA520)
    
    var selectedLevel by remember { mutableStateOf(ShieldLevel.SMART) }
    var selectedTags by remember { mutableStateOf(setOf("OTPs", "Login Codes", "Fraud Alerts", "Calls")) }
    var currentKeywords by remember { mutableStateOf("") }
    
    // Tag sections for bulk config (simplified universal set)
    val tagSections = remember {
        listOf(
            BulkTagSectionForSelection("Critical & Security", Icons.Default.Lock, listOf("OTPs", "Login Codes", "Fraud Alerts", "Calls", "Alarms")),
            BulkTagSectionForSelection("Social & Chat", Icons.Default.Email, listOf("DMs", "Group Chats", "Mentions", "Replies", "Voice Msgs")),
            BulkTagSectionForSelection("Logistics & Time", Icons.Default.ShoppingCart, listOf("Rides", "Delivery", "Reminders", "Calendar", "Traffic")),
            BulkTagSectionForSelection("Money", Icons.Default.Info, listOf("Transaction", "Bill Due", "Salary", "Offers")),
            BulkTagSectionForSelection("System", Icons.Default.Settings, listOf("Updates", "Downloads", "Reviews", "System"))
        )
    }
    
    val isSmartMode = selectedLevel == ShieldLevel.SMART
    val hasRules = selectedTags.isNotEmpty() || currentKeywords.isNotEmpty()
    val canSave = !isSmartMode || hasRules

    ModalBottomSheet(
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
            LazyRow(
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
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { context ->
                                    android.widget.ImageView(context).apply {
                                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                update = { view ->
                                    view.setImageDrawable(info.icon)
                                },
                                modifier = Modifier.size(28.dp)
                            )
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Apply Mode to All",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Option 1: Allow All
                    BulkModeOptionForSelection(
                        title = "Allow All",
                        description = "Let everything through for all apps",
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color.Green,
                        selected = selectedLevel == ShieldLevel.OPEN,
                        onClick = { selectedLevel = ShieldLevel.OPEN }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 2: Smart Filter
                    BulkModeOptionForSelection(
                        title = "Smart Filter",
                        description = "Only allow selected categories",
                        icon = Icons.Default.Done,
                        iconTint = accentColor,
                        selected = selectedLevel == ShieldLevel.SMART,
                        onClick = { selectedLevel = ShieldLevel.SMART }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 3: Block All
                    BulkModeOptionForSelection(
                        title = "Block All",
                        description = "Complete silence for all apps",
                        icon = Icons.Default.Lock,
                        iconTint = Color.Red,
                        selected = selectedLevel == ShieldLevel.FORTRESS,
                        onClick = { selectedLevel = ShieldLevel.FORTRESS }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- SMART FILTER OPTIONS (Scrollable) ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedLevel == ShieldLevel.SMART) {
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
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                            
                                            BulkFlowRowForSelection(horizontalGap = 6.dp, verticalGap = 6.dp) {
                                                section.tags.forEach { tag ->
                                                    val isSelected = selectedTags.contains(tag)
                                                    BulkFilterChipForSelection(
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

data class BulkTagSectionForSelection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tags: List<String>
)

@Composable
fun BulkModeOptionForSelection(
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
fun BulkFilterChipForSelection(
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
fun BulkFlowRowForSelection(
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
