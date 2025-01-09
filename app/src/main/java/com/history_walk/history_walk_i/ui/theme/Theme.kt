package com.history_walk.history_walk_i.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


val GoldenYellow = Color(0xFFFFC004)
val TransparentGrey = Color(0x80D9D9D9)


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAAAAAA),
    onPrimary = Color.Black,
    secondary = Color(0xFFBBBBBB),
    onSecondary = Color.Black,
    tertiary = Color(0xFFCCCCCC),
    onTertiary = Color.Black,
    background = Color(0xFF222222),
    onBackground = Color.White,
    surface = Color(0xFF333333),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF444444),
    onSurfaceVariant = Color.White,
    outline = Color.Gray
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD9D9D9),
    onPrimary = Color.Black,
    secondary = Color(0xFFC0C0C0),
    onSecondary = Color.Black,
    tertiary = Color(0xFFBEBEBE),
    onTertiary = Color.Black,
    background = Color(0xFFE0E0E0),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F2),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFDDDDDD),
    onSurfaceVariant = Color.Black,
    outline = Color.Gray
)


val shapes = Shapes(
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp)
)


@Composable
fun ThemeForHistoryWalkI(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}