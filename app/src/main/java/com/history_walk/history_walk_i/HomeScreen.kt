package com.history_walk.history_walk_i

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


@Composable
fun HomeScreen(onContinue: () -> Unit) {
    Box {
        Image(
            painter = painterResource(id = R.drawable.portrait_of_catherine_of_aragon_by_lucas_horenbout),
            contentDescription = "portrait of Catherine of Aragon by Lucas Horenbout",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(alpha = 0.5f)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Home Page",
                style = typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome Home!",
                style = typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onContinue) {
                Text("Continue")
            }
        }
    }
}