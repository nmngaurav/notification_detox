package com.aura.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aura.data.ShieldLevel
import com.aura.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
    profileId: String
) {
    val installedApps by viewModel.installedApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { 
            it.label.contains(searchQuery, ignoreCase = true) || 
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Config Sheet State
    var selectedForConfig by remember { mutableStateOf<com.aura.util.AppInfoManager.AppInfo?>(null) }

    // Custom Rule Config Sheet
    if (selectedForConfig != null) {
        val app = selectedForConfig!!
        
        // Use default SMART level for new apps
        com.aura.ui.settings.AppConfigSheet(
            appName = app.label,
            packageName = app.packageName,
            icon = app.icon,
            currentShieldLevel = ShieldLevel.SMART,
            initialCategories = "",
            keywords = "", 
            onSave = { level, categories, keywords ->
                viewModel.updateSmartRule(app.packageName, profileId, categories, keywords)
                selectedForConfig = null
                navController.popBackStack()
            },
            onRemove = {
                viewModel.deleteRule(app.packageName, profileId)
                selectedForConfig = null
            },
            onDismiss = { selectedForConfig = null }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0A0A0A))) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Add App Filter",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search 100+ apps...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedForConfig = app },
                    cornerRadius = 16.dp
                    // Note: GlassCard inside clickable vs clickable inside GlassCard. 
                    // Let's rely on standard clickable above.
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { context ->
                                    android.widget.ImageView(context).apply {
                                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                update = { view ->
                                    view.setImageDrawable(app.icon)
                                },
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF333333), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    app.label.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Add Button Visual
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF222222), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                 imageVector = Icons.Default.Add,
                                 contentDescription = "Add",
                                 tint = Color(0xFFDAA520),
                                 modifier = Modifier.size(16.dp)
                             )
                        }
                    }
                }
            }
        }
    }
}
