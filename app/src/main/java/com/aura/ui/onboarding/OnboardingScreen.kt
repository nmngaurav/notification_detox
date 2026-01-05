package com.aura.ui.onboarding

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.aura.service.AuraNotificationService
import com.aura.ui.components.ParticleSystem
import kotlinx.coroutines.delay

private enum class OnboardingStep {
    SWARM, PAIN_POINTS, SHIELD_CONFIG, GATEWAY
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardingStep.SWARM) }
    
    // Permission State
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var hasNotificationAccess by remember { mutableStateOf(false) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val componentName = ComponentName(context, AuraNotificationService::class.java)
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
            OnboardingStep.SWARM -> SwarmStage(
                onComplete = { currentStep = OnboardingStep.PAIN_POINTS }
            )
            OnboardingStep.PAIN_POINTS -> PainPointsStage(
                onComplete = { currentStep = OnboardingStep.SHIELD_CONFIG }
            )
            OnboardingStep.SHIELD_CONFIG -> OnboardingShieldConfigScreen(
                onContinue = { currentStep = OnboardingStep.GATEWAY }
            )
            OnboardingStep.GATEWAY -> GatewayStage(
                hasPermission = hasNotificationAccess,
                onRequestPermission = {
                     context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onEnter = {
                    viewModel.completeOnboarding()
                    onFinish()
                }
            )
        }
    }
}

// ------------------------------------
// STEP 1: THE SWARM (Chaos & Cleansing)
// ------------------------------------
@Composable
fun SwarmStage(onComplete: () -> Unit) {
    // Get real apps for swarm
    val context = androidx.compose.ui.platform.LocalContext.current
    val appInfoManager = remember {
        com.aura.util.AppInfoManager(context)
    }
    val appList = remember { appInfoManager.getSmartOnboardingApps() }
    
    // States
    var isStabilizing by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var screenSize by remember { mutableStateOf(Pair(0f, 0f)) }
    
    // Animations
    val vaporizeAnim = remember { Animatable(0f) }
    val notificationSystem = remember { com.aura.ui.components.NotificationCardSystem(30) }
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
                    isStabilizing -> com.aura.ui.components.SwarmState.CONTRACT
                    touchPoint != null -> com.aura.ui.components.SwarmState.REPEL
                    else -> com.aura.ui.components.SwarmState.CHAOS
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
            onComplete() // NAVIGATE TO SHIELD CONFIG
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
                com.aura.ui.components.NotificationCardComposable(card = card)
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
        
        // Progress Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = vaporizeAnim.value
            
            if (holdProgress > 0f && touchPoint != null && progress < 0.1f) {
                 drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = 200f,
                    center = touchPoint!!,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
                drawArc(
                    color = Color(0xFFDAA520),
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    topLeft = touchPoint!!.minus(Offset(200f, 200f)),
                    size = androidx.compose.ui.geometry.Size(400f, 400f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            // Vaporize Flash
            if (progress > 0f) {
                 drawCircle(
                    color = Color(0xFFDAA520).copy(alpha = progress * 0.8f), 
                    radius = size.maxDimension * progress * 2f,
                    center = center
                )
            }
        }
        
        // Dynamic Text
        if (vaporizeAnim.value < 0.1f) {
            val alpha by remember(frameTick) { mutableFloatStateOf(0.5f + 0.3f * kotlin.math.sin(frameTick / 20.0).toFloat()) }
            
            val displayText = when {
                isStabilizing -> "" // No text during hold (progress ring visible)
                touchPoint != null -> "HOLD TO TAKE CONTROL"
                else -> "YOUR NOTIFICATIONS NEVER STOP"
            }
            
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = displayText,
                    color = Color.White.copy(alpha = alpha),
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
    val particleSystem = remember { com.aura.ui.components.ParticleSystem(1000) }
    
    // Pager State
    val pagerState = rememberPagerState(pageCount = { 4 })
    
    // Haptic on Page Change
    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }
    
    // Physics Parameters based on Page using derivedStateOf for performance
    val targetPhysics by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> Triple(Color.White, 3f, 0.5f)    // Page 0: Chaos (White, Fast, some jitter)
                1 -> Triple(Color.Gray, 0.2f, 0.0f)   // Page 1: Sluggish (Gray, Slow, no jitter)
                2 -> Triple(Color(0xFFEF5350), 8f, 2f) // Page 2: Stress (Red, Super Fast, High Jitter)
                3 -> Triple(Color(0xFFDAA520), 1f, 0.1f) // Page 3: Solution (Gold, Flow, Low jitter)
                else -> Triple(Color.White, 1f, 0f)
            }
        }
    }

    // Background Gradients
    val targetGradient by remember {
        derivedStateOf {
            when (pagerState.currentPage) {
                0 -> listOf(Color(0xFF222222), Color(0xFF000000)) // Chaos
                1 -> listOf(Color(0xFF263238), Color(0xFF000000)) // Fog
                2 -> listOf(Color(0xFF4B0000), Color(0xFF100000)) // Stress (Deep Red)
                3 -> listOf(Color(0xFF5D4037), Color(0xFF1B0000)) // Hope (Warm Gold/Brown)
                else -> listOf(Color.Black, Color.Black)
            }
        }
    }
    val bgStart by animateColorAsState(targetGradient[0], tween(1000), label = "bgStart")
    val bgEnd by animateColorAsState(targetGradient[1], tween(1000), label = "bgEnd")
    
    // Canvas Size
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Animation Loop
    LaunchedEffect(Unit) {
        particleSystem.init(1000f, 2000f) // Init with dummy until layout
        while (true) {
            if (canvasSize.width > 0) {
                 particleSystem.updatePainPoints(
                    width = canvasSize.width,
                    height = canvasSize.height,
                    targetColor = targetPhysics.first,
                    speedMultiplier = targetPhysics.second,
                    turbulence = targetPhysics.third
                )
            }
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(bgStart, bgEnd)
                )
            )
    ) {
        // 1. Particle Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            particleSystem.getParticles().forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.alpha * 0.6f),
                    radius = p.size,
                    center = p.position
                )
            }
        }

        // 2. Content Pager
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                PainPointStoryPage(page, pagerState.currentPage == page, onComplete)
            }
            
            // Pager Indicators
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val color = if (isSelected) Color(0xFFDAA520) else Color.DarkGray
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = tween(300), 
                        label = "indicatorWidth"
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(8.dp)
                            .width(width)
                    )
                }
            }
        }
    }
}

