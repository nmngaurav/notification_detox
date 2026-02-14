package aura.notification.filter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura.notification.filter.ai.HeuristicEngine
import aura.notification.filter.data.ShieldLevel

// -----------------------------------------------------------------------------
// 1. AURA CONFIG HERO
// -----------------------------------------------------------------------------
@Composable
fun AuraConfigHero(
    title: String = "Aura",
    subtitle: String = "Notification Blocker Active",
    icon: (@Composable () -> Unit)? = null,
    shieldCount: Int? = null,
    accentColor: Color = Color(0xFFDAA520)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT: ICON AREA ---
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            if (shieldCount != null) {
                ParallaxShieldHero(count = shieldCount)
            } else if (icon != null) {
                icon()
            }
        }

        Spacer(Modifier.width(20.dp))

        // --- RIGHT: TEXT AREA ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                maxLines = 1
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Status Badge
            Surface(
                color = accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = subtitle.uppercase(),
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. AURA PRECISION SECTION
// -----------------------------------------------------------------------------
@Composable
fun AuraPrecisionSection(
    selectedLevel: ShieldLevel,
    onLevelSelected: (ShieldLevel) -> Unit
) {
    val accentColor = when (selectedLevel) {
        ShieldLevel.OPEN -> Color.Gray
        ShieldLevel.SMART -> Color(0xFFDAA520)
        ShieldLevel.FORTRESS -> Color(0xFFFF4D4D)
        else -> Color(0xFFDAA520)
    }

    Column {
        AuraSectionHeader("Aura Precision")
        AuraSegmentedControl(
            options = listOf("OFF", "SMART", "STRICT"),
            selectedOption = when (selectedLevel) {
                ShieldLevel.OPEN -> "OFF"
                ShieldLevel.SMART -> "SMART"
                ShieldLevel.FORTRESS -> "STRICT"
                else -> "SMART"
            },
            onOptionSelected = { 
                val level = when (it) {
                    "OFF" -> ShieldLevel.OPEN
                    "SMART" -> ShieldLevel.SMART
                    "STRICT" -> ShieldLevel.FORTRESS
                    else -> ShieldLevel.SMART
                }
                onLevelSelected(level) 
            },
            accentColor = accentColor
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Helper Description with stabilized height
        Box(modifier = Modifier.heightIn(min = 48.dp)) {
            Text(
                text = when (selectedLevel) {
                    ShieldLevel.OPEN -> "Show all notifications. No Aura filtering applied."
                    ShieldLevel.SMART -> "Only notifications from your selected categories will be allowed through, silencing everything else."
                    ShieldLevel.FORTRESS -> "Total silence. All notifications are blocked by Aura."
                    else -> ""
                },
                color = if (selectedLevel == ShieldLevel.OPEN) Color.Gray else Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

// -----------------------------------------------------------------------------
// 3. AURA FILTER HIERARCHY
// -----------------------------------------------------------------------------
@Composable
fun AuraFilterHierarchy(
    categorizedTags: Map<String, List<HeuristicEngine.TagMetadata>>,
    selectedTags: Set<String>,
    expandedCategories: Map<String, Boolean>,
    onTagToggle: (String) -> Unit,
    onCategoryToggle: (String, List<String>, androidx.compose.ui.state.ToggleableState) -> Unit,
    onExpandToggle: (String) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Column {
        categorizedTags.forEach { (category, tags) ->
            val isExpanded = expandedCategories[category] ?: false
            
            // Logic for 3-state checkbox
            val selectedInCategory = tags.count { selectedTags.contains(it.id) }
            val selectionState = when {
                selectedInCategory == 0 -> androidx.compose.ui.state.ToggleableState.Off
                selectedInCategory == tags.size -> androidx.compose.ui.state.ToggleableState.On
                else -> androidx.compose.ui.state.ToggleableState.Indeterminate
            }

            BlockerCategoryCrystalHeader(
                title = category,
                icon = when(category) {
                    "Safety & Finance" -> Icons.Default.Lock
                    "Personal" -> Icons.Default.Email
                    "Work" -> Icons.Default.DateRange
                    "Home & Activity" -> Icons.Default.Home
                    "Discover" -> Icons.Default.Search
                    else -> Icons.Default.Info
                },
                isSelectedCount = selectedInCategory,
                totalCount = tags.size,
                isExpanded = isExpanded,
                selectionState = selectionState,
                onToggle = {
                    onCategoryToggle(category, tags.map { it.id }, selectionState)
                },
                onExpandToggle = {
                    onExpandToggle(category)
                }
            )

            if (isExpanded) {
                tags.forEach { metadata ->
                    val isSelected = selectedTags.contains(metadata.id)
                    BlockerHierarchyRow(
                        label = metadata.label,
                        description = metadata.description,
                        isSelected = isSelected,
                        onToggle = {
                            onTagToggle(metadata.id)
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. AURA CUSTOM RULES SECTION
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraCustomRulesSection(
    keywords: String,
    onKeywordsChange: (String) -> Unit
) {
    Column {
        AuraSectionHeader("Premium Custom Filters (White-list)")
        Text(
            text = "Notifications with these keywords will always break through the blocker.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        OutlinedTextField(
            value = keywords,
            onValueChange = onKeywordsChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("e.g. Urgent, OTP, Boss", color = Color.DarkGray) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFDAA520),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// 5. AURA CONFIG FOOTER
// -----------------------------------------------------------------------------
@Composable
fun AuraConfigFooter(
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onReset: () -> Unit,
    isLoading: Boolean = false,
    accentColor: Color = Color(0xFFDAA520)
) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Secondary Reset Action
            TextButton(
                onClick = onReset,
                modifier = Modifier.weight(0.4f).height(56.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("RESET")
            }
            
            // Primary Action
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .weight(0.6f)
                    .height(56.dp)
                    .borderBeam(width = 2.dp, shape = RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text(primaryActionLabel, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}
