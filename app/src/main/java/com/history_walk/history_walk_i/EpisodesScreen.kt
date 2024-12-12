package com.history_walk.history_walk_i

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


data class Episode(
    val index: Int,
    val isCompleted: Boolean,
    val isAvailable: Boolean,
    val stepsRequired: Int
)


@Composable
fun EpisodesScreen(
    onGoToSettings: () -> Unit,
    viewModel: ViewModelForHistoryWalkI
) {

    val numberOfHistoricalStepsPerRealStep = viewModel.getHistoricalStepsPerRealStepValue()

    val episodes = listOf(
        Episode(index = 1, isCompleted = false, isAvailable = true, stepsRequired = 198 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 2, isCompleted = false, isAvailable = false, stepsRequired = 396 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 3, isCompleted = false, isAvailable = false, stepsRequired = 300 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 4, isCompleted = false, isAvailable = false, stepsRequired = 600 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 5, isCompleted = false, isAvailable = false, stepsRequired = 498 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 6, isCompleted = false, isAvailable = false, stepsRequired = 198 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 7, isCompleted = false, isAvailable = false, stepsRequired = 396 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 8, isCompleted = false, isAvailable = false, stepsRequired = 300 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 9, isCompleted = false, isAvailable = false, stepsRequired = 600 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 10, isCompleted = false, isAvailable = false, stepsRequired = 498 / numberOfHistoricalStepsPerRealStep)
    )

    Box {
        Image(
            painter = painterResource(id = R.drawable.portrait_of_catherine_of_aragon_by_lucas_horenbout),
            contentDescription = "portrait of Catherine of Aragon by Lucas Horenbout",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(alpha = 0.5f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 21.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Episodes",
                    style = typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TableHeaderCell(text = "Index")
                        TableHeaderCell(text = "Completed?")
                        TableHeaderCell(text = "Available?")
                        TableHeaderCell(text = "Real Steps")
                    }
                }
                LazyColumn {
                    items(episodes) { episode ->
                        EpisodeRow(episode = episode)
                    }
                }
            }

            Button(
                onClick = onGoToSettings,
                modifier = Modifier
                    .border(width = 1.dp, color = Color.Black)
                    .defaultMinSize(minHeight = 48.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.Black
                    )
                    Text(
                        text = "settings",
                        style = typography.displaySmall,
                        color = Color.Black
                    )
                }
            }
        }
    }
}


@Composable
fun TableHeaderCell(text: String) {
    Text(text = text, style = typography.displayMedium)
}


@Composable
fun EpisodeRow(episode: Episode) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TableCell(text = episode.index.toString().padStart(2, '0'))
            TableCell(text = if (episode.isCompleted) "Yes" else "No")
            TableCell(text = if (episode.isAvailable) "Yes" else "No")
            TableCell(text = episode.stepsRequired.toString())
        }
    }
}


@Composable
fun TableCell(text: String) {
    Text(text = text, style = typography.displayMedium)
}