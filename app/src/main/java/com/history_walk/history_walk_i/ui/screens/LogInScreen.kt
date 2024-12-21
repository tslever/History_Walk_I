package com.history_walk.history_walk_i.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun LogInScreen(
    viewModelForHistoryWalkI: ViewModelForHistoryWalkI,
    onLogInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var emailAddress by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    val isLogInEnabled = emailAddress.trim().isNotEmpty() && password.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Log In", style = typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = emailAddress,
            onValueChange = { emailAddress = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModelForHistoryWalkI.signIn(emailAddress, password) { success, error ->
                    if (success) {
                        onLogInSuccess()
                    } else {
                        errorMessage = error ?: "Unknown error occurred."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isLogInEnabled
        ) {
            Text(text = "Log In")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text(text = "Don't have an account? Sign up.")
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}