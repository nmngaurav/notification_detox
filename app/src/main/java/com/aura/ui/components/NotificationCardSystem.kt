package com.aura.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class NotificationCard(
    var position: Offset,
    var targetPosition: Offset,
    var velocity: Offset,
    var rotation: Float,
    var targetRotation: Float,
    val type: NotificationType,
    val appName: String,
    val title: String,
    val preview: String,
    val color: Color,
    var alpha: Float = 0.7f,
    val orbitAngle: Float,      // Starting angle for orbit
    val orbitSpeed: Float,      // Individual orbit speed
    val orbitRadius: Float,     // Individual orbit radius multiplier
    val iconData: Any? = null,  // Stores app icon (Drawable or ImageBitmap)
    val isUrgent: Boolean = false // Priority card (Bank, UPI, etc.)
)

enum class NotificationType {
    SOCIAL,
    MESSAGING,
    PROMOTIONAL,
    NEWS,
    UTILITY
}

enum class SwarmState {
    CHAOS,      // Orbiting center chaotically
    REPEL,      // Pushed to outer ring
    CONTRACT    // Contracting to finger
}

class NotificationCardSystem(private val count: Int = 30) {
    private val cards = mutableListOf<NotificationCard>()
    private val random = Random(System.currentTimeMillis())
    private var frameCount = 0L
    
    private val templates = listOf(
        NotificationType.SOCIAL to Triple("Instagram", "john liked your photo", "Just now"),
        NotificationType.SOCIAL to Triple("Instagram", "sarah commented", "Nice shot! 🔥"),
        NotificationType.SOCIAL to Triple("Facebook", "3 new friend requests", "See who"),
        NotificationType.MESSAGING to Triple("WhatsApp", "Good morning", "Hey! How are you?"),
        NotificationType.MESSAGING to Triple("WhatsApp", "Ok", "👍"),
        NotificationType.MESSAGING to Triple("Telegram", "New message", "Lol 😂"),
        NotificationType.PROMOTIONAL to Triple("Amazon", "50% OFF SALE", "Ends tonight!"),
        NotificationType.PROMOTIONAL to Triple("Zomato", "Get 40% off", "Order now"),
        NotificationType.NEWS to Triple("News", "Breaking news", "Tap to read"),
        NotificationType.UTILITY to Triple("Amazon", "Package delivered", "Track order"),
        NotificationType.UTILITY to Triple("Gmail", "New email", "Important")
    )
    
    private val typeColors = mapOf(
        NotificationType.SOCIAL to Color(0xFFE1306C),
        NotificationType.MESSAGING to Color(0xFF25D366),
        NotificationType.PROMOTIONAL to Color(0xFFFF9800),
        NotificationType.NEWS to Color(0xFF2196F3),
        NotificationType.UTILITY to Color(0xFF9C27B0)
    )

