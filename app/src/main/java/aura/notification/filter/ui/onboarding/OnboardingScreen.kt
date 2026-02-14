package aura.notification.filter.ui.onboarding
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.navigation.NavController
import aura.notification.filter.ui.components.borderBeam
import aura.notification.filter.service.BlockerNotificationService
import aura.notification.filter.ui.components.ParticleSystem
import kotlinx.coroutines.delay


private enum class OnboardingStep {
    CHAOS, PAIN_POINTS, GATEWAY, INITIATION
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onFinish: () -> Unit,
    startStep: String? = null,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    aura.notification.filter.ui.theme.AuraTheme {
    val context = LocalContext.current
    var currentStep by remember { 
        mutableStateOf(
            when(startStep) {
                "GATEWAY" -> OnboardingStep.GATEWAY
                else -> OnboardingStep.CHAOS
            }
        ) 
    }
    
    // Permission State
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var hasNotificationAccess by remember { mutableStateOf(false) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val componentName = ComponentName(context, aura.notification.filter.service.BlockerNotificationService::class.java)
                val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                hasNotificationAccess = flat != null && flat.contains(componentName.flattenToString())
                
                // Auto-advance REMOVED to allow user to acknowledge success.
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
        },
        label = "OnboardingNav"
    ) { step ->
        when (step) {
            OnboardingStep.CHAOS -> SwarmStage(onComplete = { currentStep = OnboardingStep.PAIN_POINTS })
            OnboardingStep.PAIN_POINTS -> PainPointsStage(onComplete = { currentStep = OnboardingStep.GATEWAY })
            OnboardingStep.GATEWAY -> GatewayStage(
                hasPermission = hasNotificationAccess,
                onRequestPermission = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    context.startActivity(intent)
                },
                onEnter = {
                    currentStep = OnboardingStep.INITIATION
                }
            )
            OnboardingStep.INITIATION -> InitiationStage(
                onComplete = {
                    // Regression Fix: Don't complete here. Wait for Celebration.
                    onFinish()
                }
            )
        }
    }
}
}

