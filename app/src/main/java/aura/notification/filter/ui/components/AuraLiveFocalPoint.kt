package aura.notification.filter.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import aura.notification.filter.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate

@Composable
fun AuraLiveFocalPoint(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFDAA520),
    isMonitoring: Boolean = true,
    monitoredAppsCount: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraFocal")
    
    // Unified pulse scale (shared across dashboard elements for "sync")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 1. Atmosphere (Outer Glow)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale * 1.1f)
                    .alpha(alpha * 0.5f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            // 2. Sweeping Shimmer (Vigilance)
            androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp).rotate(rotation)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            
            // 3. The Core (Breathing Orb)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .scale(scale * 0.95f)
                    .background(
                        Brush.radialGradient(
                            0.0f to accentColor,
                            0.7f to accentColor.copy(alpha = 0.4f),
                            1.0f to Color.Transparent
                        ),
                        CircleShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // 4. Inner Focal Light
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale * 0.8f)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Contextual Status Text
        Text(
            text = if (monitoredAppsCount > 0) "SMART FILTERING ACTIVE" else "AURA PROTECTION READY",
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            modifier = Modifier.alpha(0.9f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (monitoredAppsCount > 0) 
                "$monitoredAppsCount Apps Perfectly Isolated" 
                else "Add your first app to start filtering.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp).alpha(0.7f)
        )
    }
}