    fun init(width: Float, height: Float, appInfoList: List<com.aura.util.AppInfoManager.AppInfo>? = null) {
        cards.clear()
        val center = Offset(width / 2, height / 2)
        
        // If we have real apps, use them. Otherwise fall back to templates.
        val dataSource = if (appInfoList != null && appInfoList.isNotEmpty()) {
            // Mix real apps with templates to ensure variety
            val realAppCards = appInfoList.take(count - 2).map { appInfo ->
                // Infer type from app name
                val type = when {
                    appInfo.label.contains("Instagram", ignoreCase = true) || 
                    appInfo.label.contains("Facebook", ignoreCase = true) -> NotificationType.SOCIAL
                    appInfo.label.contains("WhatsApp", ignoreCase = true) || 
                    appInfo.label.contains("Telegram", ignoreCase = true) -> NotificationType.MESSAGING
                    appInfo.label.contains("Gmail", ignoreCase = true) -> NotificationType.UTILITY
                    appInfo.label.contains("Amazon", ignoreCase = true) || 
                    appInfo.label.contains("Zomato", ignoreCase = true) -> NotificationType.PROMOTIONAL
                    else -> NotificationType.NEWS
                }
                
                // Generate preview text based on type
                val (title, preview) = when (type) {
                    NotificationType.SOCIAL -> "New post" to "Someone tagged you"
                    NotificationType.MESSAGING -> "New message" to "Hey! 👋"
                    NotificationType.PROMOTIONAL -> "Special offer" to "Get 50% off"
                    NotificationType.NEWS -> "Breaking news" to "Tap to read"
                    NotificationType.UTILITY -> "New email" to "Important"
                }
                
                Triple(type, appInfo, Pair(title, preview))
            }
            
            // Add 1-2 urgent priority cards (Bank/UPI/Security)
            // Use real urgent apps if found, otherwise generic fallback
            val realUrgentApps = appInfoList.filter { app ->
                val label = app.label.lowercase()
                val pkg = app.packageName.lowercase()
                // Simple urgent heuristics
                label.contains("bank") || label.contains("pay") || label.contains("upi") || 
                label.contains("wallet") || label.contains("auth") || label.contains("secure") ||
                pkg.contains("bank") || pkg.contains("pay")
            }

            val urgentCards = if (realUrgentApps.isNotEmpty()) {
                realUrgentApps.take(2).map { app ->
                    Triple(NotificationType.UTILITY, app, Pair("Security Alert", "Tap to review activity"))
                }
            } else {
                // Fallback Generic for International/General users
                listOf(
                    Triple(NotificationType.UTILITY, 
                        com.aura.util.AppInfoManager.AppInfo("Bank Alert", null, ""),
                        Pair("Transaction Alert", "$500.00 debited")),
                    Triple(NotificationType.UTILITY,
                        com.aura.util.AppInfoManager.AppInfo("Security", null, ""),
                        Pair("Login Attempt", "New device detected"))
                )
            }
            
            realAppCards + urgentCards
        } else {
            // Fallback to templates if no app list provided
            templates.map { (type, data) ->
                val (appName, title, preview) = data
                Triple(type, com.aura.util.AppInfoManager.AppInfo(appName, null, ""), Pair(title, preview))
            }
        }
        
        dataSource.take(count).forEachIndexed { index, (type, appInfo, textData) ->
            val (title, preview) = textData
            
            // Each card gets unique orbit parameters
            val angle = (index.toFloat() / count) * 2f * PI.toFloat()
            val radiusMultiplier = 0.5f + random.nextFloat() * 1.0f // 0.5 to 1.5 (more spread)
            val speed = 0.005f + random.nextFloat() * 0.01f // Varied speeds
            
            // Start spread across entire screen
            val startRadius = (minOf(width, height) / 2.5f) * radiusMultiplier
            val startPos = Offset(
                center.x + cos(angle) * startRadius,
                center.y + sin(angle) * startRadius * 1.3f // More vertical spread
            )
            
            // Check if this is an urgent card
            val isUrgent = appInfo.label.contains("Bank", ignoreCase = true) || 
                          appInfo.label.contains("Pay", ignoreCase = true) ||
                          appInfo.label.contains("Security", ignoreCase = true) ||
                          appInfo.label.contains("Auth", ignoreCase = true) ||
                          appInfo.label.contains("Wallet", ignoreCase = true)
            
            cards.add(
                NotificationCard(
                    position = startPos,
                    targetPosition = startPos,
                    velocity = Offset.Zero,
                    rotation = random.nextFloat() * 10f - 5f,
                    targetRotation = 0f,
                    type = type,
                    appName = appInfo.label,
                    title = title,
                    preview = preview,
                    color = typeColors[type] ?: Color.Gray,
                    alpha = 0.7f,
                    orbitAngle = angle,
                    orbitSpeed = speed,
                    orbitRadius = radiusMultiplier,
                    iconData = appInfo.icon, // Real app icon
                    isUrgent = isUrgent
                )
            )
        }
    }

