package com.history_walk.history_walk_i

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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

    val navController = rememberNavController()
    val stateOfFirebaseUser by viewModel.firebaseUser.observeAsState()

    NavHost(
        navController = navController,
        startDestination = if (stateOfFirebaseUser == null) "logIn" else "intro"
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
                    onGoToSettings = { navController.navigate("settings") },
                    onBack = { navController.popBackStack() }
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
                        popUpTo("home") { inclusive = true }
                    }
                },
                onUpgrade = { activity ->
                    activity?.let {
                        viewModel.purchasePremium(it)
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

        composable("logIn") {
            LogInScreen(
                viewModelForHistoryWalkI = viewModel,
                onLogInSuccess = {
                    navController.navigate("intro") {
                        popUpTo("login") { inclusive = true }
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
                    navController.navigate("login") {
                        popUpTo("settings") { inclusive = true }
                    }
                }
            )
        }

        composable("signUp") {
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = {
                    navController.navigate("intro") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

    }
}