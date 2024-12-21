package com.history_walk.history_walk_i.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.history_walk.history_walk_i.billing.BillingRepository
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application), BillingRepository.BillingListener {

    private var billingRepository: BillingRepository? = null

    private var currentActivityRef: WeakReference<Activity>? = null

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

    private val mutableLiveDataOfFirebaseUser = MutableLiveData<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: LiveData<FirebaseUser?> get() = mutableLiveDataOfFirebaseUser

    private val mutableLiveDataOfIndexOfPresentEpisode = MutableLiveData<Int>()
    val indexOfPresentEpisode: LiveData<Int> get() = mutableLiveDataOfIndexOfPresentEpisode

    private val mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded = MutableLiveData(false)
    val userHasUpgraded: LiveData<Boolean> = mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded

    private val mutableLiveDataOfNotification = MutableLiveData<String?>()
    val notification: LiveData<String?> get() = mutableLiveDataOfNotification

    private val mutableLiveDataOfIndicatorOfTfaVerified = MutableLiveData(false)
    val tfaVerified: LiveData<Boolean> get() = mutableLiveDataOfIndicatorOfTfaVerified

    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)

    private var phoneNumberOfUser: String? = null

    private var verificationId: String? = null


    init {
        firebaseAuth.addAuthStateListener { theFirebaseAuth ->
            val currentUser = theFirebaseAuth.currentUser
            mutableLiveDataOfFirebaseUser.value = currentUser
            if (currentUser != null) {
                val tfaWasPreviouslyVerified = sharedPref.getBoolean("tfaVerifiedForUser_${currentUser.uid}", false)
                mutableLiveDataOfIndicatorOfTfaVerified.value = tfaWasPreviouslyVerified

                billingRepository?.endConnection()
                billingRepository = BillingRepository(getApplication(), currentUser.uid)
                billingRepository?.setBillingListener(this)
                billingRepository?.startBillingConnection()
                fetchIndicatorOfWhetherUserHasUpgraded()
                fetchIndexOfPresentEpisode()

                fetchPhoneNumberOfUser {
                    val tfaVerified = mutableLiveDataOfIndicatorOfTfaVerified.value ?: false
                    if (!tfaVerified) {
                        sendCodeForTfaIfAvailable()
                    }
                }
            } else {
                billingRepository?.endConnection()
                billingRepository = null
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
                mutableLiveDataOfIndicatorOfTfaVerified.value = false
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
        val userDataDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
        userDataDoc.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val index = document.getLong("currentIndex")?.toInt() ?: 1
                    mutableLiveDataOfIndexOfPresentEpisode.postValue(index)
                } else {
                    mutableLiveDataOfIndexOfPresentEpisode.postValue(1)
                    userDataDoc.set(mapOf("currentIndex" to 1))
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
        val userDataDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
        userDataDoc.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val premium = document.getBoolean("isPremium") ?: false
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(premium)
                } else {
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
                    userDataDoc.set(mapOf("isPremium" to false))
                        .addOnFailureListener { e ->
                            Log.e("ViewModelForHistoryWalkI", "Error initializing premium status: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error fetching user data: $e")
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
            }
    }


    private fun fetchPhoneNumberOfUser(onComplete: () -> Unit) {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            Log.w("ViewModelForHistoryWalkI", "No user is logged in to fetch phone number.")
            onComplete()
            return
        }
        firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    phoneNumberOfUser = document.getString("phoneNumber")
                    Log.d("ViewModelForHistoryWalkI", "Phone number fetched: $phoneNumberOfUser")
                } else {
                    Log.w("ViewModelForHistoryWalkI", "No phone number found for user.")
                }
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error fetching phone number: $e")
                onComplete()
            }
    }


    fun getSelectedNumberOfSteps(): Int {
        return sharedPref.getInt("selectedNumberOfSteps", 70_000)
    }


    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean("hasSeenHome", false)
    }


    fun setCurrentIndex(newIndex: Int) {
        mutableLiveDataOfIndexOfPresentEpisode.value = newIndex
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            return
        }
        val userDataDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
        userDataDoc.update(mapOf("currentIndex" to newIndex))
            .addOnSuccessListener {
                Log.d("ViewModelForHistoryWalkI", "Index updated to $newIndex")
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Error updating index: $e")
            }
    }


    fun incrementEpisodeIndex() {
        val currentIndex = mutableLiveDataOfIndexOfPresentEpisode.value ?: 1
        val newIndex = currentIndex + 1
        setCurrentIndex(newIndex)
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


    fun sendCodeForTfaIfAvailable() {
        val phoneNumber = phoneNumberOfUser
        if (phoneNumber.isNullOrEmpty()) {
            Log.e("ViewModelForHistoryWalkI", "User phone number not available.")
            mutableLiveDataOfNotification.value = "User phone number not available."
            return
        }

        val currentActivity = currentActivityRef?.get()
        if (currentActivity == null) {
            Log.e("ViewModelForHistoryWalkI", "Activity context not available for TFA.")
            mutableLiveDataOfNotification.value = "Activity context not available for TFA."
            return
        }

        val options = PhoneAuthOptions
            .newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(currentActivity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null) {
                        currentUser.linkWithCredential(credential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onTwoFactorVerified()
                            } else {
                                mutableLiveDataOfNotification.value = "Auto-verification failed: ${task.exception?.message}"
                            }
                        }
                    } else {
                        mutableLiveDataOfNotification.value = "No user is signed in to link phone number."
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("ViewModelForHistoryWalkI", "Verification failed: $e")
                    mutableLiveDataOfNotification.value = "Verification failed: ${e.message}"
                }

                override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = vid
                    Log.d("ViewModelForHistoryWalkI", "Code sent to $phoneNumber")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
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
        val userDataDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
        userDataDoc.update("isPremium", status)
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
                    mutableLiveDataOfIndicatorOfTfaVerified.value = false
                    val uid = firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        sharedPref.edit().putBoolean("tfaVerifiedForUser_$uid", false).apply()
                    }
                    onResult(true, null)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Email sign-in failed.", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }


    fun signOut() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            currentUser.unlink(PhoneAuthProvider.PROVIDER_ID)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ViewModelForHistoryWalkI", "Phone provider unlinked successfully.")
                    } else {
                        Log.e("ViewModelForHistoryWalkI", "Failed to unlink phone provider: ${task.exception}")
                        mutableLiveDataOfNotification.postValue("Failed to unlink phone provider: ${task.exception?.message}")
                    }
                    firebaseAuth.signOut()
                    mutableLiveDataOfIndicatorOfTfaVerified.value = false
                    if (currentUser.uid.isNotEmpty()) {
                        sharedPref.edit().putBoolean("tfaVerifiedForUser_${currentUser.uid}", false).apply()
                    }
                }
        } else {
            firebaseAuth.signOut()
            mutableLiveDataOfIndicatorOfTfaVerified.value = false
        }
    }


    fun signUp(emailAddress: String, password: String, phoneNumber: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(emailAddress, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "Email sign-up successful.")
                    val uid = firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        val userDataDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
                        userDataDoc.set(
                            mapOf(
                                "isPremium" to false,
                                "phoneNumber" to phoneNumber,
                                "currentIndex" to 1
                            )
                        )
                            .addOnSuccessListener {
                                Log.d("ViewModelForHistoryWalkI", "User data added to Firestore.")
                                mutableLiveDataOfIndexOfPresentEpisode.postValue(1)
                                onResult(true, null)
                            }
                            .addOnFailureListener { e ->
                                Log.e("ViewModelForHistoryWalkI", "Error saving user data: $e")
                                onResult(true, "Sign-up successful but failed to save user data.")
                            }
                    } else {
                        onResult(true, "Sign-up successful but UID not found for data storage.")
                    }
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Email sign-up failed.", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }


    fun verifyCodeForTfa(codeEntered: String, onResult: (Boolean) -> Unit) {
        val vid = verificationId
        if (vid == null) {
            Log.e("ViewModelForHistoryWalkI", "No verificationId stored. Cannot verify code.")
            onResult(false)
            return
        }

        val credential = PhoneAuthProvider.getCredential(vid, codeEntered)
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e("ViewModelForHistoryWalkI", "No user is currently signed in.")
            onResult(false)
            return
        }
        currentUser
            .linkWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTwoFactorVerified()
                    onResult(true)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "TFA linking failed: ${task.exception}")
                    mutableLiveDataOfNotification.value = task.exception?.message ?: "TFA linking failed."
                    onResult(false)
                }
            }
    }

    private fun onTwoFactorVerified() {
        mutableLiveDataOfIndicatorOfTfaVerified.value = true
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            sharedPref.edit().putBoolean("tfaVerifiedForUser_$uid", true).apply()
        }
    }
}