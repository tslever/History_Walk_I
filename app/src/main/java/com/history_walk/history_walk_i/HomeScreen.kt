package com.history_walk.history_walk_i

import SettingsButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


data class TupleOfPaceAndDescription(val pace: Float, val description: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToEpisodes: () -> Unit,
    onGoToSettings: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: ViewModelForHistoryWalkI
) {
    val listOfTuplesOfPaceAndDescription = listOf(
        TupleOfPaceAndDescription(0.5f,"slow"),
        TupleOfPaceAndDescription(1f, "medium"),
        TupleOfPaceAndDescription(2f, "fast")
    )
    val paceInSharedPreferences = viewModel.getSelectedPace()
    var selectedTupleOfPaceAndDescription by remember {
        mutableStateOf(
            listOfTuplesOfPaceAndDescription.find {
                it.pace == paceInSharedPreferences
            } ?: listOfTuplesOfPaceAndDescription[0]
        )
    }
    var dropdownMenuOfPacesIsExpanded by remember { mutableStateOf(false) }

    Box {
        Image(
            painter = painterResource(id = R.drawable.portrait_of_catherine_of_aragon_by_lucas_horenbout),
            contentDescription = "portrait of Catherine of Aragon by Lucas Horenbout",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha = 0.5f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 32.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 21.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(color = Color(0x80D9D9D9))
                    .padding(
                        top = 2.dp,
                        bottom = 2.dp
                    )
            ) {
                Text(
                    text = "Welcome to History Walk!",
                    style = typography.displayLarge
                )
                Spacer(
                    modifier = Modifier.height(16.dp)
                )
                Text(
                    text = "With this app you’ll be able to  learn loads of fascinating history while getting exercise. Every step you take translates into movement across the map. Whenever you reach an event icon, you’ll learn a little more about the life of Catherine of Aragon, the first Queen of Henry VIII of England.\n\nGo to the Episodes page to begin the adventure!",
                    style = typography.displayMedium,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Choose your pace:",
                    style = typography.displayMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownMenuOfPacesIsExpanded,
                    onExpandedChange = { dropdownMenuOfPacesIsExpanded = !dropdownMenuOfPacesIsExpanded }
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor(),
                        value = selectedTupleOfPaceAndDescription.description,
                        onValueChange = { },
                        readOnly = true,
                        textStyle = typography.displaySmall,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownMenuOfPacesIsExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownMenuOfPacesIsExpanded,
                        onDismissRequest = { dropdownMenuOfPacesIsExpanded = false }
                    ) {
                        listOfTuplesOfPaceAndDescription.forEach { tupleOfPaceAndDescription ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = tupleOfPaceAndDescription.description,
                                        style = typography.displaySmall
                                    ) },
                                onClick = {
                                    selectedTupleOfPaceAndDescription = tupleOfPaceAndDescription
                                    dropdownMenuOfPacesIsExpanded = false
                                    viewModel.setSelectedPace(tupleOfPaceAndDescription.pace)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onGoToEpisodes,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.Black
                        )
                        .defaultMinSize(minHeight = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Go to Episodes",
                        style = typography.displayLarge,
                        color = Color.Black
                    )
                }
            }
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Note: The free version of this app allows you to log 2,000 steps per day. For unlimited steps, make a one-time purchase of \$5.99.",
                    style = typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(color = Color(0x80D9D9D9))
                        .padding(vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.Black
                        )
                        .defaultMinSize(minHeight = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Upgrade Now",
                        style = typography.displaySmall,
                        color = Color.Black
                    )
                }
            }
            SettingsButton (onGoToSettings = onGoToSettings)
        }
    }
}