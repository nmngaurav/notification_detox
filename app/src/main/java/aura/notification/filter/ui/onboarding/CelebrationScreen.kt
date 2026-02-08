package aura.notification.filter.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import aura.notification.filter.ui.components.ParticleSystem
import kotlinx.coroutines.delay

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CelebrationScreen(
    navController: NavController,
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val particleSystem = remember { ParticleSystem(200) } // Confetti count
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
        // CRITICAL: Mark onboarding as complete ONLY here at the end of the flow
        onboardingViewModel.completeOnboarding()
        
        delay(2500) // Stay for 2.5s
        navController.navigate("dashboard") {
            popUpTo(0) // Clear backstack
        }
    }
    
    // Confetti Animation (Reusable ParticleSystem)
    // We'll customize it slightly for "Confetti" (Gold/White)
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        // Simple Particle Explosion
        ConfettiExplosion()
        
        // Text
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.fadeIn(tween(500)) + androidx.compose.animation.scaleIn(tween(500))
        ) {
            Text(
                "You're all set!",
                color = Color(0xFFDAA520),
                fontWeight = FontWeight.Black,
                fontSize = 32.sp
            )
        }
    }
}

@Composable
fun ConfettiExplosion() {
    val particles = remember { List(100) { ConfettiParticle() } }
    val frame = remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while(System.currentTimeMillis() - startTime < 3000) {
            particles.forEach { it.update() }
            frame.value++
            delay(16)
        }
    }
    
    Canvas(Modifier.fillMaxSize()) {
        frame.value // Recompose trigger
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        
        particles.forEach { p ->
            if (p.active) {
                // Init position at center if fresh
                if (p.x == 0f) { p.x = centerX; p.y = centerY }
                
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = androidx.compose.ui.geometry.Offset(p.x, p.y)
                )
            }
        }
    }
}

class ConfettiParticle {
    var x = 0f
    var y = 0f
    var vx = (Math.random() - 0.5f).toFloat() * 20f
    var vy = (Math.random() - 0.5f).toFloat() * 20f
    var size = (Math.random() * 8 + 4).toFloat()
    var color = if (Math.random() > 0.5) Color(0xFFDAA520) else Color.White
    var active = true
    
    fun update() {
        x += vx
        y += vy
        vy += 0.5f // Gravity
        size *= 0.98f // Shrink
        if (size < 0.5f) active = false
    }
}
