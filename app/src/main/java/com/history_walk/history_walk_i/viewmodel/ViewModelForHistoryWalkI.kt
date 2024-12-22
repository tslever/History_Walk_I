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
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.history_walk.history_walk_i.billing.BillingRepository
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class ViewModelForHistoryWalkI (application: Application) : AndroidViewModel(application), BillingRepository.BillingListener {

    private var billingRepository: BillingRepository? = null

    private var currentActivityRef: WeakReference<Activity>? = null

    private var enrollmentSession: MultiFactorSession? = null

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

    private var multiFactorResolver: MultiFactorResolver? = null

    private val mutableLiveDataOfFirebaseUser = MutableLiveData<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: LiveData<FirebaseUser?> get() = mutableLiveDataOfFirebaseUser

    private val mutableLiveDataOfIndexOfPresentEpisode = MutableLiveData<Int>()
    val indexOfPresentEpisode: LiveData<Int> get() = mutableLiveDataOfIndexOfPresentEpisode

    private val mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded = MutableLiveData(false)
    val userHasUpgraded: LiveData<Boolean> = mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded

    private val mutableLiveDataOfMfaVerified = MutableLiveData(false)
    val mfaVerified: LiveData<Boolean> get() = mutableLiveDataOfMfaVerified

    private val mutableLiveDataOfNotification = MutableLiveData<String?>()
    val notification: LiveData<String?> get() = mutableLiveDataOfNotification

    private val sharedPref = application.getSharedPreferences("appPreferences", Context.MODE_PRIVATE)

    private var phoneNumberOfUser: String? = null

    private val mutableLiveDataOfVerificationId = MutableLiveData<String?>()
    val verificationId: LiveData<String?> get() = mutableLiveDataOfVerificationId

    private var verificationIdInternal: String? = null


    init {
        firebaseAuth.addAuthStateListener { theFirebaseAuth ->
            val currentUser = theFirebaseAuth.currentUser
            mutableLiveDataOfFirebaseUser.value = currentUser
            if (currentUser != null) {
                billingRepository?.endConnection()
                billingRepository = BillingRepository(getApplication(), currentUser.uid)
                billingRepository?.setBillingListener(this)
                billingRepository?.startBillingConnection()

                fetchIndicatorOfWhetherUserHasUpgraded()
                fetchIndexOfPresentEpisode()
                fetchPhoneNumberOfUser {

                }
            } else {
                billingRepository?.endConnection()
                billingRepository = null
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
                mutableLiveDataOfMfaVerified.postValue(false)
                multiFactorResolver = null
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
        val userDataDoc = firebaseFirestore.collection("users").document(uid).collection("data").document("userData")
        userDataDoc
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


    fun incrementEpisodeIndex() {
        val currentIndex = mutableLiveDataOfIndexOfPresentEpisode.value ?: 1
        val newIndex = currentIndex + 1
        setCurrentIndex(newIndex)
    }


    fun initiateMfaEnrollment(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            onResult(false, "User is not authenticated.")
            return
        }
        user.multiFactor.getSession()
            .addOnCompleteListener { sessionTask ->
                if (sessionTask.isSuccessful) {
                    enrollmentSession = sessionTask.result
                    sendMfaEnrollmentCode()
                    onResult(true, null)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Failed to get MFA session: ${sessionTask.exception?.message}")
                    mutableLiveDataOfNotification.postValue("Failed to initiate MFA enrollment: ${sessionTask.exception?.message}")
                    onResult(false, "Failed to initiate MFA enrollment: ${sessionTask.exception?.message}")
                }
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


    private fun resolveSignIn(credential: PhoneAuthCredential, onResult: (Boolean, String?) -> Unit) {
        val resolver = multiFactorResolver
        if (resolver == null) {
            Log.e("ViewModelForHistoryWalkI", "No MultiFactorResolver available for sign-in resolution.")
            mutableLiveDataOfNotification.postValue("No MultiFactorResolver available for sign-in resolution.")
            onResult(false, "No MultiFactorResolver available for sign-in resolution.")
            return
        }
        val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
        resolver.resolveSignIn(assertion)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "MFA Sign-In Successful")
                    mutableLiveDataOfMfaVerified.postValue(true)
                    onResult(true, null)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "MFA Sign-In Failed: ${task.exception?.message}")
                    mutableLiveDataOfNotification.postValue("MFA Sign-In Failed: ${task.exception?.message}")
                    onResult(false, "MFA Sign-In Failed: ${task.exception?.message}")
                }
            }
    }


    private fun sendMfaEnrollmentCode() {
        val session = enrollmentSession
        if (session == null) {
            Log.e("ViewModelForHistoryWalkI", "MFA Enrollment session is null.")
            mutableLiveDataOfNotification.postValue("MFA Enrollment session is not initialized.")
            return
        }
        val phoneNumber = phoneNumberOfUser
        if (phoneNumber.isNullOrEmpty()) {
            Log.e("ViewModelForHistoryWalkI", "User's phone number is not available.")
            mutableLiveDataOfNotification.postValue("User's phone number is not available.")
            return
        }
        val activity = currentActivityRef?.get()
        if (activity == null) {
            Log.e("ViewModelForHistoryWalkI", "Current Activity is not available.")
            mutableLiveDataOfNotification.postValue("Current Activity is not available.")
            return
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("ViewModelForHistoryWalkI", "Verification completed automatically.")
                enrollMfaWithCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("ViewModelForHistoryWalkI", "Verification failed: ${e.message}")
                mutableLiveDataOfNotification.postValue("MFA Verification failed: ${e.message}")
            }
            override fun onCodeSent(verificationIdParam: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationIdParam, token)
                Log.d("ViewModelForHistoryWalkI", "Verification code sent.")
                verificationIdInternal = verificationIdParam
                mutableLiveDataOfVerificationId.postValue(verificationIdParam)
                mutableLiveDataOfNotification.postValue("Verification code sent to $phoneNumber.")
            }
        }
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setMultiFactorSession(session)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun enrollMfaWithCredential(credential: PhoneAuthCredential) {
        val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
        firebaseAuth.currentUser?.multiFactor?.enroll(assertion, "Phone number MFA")
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "MFA Enrollment successful.")
                    mutableLiveDataOfNotification.postValue("MFA Enrollment successful.")
                    mutableLiveDataOfMfaVerified.postValue(true)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "MFA Enrollment failed: ${task.exception?.message}")
                    mutableLiveDataOfNotification.postValue("MFA Enrollment failed: ${task.exception?.message}")
                }
            }
    }


    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
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
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            if (!user.multiFactor.enrolledFactors.any { it is PhoneMultiFactorInfo }) {
                                initiateMfaEnrollment { success, error ->
                                    if (success) {
                                        onResult(true, null)
                                    } else {
                                        onResult(false, error)
                                    }
                                }
                            } else {
                                mutableLiveDataOfMfaVerified.postValue(true)
                                onResult(true, null)
                            }
                        } else {
                            user.sendEmailVerification()
                                .addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        Log.d("ViewModelForHistoryWalkI", "Verification email sent to ${user.email}")
                                        mutableLiveDataOfNotification.postValue("Please verify your email to proceed.")
                                        onResult(false, "Please verify your email to proceed.")
                                    } else {
                                        Log.e("ViewModelForHistoryWalkI", "Failed to send verification email: ${verifyTask.exception?.message}")
                                        onResult(false, "Failed to send verification email: ${verifyTask.exception?.message}")
                                    }
                                }
                        }
                    } else {
                        onResult(false, "User is null after sign-in.")
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthMultiFactorException) {
                        multiFactorResolver = exception.resolver
                        onResult(false, "MFA required")
                    } else {
                        Log.e("ViewModelForHistoryWalkI", "Email sign-in failed.", exception)
                        onResult(false, exception?.message)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Sign-in error: ${e.message}")
                onResult(false, e.message)
            }
    }


    fun signOut() {
        firebaseAuth.signOut()
        mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
        mutableLiveDataOfMfaVerified.postValue(false)
        multiFactorResolver = null
    }


    fun signUp(
        emailAddress: String,
        password: String,
        phoneNumber: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(emailAddress, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "Email sign-up successful.")
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        user.sendEmailVerification()
                            .addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    Log.d("ViewModelForHistoryWalkI", "Verification email sent to ${user.email}.")
                                    mutableLiveDataOfNotification.postValue("Verification email sent. Please verify your email before proceeding.")
                                    val uid = user.uid
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
                                            phoneNumberOfUser = phoneNumber
                                            onResult(true, "Sign-up successful. Please verify your email before enrolling MFA.")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ViewModelForHistoryWalkI", "Error saving user data: $e")
                                            onResult(true, "Sign-up successful but failed to save user data.")
                                        }
                                } else {
                                    Log.e("ViewModelForHistoryWalkI", "Failed to send verification email: ${verifyTask.exception?.message}")
                                    onResult(false, "Failed to send verification email: ${verifyTask.exception?.message}")
                                }
                            }
                    } else {
                        onResult(true, "Sign-up successful but UID not found for data storage.")
                    }
                } else {
                    Log.e("ViewModelForHistoryWalkI", "Email sign-up failed.", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewModelForHistoryWalkI", "Sign-up error: ${e.message}")
                onResult(false, e.message)
            }
    }


    fun verifyMfaCode(code: String, onResult: (Boolean, String?) -> Unit) {
        val resolver = multiFactorResolver
        if (resolver == null) {
            Log.e("ViewModelForHistoryWalkI", "No MultiFactorResolver available.")
            mutableLiveDataOfNotification.postValue("No MultiFactorResolver available.")
            onResult(false, "No MultiFactorResolver available.")
            return
        }

        val verificationId = verificationIdInternal
        if (verificationId.isNullOrEmpty()) {
            Log.e("ViewModelForHistoryWalkI", "Verification ID is not available.")
            mutableLiveDataOfNotification.postValue("Verification ID is not available.")
            onResult(false, "Verification ID is not available.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        resolveSignIn(credential, onResult)
    }


    fun verifyMfaEnrollmentCode(code: String, onResult: (Boolean, String?) -> Unit) {
        val session = enrollmentSession
        if (session == null) {
            Log.e("ViewModelForHistoryWalkI", "MFA Enrollment session is null.")
            mutableLiveDataOfNotification.postValue("MFA Enrollment session is not initialized.")
            onResult(false, "MFA Enrollment session is not initialized.")
            return
        }
        val verificationId = verificationIdInternal
        if (verificationId.isNullOrEmpty()) {
            Log.e("ViewModelForHistoryWalkI", "Verification ID is not available.")
            mutableLiveDataOfNotification.postValue("Verification ID is not available.")
            onResult(false, "Verification ID is not available.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        enrollMfaWithCredential(credential)
        onResult(true, null)
    }

}