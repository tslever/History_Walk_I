package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.ui.components.MapWithPathAndCircle
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EpisodeScreen(
    episodeId: Int,
    viewModel: ViewModelForHistoryWalkI
) {
    val stepCounts by viewModel.stepCounts.observeAsState(emptyMap())
    val currentStepCount = stepCounts[episodeId] ?: 0

    MapWithPathAndCircle(episodeId)

    Row() {
        Text(
            text = "Steps: $currentStepCount",
            modifier = Modifier.weight(1f)
            )
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = { viewModel.incrementStepCount(episodeId) },
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Add Step")
        }
    }
}