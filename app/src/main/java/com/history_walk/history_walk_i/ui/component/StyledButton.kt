import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp


@Composable
fun StyledButton(
    onClick: () -> Unit,
    text: String,
    textStyle: TextStyle
) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = buttonColors(containerColor = Color(0xFFD9D9D9)),
        modifier = Modifier
            .border(width = 1.dp, color = Color.Black)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Text(text = text, style = textStyle, color = Color.Black)
    }
}