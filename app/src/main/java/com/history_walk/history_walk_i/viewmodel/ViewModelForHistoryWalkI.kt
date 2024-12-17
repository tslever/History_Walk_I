package com.history_walk.history_walk_i.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.history_walk.history_walk_i.billing.BillingRepository
import kotlinx.coroutines.launch


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val billingRepository = BillingRepository(application.applicationContext)
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
    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)
    val userHasUpgraded = billingRepository.userHasUpgraded


    init {
        billingRepository.startBillingConnection()
        mutableLiveDataOfFirebaseUser.value = firebaseAuth.currentUser
        firebaseAuth.addAuthStateListener { theFirebaseAuth ->
            mutableLiveDataOfFirebaseUser.value = theFirebaseAuth.currentUser
            if (theFirebaseAuth.currentUser != null) {
                fetchIndexOfPresentEpisode()
            }
        }
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
                    onResult(true, null)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Email sign-up failed", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }
}