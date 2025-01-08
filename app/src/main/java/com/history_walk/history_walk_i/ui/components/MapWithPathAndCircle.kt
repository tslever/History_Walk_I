package com.history_walk.history_walk_i.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.history_walk.history_walk_i.R
import kotlin.math.sqrt

@Composable
fun MapWithPathAndCircle(episodeId: Int) {

    val context = LocalContext.current
    val fraction = 10_000f / 70_000f
    val maxScale = 5f
    val minScale = 1f
    val modifier = Modifier.fillMaxSize()
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }


    var painterResource: Painter = painterResource(id = R.drawable.placeholder)
    if (episodeId == 1) {
        painterResource = painterResource(id = R.drawable.the_alhambra)
    }
    val intrinsicSize = painterResource.intrinsicSize
    val aspectRatio = intrinsicSize.width / intrinsicSize.height


    val pathPoint = PathPoint(0f, 0f)
    val listOfPathPoint: List<PathPoint> = listOf(pathPoint)
    var listOfPathPoints by remember { mutableStateOf(listOfPathPoint) }
    LaunchedEffect(Unit) {
        if (episodeId == 1) {
            listOfPathPoints = loadPathPoints(context, R.raw.points_of_path_of_the_alhambra)
        }
    }
    val listOfOffsets = listOfPathPoints.map { Offset(it.x, it.y) }


    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
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
            painter = painterResource,
            contentDescription = "map",
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
            val scaledPathPoints = listOfOffsets.map { pt ->
                Offset(
                    x = pt.x * scaleFactorX,
                    y = pt.y * scaleFactorY
                )
            }
            val path = Path().apply {
                moveTo(scaledPathPoints[0].x, scaledPathPoints[0].y)
                for (i in 1 until scaledPathPoints.size) {
                    lineTo(scaledPathPoints[i].x, scaledPathPoints[i].y)
                }
            }
            drawPath(
                path = path,
                color = Color.Red,
                style = Stroke(
                    width = 1f,
                    cap = StrokeCap.Round
                )
            )
            val totalDistance = scaledPathPoints.zipWithNext { p1, p2 -> (p2 - p1).getDistance() }.sum()
            val distanceAlongPath = fraction * totalDistance
            var distanceAccum = 0f
            var circleCenter = scaledPathPoints.first()
            for (i in 0 until scaledPathPoints.lastIndex) {
                val p1 = scaledPathPoints[i]
                val p2 = scaledPathPoints[i + 1]
                val segmentLength = (p2 - p1).getDistance()
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


private fun Offset.getDistance(): Float {
    return sqrt(this.x * this.x + this.y * this.y)
}