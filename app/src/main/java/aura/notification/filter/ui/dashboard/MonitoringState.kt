package aura.notification.filter.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonitoringState(appCount: Int, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Dual-Pulse Ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(accentColor.copy(alpha = alpha), CircleShape)
                    .border(1.dp, accentColor.copy(alpha = alpha), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale * 1.2f)
                    .background(accentColor.copy(alpha = alpha * 0.4f), CircleShape)
            )
            // Icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF1E1E1E), CircleShape)
                    .padding(16.dp)
                    .border(2.dp, accentColor.copy(alpha = 0.3f), CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Aura Protection Active",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DEEP FILTERING ENGAGED",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF00E676), // System Live Green
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Monitoring $appCount apps for distractions.\nYou are in the flow.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
