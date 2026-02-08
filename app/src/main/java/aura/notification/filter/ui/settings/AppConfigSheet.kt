package aura.notification.filter.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import android.widget.ImageView
import aura.notification.filter.ai.HeuristicEngine
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigSheet(
    appName: String,
    packageName: String,
    icon: android.graphics.drawable.Drawable? = null,
    isPro: Boolean,
    currentShieldLevel: aura.notification.filter.data.ShieldLevel,
    initialCategories: String = "",
    keywords: String,
    onSave: (aura.notification.filter.data.ShieldLevel, String, String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
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

    val isSmartMode = selectedLevel == ShieldLevel.SMART
    val hasRules = selectedTags.isNotEmpty() || currentKeywords.isNotEmpty()
    val canSave = !isSmartMode || hasRules

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 56.dp)
        ) {
             // --- HEADER ---
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    if (icon != null) {
                        AndroidView(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                            factory = { ctx -> ImageView(ctx).apply { setImageDrawable(icon); scaleType = ImageView.ScaleType.FIT_CENTER } }
                        )
                    } else {
                        Box(Modifier.size(48.dp).background(Color(0xFF222222), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(appName.take(1), color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = appName, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "App Configuration", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // --- MODE SELECTOR (Redesigned) ---
            item {
                val options = listOf("Allow All", "Smart Filter", "Block All")
                val currentOption = when(selectedLevel) {
                    ShieldLevel.OPEN -> "Allow All"
                    ShieldLevel.SMART -> "Smart Filter"
                    ShieldLevel.FORTRESS -> "Block All"
                    else -> "Smart Filter"
                }

                aura.notification.filter.ui.components.AuraSegmentedControl(
                    options = options,
                    selectedOption = currentOption,
                    onOptionSelected = { option ->
                        selectedLevel = when(option) {
                            "Allow All" -> ShieldLevel.OPEN
                            "Block All" -> ShieldLevel.FORTRESS
                            else -> ShieldLevel.SMART
                        }
                    }
                )
            }

            // --- SMART FILTER CONTENT ---
            if (selectedLevel == ShieldLevel.SMART) {
                categorizedTags.forEach { (category, tags) ->
                    item {
                        aura.notification.filter.ui.components.SectionHeader(category)
                    }
                    
                    items(tags) { metadata ->
                        val isSelected = selectedTags.contains(metadata.id)
                        val tagIcon = when(metadata.id) {
                            HeuristicEngine.TAG_SECURITY -> Icons.Default.Lock
                            HeuristicEngine.TAG_MONEY -> Icons.Default.Star 
                            HeuristicEngine.TAG_UPDATES -> Icons.Default.Settings
                            HeuristicEngine.TAG_MESSAGES -> Icons.Default.Email
                            HeuristicEngine.TAG_MENTIONS -> Icons.Default.AccountCircle
                            HeuristicEngine.TAG_CALLS -> Icons.Default.Call
                            HeuristicEngine.TAG_ORDERS -> Icons.Default.ShoppingCart
                            HeuristicEngine.TAG_SCHEDULE -> Icons.Default.Info
                            HeuristicEngine.TAG_PROMOS -> Icons.Default.Star
                            HeuristicEngine.TAG_NEWS -> Icons.Default.Info
                            else -> Icons.Default.Info
                        }

                        aura.notification.filter.ui.components.TagTile(
                            label = metadata.label,
                            description = metadata.description,
                            icon = tagIcon,
                            isSelected = isSelected,
                            onClick = {
                                selectedTags = if (isSelected) selectedTags - metadata.id else selectedTags + metadata.id
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // --- KEYWORDS SECTION ---
            item {
                aura.notification.filter.ui.components.SectionHeader("Custom Keywords")
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isPro) Color(0xFF151515) else Color(0xFF1A1A1A).copy(alpha = 0.5f)
                ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         if (isPro) {
                             val keywordsList = currentKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                             if (keywordsList.isNotEmpty()) {
                                 SimpleFlowRow(horizontalGap = 8.dp, verticalGap = 8.dp) {
                                     keywordsList.forEach { k ->
                                         Surface(
                                             shape = RoundedCornerShape(8.dp),
                                             color = Color(0xFFDAA520).copy(alpha = 0.1f),
                                             border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDAA520).copy(alpha = 0.3f)),
                                             modifier = Modifier.clickable { 
                                                 currentKeywords = (keywordsList - k).joinToString(",")
                                             }
                                         ) {
                                             Row(
                                                 modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(k, color = Color(0xFFDAA520), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                 Spacer(Modifier.width(6.dp))
                                                 Icon(Icons.Default.Close, null, tint = Color(0xFFDAA520).copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                             }
                                         }
                                     }
                                 }
                                 Spacer(Modifier.height(16.dp))
                             }
                             
                             var showAddDialog by remember { mutableStateOf(false) }
                             OutlinedButton(
                                 onClick = { showAddDialog = true },
                                 modifier = Modifier.fillMaxWidth(),
                                 shape = RoundedCornerShape(12.dp),
                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDAA520)),
                                 border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDAA520).copy(alpha = 0.4f))
                             ) {
                                 Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                 Spacer(Modifier.width(8.dp))
                                 Text("Add Custom Keyword")
                             }
                             
                             if (showAddDialog) {
                                 aura.notification.filter.ui.settings.KeywordDialog(onDismiss = { showAddDialog = false }) { newWord ->
                                     if (newWord.isNotBlank()) {
                                         val current = currentKeywords.split(",").filter { it.isNotBlank() }
                                         currentKeywords = (current + newWord.trim()).joinToString(",")
                                     }
                                     showAddDialog = false
                                 }
                             }
                         } else {
                             // Locked State for Free Users
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                 Spacer(Modifier.width(8.dp))
                                 Text("Pro Feature", color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                             }
                             Spacer(Modifier.height(4.dp))
                             Text("Upgrade to add unlimited custom keywords for ultra-precise filtering.", color = Color.Gray.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                         }
                     }
                }
            }

            // --- FOOTER ACTIONS ---
            item {
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) { 
                        Text("Reset Factory") 
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { if (canSave) onSave(selectedLevel, selectedTags.joinToString(","), currentKeywords) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDAA520),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF2A2A2A)
                        ),
                        modifier = Modifier.weight(1.5f).height(50.dp).clip(RoundedCornerShape(12.dp)),
                        enabled = canSave,
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Text("Deploy Settings", fontWeight = FontWeight.ExtraBold) 
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    verticalGap: androidx.compose.ui.unit.Dp = 0.dp,
    horizontalGap: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val hGap = horizontalGap.roundToPx()
        val vGap = verticalGap.roundToPx()
        var currentRowWidth = 0
        var nextRowWidth = 0
        var totalHeight = 0
        val rowHeights = mutableListOf<Int>()
        val rowWidths = mutableListOf<Int>()
        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rowWidths.add(currentRowWidth - hGap)
                rowHeights.add(nextRowWidth)
                totalHeight += nextRowWidth + vGap
                currentRowWidth = placeable.width + hGap
                nextRowWidth = placeable.height
            } else {
                currentRowWidth += placeable.width + hGap
                nextRowWidth = maxOf(nextRowWidth, placeable.height)
            }
            placeable
        }
        rowWidths.add(currentRowWidth - hGap)
        rowHeights.add(nextRowWidth)
        totalHeight += nextRowWidth
        
        layout(constraints.maxWidth, totalHeight) {
            var x = 0
            var y = 0
            var rowIndex = 0
            var rowMaxHeight = rowHeights.getOrElse(0) { 0 }
            placeables.forEach { placeable ->
                if (x + placeable.width > constraints.maxWidth) {
                    x = 0
                    y += rowMaxHeight + vGap
                    rowIndex++
                    rowMaxHeight = rowHeights.getOrElse(rowIndex) { 0 }
                }
                placeable.placeRelative(x, y)
                x += placeable.width + hGap
            }
        }
    }
}