// ------------------------------------
// STEP 1: CHAOS (Environmental Cleansing)
// ------------------------------------
@Composable
fun SwarmStage(onComplete: () -> Unit) {
    // Get real apps for swarm
    val context = androidx.compose.ui.platform.LocalContext.current
    val appInfoManager = remember {
        aura.notification.filter.util.AppInfoManager(context)
    }
    val appList = remember { appInfoManager.getSmartOnboardingApps() }
    
    // States
    var isStabilizing by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var screenSize by remember { mutableStateOf(Pair(0f, 0f)) }
    
    // Animations
    val vaporizeAnim = remember { Animatable(0f) }
    val notificationSystem = remember { aura.notification.filter.ui.components.NotificationCardSystem(30) }
    val swarmTransition = rememberInfiniteTransition(label = "SwarmBreathe")
    val breatheFactor by swarmTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Breathe"
    )
    
    // Game Loop
    var frameTick by remember { mutableLongStateOf(0L) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Initialize when screen size is known
    LaunchedEffect(screenSize) {
        if (screenSize.first > 0 && screenSize.second > 0) {
            notificationSystem.init(screenSize.first, screenSize.second, appList)
        }
    }
    
    // Animation loop
    LaunchedEffect(Unit) {
        while (true) {
            if (screenSize.first > 0) {
                // Compute current state
                val currentState = when {
                    isStabilizing -> aura.notification.filter.ui.components.SwarmState.CONTRACT
                    touchPoint != null -> aura.notification.filter.ui.components.SwarmState.REPEL
                    else -> aura.notification.filter.ui.components.SwarmState.CHAOS
                }
                
                notificationSystem.update(
                    width = screenSize.first,
                    height = screenSize.second,
                    touchPoint = touchPoint,
                    state = currentState,
                    vaporizeFactor = vaporizeAnim.value,
                    breatheFactor = breatheFactor
                )
            }
            frameTick++
            delay(16)
        }
    }

    // Interaction vars
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var holdProgress by remember { mutableFloatStateOf(0f) }
    
    // Buzzing haptic effect every 2 seconds
    LaunchedEffect(Unit) {
        while (!isCompleted) {
            delay(2000)
            if (!isStabilizing) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        }
    }
    
    // Logic
    LaunchedEffect(isStabilizing) {
        if (isStabilizing && !isCompleted) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            val duration = 2500L 
            val startTime = System.currentTimeMillis()
            while (isStabilizing && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                delay(16)
            }
            if (holdProgress >= 1f) isCompleted = true
        } else if (!isCompleted) {
             while (!isStabilizing && holdProgress > 0f) {
                 holdProgress -= 0.05f 
                 delay(16)
             }
        }
    }
    
    // Success Sequence
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            vaporizeAnim.animateTo(1f, animationSpec = tween(1500, easing = LinearEasing))
            delay(500)
            onComplete() // NAVIGATE TO PAIN POINTS
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (!isCompleted) {
                            touchPoint = offset
                            isStabilizing = true
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove) 
                            tryAwaitRelease()
                            isStabilizing = false
                            touchPoint = null
                        }
                    }
                )
            }
    ) {
        // Get actual screen size in pixels
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        // Update screen size
        LaunchedEffect(widthPx, heightPx) {
            screenSize = Pair(widthPx, heightPx)
        }
        
        // Render Notification Cards
        val cards = notificationSystem.getCards()
        cards.forEach { card ->
            // Position is in pixels, convert to dp for offset
            val offsetX = with(density) { card.position.x.toDp() }
            val offsetY = with(density) { card.position.y.toDp() }
            
            Box(
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            ) {
                aura.notification.filter.ui.components.NotificationCardComposable(card = card)
            }
        }

        // Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        radius = kotlin.math.max(widthPx, heightPx) * 0.7f,
                        center = Offset(widthPx / 2f, heightPx / 2f)
                    )
                )
        )
        
        // Progress Ring & Premium Touch Feedback
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = vaporizeAnim.value
            
            if (holdProgress > 0f && touchPoint != null && progress < 0.1f) {
                // 1. Energy Core Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFDAA520).copy(alpha = 0.4f * holdProgress), Color.Transparent),
                        center = touchPoint!!,
                        radius = 300f * holdProgress
                    ),
                    center = touchPoint!!,
                    radius = 300f
                )

                // 2. Rotating Sci-Fi Brackets (Outer Ring)
                val baseOver = holdProgress * 360f
                val numBrackets = 3
                val sweep = 60f
                
                rotate(degrees = frameTick * 2f, pivot = touchPoint!!) {
                    for (i in 0 until numBrackets) {
                        val startAngle = (360f / numBrackets) * i
                        drawArc(
                            color = Color(0xFFDAA520).copy(alpha = 0.8f),
                            startAngle = startAngle,
                            sweepAngle = sweep * holdProgress,
                            useCenter = false,
                            topLeft = touchPoint!!.minus(Offset(180f, 180f)),
                            size = androidx.compose.ui.geometry.Size(360f, 360f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f, 
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }

                // 3. Inner Charging Ring (Fills up)
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    topLeft = touchPoint!!.minus(Offset(140f, 140f)),
                    size = androidx.compose.ui.geometry.Size(280f, 280f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                
                // 4. Particles sucking in (implosion) simulation via lines
                if (holdProgress > 0.2f) {
                    val numLines = 12
                    rotate(degrees = -frameTick * 3f, pivot = touchPoint!!) {
                        for (i in 0 until numLines) {
                            val angle = (360f / numLines) * i
                            val rad = 220f * (1f - (frameTick % 20 / 20f)) // ripples in
                            val start = Offset(
                                touchPoint!!.x + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * rad,
                                touchPoint!!.y + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * rad
                            )
                            val end = Offset(
                                touchPoint!!.x + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * (rad - 30f),
                                touchPoint!!.y + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * (rad - 30f)
                            )
                            drawLine(
                                color = Color(0xFFDAA520).copy(alpha = 0.5f),
                                start = start,
                                end = end,
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
            
            // Vaporize Flash
            if (progress > 0f) {
                 drawCircle(
                    color = Color(0xFFDAA520).copy(alpha = (1f - progress) * 0.8f), 
                    radius = size.maxDimension * progress * 1.5f,
                    center = center
                )
            }
        }
        
        // Dynamic Text
        if (vaporizeAnim.value < 0.1f) {
            val alpha by remember(frameTick) { mutableFloatStateOf(0.5f + 0.3f * kotlin.math.sin(frameTick / 20.0).toFloat()) }
            
            val displayText = when {
                isStabilizing -> "" // No text during hold (progress ring visible)
                touchPoint != null -> "KEEP HOLDING..."
                else -> "HOLD TO BEGIN AURA"
            }
            
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = displayText,
                    color = Color.White.copy(alpha = if (touchPoint != null) 1f else alpha), // Solid when holding
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}



// ------------------------------------
// STEP 2: PAIN POINTS (Realization)
// ------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PainPointsStage(onComplete: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val particleSystem = remember { aura.notification.filter.ui.components.ParticleSystem(1000) }
    
    // Pager State
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    // Haptic on Page Change
    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }
    
    // ENHANCED: Smooth Particle Physics Transitions
    // Particles now smoothly morph between emotional states
    val targetPhysics by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> Triple(Color(0xFF888888), 1.5f, 0.8f)  // Gray, chaotic
                1 -> Triple(Color(0xFFEF5350), 1.2f, 0.6f)  // Red, stressed  
                2 -> Triple(Color(0xFFFF6F00), 0.9f, 0.4f)  // Orange, anxious
                3 -> Triple(Color(0xFFDAA520), 0.5f, 0.2f)  // Gold, calm (solution)
                else -> Triple(Color.Gray, 1.0f, 0.5f)
            }
        }
    }
    
    // Animate particle properties for smooth transitions
    val particleColor by animateColorAsState(
        targetValue = targetPhysics.first,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "particleColor"
    )
    val particleSpeed by animateFloatAsState(
        targetValue = targetPhysics.second,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "particleSpeed"
    )
    val particleTurbulence by animateFloatAsState(
        targetValue = targetPhysics.third,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "particleTurbulence"
    )

    // Background Gradients (matching particle emotional themes)
    // We will use a dynamic Nebula effect instead of a static gradient
    
    // Use BoxWithConstraints to ensure we get size immediately
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        // NEBULA BACKGROUND EFFECT
        val nebulaColor by animateColorAsState(targetPhysics.first, tween(1500))
        val infiniteTransition = rememberInfiniteTransition(label = "Nebula")
        val nebulaOffset1 by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f, 
            animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "N1"
        )
        val nebulaOffset2 by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f, 
            animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "N2"
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Aurora Effect (Soft, diffuse, no hard circles)
            drawRect(Color.Black)
            
            val auroraX = size.width * (0.2f + 0.6f * nebulaOffset1)
            val auroraY = size.height * 0.4f
            
            // Soft diffuse glow 1
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(nebulaColor.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(auroraX, auroraY),
                    radius = size.maxDimension * 0.8f
                )
            )
            
            // Soft diffuse glow 2 (Counter)
            val auroraX2 = size.width * (0.8f - 0.6f * nebulaOffset2)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(nebulaColor.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(auroraX2, size.height * 0.7f),
                    radius = size.maxDimension * 0.9f
                )
            )
            
            // Bottom ambient glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, nebulaColor.copy(alpha = 0.15f)),
                    startY = size.height * 0.5f,
                    endY = size.height
                )
            )
        }

        // CRITICAL FIX: Initialize particles SYNCHRONOUSLY
        val ctx = androidx.compose.ui.platform.LocalContext.current
        remember {
            if (particleSystem.getParticles().isEmpty()) {
                val displayMetrics = ctx.resources.displayMetrics
                particleSystem.init(displayMetrics.widthPixels.toFloat(), displayMetrics.heightPixels.toFloat())
            }
            true
        }

        LaunchedEffect(widthPx, heightPx) {
            if (widthPx > 0 && heightPx > 0 && particleSystem.getParticles().isEmpty()) {
                particleSystem.init(widthPx, heightPx)
            }
        }

        var frameCounter by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                if (widthPx > 0) {
                     particleSystem.updatePainPoints(
                        width = widthPx,
                        height = heightPx,
                        targetColor = particleColor,
                        speedMultiplier = particleSpeed,
                        turbulence = particleTurbulence
                    )
                    frameCounter++
                }
                delay(16)
            }
        }

        // Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            frameCounter.let { } 
            particleSystem.getParticles().forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.alpha * 0.6f),
                    radius = p.size,
                    center = p.position
                )
            }
        }

        // Content Pager
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                PainPointStoryPage(page, pagerState.currentPage == page, onComplete)
            }
            
            // Pager Indicators
            Row(
                Modifier.wrapContentHeight().fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val color = if (isSelected) Color(0xFFDAA520) else Color.DarkGray
                    val width by animateDpAsState(targetValue = if (isSelected) 32.dp else 8.dp, animationSpec = tween(300))
                    
                    Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).height(8.dp).width(width))
                }
            }
        }
    }
}

