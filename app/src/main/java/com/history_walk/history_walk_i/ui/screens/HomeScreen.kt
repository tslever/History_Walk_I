package com.history_walk.history_walk_i.ui.screens

import com.history_walk.history_walk_i.ui.components.SettingsButton
import android.app.Activity
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.R
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI
import java.text.DecimalFormat


data class TupleOfDescriptionAndNumberOfSteps(
    val description: String,
    val numberOfSteps: Int
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToEpisodes: () -> Unit,
    onGoToSettings: () -> Unit,
    onUpgrade: (Activity?) -> Unit,
    viewModel: ViewModelForHistoryWalkI
) {

    var dropdownMenuOfPacesIsExpanded by remember { mutableStateOf(false) }
    val listOfTuplesOfDescriptionsAndNumbersOfSteps = listOf(
        TupleOfDescriptionAndNumberOfSteps("low", 35_000),
        TupleOfDescriptionAndNumberOfSteps("medium", 70_000),
        TupleOfDescriptionAndNumberOfSteps("high", 140_000)
    )
    val numberOfStepsInSharedPreferences = viewModel.getSelectedNumberOfSteps()
    var selectedTupleOfDescriptionAndNumberOfSteps by remember {
        mutableStateOf(
            listOfTuplesOfDescriptionsAndNumbersOfSteps.find {
                it.numberOfSteps == numberOfStepsInSharedPreferences
            } ?: listOfTuplesOfDescriptionsAndNumbersOfSteps[0]
        )
    }
    val userHasUpgraded by viewModel.userHasUpgraded.observeAsState(initial = false)
    val context = LocalContext.current

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
                    text = "number of steps required to complete episode:",
                    style = typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownMenuOfPacesIsExpanded,
                    onExpandedChange = { dropdownMenuOfPacesIsExpanded = !dropdownMenuOfPacesIsExpanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .width(125.dp),
                        value = DecimalFormat("#,###").format(selectedTupleOfDescriptionAndNumberOfSteps.numberOfSteps),
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
                        onDismissRequest = { dropdownMenuOfPacesIsExpanded = false },
                    ) {
                        listOfTuplesOfDescriptionsAndNumbersOfSteps.forEach { tupleOfDescriptionAndNumberOfSteps ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = tupleOfDescriptionAndNumberOfSteps.description,
                                        style = typography.displaySmall
                                    ) },
                                onClick = {
                                    selectedTupleOfDescriptionAndNumberOfSteps = tupleOfDescriptionAndNumberOfSteps
                                    dropdownMenuOfPacesIsExpanded = false
                                    viewModel.setSelectedNumberOfSteps(tupleOfDescriptionAndNumberOfSteps.numberOfSteps)
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
                if (!userHasUpgraded) {
                    Text(
                        text = "Note: The free version of this app allows you to log 2,000 steps per day. For unlimited steps, make a one-time purchase of \$5.99.",
                        style = typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(color = Color(0x80D9D9D9))
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val currentActivity = context as? Activity
                            onUpgrade(currentActivity)
                        },
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
                } else {
                    Text(
                        text = "You have unlocked the premium upgrade!",
                        style = typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(color = Color(0x80D9D9D9))
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
            }
            SettingsButton (onGoToSettings = onGoToSettings)
        }
    }
}