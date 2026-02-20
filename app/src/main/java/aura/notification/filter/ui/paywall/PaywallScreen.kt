package aura.notification.filter.ui.paywall

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import aura.notification.filter.R
import androidx.compose.foundation.Image
import aura.notification.filter.util.shimmer
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import aura.notification.filter.billing.BillingManager


@Composable
fun PaywallScreen(
    billingManager: aura.notification.filter.billing.BillingManager,
    analyticsManager: aura.notification.filter.util.AnalyticsManager,
    onClose: () -> Unit,
    isOnboarding: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val productDetailsList by billingManager.productDetails.collectAsState()
    
    // Logic to extract offers from aura_basic_sub
    val product = productDetailsList.find { it.productId == "aura_basic_sub" }
    val offers = product?.subscriptionOfferDetails ?: emptyList()
    
    // Map offers to our 2 tiers (1y and 6m)
    // Fallback static prices if Play Store fails to load
    val yearlyOffer = offers.find { it.basePlanId == "1y" || it.offerId == "1y" }
    val sixMonthOffer = offers.find { it.basePlanId == "6m" || it.offerId == "6m" }
    
    val yearlyPrice = yearlyOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "₹300"
    val sixMonthPrice = sixMonthOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "₹500"
    
    var selectedTier by remember { mutableStateOf<String>("1y") }
    
    // Soft Paywall Logic (Silent circular progress over 5s)
    val timerProgress = remember { Animatable(0f) }
    val isTimerDone by remember { derivedStateOf { timerProgress.value >= 1f } }
    
    val isPro by billingManager.isPro.collectAsState(initial = false)
    
    LaunchedEffect(isPro) {
        if (isPro && isOnboarding) {
            onClose()
        }
    }

    LaunchedEffect(isOnboarding) {
        if (isOnboarding) {
            timerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    // Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse"
    )

    aura.notification.filter.ui.components.AuraBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Hero Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .background(Color(0xFFDAA520).copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, Color(0xFFDAA520).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_premium_crown),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).shimmer(),
                            colorFilter = ColorFilter.tint(Color(0xFFDAA520))
                        )
                    }
                    
                    LaunchedEffect(Unit) {
                        analyticsManager.logPaywallView(if (isOnboarding) "onboarding" else "settings")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Elevate to Aura Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Premium AI Protection • Non-recurring",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFDAA520).copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Benefits List
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BenefitItem("Unlimited App Configuration")
                        BenefitItem("Unlock Custom Filter Keywords")
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Plan Selector
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlanCard(
                            modifier = Modifier.weight(1f),
                            title = "Ultimate Access",
                            duration = "1 Year",
                            price = yearlyPrice,
                            isSelected = selectedTier == "1y",
                            isBestValue = true,
                            onClick = { 
                                selectedTier = "1y"
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                analyticsManager.logSubscriptionSelect("1y")
                            }
                        )
                        PlanCard(
                            modifier = Modifier.weight(1f),
                            title = "Standard Access",
                            duration = "6 Months",
                            price = sixMonthPrice,
                            isSelected = selectedTier == "6m",
                            isBestValue = false,
                            onClick = { 
                                selectedTier = "6m"
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                analyticsManager.logSubscriptionSelect("6m")
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // CTA Button
                    val currentOffer = if (selectedTier == "1y") yearlyOffer else sixMonthOffer
                    
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            analyticsManager.logPurchaseStart(selectedTier)
                            currentOffer?.offerToken?.let { token ->
                                 billingManager.launchBillingFlow(context as Activity, token)
                            } ?: run {
                                analyticsManager.logPurchaseFail(selectedTier, "OFFER_TOKEN_NULL")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520)),
                        enabled = true
                    ) {
                        Text(
                            text = "Upgrade to Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Secure Google Play Payment • Instant Activation",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                // Subtle cross icon with circular progress — top-right corner (onboarding only)
                if (isOnboarding) {
                    val crossAlpha by animateFloatAsState(
                        targetValue = if (isTimerDone) 0.45f else 0.25f,
                        animationSpec = tween(durationMillis = 400),
                        label = "crossAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp)
                            .size(32.dp)
                            .then(
                                if (isTimerDone) {
                                    Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onClose()
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track circle (very subtle)
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.08f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width, size.height)
                            )
                        }

                        // Animated progress arc filling over 5s
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.18f),
                                startAngle = -90f,
                                sweepAngle = 360f * timerProgress.value,
                                useCenter = false,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width, size.height)
                            )
                        }

                        // Cross icon
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isTimerDone) "Skip" else null,
                            tint = Color.White.copy(alpha = crossAlpha),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

    }
}

@Composable
fun PlanCard(
    modifier: Modifier,
    title: String,
    duration: String,
    price: String,
    isSelected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFFDAA520) else Color(0xFF222222),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        if (isSelected) Color(0xFFDAA520).copy(alpha = 0.1f) else Color(0xFF0A0A0A),
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isBestValue) {
                Surface(
                    color = Color(0xFFDAA520),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        "BEST VALUE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(duration, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(price, color = Color(0xFFDAA520), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Check, 
            contentDescription = null, 
            tint = Color(0xFFDAA520), 
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
    }
}
