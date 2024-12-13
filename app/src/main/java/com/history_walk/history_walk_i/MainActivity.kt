package com.history_walk.history_walk_i

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var pendingNavigationToEpisodes by remember { mutableStateOf(false) }

    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) {
        uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val contentResolver = viewModel.getApplication<Application>().contentResolver
            try {
                contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.setChosenDirectoryUri(uri.toString())
                Log.d("MainActivity", "Directory chosen and permissions taken: $uri")
                viewModel.getIndexAndNumberOfHistoricalStepsCompletedOfPresentEpisode()
                if (pendingNavigationToEpisodes) {
                    viewModel.setHasSeenHome()
                    navController.navigate("episodes") {
                        popUpTo("home") { inclusive = true }
                    }
                    pendingNavigationToEpisodes = false
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Failed to take persistable URI permission: ${e.message}")
            }
        } else {
            Log.e("MainActivity", "No directory selected by the user.")
        }
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
            HomeScreen(
                onGoToEpisodes = {
                    if (viewModel.isDirectoryChosen()) {
                        viewModel.setHasSeenHome()
                        navController.navigate("episodes") {
                            popUpTo("home") { inclusive = true }
                        }
                    } else {
                        pendingNavigationToEpisodes = true
                        openDirectoryLauncher.launch(null)
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
                },
                onRequestDirectory = {

                },
                viewModel = viewModel
            )
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}