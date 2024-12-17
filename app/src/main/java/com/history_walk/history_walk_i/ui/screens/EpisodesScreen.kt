package com.history_walk.history_walk_i.ui.screens

import com.history_walk.history_walk_i.ui.components.SettingsButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.R
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


data class Episode(
    val isCompleted: Boolean,
    val index: Int,
    val title: String,
    val isAvailable: Boolean
)


@Composable
fun EpisodesScreen(
    onGoToSettings: () -> Unit,
    viewModel: ViewModelForHistoryWalkI,
    onEpisodeClick: (Int) -> Unit
) {
    val indexOfPresentEpisode by viewModel.indexOfPresentEpisode.observeAsState(initial = 1)
    val listOfEpisodes = viewModel.listOfTitlesOfEpisodes.mapIndexed { index, title ->
        val theIndex = index + 1
        Episode(
            isCompleted = theIndex < indexOfPresentEpisode,
            index = theIndex,
            title = title,
            isAvailable = theIndex <= indexOfPresentEpisode
        )
    }

    Box {
        Image(
            painter = painterResource(id = R.drawable.portrait_of_catherine_of_aragon_by_michel_sittow),
            contentDescription = "portrait of Catherine of Aragon by Lucas Horenbout",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha = 0.5f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 21.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Episodes",
                    style = typography.titleMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = Offset(0f, 3f),
                            blurRadius = 4f
                        )
                    ),
                    color = Color(0xFFFFC004), // ARGB
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(16.dp)
                        )
                        .background(color = Color(0x80D9D9D9)) // ARGB
                        .border(
                            width = 1.dp,
                            color = Color.Black,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(
                            top = 2.dp,
                            bottom = 2.dp
                        )
                ) {
                    LazyColumn {
                        items(listOfEpisodes) { episode ->
                            EpisodeRow(
                                episode = episode,
                                onClick = { onEpisodeClick(episode.index) }
                            )
                        }
                    }
                }
            }
            SettingsButton (onGoToSettings = onGoToSettings)
        }
    }
}


@Composable
fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    val textColor = if (episode.isAvailable) Color(0xFF000000) else Color(0x86000000) // ARGB

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = episode.isAvailable,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = episode.isCompleted,
                onCheckedChange = null,
                enabled = false,
                colors = colors(
                    checkedColor = Color(0xFF000000),
                    uncheckedColor = Color(0x86000000)
                ),
                modifier = Modifier.weight(0.1f)
            )
            Text(
                text = "${episode.index}.",
                style = typography.labelLarge,
                color = textColor,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .weight(0.1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = episode.title,
                style = typography.labelLarge,
                color = textColor,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}