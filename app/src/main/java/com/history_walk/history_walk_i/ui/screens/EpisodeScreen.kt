package com.history_walk.history_walk_i.ui.screens

import androidx.compose.runtime.Composable
import com.history_walk.history_walk_i.ui.components.MapWithPathAndCircle


@Composable
fun EpisodeScreen(episodeId: Int) {
    MapWithPathAndCircle(episodeId)
}