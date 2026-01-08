
package com.aura.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.withStyle
import com.aura.ui.components.GlassCard

@Composable
fun KeywordDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    // Popular presets
    val presets = listOf("Urgent", "Mom", "Bank", "OTP")
    
    // Dynamic Templates for "Usage in Context"
    // Generic Templates for versatile preview
    val templates = listOf(
        "Contains: '...regarding the %s matter...'",
        "From: '%s sent you a message'",
        "Subject: 'Important update: %s'",
        "Body: 'Don't forget about %s today'"
    )
    
    // Rotate template every 3 seconds to show variety
    var currentTemplateIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(3000)
            currentTemplateIndex = (currentTemplateIndex + 1) % templates.size
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        // 1. Main Container: Premium Glass Card
        GlassCard(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDAA520).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            backgroundColor = Color(0xFF0A0A0A).copy(alpha = 0.95f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    "Add VIP Keyword",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Any notification containing this phrase will always be allowed through.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 2. LIVE SIMULATION CARD
                val isMatch = text.isNotEmpty()
                val targetBorderColor = if (isMatch) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.2f)
                val borderColor by animateColorAsState(targetBorderColor, label = "border")
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        // Notification Header (Icon + App Name + Time)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mock App Icon
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(if (isMatch) Color(0xFF4CAF50) else Color.Gray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "A", 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            // App Name
                            Text("Messaging", fontSize = 12.sp, color = Color.LightGray)
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Time
                            Text("now", fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Main Content Area
                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Title depends on match
                                Text(
                                    text = if (isMatch) "New Notification" else "Notifications Paused",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Dynamic Content Preview
                                val currentTemplate = templates[currentTemplateIndex]
                                if (isMatch) {
                                    val parts = currentTemplate.split("%s")
                                    val prefix = parts.getOrElse(0) { "" }
                                    val suffix = parts.getOrElse(1) { "" }
                                    
                                    // Realistic Text Rendering
                                    Text(
                                        text = androidx.compose.ui.text.buildAnnotatedString {
                                            append(prefix)
                                            withStyle(androidx.compose.ui.text.SpanStyle(
                                                color = Color(0xFFDAA520), 
                                                fontWeight = FontWeight.Bold,
                                                background = Color(0xFFDAA520).copy(alpha = 0.15f)
                                            )) {
                                                append(text)
                                            }
                                            append(suffix)
                                        },
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        maxLines = 2
                                    )
                                } else {
                                    Text(
                                        "Enter a keyword to allow specific alerts through...", 
                                        color = Color.Gray, 
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            // Status Badge (Right Side)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isMatch,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                                exit = androidx.compose.animation.fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .background(Color(0xFF4CAF50).copy(0.1f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF4CAF50).copy(0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 3. Input with Gold Focus
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. Urgent", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                         focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                         focusedBorderColor = Color(0xFFDAA520), unfocusedBorderColor = Color(0xFF333333),
                         cursorColor = Color(0xFFDAA520),
                         focusedContainerColor = Color(0xFF111111), unfocusedContainerColor = Color(0xFF111111)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 4. Quick Actions
                Text("POPULAR PRESETS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { text = preset }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 5. Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                    
                    Button(
                        onClick = { 
                            if (text.isNotBlank()) {
                                onAdd(text.replace(",", " ").trim()) 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520), contentColor = Color.Black),
                        modifier = Modifier.weight(1f),
                        enabled = text.isNotBlank()
                    ) {
                        Text("Add Rule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper to avoid import mess if not already imported
private fun androidx.compose.ui.text.AnnotatedString.Builder.build() = toAnnotatedString()

