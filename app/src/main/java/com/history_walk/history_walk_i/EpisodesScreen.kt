package com.history_walk.history_walk_i

import SettingsButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onRequestDirectory: () -> Unit,
    viewModel: ViewModelForHistoryWalkI
) {

    LaunchedEffect(Unit) {
        if (!viewModel.isDirectoryChosen()) {
            onRequestDirectory()
        }
    }

    val (
        indexOfPresentEpisode,
        numberOfHistoricalStepsCompletedOfPresentEpisode
    ) = if (viewModel.isDirectoryChosen()) {
        viewModel.getIndexAndNumberOfHistoricalStepsCompletedOfPresentEpisode()
    } else {
        Pair(1, 0)
    }

    val numberOfHistoricalStepsPerRealStep = viewModel.getNumberOfHistoricalStepsPerRealStep()
    val episodes = listOf(
        Episode(index = 1, isCompleted = 1 < indexOfPresentEpisode, isAvailable = 1 <= indexOfPresentEpisode, stepsRequired = 198 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 2, isCompleted = 2 < indexOfPresentEpisode, isAvailable = 2 <= indexOfPresentEpisode, stepsRequired = 396 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 3, isCompleted = 3 < indexOfPresentEpisode, isAvailable = 3 <= indexOfPresentEpisode, stepsRequired = 300 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 4, isCompleted = 4 < indexOfPresentEpisode, isAvailable = 4 <= indexOfPresentEpisode, stepsRequired = 600 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 5, isCompleted = 5 < indexOfPresentEpisode, isAvailable = 5 <= indexOfPresentEpisode, stepsRequired = 498 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 6, isCompleted = 6 < indexOfPresentEpisode, isAvailable = 6 <= indexOfPresentEpisode, stepsRequired = 198 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 7, isCompleted = 7 < indexOfPresentEpisode, isAvailable = 7 <= indexOfPresentEpisode, stepsRequired = 396 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 8, isCompleted = 8 < indexOfPresentEpisode, isAvailable = 8 <= indexOfPresentEpisode, stepsRequired = 300 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 9, isCompleted = 9 < indexOfPresentEpisode, isAvailable = 9 <= indexOfPresentEpisode, stepsRequired = 600 / numberOfHistoricalStepsPerRealStep),
        Episode(index = 10, isCompleted = 10 < indexOfPresentEpisode, isAvailable = 10 <= indexOfPresentEpisode, stepsRequired = 498 / numberOfHistoricalStepsPerRealStep)
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

            SettingsButton (onGoToSettings = onGoToSettings)
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