package aura.notification.filter.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/**
 * Adds a premium shimmering effect to any component.
 */
fun Modifier.shimmer(
    durationMillis: Int = 2000,
    shimmerColor: Color = Color.White.copy(alpha = 0.2f)
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    this.onGloballyPositioned { size = it.size }
        .drawBehind {
            if (size.width > 0) {
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        shimmerColor,
                        Color.Transparent,
                    ),
                    start = Offset(translateAnim - size.width.toFloat(), translateAnim - size.height.toFloat()),
                    end = Offset(translateAnim, translateAnim)
                )
                drawRect(brush = brush)
            }
        }
}

/**
 * Adds a premium elastic pulse feedback on click.
 */
fun Modifier.pulseClickable(
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "PulseScale"
    )

    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}
