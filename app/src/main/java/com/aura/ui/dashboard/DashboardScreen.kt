package com.aura.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import com.aura.ui.MainViewModel
import com.aura.ui.components.GlassCard
import androidx.navigation.NavController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.aura.ui.components.DigestCard
import com.aura.ui.digest.DigestViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel(),
    digestViewModel: DigestViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val blockedCount by viewModel.totalBlockedCount.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val digestItems by digestViewModel.uiState.collectAsState()
    
    // Ambient Background Animation
    val infiniteTransition = rememberInfiniteTransition()
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Ambient"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse"
    )

    // Dynamic Color for Mode
    val modeColor by animateColorAsState(
        targetValue = when (currentMode) {
            com.aura.data.FocusMode.FOCUS -> Color(0xFFDAA520) // Gold - Deep Work
            com.aura.data.FocusMode.RELAX -> Color(0xFF4CAF50) // Green - Personal Time
        },
        animationSpec = tween(1000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(modeColor.copy(alpha = 0.3f), Color.Black),
                    center = androidx.compose.ui.geometry.Offset(gradientShift, 500f),
                    radius = 1500f
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- HEADER ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Aura",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(color = modeColor, shape = CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentMode.name} Active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = modeColor 
                            )
                        }
                    }
                    
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            // --- TOGGLE ---
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (currentMode == com.aura.data.FocusMode.FOCUS) "Focus Mode" else "Relax Mode",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (currentMode == com.aura.data.FocusMode.FOCUS) 
                                        "Blocks distractions" else "Gentle filtering",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            
                            // Toggle Switch
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                                    .background(
                                        if (currentMode == com.aura.data.FocusMode.FOCUS) 
                                            Color(0xFFDAA520) else Color(0xFF4CAF50),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setMode(
                                            if (currentMode == com.aura.data.FocusMode.FOCUS)
                                                com.aura.data.FocusMode.RELAX 
                                            else com.aura.data.FocusMode.FOCUS
                                        )
                                    }
                                    .padding(4.dp),
                                contentAlignment = if (currentMode == com.aura.data.FocusMode.FOCUS) 
                                    Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // --- CONTENT FEED OR ZEN MODE ---
            if (digestItems.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Blocked Activity",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF333333), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "$blockedCount", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { digestViewModel.clearAll() }) {
                            Text("Clear All", color = Color(0xFFDAA520))
                        }
                    }
                }
                
                items(digestItems) { item ->
                    DigestCard(
                        item = item, 
                        onSummarizeClick = { digestViewModel.requestSummary(item.packageName) }
                    )
                }
            } else {
                // ZEN MODE VIEW
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             // Pulsing Lock/Shield
                             Box(contentAlignment = Alignment.Center) {
                                 Canvas(modifier = Modifier.size(120.dp)) {
                                     drawCircle(
                                         color = modeColor.copy(alpha = 0.1f),
                                         radius = size.minDimension / 2 * pulseAlpha
                                     )
                                     drawCircle(
                                         color = modeColor.copy(alpha = 0.05f),
                                         radius = size.minDimension / 1.5f * pulseAlpha
                                     )
                                 }
                                 Icon(
                                     imageVector = Icons.Default.Lock, 
                                     contentDescription = null,
                                     modifier = Modifier.size(48.dp),
                                     tint = modeColor
                                 )
                             }
                             
                             Spacer(modifier = Modifier.height(24.dp))
                             
                             Text(
                                 text = if (currentMode == com.aura.data.FocusMode.FOCUS) 
                                    "All Quiet on the Front" else "Relaxing...",
                                 style = MaterialTheme.typography.headlineSmall,
                                 color = Color.White,
                                 fontWeight = FontWeight.Bold
                             )
                             
                             Spacer(modifier = Modifier.height(8.dp))
                             
                             Text(
                                 text = "\"Silence is not empty. It's full of answers.\"",
                                 style = MaterialTheme.typography.bodyMedium,
                                 color = Color.Gray,
                                 fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center
                             )
                         }
                    }
                }
            }
        }
    }
}
