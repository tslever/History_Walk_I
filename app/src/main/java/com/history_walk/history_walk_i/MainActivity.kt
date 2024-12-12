package com.history_walk.history_walk_i

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

@Composable
fun HistoryWalkI(viewModel: ViewModelForHistoryWalkI = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "intro") {
        composable("intro") {
            IntroScreen(
                onContinue = {
                    if (!viewModel.hasSeenHome()) {
                        navController.navigate("home") {
                            popUpTo("intro") { inclusive = true }
                        }
                    } else {
                        navController.navigate("episodes") {
                            popUpTo("intro") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onGoToEpisodes = {
                    viewModel.setHasSeenHome()
                    navController.navigate("episodes") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onUpgrade = {
                    // TODO
                },
                onGoToSettings = {
                    navController.navigate("settings")
                },
                viewModel = viewModel
            )
        }
        composable("episodes") {
            EpisodesScreen(
                onGoToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}