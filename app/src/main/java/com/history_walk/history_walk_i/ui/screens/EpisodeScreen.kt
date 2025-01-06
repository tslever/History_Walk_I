package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.history_walk.history_walk_i.ui.components.MapWithPathAndCircle
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI,
    onGoToSettings: () -> Unit,
    onEpisodeCompleted: () -> Unit
) {

    if (episodeId == 1) {
        MapWithPathAndCircle(modifier = Modifier.fillMaxSize())
    } else {
        // TODO
    }

    Button(
        onClick = {
            viewModel.incrementEpisodeIndex()
            onEpisodeCompleted()
        }
    ) {
        Text(text = "Complete")
    }
}