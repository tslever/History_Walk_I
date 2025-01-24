package com.history_walk.history_walk_i.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.history_walk.history_walk_i.billing.BillingRepository
import java.lang.ref.WeakReference


class ViewModelForHistoryWalkI (application: Application) :
    AndroidViewModel(application),
    BillingRepository.BillingListener {

    private var billingRepository: BillingRepository? = null

    private var currentActivityRef: WeakReference<Activity>? = null

    private val KEY_HAS_SEEN_HOME = "hasSeenHome"
    private val KEY_SELECTED_NUMBER_OF_STEPS = "selectedNumberOfSteps"
    private val KEY_INDEX_OF_PRESENT_EPISODE = "indexOfPresentEpisode"
    private val KEY_USER_HAS_UPGRADED = "userHasUpgraded"
    private val KEY_STEPS_PREFIX = "steps_episode_"

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

    private val mutableLiveDataOfIndexOfPresentEpisode = MutableLiveData<Int>()
    val indexOfPresentEpisode: LiveData<Int> get() = mutableLiveDataOfIndexOfPresentEpisode

    private val mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded = MutableLiveData<Boolean>()
    val userHasUpgraded: LiveData<Boolean> = mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded

    val mutableLiveDataOfNotification = MutableLiveData<String?>()
    val notification: LiveData<String?> get() = mutableLiveDataOfNotification

    private val mutableLiveDataOfStepCounts = MutableLiveData<Map<Int, Int>>()
    val stepCounts: LiveData<Map<Int, Int>> get() = mutableLiveDataOfStepCounts

    private val sharedPref: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences(
            "appPreferences", Context.MODE_PRIVATE
        )
    }


    init {
        mutableLiveDataOfIndexOfPresentEpisode.value = loadIndexOfPresentEpisode()
        mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.value = loadIndicatorOfWhetherUserHasUpgraded()
        mutableLiveDataOfStepCounts.value = loadMapOfStepCounts()

        billingRepository = BillingRepository(getApplication()).also {
            it.setBillingListener(this@ViewModelForHistoryWalkI)
            it.startBillingConnection()
        }
    }


    fun clearNotification() {
        mutableLiveDataOfNotification.postValue(null)
    }


    fun getSelectedNumberOfSteps(): Int {
        return sharedPref.getInt("selectedNumberOfSteps", 70_000)
    }


    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean(KEY_HAS_SEEN_HOME, false)
    }


    fun incrementIndexOfPresentEpisode() {
        val current = (indexOfPresentEpisode.value ?: 1) + 1
        mutableLiveDataOfIndexOfPresentEpisode.value = current
        sharedPref.edit().putInt(KEY_INDEX_OF_PRESENT_EPISODE, current).apply()
    }


    fun incrementStepCount(episodeId: Int) {
        val currentMap = mutableLiveDataOfStepCounts.value?.toMutableMap() ?: mutableMapOf()
        val oldCount = currentMap[episodeId] ?: 0
        val newCount = oldCount + 1
        currentMap[episodeId] = newCount
        mutableLiveDataOfStepCounts.value = currentMap
        val epKey = "$KEY_STEPS_PREFIX$episodeId"
        sharedPref.edit().putInt(epKey, newCount).apply()
    }


    private fun loadMapOfStepCounts(): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        for (epIndex in 1..listOfTitlesOfEpisodes.size) {
            val epKey = "$KEY_STEPS_PREFIX$epIndex"
            val stepCountForEpisode = sharedPref.getInt(epKey, 0)
            counts[epIndex] = stepCountForEpisode
        }
        return counts
    }


    private fun loadIndexOfPresentEpisode(): Int {
        return sharedPref.getInt(KEY_INDEX_OF_PRESENT_EPISODE, 1)
    }


    private fun loadIndicatorOfWhetherUserHasUpgraded(): Boolean {
        return sharedPref.getBoolean(KEY_USER_HAS_UPGRADED, false)
    }


    override fun onCleared() {
        super.onCleared()
        billingRepository?.endConnection()
    }


    override fun onNotifyUser(message: String) {
        mutableLiveDataOfNotification.postValue(message)
    }


    override fun onPurchaseFailure(responseCode: Int) {
        Log.e("ViewModelForHistoryWalkI", "Purchase failed with response code: $responseCode")
        mutableLiveDataOfNotification.postValue("Purchase failed with response code: $responseCode")
    }


    override fun onPurchaseSuccess() {
        mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(true)
        sharedPref.edit().putBoolean(KEY_USER_HAS_UPGRADED, true).apply()
    }


    fun purchasePremium(activity: Activity) {
        billingRepository?.launchPurchaseFlow(activity) ?: run {
            Log.e("ViewModelForHistoryWalkI", "BillingRepository is not initialized.")
            mutableLiveDataOfNotification.postValue("BillingRepository is not initialized.")
        }
    }


    fun restorePurchases() {
        billingRepository?.restorePurchases() ?: run {
            mutableLiveDataOfNotification.postValue("Billing repository not available.")
        }
    }


    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }


    fun setIndexOfPresentEpisode(indexOfPresentEpisode: Int) {
        mutableLiveDataOfIndexOfPresentEpisode.value = indexOfPresentEpisode
        sharedPref.edit().putInt(KEY_INDEX_OF_PRESENT_EPISODE, indexOfPresentEpisode).apply()
    }


    fun setHasSeenHome() {
        sharedPref.edit().putBoolean(KEY_HAS_SEEN_HOME, true).apply()
    }


    fun setSelectedNumberOfSteps(numberOfSteps: Int) {
        sharedPref.edit().putInt(KEY_SELECTED_NUMBER_OF_STEPS, numberOfSteps).apply()
    }
}