package com.history_walk.history_walk_i.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.R
import kotlin.math.sqrt

@Composable
fun MapWithPathAndCircle(modifier: Modifier) {

    val fraction = 10_000f / 70_000f
    val minScale = 1f
    val maxScale = 5f

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val originalPathPoints = listOf(
        Offset(200f, 200f),
        Offset(600f, 200f),
        Offset(600f, 450f),
        Offset(950f, 450f)
    )

    val alhambraPainter = painterResource(id = R.drawable.the_alhambra)
    val intrinsicSize = alhambraPainter.intrinsicSize
    val imageAspectRatio = intrinsicSize.width / intrinsicSize.height

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(imageAspectRatio)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val scaleFactor = newScale / scale
                    scale = newScale
                    val panSensitivity = 3f
                    offset += pan * scaleFactor * panSensitivity
                }
            }
    ) {
        Image(
            painter = alhambraPainter,
            contentDescription = "The Alhambra Map",
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.FillBounds
        )
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            val scaleFactorX = size.width / intrinsicSize.width
            val scaleFactorY = size.height / intrinsicSize.height
            val pathPoints = originalPathPoints.map { pt ->
                Offset(
                    x = pt.x * scaleFactorX,
                    y = pt.y * scaleFactorY
                )
            }
            val path = Path().apply {
                moveTo(pathPoints[0].x, pathPoints[0].y)
                for (i in 1 until pathPoints.size) {
                    lineTo(pathPoints[i].x, pathPoints[i].y)
                }
            }
            drawPath(
                path = path,
                color = Color.Red,
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round
                )
            )
            val totalDistance = pathPoints.zipWithNext { p1, p2 -> (p2 - p1).length() }.sum()
            val distanceAlongPath = fraction * totalDistance
            var distanceAccum = 0f
            var circleCenter = pathPoints.first()
            for (i in 0 until pathPoints.lastIndex) {
                val p1 = pathPoints[i]
                val p2 = pathPoints[i + 1]
                val segmentLength = (p2 - p1).length()
                if (distanceAccum + segmentLength >= distanceAlongPath) {
                    val remaining = distanceAlongPath - distanceAccum
                    val t = remaining / segmentLength
                    circleCenter = Offset(
                        x = p1.x + t * (p2.x - p1.x),
                        y = p1.y + t * (p2.y - p1.y)
                    )
                    break
                }
                distanceAccum += segmentLength
            }
            drawCircle(
                color = Color.Blue,
                radius = 12f,
                center = circleCenter
            )
        }
    }
}

private fun Offset.length(): Float {
    return sqrt(x * x + y * y)
}
