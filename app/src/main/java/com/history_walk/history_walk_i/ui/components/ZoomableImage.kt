package com.history_walk.history_walk_i.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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

    BoxWithConstraints(modifier = modifier) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->

                        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                        val scaleFactor = newScale / scale
                        scale = newScale

                        val panSensitivity = 2.0f
                        offset += pan * scaleFactor * panSensitivity

                        val scaledWidth = containerWidth * scale
                        val scaledHeight = containerHeight * scale

                        val maxX = (scaledWidth - containerWidth) / 2
                        val maxY = (scaledHeight - containerHeight) / 2

                        val clampedOffsetX = if (scaledWidth > containerWidth) {
                            offset.x.coerceIn(-maxX, maxX)
                        } else {
                            0f
                        }
                        val clampedOffsetY = if (scaledHeight > containerHeight) {
                            offset.y.coerceIn(-maxY, maxY)
                        } else {
                            0f
                        }
                        offset = Offset(clampedOffsetX, clampedOffsetY)
                    }
                }
        )
    }
}