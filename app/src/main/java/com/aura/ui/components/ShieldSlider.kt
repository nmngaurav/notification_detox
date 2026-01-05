package com.aura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.data.ShieldLevel

@Composable
fun ShieldSlider(
    currentLevel: ShieldLevel,
    onLevelChange: (ShieldLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3 Levels: OPEN, SMART, FORTRESS
    val levels = listOf(ShieldLevel.OPEN, ShieldLevel.SMART, ShieldLevel.FORTRESS)
    val currentIndex = when(currentLevel) {
        ShieldLevel.OPEN -> 0
        ShieldLevel.SMART -> 1
        ShieldLevel.FORTRESS -> 2
        else -> 1 // Default to Smart
    }

    val targetColor = when(currentLevel) {
        ShieldLevel.OPEN -> Color(0xFF4CAF50) // Green
        ShieldLevel.SMART -> Color(0xFF00E5FF) // Cyan
        ShieldLevel.FORTRESS -> Color(0xFFFF5252) // Red
        else -> Color.Gray
    }
    
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "color")

    Column(modifier = modifier) {
        // Slider Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                levels.forEachIndexed { index, level ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onLevelChange(level) }
                            .background(
                                if (currentIndex == index) animatedColor.copy(alpha = 0.2f) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(level) {
                                ShieldLevel.OPEN -> "OPEN"
                                ShieldLevel.SMART -> "SMART"
                                ShieldLevel.FORTRESS -> "BLOCK"
                                else -> ""
                            },
                            color = if (currentIndex == index) animatedColor else Color.Gray,
                            fontWeight = if (currentIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description
        Text(
            text = when(currentLevel) {
                ShieldLevel.OPEN -> "All notifications allow."
                ShieldLevel.SMART -> "AI filters noise. Important alerts only."
                ShieldLevel.FORTRESS -> "Total silence. Nothing gets through."
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
