package com.aura.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aura.data.FilterTemplate
import com.aura.data.ShieldLevel
import com.aura.ui.components.GlassCard
import com.aura.util.AppInfoManager

data class AppShieldConfig(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
    val shieldLevel: ShieldLevel,
    val suggestedTemplate: FilterTemplate,
    val templateExplanation: String,
    val customKeywords: String = ""
)

@Composable
fun OnboardingShieldConfigScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val appInfoManager: AppInfoManager = hiltViewModel<OnboardingViewModel>().appInfoManager
    
    // State to toggle between Smart List and Full List
    var showAllApps by remember { mutableStateOf(false) }
    
    // Get apps based on selection
    val configuredApps = remember(showAllApps) {
        // Use Smart List initially, or Full List if "Customize" clicked
        val apps = if (showAllApps) {
            appInfoManager.getInstalledApps()
        } else {
            appInfoManager.getSmartOnboardingApps().take(3)
        }
        
        apps.map { appInfo ->
            // Heuristic for template based on app name
            val template = when {
                appInfo.label.contains("WhatsApp", true) -> FilterTemplate.MESSAGING
                appInfo.label.contains("Facebook", true) -> FilterTemplate.SOCIAL
                appInfo.label.contains("Instagram", true) -> FilterTemplate.SOCIAL
                appInfo.label.contains("Uber", true) -> FilterTemplate.TRANSACTIONAL
                appInfo.label.contains("Swiggy", true) -> FilterTemplate.TRANSACTIONAL
                appInfo.label.contains("Amazon", true) -> FilterTemplate.TRANSACTIONAL
                appInfo.label.contains("Slack", true) -> FilterTemplate.WORK
                appInfo.label.contains("Gmail", true) -> FilterTemplate.WORK
                else -> FilterTemplate.NONE
            }

            AppShieldConfig(
                packageName = appInfo.packageName,
                appName = appInfo.label,
                icon = appInfo.icon,
                shieldLevel = ShieldLevel.SMART, // Default to Smart for everyone
                suggestedTemplate = template,
                templateExplanation = ""
            )
        }
    }
    
    // Maintain state of choices even when list reloads
    val userChoices = remember { mutableStateMapOf<String, AppShieldConfig>() }
    
    // Sync configuredApps with userChoices
    // DO NOT use remember(userChoices) as the map reference doesn't change. 
    // We want this to strictly recompose when userChoices content changes.
    val currentDisplayList = configuredApps.map { app ->
            userChoices[app.packageName] ?: app
        }.filter { it.shieldLevel != ShieldLevel.NONE }

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Setup Your Shield",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Choose a strategy for your most distracting apps.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // App List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(currentDisplayList) { app ->
                AppConfigCard(
                    app = app,
                    onUpdate = { updatedApp ->
                        userChoices[app.packageName] = updatedApp
                    }
                )
            }
            
            // "Customize" Button at bottom of list (if not showing all)
            if (!showAllApps) {
                item {
                    OutlinedButton(
                        onClick = { showAllApps = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDAA520)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                    ) {
                        Text("Customize / See All Apps")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Continue Button
        Button(
            onClick = {
                viewModel.saveInitialShieldConfigs(currentDisplayList)
                onContinue()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDAA520)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Activate Shield",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun AppConfigCard(
    app: AppShieldConfig,
    onUpdate: (AppShieldConfig) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App Entity Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                if (app.icon != null) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            android.widget.ImageView(ctx).apply {
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        update = { view ->
                             view.setImageDrawable(app.icon)
                        },
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(42.dp).background(Color(0xFF333333), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(app.appName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    if (app.shieldLevel == ShieldLevel.SMART) {
                        Text(
                            text = if (app.customKeywords.isNotEmpty()) "Custom Filter" else "Smart: ${app.suggestedTemplate.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDAA520)
                        )
                    } else if (app.shieldLevel == ShieldLevel.NONE) {
                         Text(text = "Unprotected", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                // Close Button (Replaces Settings Icon)
                // Only show if the app is currently part of the list (not NONE? actually user said click on cross to remove from list)
                // Assuming setting to NONE removes it conceptually or visually if filtered.
                // But here we just set it to NONE. 
                // Close Button (Replaces Settings Icon)
                if (app.shieldLevel != ShieldLevel.NONE) {
                   IconButton(
                        onClick = { onUpdate(app.copy(shieldLevel = ShieldLevel.NONE)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = Color(0xFFEF5350))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Strategy Selector (3 Cards now, Remove is X button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StrategyOption(
                    title = "Monitor",
                    icon = Icons.Default.Notifications,
                    isSelected = app.shieldLevel == ShieldLevel.OPEN,
                    color = Color(0xFF4CAF50),
                    onClick = { onUpdate(app.copy(shieldLevel = ShieldLevel.OPEN)) },
                    modifier = Modifier.weight(1f)
                )
                
                StrategyOption(
                    title = "Filter",
                    icon = Icons.Default.Info,
                    isSelected = app.shieldLevel == ShieldLevel.SMART,
                    color = Color(0xFFDAA520),
                    onClick = { onUpdate(app.copy(shieldLevel = ShieldLevel.SMART)) },
                    onLongClick = { 
                        // Long press ALWAYS opens config for Filter
                        if (app.shieldLevel != ShieldLevel.SMART) {
                             onUpdate(app.copy(shieldLevel = ShieldLevel.SMART))
                        }
                        showDialog = true 
                    },
                    modifier = Modifier.weight(1f)
                )
                
                StrategyOption(
                    title = "Focus",
                    icon = Icons.Default.Clear,
                    isSelected = app.shieldLevel == ShieldLevel.FORTRESS,
                    color = Color(0xFFEF5350),
                    onClick = { onUpdate(app.copy(shieldLevel = ShieldLevel.FORTRESS)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Tune Button for Smart Mode
            androidx.compose.animation.AnimatedVisibility(
                visible = app.shieldLevel == ShieldLevel.SMART
            ) {
                 Box(
                     modifier = Modifier.fillMaxWidth().padding(top = 8.dp), 
                     contentAlignment = Alignment.CenterEnd
                 ) {
                      TextButton(
                          onClick = { showDialog = true },
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                          modifier = Modifier.height(32.dp)
                      ) {
                           Icon(
                               imageVector = Icons.Default.Settings, 
                               contentDescription = null, 
                               tint = Color(0xFFDAA520), 
                               modifier = Modifier.size(14.dp)
                           )
                           Spacer(modifier = Modifier.width(6.dp))
                           Text(
                               text = "TUNE FILTERS", 
                               color = Color(0xFFDAA520), 
                               style = MaterialTheme.typography.labelSmall,
                               fontWeight = FontWeight.Bold
                           )
                      }
                 }
            }
            
            // Contextual Preview
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedPreview(app.shieldLevel, app.suggestedTemplate)
        }
    }

    if (showDialog) {
        SmartConfigDialog(
            appLabel = app.appName,
            initialTemplate = app.suggestedTemplate,
            initialKeywords = app.customKeywords,
            onDismiss = { showDialog = false },
            onSave = { template, keywords ->
                onUpdate(app.copy(suggestedTemplate = template, customKeywords = keywords))
                showDialog = false
            }
        )
    }
}

@Composable
fun SmartConfigDialog(
    appLabel: String,
    initialTemplate: FilterTemplate,
    initialKeywords: String,
    onDismiss: () -> Unit,
    onSave: (FilterTemplate, String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(initialTemplate) }
    var keywords by remember { mutableStateOf(initialKeywords) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Filter Strategy: $appLabel", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Predefined Templates", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                
                FilterTemplate.values().forEach { template ->
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplate = template }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedTemplate == template),
                            onClick = { selectedTemplate = template },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFDAA520))
                        )
                        Text(template.displayName, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Divider(color = Color.DarkGray)
                
                Text("Custom Keywords", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    placeholder = { Text("e.g. otp, emergency") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFDAA520),
                        focusedBorderColor = Color(0xFFDAA520),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedTemplate, keywords) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520))
            ) {
                Text("Apply", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StrategyOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
    )
    val borderColor by animateColorAsState(
        if (isSelected) color else Color.Transparent
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) color else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AnimatedPreview(level: ShieldLevel, template: FilterTemplate) {
    val text = when (level) {
        ShieldLevel.OPEN -> "Passive Monitor. Logs activity without blocking."
        ShieldLevel.FORTRESS -> "Deep Focus. Blocks 100% of distractions."
        ShieldLevel.SMART -> when (template) {
            FilterTemplate.MESSAGING -> "Allows: Questions, Plans\nBlocks: 'Ok', 'Lol', Reactions"
            FilterTemplate.SOCIAL -> "Allows: Direct DMs\nBlocks: Likes, Follows, Comments"
            FilterTemplate.TRANSACTIONAL -> "Allows: OTPs, Deliveries\nBlocks: Ads, Offers, Promos"
            else -> "Smart Shield. Filters noise, protects focus."
        }
        else -> ""
    }
    
    val color = when(level) {
        ShieldLevel.OPEN -> Color(0xFF4CAF50)
        ShieldLevel.SMART -> Color(0xFFDAA520)
        ShieldLevel.FORTRESS -> Color(0xFFEF5350)
        else -> Color.Gray
    }

    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