@Composable
fun PainPointStoryPage(page: Int, isVisible: Boolean, onComplete: () -> Unit) {
    val content = when(page) {
        0 -> Triple("150", "Daily Unlocks", "Your focus is shattered every 12 minutes.")
        1 -> Triple("23m", "Lost Time", "To recover focus after just one buzz.")
        2 -> Triple("+40%", "Stress Level", "Phantom vibrations keep you on edge.")
        3 -> Triple("0", "Interruptions", "Reclaim your mind with Shield.")
        else -> Triple("", "", "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Big Stat
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(800)),
            exit = fadeOut()
        ) {
            Text(
                text = content.first,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (page == 2) Color(0xFFEF5350) else if (page == 3) Color(0xFFDAA520) else Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(800, delayMillis = 200)),
            exit = fadeOut()
        ) {
            Text(
                text = content.second.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Description
        AnimatedVisibility(
             visible = isVisible,
             enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(800, delayMillis = 400)),
             exit = fadeOut()
        ) {
            Text(
                text = content.third,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
        
        // CTA Button on last page
        if (page == 3) {
            Spacer(modifier = Modifier.height(48.dp))
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(tween(500, delayMillis = 800)) + fadeIn(tween(500, delayMillis = 800))
            ) {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDAA520)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "ACTIVATE SHIELD",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}


// ------------------------------------
// STEP 3: GATEWAY (Permission)
// ------------------------------------
@Composable
fun GatewayStage(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onEnter: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF332200), Color.Black),
                    radius = 2000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            
            // Dynamic Title
            AnimatedContent(targetState = hasPermission, label = "GatewayTitle") { granted ->
                if (granted) {
                     Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                    )
                } else {
                    Text(
                        text = "ENABLE AURA", // Changed from GRANT ACCESS
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            com.aura.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Privacy First",
                        color = Color(0xFFDAA520),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Trust Indicators
                    TrustIndicatorRow(
                        icon = "📱",
                        text = "Processing happens on your device"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TrustIndicatorRow(
                        icon = "🔒",
                        text = "Zero data sent to servers"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TrustIndicatorRow(
                        icon = "⚡",
                        text = "Works offline"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aura processes notifications locally to filter distractions. We need permission to intercept them.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedContent(targetState = hasPermission, label = "GatewayButton") { granted ->
                if (granted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Text(
                            text = "Aura is ready.",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = onEnter,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDAA520), // Gold for Entry
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.height(56.dp).fillMaxWidth(0.8f)
                        ) {
                            Text("ENTER AURA", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                     Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333), // Darker for action
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(56.dp).fillMaxWidth(0.8f).alpha(pulseAlpha)
                    ) {
                        Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Required for Smart Shield to work",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
