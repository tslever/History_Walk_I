package com.history_walk.history_walk_i.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.history_walk.history_walk_i.billing.BillingRepository
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application) {

    private val billingRepository = BillingRepository(application.applicationContext)
    val listOfTitlesOfEpisodes = listOf(
        "The Alhambra",
        "Crossing Spain",
        "Journey to London",
        "Moving to Ludlow",
        "An Impoverished Captive",
        "Knight in Shining Armour",
        "Royal Progress",
        "General Catherine",
        "Producing an Heir",
        "The King's Great Matter",
        "Dowager Princess of Wales",
        "Funeral Progress"
    )
    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
    val userHasUpgraded = billingRepository.userHasUpgraded


    init {
        billingRepository.startBillingConnection()
    }


    private fun getDocumentFileRepresentingChosenDirectory(): DocumentFile? {
        val stringRepresentingUriOfChosenDirectory = getStringRepresentingUriOfChosenDirectory() ?: return null
        val uriOfChosenDirectory = Uri.parse(stringRepresentingUriOfChosenDirectory)
        val context = getApplication<Application>().applicationContext
        val documentFile = DocumentFile.fromTreeUri(context, uriOfChosenDirectory)
        return documentFile
    }


    fun getIndexOfPresentEpisode(): Int {
        var indexOfPresentEpisode = 1

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
                val initialContent = "index of present episode: 1"
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
                        }
                    }
                }
            }
        }
        return indexOfPresentEpisode
    }


    fun getSelectedNumberOfSteps(): Int {
        return sharedPref.getInt("selectedNumberOfSteps", 70_000)
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


    override fun onCleared() {
        super.onCleared()
        billingRepository.endConnection()
    }


    fun purchasePremium(activity: Activity) {
        billingRepository.launchPurchaseFlow(activity)
    }


    fun setHasSeenHome() {
        viewModelScope.launch {
            sharedPref.edit().putBoolean("hasSeenHome", true).apply()
        }
    }


    fun setSelectedNumberOfSteps(numberOfSteps: Int) {
        viewModelScope.launch {
            sharedPref.edit().putInt("selectedNumberOfSteps", numberOfSteps).apply()
        }
    }


    fun setSharedPreferenceRepresentingUriOfChosenDirectory(stringRepresentingUri: String) {
        viewModelScope.launch {
            sharedPref.edit().putString("uriOfChosenDirectory", stringRepresentingUri).apply()
        }
    }
}