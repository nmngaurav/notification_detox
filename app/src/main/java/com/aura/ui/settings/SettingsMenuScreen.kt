package com.aura.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aura.ui.components.GlassCard
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    data class OtherApp(val name: String, val icon: ImageVector, val url: String, val color: Color)
    val otherApps = listOf(
        OtherApp("Read Fast", Icons.Default.Star, "https://play.google.com/store/apps/details?id=read.fast.app&hl=en_IN", Color(0xFF4CAF50)),
        OtherApp("Money Tracker AI", Icons.Default.Check, "https://play.google.com/store/apps/details?id=money.tracker.ai", Color(0xFF2196F3)),
        OtherApp("Summary AI", Icons.Default.List, "https://play.google.com/store/apps/details?id=summary.ai", Color(0xFF9C27B0))
    )
    

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Section 1: Shield
            item {
                MenuSectionTitle("Shield Configuration")
                Spacer(modifier = Modifier.height(8.dp))
                MenuActionItem(
                    title = "Manage Shield",
                    subtitle = "Configure blocked apps and keywords",
                    icon = Icons.Default.Lock,
                    color = Color(0xFFDAA520),
                    onClick = { navController.navigate("shield_control") }
                )
            }
            
            // Section 2: Support
            item {
                MenuSectionTitle("Support")
                Spacer(modifier = Modifier.height(8.dp))
                MenuActionItem(
                    title = "Help & Support",
                    subtitle = "Get help using Aura",
                    icon = Icons.Default.Email,
                    color = Color(0xFF2196F3),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                         val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:app.sandboxx@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Aura Support Request")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                )
                MenuActionItem(
                    title = "Suggest a Feature",
                    subtitle = "We'd love to hear your ideas",
                    icon = Icons.Default.Star,
                    color = Color(0xFF9C27B0),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:app.sandboxx@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Aura Feature Request")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                )
            }
            
            // Section 3: Community
            item {
                MenuSectionTitle("Community")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(otherApps) { app ->
                        GlassCard(
                            modifier = Modifier.width(240.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.url))
                                context.startActivity(intent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(app.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(app.icon, contentDescription = null, tint = app.color, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = app.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text("Get it", color = app.color, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            
             // Section 4: About
            item {
                MenuSectionTitle("About")
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aura", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        Text("Version ${pInfo.versionName}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuSectionTitle(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun MenuActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
