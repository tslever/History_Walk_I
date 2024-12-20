package com.history_walk.history_walk_i

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
import com.history_walk.history_walk_i.ui.screens.LogInScreen
import com.history_walk.history_walk_i.ui.screens.SettingsScreen
import com.history_walk.history_walk_i.ui.screens.SignUpScreen
import com.history_walk.history_walk_i.ui.screens.TfaScreen
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
fun HistoryWalkI(
    viewModel: ViewModelForHistoryWalkI = viewModel()
) {
    var message by remember { mutableStateOf("") }
    val navController = rememberNavController()
    var showDialog by remember { mutableStateOf(false) }
    val stateOfNotification by viewModel.notification.observeAsState()
    val stateOfFirebaseUser by viewModel.firebaseUser.observeAsState()
    val stateOfTfaVerified by viewModel.tfaHasOccurred.observeAsState(false)

    LaunchedEffect(stateOfNotification) {
        stateOfNotification?.let { theMessage ->
            message = theMessage
            showDialog = true
            viewModel.clearNotification()
        }
    }

    if (showDialog) {
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

    NavHost(
        navController = navController,
        startDestination = "logIn"
    ) {

        composable(
            "episode/{episodeId}",
            arguments = listOf(
                navArgument("episodeId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getInt("episodeId")
            if (episodeId != null) {
                EpisodeScreen(
                    episodeId = episodeId,
                    viewModel = viewModel,
                    onGoToSettings = { navController.navigate("settings") }
                )
            }
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

        composable("home") {
            HomeScreen(
                onGoToEpisodes = {
                    viewModel.setHasSeenHome()
                    navController.navigate("episodes") {
                        launchSingleTop = true
                    }
                },
                onUpgrade = { activity ->
                    if (activity != null) {
                        viewModel.purchasePremium(activity)
                    }
                },
                onGoToSettings = {
                    navController.navigate("settings")
                },
                viewModel = viewModel
            )
        }

        composable("intro") {
            IntroScreen(
                onContinue = {
                    if (!viewModel.hasSeenHome()) {
                        navController.navigate("home") {

                        }
                    } else {
                        navController.navigate("episodes") {

                        }
                    }
                }
            )
        }

        composable("logIn") {
            LogInScreen(
                viewModelForHistoryWalkI = viewModel,
                onLogInSuccess = {
                    navController.navigate("TFA") {
                        popUpTo("logIn") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signUp")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate("logIn") {
                        popUpTo("settings") { inclusive = true }
                    }
                }
            )
        }

        composable("signUp") {
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = {
                    navController.navigate("logIn") {
                        popUpTo("signUp") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("logIn") {
                        popUpTo("signUp") { inclusive = true }
                    }
                }
            )
        }

        composable("TFA") {
            TfaScreen(
                viewModel = viewModel,
                onTwoFactorSuccess = {
                    navController.navigate("intro") {
                        popUpTo("TFA") { inclusive = true }
                    }
                }
            )
        }
    }

    LaunchedEffect(stateOfFirebaseUser, stateOfTfaVerified) {
        if (stateOfFirebaseUser == null) {
            if (navController.currentDestination?.route != "logIn") {
                navController.navigate("logIn") {
                    popUpTo("logIn") { inclusive = true }
                }
            }
        } else {
            if (!stateOfTfaVerified) {
                if (navController.currentDestination?.route != "TFA") {
                    navController.navigate("TFA") {
                        popUpTo("logIn") { inclusive = true }
                    }
                }
            } else {
                if (navController.currentDestination?.route !in listOf("intro", "home", "episodes", "episode/{episodeId}", "settings")) {
                    navController.navigate("intro") {
                        popUpTo("logIn") { inclusive = true }
                    }
                }
            }
        }
    }
}