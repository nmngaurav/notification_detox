package aura.notification.filter.ui.settings

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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Star
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.ui.components.GlassCard
import aura.notification.filter.ui.components.ShieldSlider

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShieldControlScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    var selectedRule by remember { mutableStateOf<aura.notification.filter.data.AppRuleEntity?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Filter Center") },
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
                Icon(Icons.Default.Add, contentDescription = "Add to Detox", tint = Color.Black)
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        // Modal Sheet for Config
        if (selectedRule != null) {
            val appInfo = viewModel.getAppInfo(selectedRule!!.packageName)
            AppConfigSheet(
                appName = appInfo.label,
                packageName = selectedRule!!.packageName,
                icon = appInfo.icon,
                currentShieldLevel = selectedRule!!.shieldLevel,
                initialCategories = selectedRule!!.activeCategories,
                keywords = selectedRule!!.customKeywords,
                onSave = { level, categories, keywords ->
                    viewModel.updateSmartRule(selectedRule!!.packageName, selectedProfile, categories, keywords)
                    selectedRule = null
                },
                onRemove = {
                    viewModel.deleteRule(selectedRule!!.packageName, selectedProfile)
                    selectedRule = null
                },
                onDismiss = { selectedRule = null }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            
            if (rules.isEmpty()) {
                EmptyState(navController, selectedProfile)
            } else {
                // Group Rules
                val blockedRules by remember(rules) { derivedStateOf { rules.filter { it.shieldLevel == ShieldLevel.FORTRESS } } }
                val smartRules by remember(rules) { derivedStateOf { rules.filter { it.shieldLevel == ShieldLevel.SMART } } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Blocked Section
                    if (blockedRules.isNotEmpty()) {
                        stickyHeader { SectionHeader("Blocked Forever", Color(0xFFEF5350), blockedRules.size) }
                        items(blockedRules, key = { it.packageName }) { rule ->
                            RuleItem(rule, viewModel, selectedProfile, onClick = { selectedRule = rule }, Modifier.animateItemPlacement())
                        }
                    }

                    // Smart Section
                    if (smartRules.isNotEmpty()) {
                        stickyHeader { SectionHeader("Smart Detox Active", Color(0xFFDAA520), smartRules.size) }
                        items(smartRules, key = { it.packageName }) { rule ->
                            RuleItem(rule, viewModel, selectedProfile, onClick = { selectedRule = rule }, Modifier.animateItemPlacement())
                        }
                    }

                }
            }
        }
    }
}

// ... (Sub-components update) ...



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
    rule: aura.notification.filter.data.AppRuleEntity,
    viewModel: SettingsViewModel,
    profileId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appInfo = viewModel.getAppInfo(rule.packageName)
    
    GlassCard(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick
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
                    val filterDesc = when (rule.shieldLevel) {
                        ShieldLevel.SMART -> {
                            val cats = rule.activeCategories.split(",").filter { it.isNotEmpty() }
                            if (cats.isEmpty()) "Smart Detox Active"
                            else "Smart: ${cats.joinToString(", ")}"
                        }
                        ShieldLevel.FORTRESS -> "Blocked Forever"
                        else -> "Always Allowed"
                    }
                    Text(
                        text = filterDesc,
                        color = Color(0xFFDAA520),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Icon(Icons.Default.Settings, contentDescription = "Edit", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { viewModel.deleteRule(rule.packageName, profileId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove", tint = Color(0xFFEF5350))
                }
            }
        }
    }
}


@Composable
fun EmptyState(navController: NavController, profileId: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFFDAA520).copy(alpha = 0.05f), CircleShape)
                .border(2.dp, Color(0xFFDAA520).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFDAA520).copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "No Apps Filtered",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "Add apps to start blocking unwanted notifications.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = { navController.navigate("app_selection/$profileId") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520), contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp).fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Apps", fontWeight = FontWeight.Bold)
        }
    }
}