@Composable
fun PainPointStoryPage(
    page: Int, 
    isSelected: Boolean, 
    onComplete: () -> Unit
) {
    val content = when(page) {
        0 -> Triple("~150", "Phone Unlocks", "Triggered by notifications daily.")
        1 -> Triple("+40%", "Stress Level", "Phantom vibrations keep you on edge.")
        2 -> Triple("0", "Interruptions", "Reclaim your mind with Aura.")
        else -> Triple("", "", "")
    }
    
    val slideY by animateDpAsState(
        targetValue = if (isSelected) 0.dp else 50.dp,
        animationSpec = tween(500, delayMillis = 200)
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center, // Perfect centering
            modifier = Modifier.fillMaxWidth()
        ) {
            // CINEMATIC DECODER STAT
            if (isSelected) {
                DecoderText(
                    text = content.first,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 64.sp
                    ),
                    color = Color.White
                )
            } else {
                 Text(text = " ", style = MaterialTheme.typography.displayLarge, fontSize = 64.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = content.second.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFDAA520),
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = slideY)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = content.third,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = slideY)
            )
            
            if (page == 2) {
                 Spacer(modifier = Modifier.height(48.dp))
                 AnimatedVisibility(
                     visible = isSelected,
                     enter = fadeIn(tween(1000, delayMillis = 500)) + scaleIn()
                 ) {
                     Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.height(56.dp).width(200.dp)
                    ) {
                        Text("CONTINUE", fontWeight = FontWeight.Bold)
                    }
                 }
            }
        }
    }
}

