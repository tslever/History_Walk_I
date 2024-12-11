package com.history_walk.history_walk_i

import HomeScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.history_walk.history_walk_i.ui.theme.HistoryWalkITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        val hasSeenIntro = sharedPref.getBoolean("hasSeenIntro", false)

        if (!hasSeenIntro) {
            // Intro page has never been shown.
            // Show the intro page and once completed, update the SharedPreferences.
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        } else {
            // Intro has been seen before, show the home page directly.
            setContent {
                HistoryWalkITheme {
                    HomeScreen()
                }
            }
        }
    }
}