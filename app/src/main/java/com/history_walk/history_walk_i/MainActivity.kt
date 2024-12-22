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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.history_walk.history_walk_i.ui.screens.EmailVerificationScreen
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


object NavRoutes {
    const val AUTH_GRAPH = "auth_graph"
    const val MAIN_GRAPH = "main_graph"

    const val LOG_IN = "logIn"
    const val SIGN_UP = "signUp"
    const val EMAIL_VERIFICATION = "emailVerification"
    const val TFA = "TFA"

    const val INTRO = "intro"
    const val HOME = "home"
    const val EPISODES = "episodes"
    const val EPISODE = "episode/{episodeId}"
    const val SETTINGS = "settings"
}


fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    viewModel: ViewModelForHistoryWalkI
) {
    navigation(startDestination = NavRoutes.LOG_IN, route = NavRoutes.AUTH_GRAPH) {
        composable(NavRoutes.LOG_IN) {
            LogInScreen(
                viewModelForHistoryWalkI = viewModel,
                onLogInSuccess = {
                    navController.navigate(NavRoutes.TFA) {
                        popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(NavRoutes.SIGN_UP)
                }
            )
        }
        composable(NavRoutes.SIGN_UP) {
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = {
                    navController.navigate(NavRoutes.EMAIL_VERIFICATION) {
                        popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(NavRoutes.EMAIL_VERIFICATION) {
            EmailVerificationScreen(
                viewModel = viewModel,
                onProceedToMfa = {
                    navController.navigate(NavRoutes.TFA) {
                        popUpTo(NavRoutes.EMAIL_VERIFICATION) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(NavRoutes.TFA) {
            TfaScreen(
                viewModel = viewModel,
                onTwoFactorSuccess = {
                    navController.navigate(NavRoutes.INTRO) {
                        popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onCancel = {
                    viewModel.signOut()
                    navController.navigate(NavRoutes.LOG_IN) {
                        popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}


fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    viewModel: ViewModelForHistoryWalkI
) {
    navigation(startDestination = NavRoutes.INTRO, route = NavRoutes.MAIN_GRAPH) {
        composable(NavRoutes.INTRO) {
            IntroScreen(
                onContinue = {
                    if (!viewModel.hasSeenHome()) {
                        navController.navigate(NavRoutes.HOME) {
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(NavRoutes.EPISODES) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(NavRoutes.HOME) {
            HomeScreen(
                onGoToEpisodes = {
                    viewModel.setHasSeenHome()
                    navController.navigate(NavRoutes.EPISODES) {
                        launchSingleTop
                    }
                },
                onUpgrade = { activity ->
                    if (activity != null) {
                        viewModel.purchasePremium(activity)
                    }
                },
                onGoToSettings = {
                    navController.navigate(NavRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                viewModel = viewModel
            )
        }
        composable(NavRoutes.EPISODES) {
            EpisodesScreen(
                onGoToSettings = {
                    navController.navigate(NavRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                viewModel = viewModel,
                onEpisodeClick = { episodeId ->
                    navController.navigate("episode/$episodeId") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            NavRoutes.EPISODE,
            arguments = listOf(
                navArgument("episodeId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getInt("episodeId")
            if (episodeId != null && episodeId > 0 && episodeId <= viewModel.listOfTitlesOfEpisodes.size) {
                EpisodeScreen(
                    episodeId = episodeId,
                    viewModel = viewModel,
                    onGoToSettings = {
                        navController.navigate(NavRoutes.SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                    onEpisodeCompleted = {
                        navController.popBackStack(NavRoutes.EPISODES, false)
                    }
                )
            } else {
                navController.popBackStack(NavRoutes.EPISODES, false)
            }
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(NavRoutes.AUTH_GRAPH) {
                        popUpTo(NavRoutes.MAIN_GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}


@Composable
fun HistoryWalkI(
    viewModel: ViewModelForHistoryWalkI = viewModel()
) {
    val activity = LocalContext.current as Activity
    var message by remember { mutableStateOf("") }
    val navController = rememberNavController()
    var showDialog by remember { mutableStateOf(false) }
    val stateOfNotification by viewModel.notification.observeAsState()
    val stateOfFirebaseUser by viewModel.firebaseUser.observeAsState()
    val stateOfMfaVerified by viewModel.mfaVerified.observeAsState(false)


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
        startDestination = when {
            stateOfFirebaseUser == null || !stateOfMfaVerified -> NavRoutes.AUTH_GRAPH
            else -> NavRoutes.MAIN_GRAPH
        }
    ) {
        authGraph(navController, viewModel)
        mainGraph(navController, viewModel)
    }

    LaunchedEffect(stateOfFirebaseUser, stateOfMfaVerified) {
        when {
            stateOfFirebaseUser == null -> {
                if (navController.currentDestination?.route !in listOf(
                        NavRoutes.AUTH_GRAPH,
                        NavRoutes.LOG_IN,
                        NavRoutes.SIGN_UP,
                        NavRoutes.EMAIL_VERIFICATION,
                        NavRoutes.TFA
                )
                    ) {
                    navController.navigate(NavRoutes.AUTH_GRAPH) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            !stateOfMfaVerified -> {
                if (navController.currentDestination?.route != NavRoutes.TFA) {
                    navController.navigate(NavRoutes.TFA) {
                        popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            else -> {
                if (navController.currentDestination?.route !in listOf(
                        NavRoutes.INTRO,
                        NavRoutes.HOME,
                        NavRoutes.EPISODES,
                        NavRoutes.EPISODE,
                        NavRoutes.SETTINGS
                )
                    ) {
                    navController.navigate(NavRoutes.MAIN_GRAPH) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}