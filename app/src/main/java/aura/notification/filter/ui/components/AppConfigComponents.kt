package aura.notification.filter.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuraSegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = options.indexOf(selectedOption)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF151515))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val color by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Gray,
                    animationSpec = tween(300), label = "color"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOptionSelected(option) }
                        .then(
                            if (isSelected) Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFDAA520).copy(alpha = 0.2f), Color.Transparent)
                                )
                            ).border(1.dp, Color(0xFFDAA520).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = color,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TagTile(
    label: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFDAA520).copy(alpha = 0.12f) else Color(0xFF1A1A1A),
        animationSpec = tween(350), label = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFDAA520) else Color(0xFF2A2A2A),
        animationSpec = tween(350), label = "border"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFDAA520) else Color.Gray,
        animationSpec = tween(350), label = "icon"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = Color(0xFFDAA520), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            color = Color.Gray,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFFDAA520).copy(alpha = 0.8f),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

// --- LEGACY HELPERS (For BatchConfigScreen compatibility) ---

@Composable
fun ModeOption(title: String, desc: String, icon: ImageVector, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if(selected) color else Color.Gray)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if(selected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = color))
    }
}
