package com.history_walk.history_walk_i

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.ui.theme.Typography


@Composable
fun IntroScreen(onContinue: () -> Unit) {
    Box {
        Image(
            painter = painterResource(id = R.drawable.portrait_of_catherine_of_aragon_by_juan_de_flandes),
            contentDescription = "portrait of Catherine of Aragon by Juan de Flandes",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "History Walk I",
                style = Typography.titleMedium,
                color = Color(0xFFFFC004), // ARGB
                textAlign = TextAlign.Center
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StrokedText(
                    colorOfFill = Color(0xFFCF0E0E), // ARGB
                    colorOfStroke = Color(0xFF000000), // ARGB
                    modifier = Modifier,
                    text = "Catherine of Aragon",
                    textStyle = Typography.titleLarge,
                    widthOfStroke = 4f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .border(width = 1.dp, color = Color.Black)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Continue",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.Black
                    )
                }
            }
        }
    }
}


@Composable
fun StrokedText(
    colorOfFill: Color,
    colorOfStroke: Color,
    modifier: Modifier,
    text: String,
    textStyle: TextStyle,
    widthOfStroke: Float,
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            style = textStyle.copy(
                color = colorOfStroke,
                drawStyle = Stroke(width = widthOfStroke)
            )
        )
        Text(
            text = text,
            style = textStyle.copy(
                color = colorOfFill
            )
        )
    }
}