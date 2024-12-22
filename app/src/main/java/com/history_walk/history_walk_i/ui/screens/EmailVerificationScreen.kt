package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    var message by remember { mutableStateOf("") }
    var showResendButton by remember { mutableStateOf(true) }
    val user = viewModel.firebaseUser.observeAsState(initial = null).value


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
            text = message.ifEmpty { "Please verify your email to proceed with MFA enrollment." },
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
                            message = "Email not verified yet. Please check your inbox."
                        }
                    } else {
                        message = "Failed to reload user: ${task.exception?.message}"
                    }
                }
            }
        ) {
            Text(text = "I've Verified My Email")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (showResendButton) {
            Button(
                onClick = {
                    user?.sendEmailVerification()?.addOnCompleteListener { resendTask ->
                        if (resendTask.isSuccessful) {
                            message = "Verification email resent. Please check your inbox."
                            showResendButton = false
                        } else {
                            message = "Failed to resend verification email: ${resendTask.exception?.message}"
                        }
                    }
                }
            ) {
                Text(text = "Resend Verification Email")
            }
        }
    }
}