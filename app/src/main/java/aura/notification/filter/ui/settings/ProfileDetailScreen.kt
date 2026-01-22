package aura.notification.filter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import aura.notification.filter.data.ShieldLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
    profileId: String
) {
    val rules by viewModel.rules.collectAsState() // TODO: This needs to filter by profileId in ViewModel or here
    // For MVP, we assume ViewModel switches generic 'rules' flow based on profileId, OR we filter here.
    // Given the current VM impl, 'rules' is hardcoded to STANDARD. 
    // We should fix VM eventually to return a specific flow.
    // For now, let's just proceed assuming we will fix VM to be dynamic or we just show "Standard" rules.
    
    val profileRules = rules.filter { it.profileId == profileId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$profileId Rules") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("app_selection/$profileId") },
                containerColor = Color(0xFFDAA520)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        if (profileRules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No rules yet. Notifications handled by AI.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(profileRules) { rule ->
                    aura.notification.filter.ui.components.GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Header: App Name
                            val appInfo = viewModel.getAppInfo(rule.packageName)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (appInfo.icon != null) {
                                        androidx.compose.ui.viewinterop.AndroidView(
                                            factory = { ctx ->
                                                android.widget.ImageView(ctx).apply {
                                                    setImageDrawable(appInfo.icon)
                                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    
                                    Column {
                                        Text(
                                            text = appInfo.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (appInfo.label != rule.packageName) {
                                            Text(
                                                text = rule.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                                
                                // Reset / Delete
                                IconButton(onClick = { 
                                     viewModel.updateRule(rule.packageName, profileId, aura.notification.filter.data.ShieldLevel.SMART)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Reset", tint = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 1. Shield Slider
                            aura.notification.filter.ui.components.ShieldSlider(
                                currentLevel = rule.shieldLevel,
                                onLevelChange = { newLevel ->
                                    viewModel.updateRule(rule.packageName, profileId, newLevel)
                                }
                            )
                            
                            // 2. Filter Template (Only if SMART)
                            if (rule.shieldLevel == aura.notification.filter.data.ShieldLevel.SMART) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Filter Behavior",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Simple Horizontal Scroll for Categories
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(aura.notification.filter.data.DetoxCategory.entries) { category ->
                                        val isSelected = rule.activeCategories.contains(category.name)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { 
                                                val currentList = rule.activeCategories.split(",").filter { it.isNotEmpty() }.toMutableList()
                                                if (isSelected) {
                                                    currentList.remove(category.name)
                                                } else {
                                                    currentList.add(category.name)
                                                }
                                                viewModel.updateSmartRule(rule.packageName, profileId, currentList.joinToString(","), rule.customKeywords)
                                            },
                                            label = { Text(category.displayName) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                                selectedLabelColor = Color(0xFF00E5FF),
                                                labelColor = Color.Gray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
