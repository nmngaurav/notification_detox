package aura.notification.filter.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import aura.notification.filter.R
import aura.notification.filter.util.UpdateStatus
import aura.notification.filter.util.shimmer

@Composable
fun UpdateDialog(
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onBackground: () -> Unit = {},
    status: UpdateStatus = UpdateStatus.IDLE,
    progress: Float = 0f
) {
    val accentColor = Color(0xFFDAA520)

    Dialog(onDismissRequest = if (status == UpdateStatus.DOWNLOADING) ({}) else onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111111)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Golden Crown Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_premium_crown),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).shimmer(),
                        colorFilter = ColorFilter.tint(accentColor)
                    )
                }

                // Title
                Text(
                    text = when(status) {
                        UpdateStatus.DOWNLOADING -> "Downloading Update"
                        UpdateStatus.DOWNLOADED -> "Update Ready"
                        else -> "New Version Available"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                // Description
                Text(
                    text = when(status) {
                        UpdateStatus.DOWNLOADING -> "Please wait while we prepare the latest version of Aura for you."
                        UpdateStatus.DOWNLOADED -> "The update is ready to install. Restart Aura to apply changes."
                        else -> "A new version of Aura is available with improved filtering and performance. Update now for the best experience."
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                if (status == UpdateStatus.DOWNLOADING) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = accentColor,
                            trackColor = Color.DarkGray,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        
                        TextButton(onClick = onBackground) {
                            Text("Continue in Background", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else if (status == UpdateStatus.DOWNLOADED) {
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Restart to Update", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                        ) {
                            Text("Later", color = Color.Gray)
                        }

                        Button(
                            onClick = onUpdate,
                            modifier = Modifier.weight(1.2f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Update", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
