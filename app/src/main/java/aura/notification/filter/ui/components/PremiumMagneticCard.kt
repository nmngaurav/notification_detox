package aura.notification.filter.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A highly interactive card with tilt physics and glassmorphism.
 * Responds to touch by tilting towards the finger.
 */
@Composable
fun PremiumMagneticCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0F0F0F),
    accentColor: Color = Color(0xFFDAA520),
    isExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val glassBrush = Brush.verticalGradient(
        colors = listOf(
            backgroundColor.copy(alpha = 0.9f),
            backgroundColor.copy(alpha = 0.7f)
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.rotationX = rotationX.value
                this.rotationY = rotationY.value
                this.scaleX = scale.value
                this.scaleY = scale.value
                this.cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        
                        // Calculate rotation based on touch position
                        // center is (size.width/2, size.height/2)
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        
                        val rotY = (position.x - centerX) / centerX * 10f
                        val rotX = -(position.y - centerY) / centerY * 10f

                        coroutineScope.launch {
                            rotationX.animateTo(rotX, spring(stiffness = Spring.StiffnessMedium))
                            rotationY.animateTo(rotY, spring(stiffness = Spring.StiffnessMedium))
                            scale.animateTo(0.98f, spring(stiffness = Spring.StiffnessMedium))
                        }

                        if (event.changes.first().pressed.not()) {
                            coroutineScope.launch {
                                rotationX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                rotationY.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                scale.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
                            }
                        }
                    }
                }
            }
            .clip(RoundedCornerShape(20.dp))
            .background(glassBrush)
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        accentColor.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .padding(if (isExpanded) 20.dp else 16.dp)
    ) {
        androidx.compose.foundation.layout.Column {
            content()
        }
    }
}
