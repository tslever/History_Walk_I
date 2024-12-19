package com.history_walk.history_walk_i.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.api.Billing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.history_walk.history_walk_i.billing.BillingRepository
import kotlinx.coroutines.launch


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application), BillingRepository.BillingListener {

    private var billingRepository: BillingRepository? = null
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFirestore = FirebaseFirestore.getInstance()
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
    private val mutableLiveDataOfFirebaseUser = MutableLiveData<FirebaseUser?>()
    val firebaseUser: LiveData<FirebaseUser?> get() = mutableLiveDataOfFirebaseUser
    private val mutableLiveDataOfIndexOfPresentEpisode = MutableLiveData<Int>()
    val indexOfPresentEpisode: LiveData<Int> get() = mutableLiveDataOfIndexOfPresentEpisode
    private val mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded = MutableLiveData(false)
    val userHasUpgraded: LiveData<Boolean> = mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded
    private val mutableLiveDataOfNotification = MutableLiveData<String?>()
    val notification: LiveData<String?> get() = mutableLiveDataOfNotification
    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)


    init {
        mutableLiveDataOfFirebaseUser.value = firebaseAuth.currentUser
        firebaseAuth.addAuthStateListener { theFirebaseAuth ->
            val currentUser = theFirebaseAuth.currentUser
            mutableLiveDataOfFirebaseUser.value = currentUser
            if (currentUser != null) {
                billingRepository?.endConnection()
                billingRepository = BillingRepository(application.applicationContext, currentUser.uid)
                billingRepository?.setBillingListener(this)
                billingRepository?.startBillingConnection()
                fetchIndexOfPresentEpisode()
                fetchIndicatorOfWhetherUserHasUpgraded()
            } else {
                billingRepository?.endConnection()
                billingRepository = null
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
            }
        }
    }


    fun clearNotification() {
        mutableLiveDataOfNotification.postValue(null)
    }


    private fun fetchIndexOfPresentEpisode() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            mutableLiveDataOfIndexOfPresentEpisode.postValue(1)
            return
        }
        val userEpisodeDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("episodeIndex")
        userEpisodeDoc.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val index = document.getLong("currentIndex")?.toInt() ?: 1
                    mutableLiveDataOfIndexOfPresentEpisode.postValue(index)
                } else {
                    mutableLiveDataOfIndexOfPresentEpisode.postValue(1)
                    userEpisodeDoc.set(mapOf("currentIndex" to 1))
                        .addOnFailureListener { e ->
                            Log.e("ViewModelForHistoryWalkI", "Error initializing index: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error fetching index: $e")
                mutableLiveDataOfIndexOfPresentEpisode.postValue(1)
            }
    }


    private fun fetchIndicatorOfWhetherUserHasUpgraded() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
            return
        }
        val premiumDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("premiumStatus")
        premiumDoc.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val premium = document.getBoolean("isPremium") ?: false
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(premium)
                } else {
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
                    premiumDoc.set(mapOf("isPremium" to false))
                        .addOnFailureListener { e ->
                            Log.e("ViewModelForHistoryWalkI", "Error initializing premium status: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error fetching premium status: $e")
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
            }
    }


    fun getSelectedNumberOfSteps(): Int {
        return sharedPref.getInt("selectedNumberOfSteps", 70_000)
    }


    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean("hasSeenHome", false)
    }


    fun incrementEpisodeIndex() {
        val currentIndex = mutableLiveDataOfIndexOfPresentEpisode.value ?: 1
        val newIndex = currentIndex + 1
        mutableLiveDataOfIndexOfPresentEpisode.value = newIndex

        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            return
        }
        val userEpisodeDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("episodeIndex")
        userEpisodeDoc.set(mapOf("currentIndex" to newIndex))
            .addOnSuccessListener {
                Log.d("ViewModelForHistoryWalkI", "Index updated to $newIndex")
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error updating index: $e")
            }
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
        viewModelScope.launch {
            setIndicatorOfWhetherUserHasUpgraded(true)
        }
    }


    fun purchasePremium(activity: Activity) {
        billingRepository?.launchPurchaseFlow(activity)
            ?: run {
                Log.e("ViewModelForHistoryWalkI", "BillingRepository is not initialized.")
                mutableLiveDataOfNotification.postValue("BillingRepository is not initialized.")
            }
    }


    fun setHasSeenHome() {
        viewModelScope.launch {
            sharedPref.edit().putBoolean("hasSeenHome", true).apply()
        }
    }


    private fun setIndicatorOfWhetherUserHasUpgraded(status: Boolean) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated")
            return
        }
        val premiumDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("premiumStatus")
        premiumDoc.set(mapOf("isPremium" to status))
            .addOnSuccessListener {
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(status)
                Log.d("ViewModelForHistoryWalkI", "Premium status set to $status")
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error setting premium status: $e")
                mutableLiveDataOfNotification.postValue("Error setting premium status: $e")
            }
    }


    fun setSelectedNumberOfSteps(numberOfSteps: Int) {
        viewModelScope.launch {
            sharedPref.edit().putInt("selectedNumberOfSteps", numberOfSteps).apply()
        }
    }


    fun signIn(emailAddress: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(emailAddress, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "Email sign-in successful.")
                    onResult(true, null)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Email sign-in failed.", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }


    fun signOut() {
        firebaseAuth.signOut()
    }


    fun signUp(emailAddress: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(emailAddress, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "Email sign-up successful.")
                    setIndicatorOfWhetherUserHasUpgraded(false)
                    onResult(true, null)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Email sign-up failed", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }
}