    fun update(
        width: Float,
        height: Float,
        touchPoint: Offset?,
        state: SwarmState,
        vaporizeFactor: Float,
        breatheFactor: Float = 0f
    ) {
        frameCount++
        val center = Offset(width / 2, height / 2)
        val time = frameCount * 0.016f // ~60fps timing
        
        cards.forEachIndexed { index, card ->
            when {
                vaporizeFactor > 0f -> {
                    // VAPORIZE: Blast outward
                    val dirX = card.position.x - center.x
                    val dirY = card.position.y - center.y
                    val dist = sqrt(dirX * dirX + dirY * dirY).coerceAtLeast(1f)
                    
                    card.velocity = Offset(
                        dirX / dist * vaporizeFactor * 40f,
                        dirY / dist * vaporizeFactor * 40f
                    )
                    card.alpha = (1f - vaporizeFactor * 1.5f).coerceAtLeast(0f)
                    card.rotation += vaporizeFactor * 8f
                    
                    // Apply velocity
                    card.position = Offset(
                        card.position.x + card.velocity.x,
                        card.position.y + card.velocity.y
                    )
                }
                
                state == SwarmState.CONTRACT && touchPoint != null -> {
                    // CONTRACT: Form tight ring around finger
                    // URGENT cards go to exact center (radius = 0)
                    val targetRadius = if (card.isUrgent) 0f else 120f
                    val angleOffset = (index.toFloat() / count) * 2f * PI.toFloat()
                    val currentAngle = angleOffset + time * 0.3f // Slow rotation
                    
                    card.targetPosition = if (card.isUrgent) {
                        // Urgent: snap to exact touch point
                        touchPoint
                    } else {
                        // Normal: orbit around touch point
                        Offset(
                            touchPoint.x + cos(currentAngle) * targetRadius,
                            touchPoint.y + sin(currentAngle) * targetRadius
                        )
                    }
                    card.targetRotation = 0f
                    card.alpha = lerp(card.alpha, 1f, 0.1f) // Fade to full opacity
                    
                    // Smooth lerp
                    card.position = Offset(
                        lerp(card.position.x, card.targetPosition.x, 0.12f),
                        lerp(card.position.y, card.targetPosition.y, 0.12f)
                    )
                    card.rotation = lerp(card.rotation, card.targetRotation, 0.15f)
                }
                
                state == SwarmState.REPEL && touchPoint != null -> {
                    // REPEL: Push to outer ring at screen edge
                    val screenRadius = minOf(width, height) / 2 - 80f
                    
                    // Calculate angle from touch point to card
                    val dx = card.position.x - touchPoint.x
                    val dy = card.position.y - touchPoint.y
                    val angleFromTouch = atan2(dy, dx)
                    
                    // Target is on outer ring, away from touch
                    card.targetPosition = Offset(
                        center.x + cos(angleFromTouch) * screenRadius,
                        center.y + sin(angleFromTouch) * screenRadius
                    )
                    card.alpha = lerp(card.alpha, 0.9f, 0.05f)
                    
                    // Smooth lerp to target
                    card.position = Offset(
                        lerp(card.position.x, card.targetPosition.x, 0.08f),
                        lerp(card.position.y, card.targetPosition.y, 0.08f)
                    )
                    
                    // Rotation based on movement
                    card.targetRotation = (card.velocity.x * 1.5f).coerceIn(-8f, 8f)
                    card.rotation = lerp(card.rotation, card.targetRotation, 0.05f)
                }
                
                else -> {
                    // CHAOS: Floating across entire screen
                    val orbitTime = time * card.orbitSpeed * 60f + card.orbitAngle
                    
                    // Use screen-proportional radius (fill whole screen) with breathing
                    val breatheMult = 1f + breatheFactor * 0.15f
                    val baseRadiusX = (width / 2.5f) * card.orbitRadius * breatheMult
                    val baseRadiusY = (height / 3f) * card.orbitRadius * breatheMult
                    
                    // Add slight wobble for organic feel
                    val wobbleX = sin(orbitTime * 2f) * 30f
                    val wobbleY = cos(orbitTime * 1.5f) * 20f
                    
                    card.targetPosition = Offset(
                        center.x + cos(orbitTime) * baseRadiusX + wobbleX,
                        center.y + sin(orbitTime) * baseRadiusY + wobbleY
                    )
                    card.alpha = lerp(card.alpha, 0.7f, 0.02f)
                    
                    // Smooth follow
                    card.position = Offset(
                        lerp(card.position.x, card.targetPosition.x, 0.03f),
                        lerp(card.position.y, card.targetPosition.y, 0.03f)
                    )
                    
                    // Gentle rotation oscillation
                    card.targetRotation = sin(orbitTime) * 8f
                    card.rotation = lerp(card.rotation, card.targetRotation, 0.02f)
                }
            }
            
            // Update velocity for next frame (for rotation calculation)
            card.velocity = Offset(
                card.targetPosition.x - card.position.x,
                card.targetPosition.y - card.position.y
            )
        }
    }
    
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
    
    fun getCards(): List<NotificationCard> = cards
}
