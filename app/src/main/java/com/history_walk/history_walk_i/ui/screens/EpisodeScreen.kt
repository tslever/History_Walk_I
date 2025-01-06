package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import com.history_walk.history_walk_i.R
import com.history_walk.history_walk_i.ui.components.ZoomableImage
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI
import kotlin.math.hypot


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI,
    onGoToSettings: () -> Unit,
    onEpisodeCompleted: () -> Unit
) {

    val episodeImages = mapOf(
        1 to R.drawable.the_alhambra
    )
    val imageResource = episodeImages[episodeId]

    Box(modifier = Modifier.fillMaxSize()) {
        if (imageResource != null) {
            ZoomableImage(
                painter = painterResource(id = imageResource),
                contentDescription = "Episode $episodeId Image",
                modifier = Modifier.fillMaxSize()
            )
        }
        if (episodeId == 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p1 = Offset(size.width * 0.15f, size.height * 0.30f)
                val p2 = Offset(size.width * 0.40f, size.height * 0.20f)
                val p3 = Offset(size.width * 0.60f, size.height * 0.50f)
                val p4 = Offset(size.width * 0.80f, size.height * 0.70f)
                val points = listOf(p1, p2, p3, p4)
                val totalLength = (0 until points.size - 1)
                    .map { i -> distanceBetween(points[i], points[i + 1]) }
                    .sum()
                val fraction = 10_000f / 70_000f
                val targetDistance = fraction * totalLength
                val path = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                }
                val circleCenter = findPointAlongPath(points, targetDistance)
                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawCircle(
                    color = Color.Red,
                    center = circleCenter,
                    radius = 15f
                )
            }
        }
    }

    Button(
        onClick = {
            viewModel.incrementEpisodeIndex()
            onEpisodeCompleted()
        }
    ) {
        Text(text = "Complete")
    }
}


private fun distanceBetween(a: Offset, b: Offset): Float {
    return hypot(b.x - a.x, b.y - a.y)
}


private fun findPointAlongPath(points: List<Offset>, distance: Float): Offset {
    var remaining = distance
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val segmentLength = distanceBetween(p1, p2)
        if (remaining <= segmentLength) {
            val ratio = remaining / segmentLength
            val x = p1.x + ratio * (p2.x - p1.x)
            val y = p1.y + ratio * (p2.y - p1.y)
            return Offset(x, y)
        } else {
            remaining -= segmentLength
        }
    }
    return points.last()
}