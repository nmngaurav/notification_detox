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
    val tagSections = remember {
        listOf(
            TagSection("Critical & Security", Icons.Default.Lock, listOf("OTPs", "Login Codes", "Fraud Alerts", "Calls", "Alarms")),
            TagSection("Social & Chat", Icons.Default.Email, listOf("DMs", "Group Chats", "Mentions", "Replies", "Voice Msgs")),
            TagSection("Logistics & Time", Icons.Default.ShoppingCart, listOf("Rides", "Delivery", "Reminders", "Calendar", "Traffic")),
            TagSection("Money", Icons.Default.Info, listOf("Transaction", "Bill Due", "Salary", "Offers")),
            TagSection("System", Icons.Default.Settings, listOf("Updates", "Downloads", "Reviews", "System"))
        )
    }

    // 2. Smart Auto-Detection (API 26+)
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
            
            // Master Toggle
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // Toggle Logic: OPEN <-> SMART (Default)
                        // If currently FORTRESS, clicking toggles to OPEN.
                        if (selectedLevel == ShieldLevel.OPEN) {
                             selectedLevel = ShieldLevel.SMART 
                        } else {
                             selectedLevel = ShieldLevel.OPEN
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Blocking Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedLevel == ShieldLevel.OPEN) "Off: Allowing all notifications" else "Active: Blocking everything by default",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedLevel == ShieldLevel.OPEN) Color.Gray else Color(0xFFDAA520)
                        )
                    }
                    Switch(
                        checked = selectedLevel != ShieldLevel.OPEN,
                        onCheckedChange = { isChecked -> 
                            selectedLevel = if (isChecked) ShieldLevel.SMART else ShieldLevel.OPEN 
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFDAA520), checkedTrackColor = Color(0xFF332200),
                            uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF111111)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- SCROLLABLE CONTENT ---
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 if (selectedLevel != ShieldLevel.OPEN) {
                    // BLOCK ALL TOGGLE
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedLevel = if (selectedLevel == ShieldLevel.FORTRESS) ShieldLevel.SMART else ShieldLevel.FORTRESS
                            }
                        ) {
                             Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                 Icon(
                                     imageVector = if(selectedLevel == ShieldLevel.FORTRESS) Icons.Default.Lock else Icons.Default.Lock, // Could use Lock/Unlock
                                     contentDescription = null,
                                     tint = if(selectedLevel == ShieldLevel.FORTRESS) Color.Red else Color.Gray
                                 )
                                 Spacer(modifier = Modifier.width(16.dp))
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text("Block Everything", color = Color.White, fontWeight = FontWeight.SemiBold)
                                     Text("Silence this app completely. No exceptions.", color = Color.Gray, fontSize = 12.sp)
                                 }
                                 Checkbox(
                                     checked = selectedLevel == ShieldLevel.FORTRESS,
                                     onCheckedChange = { isChecked ->
                                         selectedLevel = if (isChecked) ShieldLevel.FORTRESS else ShieldLevel.SMART
                                     },
                                     colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color.Gray)
                                 )
                             }
                        }
                    }
                 
                    if (selectedLevel == ShieldLevel.SMART) {
                        item {
                            Text("ALLOWANCE RULES", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            Text(
                                text = "Select the specific types of alerts you want to let through. All others will be silenced.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
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
                               Text("CUSTOM KEYWORDS", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            // Premium Keyword Manager Card
                            GlassCard {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Notifications containing these words will always be allowed:",
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
                                            text = "AI Safety Net Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Aura will intelligently rescue any critical alerts that slip through your filters.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedLevel == ShieldLevel.FORTRESS) {
                        // FORTRESS MODE EXPLANATION or EMPTY SPACE
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Strict Blocking Enabled",
                                    color = Color.Red.copy(alpha=0.7f),
                                    style = MaterialTheme.typography.titleMedium
                                )
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
