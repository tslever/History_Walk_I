package com.history_walk.history_walk_i.viewmodel

import android.app.Activity
import android.app.Application
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.history_walk.history_walk_i.billing.BillingRepository
import com.history_walk.history_walk_i.extensions.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class ViewModelForHistoryWalkI (application: Application) :
    AndroidViewModel(application),
    BillingRepository.BillingListener {

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

    private val mutableLiveDataOfIsLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> get() = mutableLiveDataOfIsLoading

    private val mutableLiveDataOfMfaVerified = MutableLiveData(false)
    val mfaVerified: LiveData<Boolean> get() = mutableLiveDataOfMfaVerified

    private val mutableLiveDataOfNotification = MutableLiveData<String?>()
    val notification: LiveData<String?> get() = mutableLiveDataOfNotification

    private val mutableLiveDataOfStepCounts = MutableLiveData<Map<Int, Int>>()
    val stepCounts: LiveData<Map<Int, Int>> get() = mutableLiveDataOfStepCounts

    private val sharedPref = application.getSharedPreferences("appPreferences", Application.MODE_PRIVATE)

    private var phoneNumberOfUser: String? = null

    private val mutableLiveDataOfVerificationId = MutableLiveData<String?>()
    val verificationId: LiveData<String?> get() = mutableLiveDataOfVerificationId

    private var verificationIdInternal: String? = null

    private val stepIncrementMutex = Mutex()
    private var lastStepTime: Long = 0L
    private val debounceInterval = 500L

    private val stepBuffer = mutableMapOf<Int, Int>()
    private val stepBufferMutex = Mutex()


    init {
        firebaseAuth.addAuthStateListener { theFirebaseAuth ->
            val currentUser = theFirebaseAuth.currentUser
            mutableLiveDataOfFirebaseUser.value = currentUser
            if (currentUser != null) {
                billingRepository?.endConnection()
                billingRepository = BillingRepository(getApplication(), currentUser.uid).also {
                    it.setBillingListener(this)
                    it.startBillingConnection()
                }

                fetchIndicatorOfWhetherUserHasUpgraded()
                fetchIndexOfPresentEpisode()
                fetchOrInitializeStepCounts()
                fetchPhoneNumberOfUser {

                }
                currentUser.reload().addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        currentUser.getIdToken(true).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                Log.d("ViewModelForHistoryWalkI", "User reloaded and ID token refreshed.")
                                if (currentUser.isEmailVerified) {
                                    if (currentUser.multiFactor.enrolledFactors.isNotEmpty()) {
                                        mutableLiveDataOfMfaVerified.postValue(true)
                                        Log.d("ViewModelForHistoryWalkI", "User is verified.")
                                    } else {
                                        mutableLiveDataOfMfaVerified.postValue(false)
                                        Log.d("ViewModelForHistoryWalkI", "MFA is not verified.")
                                    }
                                } else {
                                    mutableLiveDataOfMfaVerified.postValue(false)
                                    Log.d("ViewModelForHistoryWalkI", "User email is not verified.")
                                }
                            } else {
                                Log.e("ViewModelForHistoryWalkI", "Failed to refresh ID token: ${tokenTask.exception?.message}")
                                mutableLiveDataOfMfaVerified.postValue(false)
                            }
                            mutableLiveDataOfIsLoading.postValue(false)
                        }
                    } else {
                        Log.e("ViewModelForHistoryWalkI", "Failed to reload user: ${reloadTask.exception?.message}")
                        mutableLiveDataOfMfaVerified.postValue(false)
                        mutableLiveDataOfIsLoading.postValue(false)
                    }
                }
            } else {
                billingRepository?.endConnection()
                billingRepository = null
                mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(false)
                mutableLiveDataOfMfaVerified.postValue(false)
                multiFactorResolver = null
                mutableLiveDataOfIsLoading.postValue(false)
            }
        }
    }


    fun clearNotification() {
        mutableLiveDataOfNotification.postValue(null)
    }


    private fun enrollMfaWithCredential(credential: PhoneAuthCredential) {
        val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
        firebaseAuth.currentUser?.multiFactor?.enroll(assertion, "Phone number MFA")
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ViewModelForHistoryWalkI", "MFA Enrollment successful.")
                    mutableLiveDataOfMfaVerified.postValue(true)
                } else {
                    Log.e("ViewModelForHistoryWalkI", "MFA Enrollment failed: ${task.exception?.message}")
                    mutableLiveDataOfNotification.postValue("MFA Enrollment failed: ${task.exception?.message}")
                }
            }
    }


    private fun fetchIndexOfPresentEpisode() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfIndexOfPresentEpisode.value = 1
                }
                return@launch
            }
            val userDataDoc = getUserDataDoc(uid)
            try {
                val document = userDataDoc.get().await()
                if (document.exists()) {
                    val indexOfPresentEpisode = document.getLong("indexOfPresentEpisode")?.toInt() ?: 1
                    withContext(Dispatchers.Main) {
                        mutableLiveDataOfIndexOfPresentEpisode.value = indexOfPresentEpisode
                    }
                } else {
                    userDataDoc.set(mapOf("indexOfPresentEpisode" to 1)).await()
                    withContext(Dispatchers.Main) {
                        mutableLiveDataOfIndexOfPresentEpisode.value = 1
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModelForHistoryWalkI", "Error fetching index of present episode: $e")
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfIndexOfPresentEpisode.value = 1
                }
            }
        }
    }


    private suspend fun flushStepBuffer(buffer: Map<Int, Int>) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            return
        }
        val userDataDoc = getUserDataDoc(uid)
        val updates = buffer
            .mapKeys { "steps.${it.key}" }
            .mapValues { (_, stepCount) ->
                FieldValue.increment(stepCount.toLong())
            }
        try {
            userDataDoc.update(updates).await()
            Log.d("ViewModelForHistoryWalkI", "Step buffer flushed to Firestore: $updates")
        } catch (e: Exception) {
            Log.e("ViewModelForHistoryWalkI", "Error flushing step buffer: $e")
            withContext(Dispatchers.Main) {
                mutableLiveDataOfNotification.value = "Error updating step count: $e"
            }
        }
    }


    private fun fetchOrInitializeStepCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
                return@launch
            }
            val userDataDoc = getUserDataDoc(uid)
            try {
                val document = userDataDoc.get().await()
                if (document.exists()) {
                    val stepsMap = document.get("steps") as? Map<String, Long>
                    if (stepsMap != null) {
                        val stepCounts = stepsMap
                            .mapKeys { it.key.toIntOrNull() ?: 0 }
                            .mapValues { it.value.toInt() }
                        withContext(Dispatchers.Main) {
                            mutableLiveDataOfStepCounts.value = stepCounts
                        }
                    } else {
                        val defaultStepCounts = listOfTitlesOfEpisodes.indices.map { indexOfTitleOfEpisode ->
                            (indexOfTitleOfEpisode + 1).toString() to 0L
                        }.toMap()
                        userDataDoc.update("steps", defaultStepCounts).await()
                        withContext(Dispatchers.Main) {
                            mutableLiveDataOfStepCounts.value = defaultStepCounts
                                .mapKeys { it.key.toInt() }
                                .mapValues { it.value.toInt() }
                        }
                    }
                } else {
                    val defaultStepCounts = listOfTitlesOfEpisodes.indices.map { indexOfTitleOfEpisode ->
                        (indexOfTitleOfEpisode + 1).toString() to 0L
                    }.toMap()
                    userDataDoc.set(
                        mapOf(
                            "indexOfPresentEpisode" to 1,
                            "isPremium" to false,
                            "phoneNumber" to phoneNumberOfUser,
                            "steps" to defaultStepCounts
                        )
                    ).await()
                    withContext(Dispatchers.Main) {
                        mutableLiveDataOfStepCounts.value = defaultStepCounts
                            .mapKeys { it.key.toInt() }
                            .mapValues { it.value.toInt() }
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModelForHistoryWalkI", "Error fetching or initializing step counts: $e")
                val defaultStepCounts = listOfTitlesOfEpisodes.indices.map { indexOfTitleOfEpisode ->
                    (indexOfTitleOfEpisode + 1).toString() to 0L
                }.toMap()
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfStepCounts.value = defaultStepCounts
                        .mapKeys { it.key.toInt() }
                        .mapValues { it.value.toInt() }
                }
            }
        }
    }


    private fun fetchIndicatorOfWhetherUserHasUpgraded() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.value = false
                }
                return@launch
            }
            val userDataDoc = getUserDataDoc(uid)
            try {
                val documentSnapshot = userDataDoc.get().await()
                if (documentSnapshot.exists()) {
                    val premium = documentSnapshot.getBoolean("isPremium") ?: false
                    withContext(Dispatchers.Main) {
                        mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.value = premium
                    }
                } else {
                    userDataDoc.set(mapOf("isPremium" to false)).await()
                    withContext(Dispatchers.Main) {
                        mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModelForHistoryWalkI", "Error fetching user data: $e")
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.value = false
                }
            }
        }
    }


    private fun fetchPhoneNumberOfUser(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Log.w("ViewModelForHistoryWalkI", "No user is logged in to fetch phone number.")
                withContext(Dispatchers.Main) {
                    onComplete()
                }
                return@launch
            }
            val userDataDoc = getUserDataDoc(uid)
            try {
                val document = userDataDoc.get().await()
                if (document.exists()) {
                    phoneNumberOfUser = document.getString("phoneNumber")
                    Log.d("ViewModelForHistoryWalkI", "Phone number fetched: $phoneNumberOfUser")
                } else {
                    Log.w("ViewModelForHistoryWalkI", "No phone number found for user.")
                }
            } catch (e: Exception) {
                Log.e("ViewModelForHistoryWalkI", "Error fetching phone number: $e")
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }


    private fun finalizeMultiFactorSignIn(
        credential: PhoneAuthCredential,
        multiFactorResolver: MultiFactorResolver,
        onResult: (Boolean, String?) -> Unit
    ) {
        val multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential)
        multiFactorResolver
            .resolveSignIn(multiFactorAssertion)
            .addOnCompleteListener { resolveTask ->
                if (resolveTask.isSuccessful) {
                    mutableLiveDataOfMfaVerified.postValue(true)
                    onResult(true, null)
                } else {
                    val errorMsg = "finalizeMultiFactorSignIn failed: ${resolveTask.exception?.message}"
                    Log.e("ViewModelForHistoryWalkI", errorMsg)
                    onResult(false, errorMsg)
                }
            }
    }


    private fun finalizeSignInWithAssertion(
        credential: PhoneAuthCredential,
        callback: (String?) -> Unit
    ) {
        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    mutableLiveDataOfMfaVerified.postValue(true)
                    callback(null)
                } else {
                    val errorMsg = "finalizeSignInWithAssertion failed: " +
                            (signInTask.exception?.message ?: "Unknown error")
                    Log.e("ViewModelForHistoryWalkI", errorMsg)
                    callback(errorMsg)
                }
            }
    }


    fun getSelectedNumberOfSteps(): Int {
        return sharedPref.getInt("selectedNumberOfSteps", 70_000)
    }


    private fun getUserDataDoc(uid: String) =
        firebaseFirestore.collection("users")
            .document(uid)
            .collection("data")
            .document("userData")


    fun hasSeenHome(): Boolean {
        return sharedPref.getBoolean("hasSeenHome", false)
    }


    fun incrementIndexOfPresentEpisode() {
        val indexOfPresentEpisode = mutableLiveDataOfIndexOfPresentEpisode.value ?: 1
        setIndexOfPresentEpisode(indexOfPresentEpisode + 1)
    }


    fun incrementStepCount(episodeId: Int) {
        viewModelScope.launch {
            stepIncrementMutex.withLock {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastStepTime >= debounceInterval) {
                    lastStepTime = currentTime
                    var bufferCopy: Map<Int, Int>? = null
                    stepBufferMutex.withLock {
                        stepBuffer[episodeId] = (stepBuffer[episodeId] ?: 0) + 1
                        val totalSteps = stepBuffer.values.sum()
                        if (totalSteps >= 100) {
                            bufferCopy = stepBuffer.toMap()
                            stepBuffer.clear()
                        }
                    }
                    if (bufferCopy != null) {
                        flushStepBuffer(bufferCopy!!)
                    }
                    val currentSteps = stepCounts.value?.toMutableMap() ?: mutableMapOf()
                    currentSteps[episodeId] = (currentSteps[episodeId] ?: 0) + 1
                    withContext(Dispatchers.Main) {
                        mutableLiveDataOfStepCounts.value = currentSteps
                    }
                }
            }
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            flushStepBuffer(stepBuffer.toMap())
        }
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


    private fun sendCodeForSecondFactor(user: FirebaseUser, callback: (String?) -> Unit) {
        user.multiFactor.session
            .addOnCompleteListener { sessionTask ->
                if (!sessionTask.isSuccessful) {
                    callback("Failed to get multi-factor session: ${sessionTask.exception?.message}")
                    return@addOnCompleteListener
                }
                val session = sessionTask.result ?: return@addOnCompleteListener
                val phoneNumber = phoneNumberOfUser
                if (phoneNumber.isNullOrEmpty()) {
                    callback("User's phone number is not available.")
                    return@addOnCompleteListener
                }
                val activity = currentActivityRef?.get()
                if (activity == null) {
                    callback("Current Activity is not available.")
                    return@addOnCompleteListener
                }
                sendPhoneVerificationCode(
                    phoneNumber = phoneNumber,
                    activity = activity,
                    session = session,
                    timeOutSeconds = 60L,
                    onCodeSent = { verificationId ->
                        verificationIdInternal = verificationId
                        mutableLiveDataOfVerificationId.postValue(verificationId)
                        callback(null)
                    },
                    onVerificationComplete = { credential ->
                        finalizeSignInWithAssertion(credential) { err ->
                            callback(err)
                        }
                    },
                    onVerificationFailed = { e ->
                        callback("Second-factor verification failed: ${e.message}")
                    }
                )
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
        sendPhoneVerificationCode(
            phoneNumber = phoneNumber,
            activity = activity,
            session = session,
            timeOutSeconds = 60L,
            onCodeSent = { verificationIdParam ->
                Log.d("ViewModelForHistoryWalkI", "Verification code sent (MFA enrollment).")
                verificationIdInternal = verificationIdParam
                mutableLiveDataOfVerificationId.postValue(verificationIdParam)
            },
            onVerificationComplete = { credential ->
                Log.d("ViewModelForHistoryWalkI", "Verification completed automatically.")
                enrollMfaWithCredential(credential)
            },
            onVerificationFailed = { e ->
                Log.e("ViewModelForHistoryWalkI", "Verification failed: ${e.message}")
                mutableLiveDataOfNotification.postValue("MFA verification failed: ${e.message}")
            }
        )
    }


    private fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity,
        session: MultiFactorSession,
        timeOutSeconds: Long,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationComplete: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(timeOutSeconds, TimeUnit.SECONDS)
            .setActivity(activity)
            .setMultiFactorSession(session)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationComplete(credential)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    onVerificationFailed(e)
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }


    fun setIndexOfPresentEpisode(indexOfPresentEpisode: Int) {
        mutableLiveDataOfIndexOfPresentEpisode.value = indexOfPresentEpisode
        val uid = firebaseAuth.currentUser?.uid ?: run {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getUserDataDoc(uid).update("indexOfPresentEpisode", indexOfPresentEpisode).await()
                Log.d("ViewModelForHistoryWalkI", "Index of present episode updated to $indexOfPresentEpisode")
            } catch (e: Exception) {
                Log.e("ViewModelForHistoryWalkI", "Error updating index of present episode: $e")
            }
        }
    }


    fun setHasSeenHome() {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPref.edit().putBoolean("hasSeenHome", true).apply()
        }
    }


    private fun setIndicatorOfWhetherUserHasUpgraded(status: Boolean) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e("ViewModelForHistoryWalkI", "User is not authenticated")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getUserDataDoc(uid).update("isPremium", status).await()
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.value = status
                }
                Log.d("ViewModelForHistoryWalkI", "Premium status set to $status")
            } catch (e: Exception) {
                Log.e("ViewModelForHistoryWalkI", "Error setting premium status: $e")
                withContext(Dispatchers.Main) {
                    mutableLiveDataOfNotification.value = "Error setting premium status: $e"
                }
            }
        }
    }


    fun setSelectedNumberOfSteps(numberOfSteps: Int) {
        viewModelScope.launch {
            sharedPref.edit().putInt("selectedNumberOfSteps", numberOfSteps).apply()
        }
    }


    fun signIn(
        emailAddress: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(emailAddress, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        onResult(false, "User is null after sign-in.")
                        return@addOnCompleteListener
                    }
                    val hasPhoneFactor = user.multiFactor.enrolledFactors.any { it is PhoneMultiFactorInfo }
                    if (!hasPhoneFactor) {
                        onResult(false, "You don't have a phone for Two Factor Authentication.")
                    } else {
                        sendCodeForSecondFactor(user) { codeSentError ->
                            if (codeSentError != null) {
                                onResult(false, codeSentError)
                            } else {
                                onResult(false, "MFA required")
                            }
                        }
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthMultiFactorException) {
                        handleFirebaseAuthMultiFactorException(exception, onResult)
                    } else {
                        onResult(false, exception?.message)
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }


    private fun handleFirebaseAuthMultiFactorException(
        multiFactorException: FirebaseAuthMultiFactorException,
        onResult: (Boolean, String?) -> Unit
    ) {
        val multiFactorResolver = multiFactorException.resolver
        val phoneHint = multiFactorResolver
            .hints
            .filterIsInstance<PhoneMultiFactorInfo>()
            .firstOrNull()
        if (phoneHint == null) {
            onResult(false, "No phone factor found in multiFactorResolver.")
            return
        }
        val activity = currentActivityRef?.get()
        if (activity == null) {
            onResult(false, "Current activity is null.")
            return
        }
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setActivity(activity)
            .setMultiFactorSession(multiFactorResolver.session)
            .setMultiFactorHint(phoneHint)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    finalizeMultiFactorSignIn(credential, multiFactorResolver) { success, error ->
                        onResult(success, error)
                    }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    onResult(false, "MFA verification failed: ${e.message}")
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@ViewModelForHistoryWalkI.multiFactorResolver = multiFactorResolver
                    verificationIdInternal = verificationId
                    mutableLiveDataOfVerificationId.value = verificationId
                    onResult(false, "MFA required")
                }
            })
            .setTimeout(30L, TimeUnit.SECONDS)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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
                                    Log.d(
                                        "ViewModelForHistoryWalkI",
                                        "Verification email sent to ${user.email}."
                                    )
                                    val uid = user.uid
                                    viewModelScope.launch(Dispatchers.IO) {
                                        try {
                                            getUserDataDoc(uid).set(
                                                mapOf(
                                                    "isPremium" to false,
                                                    "phoneNumber" to phoneNumber,
                                                    "indexOfPresentEpisode" to 1
                                                )
                                            ).await()
                                            withContext(Dispatchers.Main) {
                                                mutableLiveDataOfIndexOfPresentEpisode.value = 1
                                                phoneNumberOfUser = phoneNumber
                                                onResult(true, "Sign-up successful. Please verify your email before enrolling MFA.")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ViewModelForHistoryWalkI", "Error saving user data: $e")
                                            withContext(Dispatchers.Main) {
                                                onResult(true, "Sign-up successful but failed to save user data.")
                                            }
                                        }
                                    }
                                } else {
                                    Log.e(
                                        "ViewModelForHistoryWalkI",
                                        "Failed to send verification email: ${verifyTask.exception?.message}"
                                    )
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