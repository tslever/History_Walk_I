package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.history_walk.history_walk_i.R
import com.history_walk.history_walk_i.ui.components.ZoomableImage
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI,
    onGoToSettings: () -> Unit,
    onEpisodeCompleted: () -> Unit
) {

    val episodeImages = mapOf(
        1 to R.drawable.the_alhambra
    )
    val imageResource = episodeImages[episodeId]

    if (imageResource != null) {
        ZoomableImage(
            painter = painterResource(id = imageResource),
            contentDescription = "Episode $episodeId Image",
            modifier = Modifier.fillMaxSize()
        )
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