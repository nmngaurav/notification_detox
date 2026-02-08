package aura.notification.filter.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import aura.notification.filter.ai.HeuristicEngine
import aura.notification.filter.data.ShieldLevel
import aura.notification.filter.ui.MainViewModel
import aura.notification.filter.ui.components.GlassCard
// PREMIUM COMPONENTS
import aura.notification.filter.ui.components.AuroraBackground
import aura.notification.filter.ui.components.ParallaxShieldHero
import aura.notification.filter.ui.components.borderBeam
import androidx.compose.ui.draw.scale
// FlowRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchConfigScreen(
    navController: NavController,
    packageNames: String, // Comma separated
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val packages = remember(packageNames) { packageNames.split(",").filter { it.isNotEmpty() } }
    
    // --- State ---
    var selectedLevel by remember { mutableStateOf(ShieldLevel.SMART) }
    var selectedTags by remember { mutableStateOf(setOf(HeuristicEngine.TAG_SECURITY)) } // Default safe
    var customKeywords by remember { mutableStateOf("") }
    
    val isPro by mainViewModel.isPro.collectAsState()
    var isSaving by remember { mutableStateOf(false) }

    val heuristicEngine = remember { HeuristicEngine() }
    val categorizedTags = remember { heuristicEngine.getCategorizedTags() }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent, // Let Aurora shine
            bottomBar = {
                Box(Modifier.padding(24.dp)) {
                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                mainViewModel.applyBulkConfig(
                                    packageNames = packages,
                                    profileId = mainViewModel.currentMode.value.name,
                                    shieldLevel = selectedLevel,
                                    categories = selectedTags.joinToString(","),
                                    keywords = customKeywords
                                )
                                // Navigate to Celebration
                                navController.navigate("celebration") {
                                    popUpTo("onboarding/app_picker") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDAA520),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                    ) {
                        if (isSaving) {
                            // Custom Spinner to avoid crash
                            val infiniteTransition = rememberInfiniteTransition(label = "Spinner")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing)
                                ), label = "SpinnerAngle"
                            )
                            Canvas(modifier = Modifier.size(24.dp)) {
                                rotate(angle) {
                                    drawArc(
                                        color = Color.Black,
                                        startAngle = 0f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                        } else {
                            Text("FINISH SETUP", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 120.dp) // Space for button
            ) {
                // --- 1. HERO (Parallax) ---
                item {
                    Spacer(Modifier.height(16.dp))
                    ParallaxShieldHero(count = packages.size)
                    
                    Text(
                        text = "Unified Protection",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Your selected apps will follow these golden rules.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    )
                }

                // --- 2. MODE SELECTOR (Redesigned) ---
                item {
                    val options = listOf("Smart Filter", "Block All")
                    val currentOption = if (selectedLevel == ShieldLevel.SMART) "Smart Filter" else "Block All"

                    aura.notification.filter.ui.components.AuraSegmentedControl(
                        options = options,
                        selectedOption = currentOption,
                        onOptionSelected = { option ->
                            selectedLevel = if (option == "Smart Filter") ShieldLevel.SMART else ShieldLevel.FORTRESS
                        }
                    )
                    Spacer(Modifier.height(32.dp))
                }

                // --- 3. CATEGORIZED TAGS (Premium Redesign) ---
                if (selectedLevel == ShieldLevel.SMART) {
                    categorizedTags.forEach { (category, tags) ->
                        item {
                            aura.notification.filter.ui.components.SectionHeader(category)
                        }
                        
                        items(tags) { metadata ->
                            val isSelected = selectedTags.contains(metadata.id)
                            val tagIcon = when(metadata.id) {
                                HeuristicEngine.TAG_SECURITY -> Icons.Default.Lock
                                HeuristicEngine.TAG_MONEY -> Icons.Default.Star
                                HeuristicEngine.TAG_UPDATES -> Icons.Default.Settings
                                HeuristicEngine.TAG_MESSAGES -> Icons.Default.Email
                                HeuristicEngine.TAG_MENTIONS -> Icons.Default.AccountCircle
                                HeuristicEngine.TAG_CALLS -> Icons.Default.Call
                                HeuristicEngine.TAG_ORDERS -> Icons.Default.ShoppingCart
                                HeuristicEngine.TAG_SCHEDULE -> Icons.Default.Info
                                HeuristicEngine.TAG_PROMOS -> Icons.Default.Star
                                HeuristicEngine.TAG_NEWS -> Icons.Default.Info
                                else -> Icons.Default.Info
                            }

                            aura.notification.filter.ui.components.TagTile(
                                label = metadata.label,
                                description = metadata.description,
                                icon = tagIcon,
                                isSelected = isSelected,
                                onClick = {
                                    selectedTags = if (isSelected) selectedTags - metadata.id else selectedTags + metadata.id
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }

                    // --- 4. KEYWORDS (Pass-Through Triggers) ---
                    item {
                        aura.notification.filter.ui.components.SectionHeader("Pass-Through Triggers")
                        Text("Notifications with these exact words will always break through.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                        
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                             Column(modifier = Modifier.padding(16.dp)) {
                                 if (isPro) {
                                     val keywordsList = customKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                     
                                     if (keywordsList.isNotEmpty()) {
                                         aura.notification.filter.ui.settings.SimpleFlowRow(horizontalGap = 8.dp, verticalGap = 8.dp) {
                                             keywordsList.forEach { k ->
                                                 Surface(
                                                     shape = RoundedCornerShape(8.dp),
                                                     color = Color(0xFFDAA520).copy(alpha = 0.1f),
                                                     border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDAA520).copy(alpha = 0.3f)),
                                                     modifier = Modifier.clickable { 
                                                         customKeywords = (keywordsList - k).joinToString(",")
                                                     }
                                                 ) {
                                                     Row(
                                                         modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Text(k, color = Color(0xFFDAA520), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                         Spacer(Modifier.width(6.dp))
                                                         Icon(Icons.Default.Close, null, tint = Color(0xFFDAA520).copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                                     }
                                                 }
                                             }
                                         }
                                         Spacer(Modifier.height(16.dp))
                                     }
                                     
                                     // Input Button (Match AppConfigSheet style)
                                     var showAddDialog by remember { mutableStateOf(false) }
                                     OutlinedButton(
                                         onClick = { showAddDialog = true },
                                         modifier = Modifier.fillMaxWidth(),
                                         shape = RoundedCornerShape(12.dp),
                                         colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDAA520)),
                                         border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDAA520).copy(alpha = 0.4f))
                                     ) {
                                         Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                         Spacer(Modifier.width(8.dp))
                                         Text("Add Priority Keyword")
                                     }
                                     
                                     if (showAddDialog) {
                                         KeywordDialog(onDismiss = { showAddDialog = false }) { newWord ->
                                             if (newWord.isNotBlank()) {
                                                 val current = customKeywords.split(",").filter { it.isNotBlank() }
                                                 customKeywords = (current + newWord.trim()).joinToString(",")
                                             }
                                             showAddDialog = false
                                         }
                                     }
                                 } else {
                                     Row(
                                         verticalAlignment = Alignment.CenterVertically,
                                         modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navController.navigate("paywall") }
                                     ) {
                                         Icon(Icons.Default.Lock, null, tint = Color(0xFFDAA520), modifier = Modifier.size(20.dp))
                                         Spacer(Modifier.width(12.dp))
                                         Text("Upgrade to add triggers like 'Salary' or 'Mom'.", color = Color.Gray, fontSize = 13.sp)
                                     }
                                 }
                             }
                        }
                    }
                }
            }
        }
    }
}
