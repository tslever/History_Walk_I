package com.history_walk.history_walk_i.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application) {
    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)

    private fun getChosenDirectoryDocumentFile(): DocumentFile? {
        val uriString = getChosenDirectoryUriString() ?: return null
        val directoryUri = Uri.parse(uriString)
        val context = getApplication<Application>().applicationContext
        val docFile = DocumentFile.fromTreeUri(context, directoryUri)
        if (docFile == null) {
            Log.e("ViewModelForHistoryWalkI", "DocumentFile is null. Invalid Uri: $directoryUri")
        } else {
            Log.d("ViewModelForHistoryWalkI", "Obtained DocumentFile: ${docFile.uri}, canWrite=${docFile.canWrite()}")
        }
        return docFile
    }

    fun getChosenDirectoryUriString(): String? {
        return sharedPref.getString("chosenDirectoryUri", null)
    }

    fun getHistoricalStepsPerRealStepValue(): Int {
        val pace = getSelectedPace()
        val regex = Regex("1 real step = (\\d+) historical step[s]?")
        val match = regex.find(pace)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    fun getIndexAndNumberOfHistoricalStepsCompletedOfPresentEpisode(): Pair<Int, Int> {
        var indexOfPresentEpisode = 1
        var numberOfHistoricalStepsCompletedOfPresentEpisode = 0

        val directoryDocumentFile = getChosenDirectoryDocumentFile()
            ?: return Pair(indexOfPresentEpisode, numberOfHistoricalStepsCompletedOfPresentEpisode)

        if (!directoryDocumentFile.canWrite()) {
            Log.e("ViewModelForHistoryWalkI", "Directory is not writeable!")
            return Pair(indexOfPresentEpisode, numberOfHistoricalStepsCompletedOfPresentEpisode)
        }

        val fileName = "data_for_History_Walk_I.txt"
        val context = getApplication<Application>().applicationContext
        val contentResolver = context.contentResolver

        var targetFile = directoryDocumentFile.findFile(fileName)
        Log.d("ViewModelForHistoryWalkI", "Looking for file: $fileName")

        if (targetFile == null) {
            Log.d("ViewModelForHistoryWalkI", "$fileName not found, attempting to create file.")
            targetFile = directoryDocumentFile.createFile("text/plain", fileName)
            if (targetFile == null) {
                Log.e("ViewModelForHistoryWalkI", "Failed to create file $fileName. DocumentFile.createFile() returned null.")
                return Pair(indexOfPresentEpisode, numberOfHistoricalStepsCompletedOfPresentEpisode)
            } else {
                Log.d("ViewModelForHistoryWalkI", "File created: ${targetFile.uri}")
                contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    val initialContent = """index of present episode: 1
number of historical steps completed of present episode: 0""".trimIndent()
                    output.write(initialContent.toByteArray())
                    output.flush()
                    Log.d("ViewModelForHistoryWalkI", "Wrote initial content to file $fileName")
                }
            }
        }

        targetFile?.uri?.let { uri ->
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    for (line in lines) {
                        when {
                            line.startsWith("index of present episode:") -> {
                                indexOfPresentEpisode = line.substringAfter(":").trim().toIntOrNull() ?: 1
                            }
                            line.startsWith("number of historical steps completed of present episode:") -> {
                                numberOfHistoricalStepsCompletedOfPresentEpisode = line.substringAfter(":").trim().toIntOrNull() ?: 0
                            }
                        }
                    }
                    Log.d("ViewModelForHistoryWalkI", "Read file content: index=$indexOfPresentEpisode, steps=$numberOfHistoricalStepsCompletedOfPresentEpisode")
                }
            }
        }
        return Pair(indexOfPresentEpisode, numberOfHistoricalStepsCompletedOfPresentEpisode)
    }

    fun getSelectedPace(): String {
        return sharedPref.getString("selectedPace", "1 real step = 1 historical step") ?: "1 real step = 1 historical step"
    }

    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean("hasSeenHome", false)
    }

    fun isDirectoryChosen(): Boolean {
        return getChosenDirectoryUriString() != null
    }

    fun setChosenDirectoryUri(uriString: String) {
        viewModelScope.launch {
            sharedPref.edit().putString("chosenDirectoryUri", uriString).apply()
            Log.d("ViewModelForHistoryWalkI", "Chosen directory uri saved: $uriString")
        }
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
}