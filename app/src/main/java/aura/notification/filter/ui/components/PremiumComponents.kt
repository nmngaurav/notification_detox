package aura.notification.filter.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import aura.notification.filter.util.shimmer
import androidx.compose.ui.res.painterResource
import aura.notification.filter.R
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.cos
import kotlin.math.sin

// -----------------------------------------------------------------------------
// 1. AURA BACKGROUND (Mesh Gradient)
// -----------------------------------------------------------------------------
@Composable
fun AuraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "Aurora")
    
    // Animate color positions
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "t1"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505)) // Deep base
            .drawBehind {
                val w = size.width
                val h = size.height
                
                // Orb 1: Deep Blue (Top Left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0F2027).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(
                            x = w * (0.2f + 0.1f * sin(t1)),
                            y = h * (0.2f + 0.1f * cos(t1))
                        ),
                        radius = w * 0.8f
                    )
                )

                // Orb 2: Midnight Purple (Bottom Right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF203A43).copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(
                            x = w * (0.8f - 0.15f * cos(t1 * 0.7f)),
                            y = h * (0.8f - 0.15f * sin(t1 * 0.7f))
                        ),
                        radius = w * 0.9f
                    )
                )

                // Orb 3: Faint Gold (Center-ish, breathing)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFDAA520).copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(
                            x = w * 0.5f,
                            y = h * (0.5f + 0.2f * sin(t1 * 1.3f))
                        ),
                        radius = w * 0.6f
                    )
                )
            }
    ) {
        content()
    }
}

// -----------------------------------------------------------------------------
// 2. BORDER BEAM (Modifier)
// -----------------------------------------------------------------------------
fun Modifier.borderBeam(
    width: Dp = 1.dp,
    color: Color = Color(0xFFDAA520),
    durationMillis: Int = 3000,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "Beam")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing)
        ), label = "BeamAngle"
    )

    this
        .clip(shape) // Clip content to shape
        .drawBehind {
            // Draw the rotating gradient border
            rotate(angle) {
                drawRect(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.8f to Color.Transparent,
                        0.95f to color,
                        1.0f to color.copy(alpha = 0f) // Soft tail
                    ),
                    style = Stroke(width = width.toPx())
                )
            }
        }
}

// -----------------------------------------------------------------------------
// 3. PARALLAX SHIELD HERO
// -----------------------------------------------------------------------------
@Composable
fun ParallaxShieldHero(count: Int) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    
    // Sensor State
    var roll by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Smooth minimal filtering
                    roll = roll * 0.9f + (it.values[1] * 0.05f) * 0.1f 
                    pitch = pitch * 0.9f + (it.values[0] * 0.05f) * 0.1f
                }
            }
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
        }
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    // Parallax Offsets
    val offX = roll * 40f 
    val offY = pitch * 40f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Glow (static anchor)
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFDAA520).copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        
        // 3D Layer 1: Shield Base
        Box(
            modifier = Modifier
                .offset { IntOffset((offX * 0.5f).toInt(), (offY * 0.5f).toInt()) }
                .size(80.dp)
                .background(Color(0xFFDAA520).copy(alpha = 0.1f), CircleShape)
                .border(1.dp, Color(0xFFDAA520).copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Layer 2: Icon (Moves more)
            Image(
                painter = painterResource(id = R.drawable.ic_premium_crown),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .offset { IntOffset(offX.toInt(), offY.toInt()) }
                    .shimmer(), // Premium Glow
                colorFilter = ColorFilter.tint(Color(0xFFDAA520))
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 4. ALIVE CARD (Touch Physics)
// -----------------------------------------------------------------------------
@Composable
fun AliveCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    // Physics Anims
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "Scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFDAA520) else Color.Transparent,
        animationSpec = tween(300), label = "Border"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.1f else 0f,
        animationSpec = tween(500), label = "Glow"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { 
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { 
                        // Haptic Tick
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onClick() 
                    }
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121212)) // Base Surface
            .drawBehind {
                if (isSelected) {
                    drawRect(Color(0xFFDAA520).copy(alpha = glowAlpha)) // Inner glow
                }
            }
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Selection Light Beam (Conditional)
        if (isSelected) {
            Box(Modifier.matchParentSize().borderBeam(width = 2.dp, shape = RoundedCornerShape(16.dp)))
        }
        
        Box(Modifier.padding(16.dp)) {
            content()
        }
    }
}

// -----------------------------------------------------------------------------
// 5. COMMAND ISLAND (Multi-Select Header)
// -----------------------------------------------------------------------------
@Composable
fun CommandIsland(
    count: Int,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close Action
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
            
            // Status
            Text(
                text = "$count Selected",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            // Done Action (Gold Text)
            TextButton(onClick = onDone) {
                Text("DONE", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
            }
        }
    }
}
