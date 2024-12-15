package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI,
    onGoToSettings: () -> Unit,
    onBack: () -> Unit
) {

    val (title, _) = viewModel.listOfPairsOfEpisodeTitlesAndNumbersOfHistoricalSteps[episodeId - 1]

    Column() {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Episode $episodeId: $title",
            style = typography.titleLarge
        )
    }
}