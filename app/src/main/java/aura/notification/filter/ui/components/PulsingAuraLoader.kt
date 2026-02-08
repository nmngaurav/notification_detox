package aura.notification.filter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PulsingAuraLoader(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFDAA520)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraLoader")

    // Outer Ring Pulse
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OuterScale"
    )

    val outerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OuterAlpha"
    )

    // Inner Core Pulse
    val innerScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "InnerScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer Ring
        Canvas(modifier = Modifier.size(48.dp)) {
            drawCircle(
                color = color.copy(alpha = outerAlpha),
                radius = size.minDimension / 2 * outerScale,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Inner Core
        Canvas(modifier = Modifier.size(48.dp)) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2 * innerScale
            )
        }
    }
}
