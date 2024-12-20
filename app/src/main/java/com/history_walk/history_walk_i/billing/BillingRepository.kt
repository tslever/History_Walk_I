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
import com.google.firebase.firestore.FirebaseFirestore
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
    private val context: Context,
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


    // Function acknowledgePurchase acknowledge a purchase.
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase acknowledged successfully.")
                associatePurchaseWithUser(purchase.purchaseToken)
                consumePurchase(purchase)
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.responseCode}")
                billingListener?.onNotifyUser("Failed to acknowledge purchase: ${billingResult.responseCode}")
            }
        }
    }


    private fun associatePurchaseWithUser(purchaseToken: String) {
        val userDataDoc = firestore.collection("users").document(usersUid).collection("data").document("userData")
        userDataDoc.update(
            mapOf(
                "isPremium" to true,
                "purchaseToken" to purchaseToken
            )
        )
            .addOnSuccessListener {
                Log.d(TAG, "Premium status updated in Firestore for user $usersUid")
                billingListener?.onPurchaseSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating premium status: $e")
                billingListener?.onNotifyUser("Error updating premium status: $e")
            }
    }


    // Function checkExistingPurchases checks existing purchases to restore premium status.
    private fun checkExistingPurchases() {
        val userDataDoc = firestore.collection("users").document(usersUid).collection("data").document("userData")
        userDataDoc.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val isPremium = document.getBoolean("isPremium") ?: false
                    if (isPremium) {
                        billingListener?.onPurchaseSuccess()
                    } else {
                        Log.i(TAG, "User does not have premium status")
                    }
                } else {
                    Log.i(TAG, "User data document does not exist.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user data: $e")
                billingListener?.onNotifyUser("Error fetching user data: $e")
            }
    }


    fun clearBillingListener() {
        billingListener = null
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
                Log.e(TAG, "Failed to consume purchase: ${billingResult.responseCode}")
                billingListener?.onNotifyUser("Failed to consume purchase: ${billingResult.responseCode}")
            }
        }
    }


    fun dispose() {
        endConnection()
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


    // Function getProductDetails gets product details.
    private fun getProductDetails(
        callback: (ProductDetails?) -> Unit
    ) {
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
            val productDetails = if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsResult.productDetailsList?.firstOrNull()
            } else {
                null
            }

            withContext(Dispatchers.Main) {
                callback(productDetails)
            }
        }
    }


    /* Function handleBillingServiceDisconnected handles billing service disconnection
       with exponential backoff. */
    private fun handleBillingServiceDisconnected() {
        coroutineScope.launch {
            indexOfRetry++
            if (indexOfRetry > maximumNumberOfRetries) {
                Log.e(TAG, "Exceeded maximum retry attempts for BillingClient connection.")
                billingListener?.onNotifyUser("Exceeded maximum retry attempts for BillingClient connection.")
                return@launch
            }

            val delayBeforeRetry = minOf(delayBeforeInitialRetry * (2.0.pow(indexOfRetry.toDouble())).toLong(), maximumDelay)
            Log.i(TAG, "Retrying BillingClient connection in $delayBeforeRetry ms (Attempt $indexOfRetry)")
            billingListener?.onNotifyUser("Retrying BillingClient connection in $delayBeforeInitialRetry ms (Attempt $indexOfRetry)")
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
                checkExistingPurchases()
            }


            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                Log.e(
                    TAG,
                    "Billing setup failed with irrecoverable response code: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Billing setup failed with irrecoverable response code: ${billingResult.responseCode}")
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                Log.e(
                    TAG,
                    "Billing setup failed with recoverable response code: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Billing setup failed with recoverable error code: ${billingResult.responseCode}")
                handleBillingServiceDisconnected()
            }

            else -> {
                Log.e(
                    TAG,
                    "Billing setup failed with unknown response code: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Billing setup failed with unknown error code: ${billingResult.responseCode}")
                handleBillingServiceDisconnected()
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
                Log.e(
                    TAG,
                    "Product details not found for Product ID: $productId"
                )
                billingListener?.onNotifyUser("Product details not found for Product ID: $productId")
            }
        }
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
            Log.i(
                TAG,
                "User canceled the purchase."
            )
            billingListener?.onNotifyUser("You canceled your purchase.")
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.i(TAG, "User already owns this item.")
            billingListener?.onNotifyUser("You already own this item.")
            restorePurchases()
        }
        else {
            Log.e(
                TAG,
                "Purchase update failed with response code: ${billingResult.responseCode}"
            )
            billingListener?.onPurchaseFailure(billingResult.responseCode)
            billingListener?.onNotifyUser("Purchase update failed with response code: ${billingResult.responseCode}")
        }
    }


    fun restorePurchases() {
        coroutineScope.launch {
            mutex.withLock {
                if (!billingClient.isReady) {
                    billingClient.startConnection(object : BillingClientStateListener {
                        override fun onBillingServiceDisconnected() {
                            handleBillingServiceDisconnected()
                        }

                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            handleBillingSetupFinished(billingResult)
                        }
                    })
                    delay(1000L)
                }
            }

            val queryPurchasesParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { purchase ->
                        if (purchase.products.contains(productId) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            coroutineScope.launch {
                                associatePurchaseIfNeeded(purchase)
                                consumePurchase(purchase)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error querying purchases: ${billingResult.responseCode}")
                    billingListener?.onNotifyUser("Error restoring purchases: ${billingResult.responseCode}")
                }
            }
        }
    }


    private suspend fun associatePurchaseIfNeeded(purchase: Purchase) {
        withContext(Dispatchers.IO) {
            val userDataDoc = firestore.collection("users").document(usersUid).collection("data").document("userData")
            userDataDoc.get().addOnSuccessListener { document ->
                val isPremium = document.getBoolean("isPremium") ?: false
                if (!isPremium) {
                    acknowledgePurchase(purchase)
                } else {
                    Log.i(TAG, "User already has premium status.")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user data: $e")
                billingListener?.onNotifyUser("Error restoring purchases: $e")
            }
        }
    }


    fun setBillingListener(listener: BillingListener) {
        billingListener = listener
    }


    // Function startBillingConnection is called by an object of type ViewModelForHistoryWalkI.
    fun startBillingConnection() {
        coroutineScope.launch {
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
}