@Composable
fun DecoderText(text: String, style: TextStyle, color: Color) {
    var displayedText by remember { mutableStateOf("") }
    val chars = "0123456789%~+".toList()
    
    LaunchedEffect(text) {
        // Scramble effect
        val length = text.length
        for (i in 0..15) { // scramble frames
            displayedText = (1..length).map { chars.random() }.joinToString("")
            delay(50)
        }
        displayedText = text // Final reveal
    }
    
    Text(text = displayedText, style = style, color = color)
}

// ------------------------------------
// STEP 3: SETUP (Access)
// ------------------------------------
@Composable
fun GatewayStage(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onEnter: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp) // Adjusted padding
        ) {
            // Push content down to center it vertically (More spacing)
            Spacer(modifier = Modifier.weight(0.65f))
            
            Text(
                text = "ACTIVATE AURA",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp)) // Increased spacing
            
            // Primary Action Button (Premium Design)
            AnimatedContent(targetState = hasPermission, label = "GatewayButton") { granted ->
                if (granted) {
                    Button(
                        onClick = onEnter,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDAA520),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.height(60.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text("START", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                } else {
                     Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDAA520), 
                            contentColor = Color(0xFF1A0000) // Deep dark brown text for premium look
                        ),
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth()
                            .scale(pulseScale)
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(16.dp)) // Gold border
                            .borderBeam(width = 2.dp, shape = RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp, pressedElevation = 4.dp)
                    ) {
                        Text("ACTIVATE NOW", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(56.dp)) // Increased spacing
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How to enable:",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
                PermissionTutorial()
            }
            
            // Push Privacy Card to Bottom
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Privacy Context (Refined & Bottom Stick)
            aura.notification.filter.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF111111)
            ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color(0xFFDAA520)),
                            modifier = Modifier.size(24.dp)
                        )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Zero Data Collection",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your personal data stays on this device. Aura operates locally and privately, respecting your digital privacy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionTutorial() {
    // Animation Steps: 
    // 0: List View
    // 1: Click Aura
    // 2: Toggle Switch
    // 3: Dialog Allow
    val transition = rememberInfiniteTransition(label = "Tutorial")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing), // Slower for clarity
            repeatMode = RepeatMode.Restart
        ), label = "Step"
    )
    
    val currentStep = step.toInt()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121212)) // Android dark theme bg
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fake Toolbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (currentStep < 2) "Device & App Notifications" else "Notification Access",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Content
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (currentStep < 2) {
                    // LIST VIEW
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Dummy App
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.3f)) {
                            Box(Modifier.size(32.dp).background(Color.Gray, CircleShape))
                            Spacer(Modifier.width(16.dp))
                            Box(Modifier.height(10.dp).width(100.dp).background(Color.Gray, RoundedCornerShape(5.dp)))
                        }
                        
                        // Aura App (Highlighted)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (currentStep == 1) Color(0xFFDAA520).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).background(Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = aura.notification.filter.R.mipmap.ic_launcher_round),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text("Aura", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(if(currentStep > 0) "Not allowed" else "Not allowed", color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        // Hand Indicator
                        if (currentStep == 1) {
                             Icon(
                                imageVector = Icons.Default.Check, // Using Check as a 'touch' indicator for now or just the highlight is enough
                                contentDescription = null,
                                tint = Color(0xFFDAA520),
                                modifier = Modifier.align(Alignment.End).offset(y = (-10).dp)
                            )
                        }
                    }
                } else {
                    // DETAIL TOGGLE VIEW
                    Column {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = aura.notification.filter.R.mipmap.ic_launcher_round),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Allow notification access", color = Color.White, fontSize = 16.sp)
                                Text("Aura", color = Color.Gray, fontSize = 12.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            
                            // Toggle Switch
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(28.dp)
                                    .background(
                                        if (currentStep >= 3) Color(0xFFDAA520) else Color.DarkGray, 
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = if (currentStep >= 3) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(24.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                        
                        // Allow Dialog Simulation
                         if (currentStep == 3) {
                             Spacer(Modifier.height(16.dp))
                             Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                             ) {
                                 Text("Allow Aura to access notifications?", color = Color.White, fontSize = 12.sp)
                             }
                         }
                    }
                }
            }
        }
    }
}

