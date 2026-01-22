package aura.notification.filter.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // Show splash for 2.5 seconds (Premium feel)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(1500))
            ) {
                Text(
                    text = "AURA",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp
                    ),
                    color = Color(0xFFDAA520) // Gold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(2000, delayMillis = 500))
            ) {
                Text(
                    text = "Reclaim your attention",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Footer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(2000, delayMillis = 1500))
            ) {
                Text(
                    text = "Powered by AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}
