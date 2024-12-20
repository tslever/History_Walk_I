package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI,
    onGoToSettings: () -> Unit,
    onEpisodeCompleted: () -> Unit
) {
    val title = viewModel.listOfTitlesOfEpisodes[episodeId - 1]

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Episode $episodeId: $title",
                style = typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = {
                viewModel.incrementEpisodeIndex()
                onEpisodeCompleted()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Mark as Completed")
        }
    }
}