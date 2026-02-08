package aura.notification.filter.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmartTagChip(
    tag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Premium Animation States
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "Scale")
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.6f, label = "Alpha")
    
    // Glassmorphism Colors
    val activeGradient = Brush.horizontalGradient(
        listOf(Color(0xFFDAA520), Color(0xFFFFD700)) // Gold Gradient
    )
    val inactiveColor = Color(0xFF1E1E1E) // Dark Glass
    
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFFFFD700).copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.3f),
        label = "Border"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color.Transparent else inactiveColor.copy(alpha = 0.5f))
            .run {
                if (isSelected) background(activeGradient) else this
            }
            .border(
                BorderStroke(1.dp, borderColor),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$tag",
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}
