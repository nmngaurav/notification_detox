package aura.notification.filter.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Brush
import aura.notification.filter.data.DigestItem
import aura.notification.filter.data.NotificationEntity

@Composable
fun DigestCard(
    item: DigestItem,
    onSummarizeClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded } 
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- HEADER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon
                if (item.icon != null) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { context ->
                            android.widget.ImageView(context).apply {
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        update = { view -> view.setImageDrawable(item.icon) },
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF333333), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.label.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleLarge, // Outfit
                        color = Color.White
                    )
                    Text(
                        text = "${item.count} notifications blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- CONTENT AREA ---
            if (item.summary != null) {
                // STATE 1: AI SUMMARY (Premium View)
                Text(
                    text = "AI SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFDAA520), // Gold
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            } else {
                // STATE 2: RAW LIST (Default View)
                val displayList = if (expanded) item.notifications else item.notifications.take(3)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayList.forEach { notif ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(notif.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.width(40.dp)
                            )
                            Column {
                                Text(
                                    text = notif.title, // Sender Name
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEEEEEE)
                                )
                                Text(
                                    text = notif.content, // Message
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBBBBBB),
                                    maxLines = if (expanded) 10 else 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    if (!expanded && item.notifications.size > 3) {
                        Text(
                            text = "+ ${item.notifications.size - 3} more...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }
                
                // --- ACTION BUTTON (Summarize) ---
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summarize Button
                Button(
                    onClick = onSummarizeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33DAA520) // Deep Gold Tint
                    ),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✨ Analyze & Summarize", 
                        color = Color(0xFFDAA520),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            // Open App Button (Only visible when expanded, for both states)
            if (expanded) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            // Feedback if app cannot be opened
                            android.widget.Toast.makeText(context, "Cannot open this app", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open App", color = Color(0xFFDAA520))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DigestCardPreview() {
    val sampleNotifications = listOf(
        NotificationEntity(
            packageName = "com.whatsapp",
            title = "John Doe",
            content = "Hey! How are you doing?",
            timestamp = System.currentTimeMillis() - 3600000,
            category = "messaging",
            isBlocked = true
        ),
        NotificationEntity(
            packageName = "com.whatsapp",
            title = "Jane Smith",
            content = "Don't forget about the meeting tomorrow",
            timestamp = System.currentTimeMillis() - 7200000,
            category = "messaging",
            isBlocked = true
        ),
        NotificationEntity(
            packageName = "com.whatsapp",
            title = "Team Chat",
            content = "New message in group",
            timestamp = System.currentTimeMillis() - 10800000,
            category = "messaging",
            isBlocked = true
        ),
        NotificationEntity(
            packageName = "com.whatsapp",
            title = "Alice",
            content = "Thanks for your help!",
            timestamp = System.currentTimeMillis() - 14400000,
            category = "messaging",
            isBlocked = true
        ),
        NotificationEntity(
            packageName = "com.whatsapp",
            title = "Bob",
            content = "Can we reschedule?",
            timestamp = System.currentTimeMillis() - 18000000,
            category = "messaging",
            isBlocked = true
        )
    )
    
    val sampleItem = DigestItem(
        packageName = "com.whatsapp",
        label = "WhatsApp",
        icon = null,
        summary = null,
        count = 5,
        notifications = sampleNotifications
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
            .padding(16.dp)
    ) {
        DigestCard(
            item = sampleItem,
            onSummarizeClick = {}
        )
    }
}

@Preview(showBackground = true, name = "With AI Summary")
@Composable
private fun DigestCardWithSummaryPreview() {
    val sampleNotifications = listOf(
        NotificationEntity(
            packageName = "com.instagram",
            title = "Instagram",
            content = "New post from friend",
            timestamp = System.currentTimeMillis() - 3600000,
            category = "social",
            isBlocked = true
        ),
        NotificationEntity(
            packageName = "com.instagram",
            title = "Instagram",
            content = "You have 3 new followers",
            timestamp = System.currentTimeMillis() - 7200000,
            category = "social",
            isBlocked = true
        )
    )
    
    val sampleItem = DigestItem(
        packageName = "com.instagram",
        label = "Instagram",
        icon = null,
        summary = "You received 2 social notifications today. One new post from a friend and 3 new followers. Nothing urgent that requires immediate attention.",
        count = 2,
        notifications = sampleNotifications
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
            .padding(16.dp)
    ) {
        DigestCard(
            item = sampleItem,
            onSummarizeClick = {}
        )
    }
}
