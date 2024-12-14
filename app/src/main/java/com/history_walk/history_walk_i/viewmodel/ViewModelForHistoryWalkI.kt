package com.history_walk.history_walk_i.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application) {

    val listOfPairsOfEpisodeTitlesAndNumbersOfHistoricalSteps = listOf(
        "The Alhambra" to 198,
        "Crossing Spain" to 396,
        "Journey to London" to 300,
        "Moving to Ludlow" to 600,
        "An Impoverished Captive" to 498,
        "Knight in Shining Armour" to 198,
        "Royal Progress" to 396,
        "General Catherine" to 300,
        "Producing an Heir" to 600,
        "The King's Great Matter" to 498,
        "Dowager Princess of Wales" to 198,
        "Funeral Progress" to 396
    )
    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)


    private fun getDocumentFileRepresentingChosenDirectory(): DocumentFile? {
        val stringRepresentingUriOfChosenDirectory = getStringRepresentingUriOfChosenDirectory() ?: return null
        val uriOfChosenDirectory = Uri.parse(stringRepresentingUriOfChosenDirectory)
        val context = getApplication<Application>().applicationContext
        val documentFile = DocumentFile.fromTreeUri(context, uriOfChosenDirectory)
        return documentFile
    }


    fun getIndexAndNumberOfHistoricalStepsCompletedOfPresentEpisode(): Pair<Int, Int> {
        var indexOfPresentEpisode = 1
        var numberOfHistoricalStepsCompletedOfPresentEpisode = 0

        val documentFileRepresentingChosenDirectory = getDocumentFileRepresentingChosenDirectory()
        val fileName = "data_for_History_Walk_I.txt"
        var documentFileRepresentingFile = documentFileRepresentingChosenDirectory?.findFile(fileName)

        val context = getApplication<Application>().applicationContext
        val contentResolver = context.contentResolver

        if (documentFileRepresentingFile == null) {
            documentFileRepresentingFile = documentFileRepresentingChosenDirectory?.createFile(
                "text/plain",
                fileName
            )
            val outputStream = documentFileRepresentingFile?.uri?.let {
                contentResolver.openOutputStream(it)
            }
            outputStream?.use { output ->
                val initialContent = """index of present episode: 1
number of historical steps completed of present episode: 0"""
                output.write(initialContent.toByteArray())
                output.flush()
            }
        }

        documentFileRepresentingFile?.uri?.let { uri ->
            val inputStream = contentResolver.openInputStream(uri)
            inputStream.use { theInputStream ->
                val inputStreamReader = InputStreamReader(theInputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                bufferedReader.use { theBufferedReader ->
                    val listOfLines = theBufferedReader.readLines()
                    for (line in listOfLines) {
                        when {
                            line.startsWith("index of present episode:") -> {
                                indexOfPresentEpisode = line.substringAfter(":").trim().toIntOrNull() ?: 1
                            }
                            line.startsWith("number of historical steps completed of present episode:") -> {
                                numberOfHistoricalStepsCompletedOfPresentEpisode = line.substringAfter(":").trim().toIntOrNull() ?: 0
                            }
                        }
                    }
                }
            }
        }
        return Pair(indexOfPresentEpisode, numberOfHistoricalStepsCompletedOfPresentEpisode)
    }


    fun getSelectedPace(): Int {
        return sharedPref.getInt("selectedPace", 1)
    }


    private fun getStringRepresentingUriOfChosenDirectory(): String? {
        return sharedPref.getString("uriOfChosenDirectory", null)
    }


    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean("hasSeenHome", false)
    }


    fun isDirectoryChosen(): Boolean {
        return getStringRepresentingUriOfChosenDirectory() != null
    }


    fun setSharedPreferenceRepresentingUriOfChosenDirectory(stringRepresentingUri: String) {
        viewModelScope.launch {
            sharedPref.edit().putString("uriOfChosenDirectory", stringRepresentingUri).apply()
        }
    }


    fun setHasSeenHome() {
        viewModelScope.launch {
            sharedPref.edit().putBoolean("hasSeenHome", true).apply()
        }
    }


    fun setSelectedPace(pace: Int) {
        viewModelScope.launch {
            sharedPref.edit().putInt("selectedPace", pace).apply()
        }
    }
}