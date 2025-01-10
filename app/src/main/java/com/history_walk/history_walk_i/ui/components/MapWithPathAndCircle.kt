package com.history_walk.history_walk_i.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
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


@Composable
fun MapWithPathAndCircle(
    episodeId: Int,
    stepCount: Int,
    numberOfStepsPerEpisode: Int
) {
    val context = LocalContext.current
    val fraction = 1_000 * (stepCount.toFloat() / numberOfStepsPerEpisode.toFloat()).coerceIn(0f, 1f)
    val maxScale = 10f
    val minScale = 1f
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }


    var mapResourceId = remember(episodeId) {
        if (episodeId == 1) { R.drawable.the_alhambra }
        else { R.drawable.placeholder }
    }
    var painter: Painter = painterResource(id = mapResourceId)
    val intrinsicSize = painter.intrinsicSize
    val aspectRatio = if (intrinsicSize.height != 0f) {
        intrinsicSize.width / intrinsicSize.height
    } else { 1f }


    var pathPoints by remember { mutableStateOf(emptyList<PathPoint>()) }
    LaunchedEffect(episodeId) {
        pathPoints = if (episodeId == 1) {
            loadPathPoints(context, R.raw.points_of_path_of_the_alhambra)
        } else {
            emptyList()
        }
    }
    val listOfOffsets = remember(pathPoints) {
        pathPoints.map { Offset(it.x, it.y) }
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
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
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val scaleFactorX = containerWidth / intrinsicSize.width
        val scaleFactorY = containerHeight / intrinsicSize.height
        val scaledPathPoints = remember(listOfOffsets, scaleFactorX, scaleFactorY) {
            listOfOffsets.map { pt ->
                Offset(pt.x * scaleFactorX, pt.y * scaleFactorY)
            }
        }
        val path = remember(scaledPathPoints) {
            Path().apply {
                if (scaledPathPoints.isNotEmpty()) {
                    moveTo(scaledPathPoints[0].x, scaledPathPoints[0].y)
                    for (i in 1 until scaledPathPoints.size) {
                        lineTo(scaledPathPoints[i].x, scaledPathPoints[i].y)
                    }
                }
            }
        }
        val totalDistance = scaledPathPoints
            .zipWithNext { p1, p2 -> (p2 - p1).getDistance() }
            .sum()
        val distanceAlongPath = fraction * totalDistance
        var distanceAccum = 0f
        var circleCenter = scaledPathPoints.firstOrNull() ?: Offset.Zero
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
        Image(
            painter = painter,
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
            drawPath(
                path = path,
                color = Color.Red,
                style = Stroke(
                    width = 1f,
                    cap = StrokeCap.Round
                )
            )
            drawCircle(
                color = Color.Blue,
                radius = 12f,
                center = circleCenter
            )
        }
    }
}