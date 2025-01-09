package com.history_walk.history_walk_i.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.history_walk.history_walk_i.extensions.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.pow


class BillingRepository(
    context: Context,
    private val usersUid: String
) : PurchasesUpdatedListener {


    interface BillingListener {
        fun onPurchaseSuccess()
        fun onPurchaseFailure(responseCode: Int)
        fun onNotifyUser(message: String)
    }


    companion object {
        private const val TAG = "BillingRepository"
    }


    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    private var billingListener: BillingListener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val delayBeforeInitialRetry = 3000L // milliseconds
    private val firestore = FirebaseFirestore.getInstance()
    private var indexOfRetry = 0
    private val maximumDelay = 30_000L
    private val maximumNumberOfRetries = 5
    private val mutex = Mutex()
    private val productId = "consumable_premium_upgrade"


    // Function acknowledgePurchase acknowledges a purchase.
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase acknowledged successfully.")
                coroutineScope.launch {
                    associatePurchaseWithUserSusp()
                    consumePurchase(purchase)
                }
            } else {
                val msg = "Failed to acknowledge purchase: ${billingResult.responseCode}"
                Log.e(TAG, msg)
                billingListener?.onNotifyUser(msg)
            }
        }
    }


    private suspend fun associatePurchaseWithUserSusp() {
        try {
            getUserDataDoc(usersUid)
                .update(mapOf("isPremium" to true))
                .await()
            Log.d(TAG, "Premium status updated in Firestore for user $usersUid")
            withContext(Dispatchers.Main) {
                billingListener?.onPurchaseSuccess()
            }
        } catch (e: Exception) {
            val msg = "Error updating premium status: $e"
            Log.e(TAG, msg)
            withContext(Dispatchers.Main) {
                billingListener?.onNotifyUser(msg)
            }
        }
    }


    // Function checkExistingPurchases checks existing purchases to restore premium status.
    private suspend fun checkExistingPurchasesSusp() {
        try {
            val documentSnapshot = getUserDataDoc(usersUid).get().await()
            if (documentSnapshot.exists()) {
                val isPremium = documentSnapshot.getBoolean("isPremium") ?: false
                if (isPremium) {
                    withContext(Dispatchers.Main) {
                        billingListener?.onPurchaseSuccess()
                    }
                } else {
                    Log.i(TAG, "User does not have premium status.")
                }
            } else {
                Log.i(TAG, "User data document does not exist.")
            }
        } catch (e: Exception) {
            val msg = "Error fetching user data: $e"
            Log.e(TAG, msg)
            withContext(Dispatchers.Main) {
                billingListener?.onNotifyUser(msg)
            }
        }
    }


    private fun connectToBillingClient(forceRetry: Boolean) {
        coroutineScope.launch {
            if (forceRetry) { indexOfRetry++ }
            if (indexOfRetry > maximumNumberOfRetries) {
                val msg = "Exceeded maximum retry attempts for BillingClient connection."
                Log.e(TAG, msg)
                withContext(Dispatchers.Main) {
                    billingListener?.onNotifyUser(msg)
                }
                return@launch
            }
            if (forceRetry) {
                val delayBeforeRetry = minOf(
                    delayBeforeInitialRetry * (2.0.pow(indexOfRetry.toDouble())).toLong(),
                    maximumDelay
                )
                Log.i(TAG, "Retrying BillingClient connection in $delayBeforeRetry ms (Attempt $indexOfRetry)")
                withContext(Dispatchers.Main) {
                    billingListener?.onNotifyUser("Retrying BillingClient connection in $delayBeforeRetry ms (Attempt $indexOfRetry)")
                }
                delay(delayBeforeRetry)
            }
            mutex.withLock {
                if (!billingClient.isReady) {
                    billingClient.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingServiceDisconnected() {
                                connectToBillingClient(forceRetry = true)
                            }
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                handleBillingSetupFinished(billingResult)
                            }
                        }
                    )
                } else {
                    indexOfRetry = 0
                }
            }
        }
    }


    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams
            .newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase consumed successfully: $purchaseToken")
            } else {
                val msg = "Failed to consume purchase: ${billingResult.responseCode}"
                Log.e(TAG, msg)
                billingListener?.onNotifyUser(msg)
            }
        }
    }


    // Function endConnection ends the connection of this listener's billing client.
    // endConnection is called by an instance of ViewModelForHistoryWalkI.
    fun endConnection() {
        coroutineScope.launch {
            mutex.withLock {
                if (billingClient.isReady) {
                    billingClient.endConnection()
                    Log.i(TAG, "BillingClient connection ended.")
                }
            }
            coroutineScope.cancel()
        }
    }


    private suspend fun ensureBillingClientConnected() {
        mutex.withLock {
            if (!billingClient.isReady) {
                connectToBillingClient(forceRetry = false)
            }
        }
        delay(1_000L)
    }


    // Function getProductDetails gets product details.
    private fun getProductDetails(callback: (ProductDetails?) -> Unit) {
        val product = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(product)
            .build()

        coroutineScope.launch {
            val productDetailsResult = billingClient.queryProductDetails(queryProductDetailsParams)
            val productDetails =
                if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetailsResult.productDetailsList?.firstOrNull()
                } else { null }
            withContext(Dispatchers.Main) {
                callback(productDetails)
            }
        }
    }

    
    fun getUserDataDoc(uid: String): DocumentReference {
        return firestore.collection("users")
            .document(uid)
            .collection("data")
            .document("userData")
    }


    /* Function handleBillingServiceDisconnected handles billing service disconnection
       with exponential backoff. */
    private fun handleBillingServiceDisconnected() {
        coroutineScope.launch {
            indexOfRetry++
            if (indexOfRetry > maximumNumberOfRetries) {
                Log.e(TAG, "Exceeded maximum retry attempts for BillingClient connection.")
                withContext(Dispatchers.Main) {
                    billingListener?.onNotifyUser(
                        "Exceeded maximum retry attempts for BillingClient connection."
                    )
                }
                return@launch
            }

            val delayBeforeRetry = minOf(
                delayBeforeInitialRetry * (2.0.pow(indexOfRetry.toDouble())).toLong(),
                maximumDelay
            )
            Log.i(
                TAG,
                "Retrying BillingClient connection in $delayBeforeRetry ms (Attempt $indexOfRetry)"
            )
            withContext(Dispatchers.Main) {
                billingListener?.onNotifyUser(
                    "Retrying BillingClient connection in $delayBeforeInitialRetry ms (Attempt $indexOfRetry)"
                )
            }
            delay(delayBeforeRetry)

            mutex.withLock {
                if (!billingClient.isReady) {
                    billingClient.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingServiceDisconnected() {
                                handleBillingServiceDisconnected()
                            }
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                handleBillingSetupFinished(billingResult)
                            }
                        }
                    )
                }
            }
        }
    }


    /* Function handleBillingSetupFinished handles different response codes and
       decides whether to retry the connection. */
    private fun handleBillingSetupFinished(billingResult: BillingResult) {
        /* Possible response codes:
        BILLING_UNAVAILABLE: 3
        DEVELOPER_ERROR: 5
        ERROR: 6
        FEATURE_NOT_SUPPORTED: -2
        ITEM_ALREADY_OWNED: 7
        ITEM_NOT_OWNED: 8
        ITEM_UNAVAILABLE: 4
        NETWORK_ERROR: 12
        OK: 0
        SERVICE_DISCONNECTED: -1
        SERVICE_TIMEOUT: -3
        SERVICE_UNAVAILABLE: 2
        USER_CANCELED: 1*/

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                indexOfRetry = 0
                Log.i(TAG, "BillingClient setup successful.")
                coroutineScope.launch {
                    checkExistingPurchasesSusp()
                }
            }

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                val msg = "Billing setup failed with irrecoverable response code: ${billingResult.responseCode}"
                notifyOfError(msg)
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                val msg = "Billing setup failed with recoverable response code: ${billingResult.responseCode}"
                notifyOfError(msg)
                connectToBillingClient(forceRetry = true)
            }
            else -> {
                val msg = "Billing setup failed with unknown response code: ${billingResult.responseCode}"
                notifyOfError(msg)
                connectToBillingClient(forceRetry = true)
            }
        }
    }


    // Function handlePurchase handles a purchase.
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(productId)) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        consumePurchase(purchase)
                        billingListener?.onPurchaseSuccess()
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    Log.i(TAG, "Purchase is pending.")
                    billingListener?.onNotifyUser("Purchase is pending.")
                }
                else -> {
                    Log.w(TAG, "Unhandled purchase state: ${purchase.purchaseState}")
                }
            }
        }
    }


    // Function launchPurchaseFlow is called by an object of type ViewModelForHistoryWalkI.
    fun launchPurchaseFlow(activity: Activity) {
        getProductDetails { productDetails ->
            if (productDetails != null) {
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()
                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                val msg = "Product details not found for Product ID: $productId"
                Log.e(TAG, msg)
                billingListener?.onNotifyUser(msg)
            }
        }
    }


    private fun notifyOfError(message: String) {
        Log.e(TAG, message)
        billingListener?.onNotifyUser(message)
    }


    /* Function onPurchasesUpdated delivers the result of the purchase operation to this listener.
       onPurchasesUpdated is called by Google Play. */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User canceled the purchase.")
            billingListener?.onNotifyUser("You canceled your purchase.")
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.i(TAG, "User already owns this item.")
            billingListener?.onNotifyUser("You already own this item.")
            restorePurchases()
        } else {
            val msg = "Purchase update failed with response code: ${billingResult.responseCode}"
            Log.e(TAG, msg)
            billingListener?.onPurchaseFailure(billingResult.responseCode)
            billingListener?.onNotifyUser(msg)
        }
    }


    fun restorePurchases() {
        coroutineScope.launch {
            ensureBillingClientConnected()
            val queryPurchasesParams = QueryPurchasesParams
                .newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { purchase ->
                        if (
                            purchase.products.contains(productId) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                            ) {
                            coroutineScope.launch {
                                associatePurchaseIfNeededSusp(purchase)
                                consumePurchase(purchase)
                            }
                        }
                    }
                } else {
                    val msg = "Error restoring purchases: ${billingResult.responseCode}"
                    Log.e(TAG, msg)
                    billingListener?.onNotifyUser(msg)
                }
            }
        }
    }


    private suspend fun associatePurchaseIfNeededSusp(purchase: Purchase) {
        try {
            val docSnapshot = getUserDataDoc(usersUid).get().await()
            val isPremium = docSnapshot.getBoolean("isPremium") ?: false
            if (!isPremium) {
                withContext(Dispatchers.Main) {
                    acknowledgePurchase(purchase)
                }
            } else {
                Log.i(TAG, "User already has premium status.")
            }
        } catch (e: Exception) {
            val msg = "Error restoring purchase: $e"
            Log.e(TAG, msg)
            withContext(Dispatchers.Main) {
                billingListener?.onNotifyUser(msg)
            }
        }
    }


    fun setBillingListener(listener: BillingListener) {
        billingListener = listener
    }


    // Function startBillingConnection is called by an object of type ViewModelForHistoryWalkI.
    fun startBillingConnection() {
        connectToBillingClient(forceRetry = false)
    }
}