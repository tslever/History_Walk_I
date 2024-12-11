package com.history_walk.history_walk_i

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.history_walk.history_walk_i.ui.theme.ThemeForIntroScreen


class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeForIntroScreen {
                IntroScreen(
                    onContinue = {
                        val sharedPref = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("hasSeenIntro", true)
                            apply()
                        }
                        startActivity(Intent(this@IntroActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}


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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "History Walk I",
                style = MaterialTheme.typography.titleMedium,
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
                    textStyle = MaterialTheme.typography.titleLarge,
                    widthOfStroke = 4f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onContinue) {
                    Text("Continue")
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