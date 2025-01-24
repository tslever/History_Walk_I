package com.history_walk.history_walk_i

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.history_walk.history_walk_i.ui.screens.EpisodeScreen
import com.history_walk.history_walk_i.ui.screens.EpisodesScreen
import com.history_walk.history_walk_i.ui.screens.HomeScreen
import com.history_walk.history_walk_i.ui.screens.IntroScreen
import com.history_walk.history_walk_i.ui.screens.SettingsScreen
import com.history_walk.history_walk_i.ui.theme.ThemeForHistoryWalkI
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeForHistoryWalkI {
                HistoryWalkI()
            }
        }
    }
}


object NavRoutes {
    const val INTRO = "intro"
    const val HOME = "home"
    const val EPISODES = "episodes"
    const val EPISODE = "episode"
    const val SETTINGS = "settings"
}


@Composable
fun HistoryWalkI(
    viewModel: ViewModelForHistoryWalkI = viewModel()
) {
    val activity = LocalContext.current as Activity
    viewModel.setCurrentActivity(activity)

    val navController = rememberNavController()
    val stateOfNotification by viewModel.notification.observeAsState()
    var showDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(stateOfNotification) {
        stateOfNotification?.let { theMessage ->
            message = theMessage
            showDialog = true
            viewModel.clearNotification()
        }
    }

    if (showDialog && message.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Notification") },
            text = { Text(text = message) },
            confirmButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }

    NavHost(navController = navController, startDestination = NavRoutes.INTRO) {
        composable(NavRoutes.INTRO) {
            IntroScreen(
                onContinue = {
                    if (!viewModel.hasSeenHome()) {
                        navController.navigate(NavRoutes.HOME)
                    } else {
                        navController.navigate(NavRoutes.EPISODES)
                    }
                }
            )
        }
        composable(NavRoutes.HOME) {
            HomeScreen(
                onGoToEpisodes = {
                    navController.navigate(NavRoutes.EPISODES) {
                        popUpTo(NavRoutes.INTRO) { inclusive = false }
                    }
                },
                onGoToSettings = {
                    navController.navigate(NavRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onUpgrade = { act ->
                    if (act != null) {
                        viewModel.purchasePremium(act)
                    }
                },
                viewModel = viewModel
            )
        }
        composable(NavRoutes.EPISODES) {
            EpisodesScreen(
                onEpisodeClick = { episodeId ->
                    navController.navigate("${NavRoutes.EPISODE}/$episodeId")
                },
                onGoToSettings = {
                    navController.navigate(NavRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                viewModel = viewModel
            )
        }
        composable(
            route = "${NavRoutes.EPISODE}/{episodeId}",
            arguments = listOf(navArgument("episodeId") {type = NavType.IntType})
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getInt("episodeId") ?: 1
            EpisodeScreen(
                episodeId = episodeId,
                viewModel = viewModel
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(viewModel = viewModel)
        }
    }
}