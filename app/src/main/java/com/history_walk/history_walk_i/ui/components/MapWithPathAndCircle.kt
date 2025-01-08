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
        Offset(264f,1375f),
        Offset(277f,1369f),
        Offset(350f,1348f),
        Offset(355f,1365f),
        Offset(390f, 1362f),
        Offset(427f,1349f),
        Offset(424f,1323f),
        Offset(415f,1306f),
        Offset(412f,1290f),
        Offset(462f,1281f),
        Offset(718f,1049f),
        Offset(776f,1035f),
        Offset(785f,1021f),
        Offset(842f,1006f),
        Offset(858f,1012f),
        Offset(871f,1024f),
        Offset(894f,1054f),
        Offset(984f, 1084f),
        Offset(1029f,1083f),
        Offset(1131f,1066f),
        Offset(1170f,1064f),
        Offset(1259f,1048f),
        Offset(1325f,1030f),
        Offset(1340f,1115f),
        Offset(1353f,1163f),
        Offset(1365f,1174f),
        Offset(1410f,1168f),
        Offset(1390f,1092f),
        Offset(1386f,1050f),
        Offset(1365f,1011f),
        Offset(1360f,980f),
        Offset(1368f,972f),
        Offset(1410f,963f),
        Offset(1416f,1024f),
        Offset(1430f,1024f),
        Offset(1432f,1046f),
        Offset(1444f,1048f),
        Offset(1461f,1044f),
        Offset(1456f,1017f),
        Offset(1474f,1016f),
        Offset(1472f,975f),
        Offset(1521f,965f),
        Offset(1514f,879f),
        Offset(1486f,875f),
        Offset(1480f,824f),
        Offset(1490f,809f),
        Offset(1501f,801f),
        Offset(1518f,802f),
        Offset(1529f,816f),
        Offset(1534f,863f),
        Offset(1520f,877f),
        Offset(1525f,955f),
        Offset(1529f,965f),
        Offset(1539f,967f),
        Offset(1573f,958f),
        Offset(1582f,1000f),
        Offset(1596f, 1000f),
        Offset(1603f,997f),
        Offset(1599f,978f),
        Offset(1658f,971f),
        Offset(1665f,987f),
        Offset(1645f,991f),
        Offset(1656f,1051f),
        Offset(1662f,1075f),
        Offset(1684f,1075f),
        Offset(1682f,1059f),
        Offset(1696f,1057f),
        Offset(1688f,1006f),
        Offset(1684f,1003f),
        Offset(1683f,993f),
        Offset(1689f,987f),
        Offset(1705f,987f),
        Offset(1714f,984f),
        Offset(1712f,964f),
        Offset(1697f,955f),
        Offset(1688f,955f),
        Offset(1682f,919f),
        Offset(1694f,914f),
        Offset(1701f,907f),
        Offset(1719f,919f),
        Offset(1732f,915f),
        Offset(1745f,922f),
        Offset(1750f,935f),
        Offset(1743f,945f),
        Offset(1730f,951f),
        Offset(1731f,974f),
        Offset(1741f,1025f),
        Offset(1730f,1039f),
        Offset(1742f,1057f),
        Offset(1752f, 1110f),
        Offset(1746f,1078f),
        Offset(1659f, 1093f),
        Offset(1676f,1192f),
        Offset(1764f,1177f),
        Offset(1769f,1202f),
        Offset(1761f,1215f),
        Offset(1773f,1224f),
        Offset(1786f,1214f),
        Offset(1773f,1198f),
        Offset(1769f,1175f),
        Offset(1853f,1161f),
        Offset(1855f,1173f),
        Offset(1846f,1176f),
        Offset(1849f, 1196f),
        Offset(1917f,1189f),
        Offset(1913f,1162f),
        Offset(1909f,1151f),
        Offset(1906f,1100f),
        Offset(1913f,1087f),
        Offset(2000f,1068f),
        Offset(2029f,1223f),
        Offset(2010f,1223f),
        Offset(2013f,1254f),
        Offset(1989f,1259f),
        Offset(1964f,1258f),
        Offset(1948f,1262f),
        Offset(1936f,1267f),
        Offset(1932f,1272f),
        Offset(1944f,1333f),
        Offset(1946f,1361f),
        Offset(1910f,1370f),
        Offset(1869f,1370f),
        Offset(1790f,1364f),
        Offset(1737f,1364f),
        Offset(1693f,1366f),
        Offset(1685f,1368f),
        Offset(1678f,1372f),
        Offset(1670f,1378f),
        Offset(1657f,1392f),
        Offset(1643f,1411f),
        Offset(1639f,1434f),
        Offset(1643f,1460f),
        Offset(1653f,1468f),
        Offset(1690f,1483f),
        Offset(1810f,1489f),
        Offset(1824f,1492f),
        Offset(1838f,1513f),
        Offset(1845f,1537f),
        Offset(1835f, 1572f),
        Offset(1832f,1614f),
        Offset(1843f,1636f),
        Offset(1852f,1645f),
        Offset(1891f,1660f),
        Offset(1942f,1672f),
        Offset(2012f,1675f),
        Offset(2146f,1694f),
        Offset(2255f,1722f),
        Offset(2391f,1752f),
        Offset(2439f,1777f),
        Offset(2475f,1802f),
        Offset(2511f,1849f),
        Offset(2534f,1894f),
        Offset(2553f,1933f),
        Offset(2566f,1943f),
        Offset(2601f,1991f),
        Offset(2646f,2021f),
        Offset(2713f,2047f),
        Offset(2799f,2085f),
        Offset(2851f,2096f),
        Offset(2914f,2117f),
        Offset(2989f,2132f),
        Offset(3043f,2138f),
        Offset(3092f,2137f),
        Offset(3131f,2130f),
        Offset(3184f,2114f),
        Offset(3231f,2093f),
        Offset(3360f,2046f),
        Offset(3401f,2041f),
        Offset(3560f,2045f),
        Offset(3660f,2029f),
        Offset(3717f,2004f),
        Offset(3750f,1975f),
        Offset(3791f,1911f),
        Offset(3812f,1899f),
        Offset(3855f,1886f),
        Offset(3883f,1889f),
        Offset(3919f,1880f),
        Offset(3951f,1863f),
        Offset(3984f,1829f),
        Offset(3994f,1811f),
        Offset(3998f,1784f),
        Offset(3997f,1760f),
        Offset(3988f,1729f),
        Offset(3907f,1586f),
        Offset(3873f,1543f),
        Offset(3802f,1420f),
        Offset(3724f,1249f),
        Offset(3602f,1049f),
        Offset(3586f,1010f),
        Offset(3596f,974f),
        Offset(3722f,908f),
        Offset(3731f,888f),
        Offset(3725f,849f),
        Offset(3695f,802f),
        Offset(3665f,740f),
        Offset(3621f,666f),
        Offset(3591f,594f),
        Offset(3583f,558f),
        Offset(3589f,527f),
        Offset(3581f,481f),
        Offset(3568f,455f),
        Offset(3528f,428f),
        Offset(3489f,420f),
        Offset(3455f,392f),
        Offset(3447f,380f),
        Offset(3357f,303f),
        Offset(3318f,262f),
        Offset(3319f,236f),
        Offset(3328f,226f),
        Offset(3345f,215f),
        Offset(3344f,195f),
        Offset(3326f,183f)
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
