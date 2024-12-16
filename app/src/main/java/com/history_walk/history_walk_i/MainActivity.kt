package com.history_walk.history_walk_i

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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


@Composable
fun HistoryWalkI(viewModel: ViewModelForHistoryWalkI = viewModel()) {
    val navController = rememberNavController()
    var weAreNavigatingToEpisodesScreen by remember { mutableStateOf(false) }

    val managedActivityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) {
        uri: Uri? ->
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val application = viewModel.getApplication<Application>()
        val contentResolver = application.contentResolver
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, flags)
            val stringRepresentingUri = uri.toString()
            viewModel.setSharedPreferenceRepresentingUriOfChosenDirectory(stringRepresentingUri)
            if (weAreNavigatingToEpisodesScreen) {
                viewModel.setHasSeenHome()
                navController.navigate("episodes") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
        weAreNavigatingToEpisodesScreen = false
    }

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
            val context = LocalContext.current
            val currentActivity = context as? Activity

            HomeScreen(
                onGoToEpisodes = {
                    if (viewModel.isDirectoryChosen()) {
                        viewModel.setHasSeenHome()
                        navController.navigate("episodes") {
                            popUpTo("home") { inclusive = true }
                        }
                    } else {
                        weAreNavigatingToEpisodesScreen = true
                        managedActivityResultLauncher.launch(null)
                    }
                },
                onUpgrade = {
                    currentActivity?.let {
                        viewModel.purchasePremium(it)
                    }
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
                },
                viewModel = viewModel,
                onEpisodeClick = { episodeId ->
                    navController.navigate("episode/$episodeId")
                }
            )
        }
        composable("episode/{episodeId}") { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getString("episodeId")?.toIntOrNull()
            if (episodeId != null) {
                EpisodeScreen(
                    episodeId = episodeId,
                    viewModel = viewModel,
                    onGoToSettings = { navController.navigate("settings") },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}