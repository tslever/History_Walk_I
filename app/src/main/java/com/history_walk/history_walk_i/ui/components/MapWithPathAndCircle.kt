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
        Offset(2176f, 2477f),
        Offset(2194f,2424f),
        Offset(2200f,2372f),
        Offset(2200f,2341f),
        Offset(2192f,2302f),
        Offset(2163f,2239f),
        Offset(2103f,2185f),
        Offset(1987f,2114f),
        Offset(1658f,1954f),
        Offset(1630f,1943f),
        Offset(1535f,1884f),
        Offset(1476f,1838f),
        Offset(1465f,1820f),
        Offset(1411f,1785f),
        Offset(1386f,1781f),
        Offset(1349f,1770f),
        Offset(1349f,1745f),
        Offset(1357f,1718f),
        Offset(1315f,1720f),
        Offset(1297f,1721f),
        Offset(1237f,1705f),
        Offset(1097f,1679f),
        Offset(1058f,1660f),
        Offset(1028f,1630f),
        Offset(1013f,1597f),
        Offset(1022f,1556f),
        Offset(1008f,1532f),
        Offset(864f,1453f),
        Offset(822f,1453f),
        Offset(803f, 1221f),
        Offset(803f,1107f),
        Offset(799f,1092f),
        Offset(780f,1087f),
        Offset(727f,1104f),
        Offset(735f,1130f),
        Offset(709f,1146f),
        Offset(710f,1154f),
        Offset(716f,1155f),
        Offset(735f,1151f),
        Offset(740f,1172f),
        Offset(729f, 1181f),
        Offset(719f,1184f),
        Offset(727f,1290f),
        Offset(723f,1325f),
        Offset(716f,1331f),
        Offset(523f,1380f),
        Offset(512f,1380f),
        Offset(373f,1424f),
        Offset(372f,1448f),
        Offset(361f,1455f),
        Offset(349f,1454f),
        Offset(343f,1416f),
        Offset(324f,1408f),
        Offset(271f,1423f),
        Offset(264f,1375f)
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
                    width = 1f,
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
