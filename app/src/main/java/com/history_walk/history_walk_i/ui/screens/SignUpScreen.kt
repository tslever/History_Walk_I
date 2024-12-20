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
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.history_walk.history_walk_i.viewmodel.ViewModelForHistoryWalkI


@Composable
fun SignUpScreen(
    viewModel: ViewModelForHistoryWalkI,
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var emailAddress by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isSignUpEnabled = emailAddress.trim().isNotEmpty() &&
            phoneNumber.trim().isNotEmpty() &&
            password.trim().isNotEmpty() &&
            confirmPassword.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign Up",
            style = typography.titleLarge
        )
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
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
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
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match."
                } else {
                    val phoneNumberUtil = PhoneNumberUtil.getInstance()
                    try {
                        val parsedPhoneNumber = phoneNumberUtil.parse(phoneNumber, "US")
                        if (!phoneNumberUtil.isValidNumber(parsedPhoneNumber)) {
                            errorMessage = "Invalid phone number."
                            return@Button
                        }
                        val formattedPhoneNumber = phoneNumberUtil.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
                        phoneNumber = formattedPhoneNumber
                        viewModel.signUp(emailAddress, password, phoneNumber) { success, error ->
                            if (success) {
                                onSignUpSuccess()
                            } else {
                                errorMessage = error ?: "Unknown error occurred."
                            }
                        }
                    } catch (e: NumberParseException) {
                        errorMessage = "Invalid phone number format."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isSignUpEnabled
        ) {
            Text(text = "Sign Up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text(text = "Already have an account? Log in.")
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}