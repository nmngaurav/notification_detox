package com.aura.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.data.ShieldLevel
import com.aura.ui.components.GlassCard
import com.aura.ui.components.ShieldSlider

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShieldControlScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Shield") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("app_selection/$selectedProfile") },
                containerColor = Color(0xFFDAA520)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shield", tint = Color.Black)
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            
            // Profile Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileTab(
                    title = "Focus Mode",
                    isSelected = selectedProfile == "FOCUS",
                    onClick = { viewModel.setProfile("FOCUS") }
                )
                ProfileTab(
                    title = "Relax Mode",
                    isSelected = selectedProfile == "RELAX",
                    onClick = { viewModel.setProfile("RELAX") }
                )
            }

            if (rules.isEmpty()) {
                EmptyState()
            } else {
                // Group Rules
                // Group Rules (Optimized)
                val blockedRules by remember(rules) { derivedStateOf { rules.filter { it.shieldLevel == ShieldLevel.FORTRESS } } }
                val smartRules by remember(rules) { derivedStateOf { rules.filter { it.shieldLevel == ShieldLevel.SMART } } }
                val allowedRules by remember(rules) { derivedStateOf { rules.filter { it.shieldLevel == ShieldLevel.OPEN } } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Blocked Section
                    if (blockedRules.isNotEmpty()) {
                        stickyHeader { SectionHeader("Blocked", Color(0xFFEF5350), blockedRules.size) }
                        items(blockedRules, key = { it.packageName }) { rule ->
                            RuleItem(rule, viewModel, selectedProfile, Modifier.animateItemPlacement())
                        }
                    }

                    // Smart Section
                    if (smartRules.isNotEmpty()) {
                        stickyHeader { SectionHeader("Smart Filtered", Color(0xFFDAA520), smartRules.size) }
                        items(smartRules, key = { it.packageName }) { rule ->
                            RuleItem(rule, viewModel, selectedProfile, Modifier.animateItemPlacement())
                        }
                    }

                    // Allowed Section (Collapsed by default logic or just shown)
                    if (allowedRules.isNotEmpty()) {
                        stickyHeader { SectionHeader("Allowed", Color(0xFF4CAF50), allowedRules.size) }
                        items(allowedRules, key = { it.packageName }) { rule ->
                            RuleItem(rule, viewModel, selectedProfile, Modifier.animateItemPlacement())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFDAA520) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color.Gray,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun SectionHeader(title: String, color: Color, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A).copy(alpha = 0.95f)) // Almost opaque for sticky effect
            .padding(vertical = 12.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text("($count)", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun RuleItem(
    rule: com.aura.data.AppRuleEntity,
    viewModel: SettingsViewModel,
    profileId: String,
    modifier: Modifier = Modifier
) {
    val appInfo = viewModel.getAppInfo(rule.packageName)
    var showDialog by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (appInfo.icon != null) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            android.widget.ImageView(ctx).apply {
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        update = { view ->
                             view.setImageDrawable(appInfo.icon)
                        },
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(appInfo.label, color = Color.White, fontWeight = FontWeight.Bold)
                    if (rule.filterTemplate != com.aura.data.FilterTemplate.NONE && rule.shieldLevel == ShieldLevel.SMART) {
                        Text(
                            "Smart: ${rule.filterTemplate.displayName}",
                            color = Color(0xFFDAA520),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (rule.customKeywords.isNotEmpty()) {
                         Text(
                            "Custom Rules: ${rule.customKeywords.take(20)}...",
                            color = Color(0xFFDAA520),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Icon(Icons.Default.Settings, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { viewModel.updateRule(rule.packageName, profileId, ShieldLevel.NONE) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove", tint = Color(0xFFEF5350))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ShieldSlider(
                currentLevel = rule.shieldLevel,
                onLevelChange = { newLevel ->
                    viewModel.updateRule(rule.packageName, profileId, newLevel)
                }
            )
        }
    }
    
    if (showDialog) {
        SmartConfigDialog(
            appLabel = appInfo.label,
            currentRule = rule,
            onDismiss = { showDialog = false },
            onSave = { template, keywords ->
                viewModel.updateSmartRule(rule.packageName, profileId, template, keywords)
                showDialog = false
            }
        )
    }
}

@Composable
fun SmartConfigDialog(
    appLabel: String,
    currentRule: com.aura.data.AppRuleEntity,
    onDismiss: () -> Unit,
    onSave: (com.aura.data.FilterTemplate, String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(currentRule.filterTemplate) }
    var keywords by remember { mutableStateOf(currentRule.customKeywords) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Configure $appLabel", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Template Selector
                Text("Smart Filter Type", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                
                // Simple dropdown or radio replacement for simplicity
                val templates = com.aura.data.FilterTemplate.values()
                templates.forEach { template ->
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
                
                // Keywords Input
                Text("Always Allow Keywords", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    placeholder = { Text("e.g. otp, emergency, salary") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFDAA520),
                        focusedBorderColor = Color(0xFFDAA520),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Notifications containing these words will pass through the Smart Shield.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedTemplate, keywords) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520))
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color(0xFF333333),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Shield is Inactive",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add apps to start reclaiming your focus.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