// ------------------------------------
// STEP 4: SCAN (Finding Apps)
// ------------------------------------
@Composable
fun InitiationStage(onComplete: () -> Unit) {
    val context = LocalContext.current
    val appInfoManager = remember { aura.notification.filter.util.AppInfoManager(context) }
    // We only want user apps for the visualization
    val apps = remember { 
        appInfoManager.getInstalledApps().filter { !appInfoManager.isSystemApp(it.packageName) }.shuffled() 
    }
    
    // States
    var scanState by remember { mutableStateOf("SCANNING") } // SCANNING, FOUND, READY
    var detectedCount by remember { mutableIntStateOf(0) }
    
    // Animations
    val transition = rememberInfiniteTransition(label = "Scanner")
    val radarRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "Radar"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Pulse"
    )
    
    // Logic Sequence
    LaunchedEffect(Unit) {
        delay(2500) // Scan for 2.5s
        scanState = "FOUND"
        
        // Count up animation
        val target = apps.take(20).size // Just show a realistic number
        val start = System.currentTimeMillis()
        while(detectedCount < target) {
            detectedCount++
            delay(50)
        }
        
        delay(500)
        scanState = "READY"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // RADAR VISUALIZATION
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val maxRadius = size.minDimension * 0.4f
            
            // Pulse Rings
            drawCircle(
                color = Color(0xFFDAA520).copy(alpha = pulseAlpha * 0.5f),
                radius = maxRadius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
             drawCircle(
                color = Color(0xFFDAA520).copy(alpha = pulseAlpha * 0.3f),
                radius = maxRadius * 0.6f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            
            // Sweep Gradient
            rotate(degrees = radarRotation) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color(0xFFDAA520).copy(alpha = 0.5f)
                    ),
                    radius = maxRadius
                )
            }
        }
        
        // CENTRAL HUB & ICONS
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Spacer(modifier = Modifier.height(100.dp))
             
             // Dynamic Icon Stream
             if (scanState == "SCANNING" && apps.isNotEmpty()) {
                 Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                     var currentIconIndex by remember { mutableIntStateOf(0) }
                     LaunchedEffect(Unit) {
                         while(true) {
                             currentIconIndex = (currentIconIndex + 1) % apps.size
                             delay(150) // Fast cycle
                         }
                     }
                     
                     val app = apps[currentIconIndex]
                     if (app.icon != null) {
                        androidx.compose.ui.viewinterop.AndroidView(
                             factory = { ctx -> android.widget.ImageView(ctx) },
                             update = { view -> 
                                 view.setImageDrawable(app.icon) 
                                 view.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                             },
                             modifier = Modifier.size(64.dp).alpha(0.8f)
                        )
                     }
                 }
             } else {
                 // Result Icon
                  Icon(
                     imageVector = Icons.Default.Check,
                     contentDescription = null,
                     tint = Color(0xFFDAA520),
                     modifier = Modifier.size(80.dp)
                 )
             }
             
             Spacer(modifier = Modifier.height(32.dp))
             
             // TEXT UPDATES
             AnimatedContent(targetState = scanState, label = "ScanText") { state ->
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     when(state) {
                         "SCANNING" -> {
                             Text("FINDING APPS...", color = Color.Gray, style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
                             Spacer(Modifier.height(8.dp))
                             Text("Finding apps that send notifications...", color = Color.DarkGray, fontSize = 12.sp)
                         }
                         "FOUND", "READY" -> {
                             Text("ANALYSIS COMPLETE", color = Color(0xFFDAA520), style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                             Spacer(Modifier.height(16.dp))
                             Text(
                                 "Potential distraction sources identified.", 
                                 color = Color.White, 
                                 style = MaterialTheme.typography.titleMedium,
                                 textAlign = TextAlign.Center
                             )
                         }
                     }
                 }
             }
        }
        
        // BOTTOM ACTION
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = scanState == "READY",
                enter = slideInVertically { it } + fadeIn()
            ) {
                 Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDAA520),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .borderBeam(width = 2.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Select Apps to Filter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
