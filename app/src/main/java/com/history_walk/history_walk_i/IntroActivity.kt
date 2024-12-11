package com.history_walk.history_walk_i

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.history_walk.history_walk_i.MainActivity
import com.history_walk.history_walk_i.ui.theme.HistoryWalkITheme

class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HistoryWalkITheme {
                IntroScreen(
                    onContinue = {
                        // Update SharedPreferences
                        val sharedPref = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("hasSeenIntro", true)
                            apply()
                        }
                        // Navigate to MainActivity
                        startActivity(Intent(this@IntroActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
@Composable
fun IntroScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to the App!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("This is an introductory page shown only the first time the app is opened.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue) {
            Text("Continue")
        }
    }
}