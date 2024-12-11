package com.history_walk.history_walk_i

import HomeScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.history_walk.history_walk_i.ui.theme.ThemeForHomeScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
        val hasSeenIntro = sharedPref.getBoolean("hasSeenIntro", false)
        val hasSeenHome = sharedPref.getBoolean("hasSeenHome", false)

        when {
            !hasSeenIntro -> {
                startActivity(Intent(this, IntroActivity::class.java))
                finish()
            }
            !hasSeenHome -> {
                setContent {
                    ThemeForHomeScreen {
                        HomeScreen(
                            onContinue = {
                                with(sharedPref.edit()) {
                                    putBoolean("hasSeenHome", true)
                                    apply()
                                }
                                startActivity(Intent(this, EpisodesActivity::class.java))
                                finish()
                            }
                        )
                    }
                }
            }
            else -> {
                startActivity(Intent(this, EpisodesActivity::class.java))
                finish()
            }
        }
    }
}