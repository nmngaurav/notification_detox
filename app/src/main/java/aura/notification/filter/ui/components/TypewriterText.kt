package aura.notification.filter.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    delayMillis: Long = 30,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.White
) {
    var textToDisplay by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        textToDisplay = ""
        text.forEach { char ->
            textToDisplay += char
            delay(delayMillis)
        }
    }

    Text(
        text = textToDisplay,
        modifier = modifier,
        style = style,
        color = color
    )
}
