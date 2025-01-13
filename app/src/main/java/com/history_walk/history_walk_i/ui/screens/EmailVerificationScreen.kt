package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun EmailVerificationScreen(
    viewModel: ViewModelForHistoryWalkI,
    onProceedToMfa: () -> Unit
) {
    val canResend by viewModel.canResendVerificationEmail.observeAsState(false)
    val notificationMessage by viewModel.notification.observeAsState()
    var showDialog by remember { mutableStateOf(false) }
    val timeRemaining by viewModel.emailVerificationCountdown.observeAsState(120)
    val user = viewModel.firebaseUser.observeAsState(initial = null).value

    LaunchedEffect(Unit) {
        viewModel.startEmailVerificationCountdown()
    }

    if (notificationMessage != null) {
        LaunchedEffect(notificationMessage) {
            showDialog = true
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "Notification") },
                text = { Text(text = notificationMessage!!) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDialog = false
                            viewModel.clearNotification()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Email Verification",
            style = typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please verify your email to proceed with MFA enrollment.",
            style = typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                user?.reload()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (user.isEmailVerified) {
                            onProceedToMfa()
                        } else {
                            viewModel.clearNotification()
                            viewModel.mutableLiveDataOfNotification.value = "Email not verified yet. Please check your inbox."
                        }
                    } else {
                        viewModel.clearNotification()
                        viewModel.mutableLiveDataOfNotification.value = "Failed to reload user: ${task.exception?.message}"
                    }
                }
            }
        ) {
            Text(text = "I've Verified My Email")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.resendVerificationEmail()
            },
            enabled = canResend
        ) {
            if (canResend) {
                Text(text = "Resend Verification Email")
            } else {
                Text(text = "Resend Verification Email In ${timeRemaining} s")
            }
        }
    }
}