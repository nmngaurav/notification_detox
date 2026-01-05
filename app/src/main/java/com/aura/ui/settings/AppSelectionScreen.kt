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

    var showRuleDialogForPackage by remember { mutableStateOf<String?>(null) }
    
    // Status color for displaying current status if possible?
    // For now just listing all apps to add.

    // Custom Rule Dialog
    if (showRuleDialogForPackage != null) {
        val packageName = showRuleDialogForPackage!!
        val appName = installedApps.find { it.packageName == packageName }?.label ?: packageName
        
        AlertDialog(
            onDismissRequest = { showRuleDialogForPackage = null },
            containerColor = Color(0xFF1E1E1E),
            title = { 
                Text(
                    text = "Add $appName",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    Text(
                        "How should Aura handle this app in $profileId mode?", 
                        color = Color(0xFFAAAAAA),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Smart Shield (Recommended)
                    Button(
                        onClick = { 
                            viewModel.updateRule(packageName, profileId, ShieldLevel.SMART)
                            showRuleDialogForPackage = null
                            navController.popBackStack() // Go back after adding? Or stay to add more? Stay is better flow.
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Smart Shield (AI)", fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Best for messaging & social", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.7f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Block Always
                    Button(
                        onClick = { 
                            viewModel.updateRule(packageName, profileId, ShieldLevel.FORTRESS)
                            showRuleDialogForPackage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                         Text("Block Entirely", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Allow Always
                    OutlinedButton(
                        onClick = { 
                            viewModel.updateRule(packageName, profileId, ShieldLevel.OPEN)
                            showRuleDialogForPackage = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Text("Allow Always") 
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRuleDialogForPackage = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
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
                        text = "Add App to Shield",
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
                        .clickable { showRuleDialogForPackage = app.packageName },
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
