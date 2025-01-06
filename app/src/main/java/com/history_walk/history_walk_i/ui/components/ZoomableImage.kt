package com.history_walk.history_walk_i.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ZoomableImage(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val maxScale = 5f
    val minScale = 1f

    val maxOffsetX = 500f
    val minOffsetX = -500f
    val maxOffsetY = 500f
    val minOffsetY = -500f

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    scale = (scale * zoom).coerceIn(minScale, maxScale)

                    val focalPoint = centroid - offset
                    val clampedX = (offset.x + pan.x + (centroid.x - focalPoint.x) * (1f - zoom)).coerceIn(minOffsetX, maxOffsetX)
                    val clampedY = (offset.y + pan.y + (centroid.y - focalPoint.y) * (1f - zoom)).coerceIn(minOffsetY, maxOffsetY)
                    offset = Offset(clampedX, clampedY)
                }
            }
    )
}