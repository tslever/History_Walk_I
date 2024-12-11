package com.history_walk.history_walk_i

import HomeScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.history_walk.history_walk_i.ui.theme.ThemeForIntroScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        val hasSeenIntro = sharedPref.getBoolean("hasSeenIntro", false)

        if (!hasSeenIntro) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        } else {
            setContent {
                ThemeForIntroScreen {
                    HomeScreen()
                }
            }
        }
    }
}