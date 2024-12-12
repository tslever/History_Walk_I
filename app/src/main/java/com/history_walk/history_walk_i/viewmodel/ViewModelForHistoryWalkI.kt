package com.history_walk.history_walk_i.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application) {
    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)

    fun getSelectedPace(): String {
        return sharedPref.getString("selectedPace", "1 real step = 1 historical step") ?: "1 real step = 1 historical step"
    }

    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean("hasSeenHome", false)
    }

    fun setHasSeenHome() {
        viewModelScope.launch {
            sharedPref.edit().putBoolean("hasSeenHome", true).apply()
        }
    }

    fun setSelectedPace(pace: String) {
        viewModelScope.launch {
            sharedPref.edit().putString("selectedPace", pace).apply()
        }
    }

    fun getHistoricalStepsPerRealStepValue(): Int {
        val pace = getSelectedPace()
        val regex = Regex("1 real step = (\\d+) historical step[s]?")
        val match = regex.find(pace)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }
}