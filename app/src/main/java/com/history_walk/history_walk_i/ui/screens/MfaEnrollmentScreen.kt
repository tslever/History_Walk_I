package com.history_walk.history_walk_i.u.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun MfaEnrollmentScreen(
    viewModel: ViewModelForHistoryWalkI,
    onEnrollmentSuccess: () -> Unit,
    onEnrollmentFailure: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val verificationId by viewModel.verificationId.observeAsState()

    BackHandler {
        onEnrollmentFailure()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enroll in Two-Factor Authentication",
            style = typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "A verification code has been sent to your phone. Please enter it below to complete MFA enrollment.",
            style = typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) { code = it } },
            label = { Text("6-digit code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (code.trim().length == 6) {
                    viewModel.verifyMfaEnrollmentCode(code) { success, error ->
                        if (success) {
                            onEnrollmentSuccess()
                        } else {
                            errorMessage = error ?: "Incorrect code. Please try again."
                        }
                    }
                }
            },
            enabled = code.trim().length == 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify and Enroll")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}