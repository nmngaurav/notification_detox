package com.aura.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.data.ShieldLevel
import com.aura.data.DetoxCategory
import com.aura.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppConfigSheet(
    appName: String,
    packageName: String,
    icon: android.graphics.drawable.Drawable? = null,
    currentShieldLevel: com.aura.data.ShieldLevel,
    initialCategories: String = "",
    keywords: String,
    onSave: (com.aura.data.ShieldLevel, String, String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLevel by remember(currentShieldLevel) { mutableStateOf(currentShieldLevel) }
    var currentKeywords by remember(keywords) { mutableStateOf(keywords) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 1. Tag Library Definition (Section -> Tags)
    val allTagSections = remember {
        listOf(
            TagSection("Critical & Security", Icons.Default.Lock, listOf("OTPs", "Login Codes", "Fraud Alerts", "Calls", "Alarms")),
            TagSection("Social & Chat", Icons.Default.Email, listOf("DMs", "Group Chats", "Mentions", "Replies", "Voice Msgs")),
            TagSection("Logistics & Time", Icons.Default.ShoppingCart, listOf("Rides", "Delivery", "Reminders", "Calendar", "Traffic")),
            TagSection("Money", Icons.Default.Info, listOf("Transaction", "Bill Due", "Salary", "Offers")),
            TagSection("System", Icons.Default.Settings, listOf("Updates", "Downloads", "Reviews", "System"))
        )
    }

    // 2. Detect App Type to Filter Relevant Tags
    val appType = remember(packageName) {
        val pm = context.packageManager
        var detectedType = "UNKNOWN"
        
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                detectedType = when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
                    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "MAPS"
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "GAMING"
                    else -> "UNKNOWN"
                }
            }
        } catch (e: Exception) { /* Ignore */ }
        
        // Fallback: Package name heuristics
        if (detectedType == "UNKNOWN") {
            val pkg = packageName.lowercase()
            detectedType = when {
                pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("messenger") || 
                pkg.contains("instagram") || pkg.contains("snapchat") || pkg.contains("twitter") || 
                pkg.contains("discord") || pkg.contains("facebook") -> "SOCIAL"
                pkg.contains("bank") || pkg.contains("pay") || pkg.contains("venmo") || 
                pkg.contains("cashapp") || pkg.contains("paypal") || pkg.contains("chase") || 
                pkg.contains("wells") || pkg.contains("citi") -> "FINANCE"
                pkg.contains("uber") || pkg.contains("lyft") || pkg.contains("doordash") || 
                pkg.contains("grubhub") || pkg.contains("instacart") || pkg.contains("fedex") || 
                pkg.contains("ups") || pkg.contains("dhl") || pkg.contains("delivery") || 
                pkg.contains("food") || pkg.contains("zomato") || pkg.contains("swiggy") -> "LOGISTICS"
                pkg.contains("netflix") || pkg.contains("prime") || pkg.contains("hulu") || 
                pkg.contains("disney") || pkg.contains("youtube") || pkg.contains("twitch") || 
                pkg.contains("video") -> "VIDEO"
                pkg.contains("spotify") || pkg.contains("pandora") || pkg.contains("soundcloud") || 
                pkg.contains("music") || pkg.contains("audio") -> "AUDIO"
                pkg.contains("calendar") || pkg.contains("gmail") || pkg.contains("outlook") || 
                pkg.contains("slack") || pkg.contains("teams") || pkg.contains("zoom") -> "PRODUCTIVITY"
                pkg.contains("maps") || pkg.contains("waze") -> "MAPS"
                pkg.contains("game") || pkg.contains("gaming") || pkg.contains("clash") || 
                pkg.contains("candy") || pkg.contains("duolingo") -> "GAMING"
                else -> "UNKNOWN"
            }
        }
        detectedType
    }
    
    // 3. Filter Tag Sections Based on App Type
    val tagSections = remember(appType) {
        val relevantSectionTitles = when (appType) {
            "SOCIAL" -> setOf("Critical & Security", "Social & Chat", "System")
            "FINANCE" -> setOf("Critical & Security", "Money", "System")
            "LOGISTICS" -> setOf("Critical & Security", "Logistics & Time", "System")
            "VIDEO", "AUDIO" -> setOf("Critical & Security", "System")
            "PRODUCTIVITY" -> setOf("Critical & Security", "Logistics & Time", "System")
            "MAPS" -> setOf("Critical & Security", "Logistics & Time", "System")
            "GAMING" -> setOf("Critical & Security", "System")
            else -> setOf("Critical & Security", "Social & Chat", "Logistics & Time", "Money", "System") // Show all for unknown
        }
        allTagSections.filter { it.title in relevantSectionTitles }
    }

    // 4. Smart Auto-Detection (API 26+)
    val autoSelectedTags = remember(packageName) {
        val pm = context.packageManager
        val tags = mutableSetOf<String>()
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> { tags.add("DMs"); tags.add("Mentions"); tags.add("Replies") }
                    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> { tags.add("Rides"); tags.add("Traffic"); tags.add("Delivery") }
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> { tags.add("Reminders"); tags.add("Calendar") }
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO, android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> tags.add("System")
                }
            }
        } catch (e: Exception) { /* Ignore */ }
        
        // Fallback Heuristics
        val pkg = packageName.lowercase()
        if (pkg.contains("bank") || pkg.contains("pay")) { tags.add("Transaction"); tags.add("OTPs") }
        if (pkg.contains("uber") || pkg.contains("lyft") || pkg.contains("food")) { tags.add("Rides"); tags.add("Delivery") }
        if (pkg.contains("whatsapp") || pkg.contains("telegram")) { tags.add("DMs"); tags.add("Group Chats"); tags.add("Calls") }
        
        tags
    }

    // 3. Selection State
    var selectedTags by remember { 
        mutableStateOf(
            if (initialCategories.isNotEmpty()) {
                initialCategories.split(",").filter { it.isNotEmpty() }.toSet()
            } else {
                autoSelectedTags 
            }
        )
    }
    
    // Validation Logic
    val isSmartMode = selectedLevel == ShieldLevel.SMART
    val hasRules = selectedTags.isNotEmpty() || currentKeywords.isNotEmpty()
    val canSave = !isSmartMode || hasRules 

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111111)
    ) {
    
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            
            // --- HEADER (Fixed) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    AndroidView(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                        factory = { ctx -> ImageView(ctx).apply { setImageDrawable(icon); scaleType = ImageView.ScaleType.FIT_CENTER } }
                    )
                } else {
                    Box(Modifier.size(56.dp).background(Color(0xFF222222), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(appName.take(1), color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = appName, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "Notification Detox", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDAA520))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Unified Mode Selector
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notification Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Option 1: Allow All
                    ModeOption(
                        title = "Allow All",
                        description = "Let everything through",
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color.Green,
                        selected = selectedLevel == ShieldLevel.OPEN,
                        onClick = { selectedLevel = ShieldLevel.OPEN }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 2: Smart Filter
                    ModeOption(
                        title = "Smart Filter",
                        description = "Only allow what you choose below",
                        icon = Icons.Default.Done,
                        iconTint = Color(0xFFDAA520),
                        selected = selectedLevel == ShieldLevel.SMART,
                        onClick = { selectedLevel = ShieldLevel.SMART }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 3: Block All
                    ModeOption(
                        title = "Block All",
                        description = "Complete silence",
                        icon = Icons.Default.Lock,
                        iconTint = Color.Red,
                        selected = selectedLevel == ShieldLevel.FORTRESS,
                        onClick = { selectedLevel = ShieldLevel.FORTRESS }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- SCROLLABLE CONTENT ---
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                    if (selectedLevel == ShieldLevel.SMART) {
                        item {
                            Text("WHAT TO ALLOW", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            Text(
                                text = "Tap what you want to see. Everything else gets blocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                lineHeight = 16.sp
                            )
                        }

                        item {
                            // UNIFIED EXCEPTIONS CARD
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
                                            // Section Icon (Left)
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp) // Align with first chip row
                                                    .size(28.dp)
                                                    .background(Color(0xFF222222), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    section.icon, 
                                                    contentDescription = null, 
                                                    tint = Color(0xFFDAA520), 
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Chips (Right)
                                            Column {
                                                Text(
                                                    text = section.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                
                                                SimpleFlowRow(verticalGap = 6.dp, horizontalGap = 6.dp) {
                                                    section.tags.forEach { tag ->
                                                        val isSelected = selectedTags.contains(tag)
                                                        SmartFilterChip(
                                                            label = tag,
                                                            selected = isSelected,
                                                            onClick = {
                                                                selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
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

                        item { // Start of new item block
                            // Custom Rules (Premium Redesign)
                            Spacer(Modifier.height(16.dp))
                            
                            // Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                               Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFDAA520), modifier = Modifier.size(18.dp))
                               Spacer(Modifier.width(10.dp))
                               Text("CUSTOM KEYWORDS (OPTIONAL)", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // Premium Keyword Manager Card
                            GlassCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Allow notifications with specific words:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    // Active Keywords Chip Cloud
                                    val keywordsList = currentKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    
                                    if (keywordsList.isNotEmpty()) {
                                        SimpleFlowRow(horizontalGap = 8.dp, verticalGap = 8.dp) {
                                            keywordsList.forEach { keyword ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFDAA520).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color(0xFFDAA520).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                        .clickable { 
                                                            // Remove keyword on click
                                                            val newList = keywordsList - keyword
                                                            currentKeywords = newList.joinToString(",")
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(keyword, color = Color(0xFFDAA520), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.width(6.dp))
                                                        Icon(Icons.Default.Close, null, tint = Color(0xFFDAA520).copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }
                                    
                                    // "Add Keyword" Button
                                    var showAddDialog by remember { mutableStateOf(false) }
                                    
                                    Button(
                                        onClick = { showAddDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222), contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color(0xFFDAA520))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Keyword Rule")
                                    }
                                    
                                    if (showAddDialog) {
                                        KeywordDialog(
                                            onDismiss = { showAddDialog = false },
                                            onAdd = { newWord ->
                                                // Robust addition: Split, Filter, Join to prevent ",,"
                                                val currentList = currentKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                                if (!currentList.contains(newWord)) {
                                                    currentKeywords = (currentList + newWord).joinToString(",")
                                                }
                                                showAddDialog = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // AI Safety Benefit
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDAA520), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "AI Protection Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Critical alerts (OTPs, fraud warnings) will still get through",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
            
            // --- FOOTER (Fixed) ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 56.dp) // Increased padding to avoid gesture navigation interference
            ) {
                Button(
                    onClick = { onRemove() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222), contentColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }
                Spacer(modifier = Modifier.width(16.dp))
                
                // SAVE BUTTON With VALIDATION
                Button(
                    onClick = { 
                        if (canSave) {
                           onSave(selectedLevel, selectedTags.joinToString(","), currentKeywords) 
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(canSave) Color(0xFFDAA520) else Color(0xFF222222), 
                        contentColor = if(canSave) Color.Black else Color.Gray
                    ),
                    modifier = Modifier.weight(2f),
                    enabled = canSave
                ) { 
                    Text(
                        if (canSave) "Save Config" else "Select a Rule",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            }
        }
    }
}

data class TagSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tags: List<String>)

@Composable
fun ModeOption(
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
fun SmartFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f)
    val glowAlpha by animateFloatAsState(if (selected) 0.3f else 0f)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .scale(scale)
            .border(1.dp, if(selected) Color(0xFFDAA520).copy(alpha = 0.5f) else Color.Transparent, CircleShape)
            // Glow effect simulation via shadow/background (simplified for perf)
            .background(if(selected) Color(0xFFDAA520).copy(alpha = 0.2f) else Color(0xFF222222), CircleShape)
            .clickable { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) // Strong tick
                onClick() 
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Text("+ ", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(label, color = if(selected) Color.White else Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    verticalGap: androidx.compose.ui.unit.Dp = 0.dp,
    horizontalGap: androidx.compose.ui.unit.Dp = 0.dp,
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
                // New row
                rowWidths.add(currentRowWidth - hGapPx) // remove trailing gap
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
        
        // Add last row
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
                val widthWithGap = placeable.width + hGapPx
                
                if (xOffset + placeable.width > constraints.maxWidth) {
                    xOffset = 0
                    yOffset += rowMaxHeight + vGapPx
                    rowIndex++
                    rowMaxHeight = rowHeights.getOrElse(rowIndex) { 0 }
                }
                
                placeable.placeRelative(x = xOffset, y = yOffset)
                xOffset += widthWithGap
            }
        }
    }
}
