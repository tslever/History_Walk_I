package com.history_walk.history_walk_i

import SettingsButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


data class Episode(
    val isCompleted: Boolean,
    val index: Int,
    val title: String,
    val stepsRequired: Int,
    val isAvailable: Boolean
)


@Composable
fun EpisodesScreen(
    onGoToSettings: () -> Unit,
    viewModel: ViewModelForHistoryWalkI
) {
    val (indexOfPresentEpisode) = viewModel.getIndexAndNumberOfHistoricalStepsCompletedOfPresentEpisode()
    val numberOfHistoricalStepsPerRealStep = viewModel.getNumberOfHistoricalStepsPerRealStep()
    val episodes = listOf(
        Episode(isCompleted = 1 < indexOfPresentEpisode, index = 1, title = "The Alhambra", stepsRequired = 198 / numberOfHistoricalStepsPerRealStep, isAvailable = 1 <= indexOfPresentEpisode),
        Episode(isCompleted = 2 < indexOfPresentEpisode, index = 2, title = "Crossing Spain", stepsRequired = 396 / numberOfHistoricalStepsPerRealStep, isAvailable = 2 <= indexOfPresentEpisode),
        Episode(isCompleted = 3 < indexOfPresentEpisode, index = 3, title = "Journey to London", stepsRequired = 300 / numberOfHistoricalStepsPerRealStep, isAvailable = 3 <= indexOfPresentEpisode),
        Episode(isCompleted = 4 < indexOfPresentEpisode, index = 4, title = "Moving to Ludlow", stepsRequired = 600 / numberOfHistoricalStepsPerRealStep, isAvailable = 4 <= indexOfPresentEpisode),
        Episode(isCompleted = 5 < indexOfPresentEpisode, index = 5, title = "An Impoverished Captive", stepsRequired = 498 / numberOfHistoricalStepsPerRealStep, isAvailable = 5 <= indexOfPresentEpisode),
        Episode(isCompleted = 6 < indexOfPresentEpisode, index = 6, title = "Knight in Shining Armour", stepsRequired = 198 / numberOfHistoricalStepsPerRealStep, isAvailable = 6 <= indexOfPresentEpisode),
        Episode(isCompleted = 7 < indexOfPresentEpisode, index = 7, title = "Royal Progress", stepsRequired = 396 / numberOfHistoricalStepsPerRealStep, isAvailable = 7 <= indexOfPresentEpisode),
        Episode(isCompleted = 8 < indexOfPresentEpisode, index = 8, title = "General Catherine", stepsRequired = 300 / numberOfHistoricalStepsPerRealStep, isAvailable = 8 <= indexOfPresentEpisode),
        Episode(isCompleted = 9 < indexOfPresentEpisode, index = 9, title = "Producing an Heir", stepsRequired = 600 / numberOfHistoricalStepsPerRealStep, isAvailable = 9 <= indexOfPresentEpisode),
        Episode(isCompleted = 10 < indexOfPresentEpisode, index = 10, title = "The King's Great Matter", stepsRequired = 498 / numberOfHistoricalStepsPerRealStep, isAvailable = 10 <= indexOfPresentEpisode),
        Episode(isCompleted = 11 < indexOfPresentEpisode, index = 11, title = "Dowager Princess of Wales", stepsRequired = 198 / numberOfHistoricalStepsPerRealStep, isAvailable = 11 <= indexOfPresentEpisode),
        Episode(isCompleted = 12 < indexOfPresentEpisode, index = 12, title = "Funeral Progress", stepsRequired = 396 / numberOfHistoricalStepsPerRealStep, isAvailable = 12 <= indexOfPresentEpisode),
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
                    style = typography.titleMedium,
                    color = Color(0xFFFFC004), // ARGB
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(16.dp)
                        )
                        .background(color = Color(0x80D9D9D9)) // ARGB
                        .padding(
                            top = 2.dp,
                            bottom = 2.dp
                        )
                ) {
                    LazyColumn {
                        items(episodes) { episode ->
                            EpisodeRow(episode = episode)
                        }
                    }
                }
            }
            SettingsButton (onGoToSettings = onGoToSettings)
        }
    }
}


@Composable
fun EpisodeRow(episode: Episode) {
    val textColor = if (episode.isAvailable) Color(0xFF000000) else Color(0x86000000) // ARGB

    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Checkbox(
                checked = episode.isCompleted,
                onCheckedChange = null,
                enabled = false,
                colors = colors(
                    checkedColor = Color(0xFF000000),
                    uncheckedColor = Color(0x86000000)
                )
            )
            TableCell(
                text = episode.index.toString().padStart(2, ' ') + ".",
                textColor = textColor
            )
            TableCell(
                text = episode.title,
                textColor = textColor
            )
            TableCell(
                text = episode.stepsRequired.toString(),
                textColor = textColor
            )
        }
    }
}


@Composable
fun TableCell(text: String, textColor: Color) {
    Text(
        text = text,
        style = typography.displayMedium,
        color = textColor
    )
}