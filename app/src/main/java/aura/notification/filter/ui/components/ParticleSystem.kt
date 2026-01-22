package aura.notification.filter.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Particle(
    var position: Offset,
    var velocity: Offset,
    var basePosition: Offset, // Home position for "Zen" or "Shield" state
    var size: Float,
    var color: Color,
    var alpha: Float,
    var life: Float = 1.0f 
)

class ParticleSystem(private val count: Int = 2000) {
    private val particles = mutableListOf<Particle>()
    private val random = Random(System.currentTimeMillis())

    // Brand Noise Palette
    private val appColors = listOf(
        Color(0xFF1877F2), // FB Blue
        Color(0xFF25D366), // WA Green
        Color(0xFFEA4335), // Google Red
        Color(0xFFE1306C), // Insta Pink
        Color(0xFF4A154B)  // Slack Purple
    )

    fun init(width: Float, height: Float) {
        particles.clear()
        repeat(count) {
            val startX = random.nextFloat() * width
            val startY = random.nextFloat() * height
            
            particles.add(
                Particle(
                    position = Offset(startX, startY),
                    velocity = Offset.Zero,
                    basePosition = Offset(startX, startY),
                    size = random.nextFloat() * 4f + 2f, // Small dots: 2..6px
                    color = appColors.random(),
                    alpha = random.nextFloat() * 0.5f + 0.3f
                )
            )
        }
    }

    fun update(
        width: Float, 
        height: Float, 
        touchPoint: Offset?, 
        isStabilizing: Boolean,
        vaporizeFactor: Float // 0..1
    ) {
        val center = Offset(width / 2, height / 2)
        
        particles.forEach { p ->
            // 1. VAPORIZE (Death)
            if (vaporizeFactor > 0f) {
                // Expand outwards violently
                val dx = p.position.x - center.x
                val dy = p.position.y - center.y
                val angle = atan2(dy, dx)
                val speed = 50f * vaporizeFactor
                
                p.velocity += Offset(cos(angle) * speed, sin(angle) * speed)
                p.alpha *= 0.9f // Fade out
                p.position += p.velocity
                return@forEach
            }

            // 2. SWARM BEHAVIOR (Attract to center, but keep chaotic)
            if (!isStabilizing) {
                // Weak attraction to center (The Hive)
                val toCenter = center - p.position
                p.velocity += toCenter * 0.0005f 
                
                // Brownian Noise (Jitter)
                p.velocity += Offset(
                    (random.nextFloat() - 0.5f) * 1.5f,
                    (random.nextFloat() - 0.5f) * 1.5f
                )
            } else {
                // 3. STABILIZE BEHAVIOR (Shield Formation)
                // Push to edges, form a ring
                val dx = p.position.x - center.x
                val dy = p.position.y - center.y
                val dist = sqrt(dx*dx + dy*dy)
                val targetRadius = width * 0.4f
                
                if (dist < targetRadius) {
                    val angle = atan2(dy, dx)
                    val push = Offset(cos(angle), sin(angle)) * 2f
                    p.velocity += push
                } else {
                     // Orbit logic? For now just drag/friction to hold ring
                     p.velocity *= 0.9f
                }
            }

            // 4. TOUCH REPULSION (The "Shoo" effect)
            if (touchPoint != null) {
                val dx = p.position.x - touchPoint.x
                val dy = p.position.y - touchPoint.y
                val dist = sqrt(dx*dx + dy*dy)
                
                if (dist < 300f) { // Interaction Radius
                    val force = (300f - dist) / 300f * 5f // Stronger closer
                    val angle = atan2(dy, dx)
                    p.velocity += Offset(cos(angle) * force, sin(angle) * force)
                }
            }

            // Physics Update
            p.position += p.velocity
            p.velocity *= 0.95f // Friction

            // Walls (Bounce)
            if (p.position.x < 0 || p.position.x > width) p.velocity = p.velocity.copy(x = -p.velocity.x)
            if (p.position.y < 0 || p.position.y > height) p.velocity = p.velocity.copy(y = -p.velocity.y)
        }
    }

    fun updatePainPoints(
        width: Float,
        height: Float,
        targetColor: Color?, // If null, use original random colors
        speedMultiplier: Float,
        turbulence: Float // 0..1 (0 = flow, 1 = chaos)
    ) {
        particles.forEach { p ->
            // Color Morphing
            if (targetColor != null) {
                // Lerp towards target color
                // distinct r,g,b lerp for smoothness
                val r = p.color.red + (targetColor.red - p.color.red) * 0.05f
                val g = p.color.green + (targetColor.green - p.color.green) * 0.05f
                val b = p.color.blue + (targetColor.blue - p.color.blue) * 0.05f
                p.color = Color(r, g, b, p.alpha)
            }

            // Movement
            // Base flow (downwards/sideways drift)
            p.velocity += Offset(0.5f, 0.2f) * speedMultiplier * 0.1f

            // Turbulence (Random noise)
            if (turbulence > 0) {
                 p.velocity += Offset(
                    (random.nextFloat() - 0.5f) * turbulence * 2f,
                    (random.nextFloat() - 0.5f) * turbulence * 2f
                )
            }

            // Apply Physics
            p.position += p.velocity
            p.velocity *= 0.95f // Friction

            // Wrap around screen (Infinite flow)
            if (p.position.x > width) p.position = p.position.copy(x = 0f)
            if (p.position.x < 0) p.position = p.position.copy(x = width)
            if (p.position.y > height) p.position = p.position.copy(y = 0f)
            if (p.position.y < 0) p.position = p.position.copy(y = height)
        }
    }

    fun getParticles(): List<Particle> = particles
}
