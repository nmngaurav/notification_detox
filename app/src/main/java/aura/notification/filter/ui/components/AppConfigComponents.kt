package aura.notification.filter.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush

@Composable
fun BlockerCategoryCrystalHeader(
    title: String,
    icon: ImageVector,
    isSelectedCount: Int,
    totalCount: Int,
    isExpanded: Boolean,
    selectionState: ToggleableState,
    onToggle: () -> Unit,
    onExpandToggle: () -> Unit
) {
    val accentColor = Color(0xFFDAA520)
    val isAnySelected = selectionState != ToggleableState.Off
    
    val iconGlowAlpha by animateFloatAsState(
        targetValue = if (isAnySelected) 0.6f else 0.2f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "iconGlow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isExpanded) Color.White.copy(alpha = 0.03f) else Color.Transparent)
            .clickable { onExpandToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- ICON WITH GLOW ---
        Box(contentAlignment = Alignment.Center) {
            if (isAnySelected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.15f * iconGlowAlpha), CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.3f * iconGlowAlpha), CircleShape)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAnySelected) accentColor else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- TITLE & STATUS ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isAnySelected) Color.White else Color.Gray,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isAnySelected) {
                Text(
                    text = "$isSelectedCount / $totalCount allowed",
                    color = accentColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // --- CHECKBOX (MASTER) ---
        Blocker3StateCheckbox(
            state = selectionState,
            onClick = onToggle
        )

        Spacer(modifier = Modifier.width(12.dp))

        // --- EXPAND CHEVRON ---
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BlockerHierarchyRow(
    label: String,
    description: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val accentColor = Color(0xFFDAA520)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp) // Indentation
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- CONNECTING GUIDE LINE ---
        Box(
            modifier = Modifier
                .width(12.dp)
                .fillMaxHeight()
                .padding(end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // This would naturally be part of a parent Column with drawBehind for the full line
        }

        Blocker3StateCheckbox(
            state = if (isSelected) ToggleableState.On else ToggleableState.Off,
            onClick = onToggle,
            size = 18.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = description,
                color = Color.Gray.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun Blocker3StateCheckbox(
    state: ToggleableState,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 22.dp
) {
    val accentColor = Color(0xFFDAA520)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when (state) {
                    ToggleableState.On -> accentColor
                    ToggleableState.Indeterminate -> accentColor.copy(alpha = 0.2f)
                    ToggleableState.Off -> Color(0xFF222222)
                }
            )
            .border(
                width = 1.dp,
                color = if (state != ToggleableState.Off) accentColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            ToggleableState.On -> Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(size * 0.7f))
            ToggleableState.Indeterminate -> Box(Modifier.size(size * 0.5f, 2.dp).background(accentColor))
            ToggleableState.Off -> {}
        }
    }
}

@Composable
fun AuraSegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFDAA520)
) {
    val selectedIndex = options.indexOf(selectedOption)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF151515))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Gray,
                    animationSpec = tween(300), label = "color"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onOptionSelected(option) 
                        }
                        .then(
                            if (isSelected) Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent)
                                )
                            ).border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = color,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AuraSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        color = Color(0xFFDAA520).copy(alpha = 0.8f),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun AuraSimpleBanner(
    modifier: Modifier = Modifier,
    text: String = "The filters selected below will be allowed through. All other notifications will be silenced."
) {
    val accentColor = Color(0xFFDAA520)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        Color(0xFF0F0F0F),
                        accentColor.copy(alpha = 0.05f)
                    )
                )
            )
            .borderBeam(
                color = accentColor,
                durationMillis = 3000,
                width = 1.dp,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Info, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
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

@Composable
fun ProLockedKeywords(
    onProClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFFDAA520)
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerTranslation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        AuraSectionHeader("Custom Filter Keywords")
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF151515))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .clickable { onProClick() }
                .padding(1.dp)
        ) {
            // --- SHIMMER OVERLAY ---
            Canvas(modifier = Modifier.matchParentSize()) {
                rotate(45f) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(shimmerTranslateX, 0f),
                            end = androidx.compose.ui.geometry.Offset(shimmerTranslateX + 200f, 0f)
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = accentColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Intelligent Priority deep-scan",
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Allow notifications only if they contain your chosen keywords, silencing everything else.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                // Feature Preview Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.alpha(0.4f)
                ) {
                    KeywordPreviewChip("VIP", accentColor)
                    KeywordPreviewChip("Boss", accentColor)
                    KeywordPreviewChip("Urgent", accentColor)
                    KeywordPreviewChip("OTP", accentColor)
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Unlock Premium Filters",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KeywordPreviewChip(text: String, accentColor: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
