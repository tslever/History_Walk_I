package com.history_walk.history_walk_i

import StyledButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToEpisodes: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: ViewModelForHistoryWalkI
) {

    val listOfPaces = listOf(
        "1 real step = few historical steps",
        "1 real step = some historical steps",
        "1 real step = many historical steps"
    )
    var selectedPace by remember { mutableStateOf("") }
    var dropdownMenuOfPacesIsExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        selectedPace = viewModel.getSelectedPace()
    }

    Box {
        Image(
            painter = painterResource(id = R.drawable.portrait_of_catherine_of_aragon_by_lucas_horenbout),
            contentDescription = "portrait of Catherine of Aragon by Lucas Horenbout",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(alpha = 0.5f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 21.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(color = Color(0x80D9D9D9))
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Welcome to History Walk!",
                    style = typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                        value = selectedPace,
                        onValueChange =  { },
                        readOnly = true,
                        textStyle = typography.displaySmall,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownMenuOfPacesIsExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownMenuOfPacesIsExpanded,
                        onDismissRequest = { dropdownMenuOfPacesIsExpanded = false }
                    ) {
                        listOfPaces.forEachIndexed { index, text ->
                            DropdownMenuItem(
                                text = { Text(text = text, style = typography.displaySmall) },
                                onClick = {
                                    selectedPace = listOfPaces[index]
                                    dropdownMenuOfPacesIsExpanded = false
                                    viewModel.setSelectedPace(selectedPace)
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
                StyledButton(
                    onClick = onGoToEpisodes,
                    text = "Go to Episodes",
                    textStyle = typography.displayLarge
                )
            }
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Note: The free version of this app allows you to log 2,000 steps per day. For unlimited steps, make a one-time purchase of \$5.99.",
                    style = typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                StyledButton(
                    onClick = onUpgrade,
                    text = "Upgrade Now",
                    textStyle = typography.displaySmall
                )
            }
            Text(
                text = "settings",
                style = typography.displaySmall
            )
        }
    }
}