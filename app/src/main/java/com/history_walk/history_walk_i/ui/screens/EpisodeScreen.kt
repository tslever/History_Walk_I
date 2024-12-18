package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI,
    onGoToSettings: () -> Unit
) {
    val title = viewModel.listOfTitlesOfEpisodes[episodeId - 1]

    Column {
        Text(
            text = "Episode $episodeId: $title",
            style = typography.titleLarge
        )
    }
}