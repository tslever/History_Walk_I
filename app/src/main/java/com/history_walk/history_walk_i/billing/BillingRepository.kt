package com.history_walk.history_walk_i.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
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


class BillingRepository(context: Context) : PurchasesUpdatedListener {

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
    private var indexOfRetry = 0
    private val maximumDelay = 30_000L
    private val maximumNumberOfRetries = 5
    private val mutex = Mutex()
    private val productId = "nonconsumable_premium_upgrade"


    // Function acknowledgePurchase acknowledges a purchase.
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase acknowledged successfully.")
                billingListener?.onPurchaseSuccess()
            } else {
                val msg = "Failed to acknowledge purchase: ${billingResult.responseCode}"
                Log.e(TAG, msg)
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
                                handleBillingServiceDisconnected()
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


    /* Function handleBillingServiceDisconnected handles billing service disconnection
       with exponential backoff. */
    private fun handleBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected. Trying to reconnect...")
        connectToBillingClient(forceRetry = true)
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
                Log.i(TAG, "BillingClient setup successful. Now restoring any past purchases.")
                restorePurchases()
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
                            if (!purchase.isAcknowledged) {
                                acknowledgePurchase(purchase)
                            } else {
                                billingListener?.onPurchaseSuccess()
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


    fun setBillingListener(listener: BillingListener) {
        billingListener = listener
    }


    // Function startBillingConnection is called by an object of type ViewModelForHistoryWalkI.
    fun startBillingConnection() {
        connectToBillingClient(forceRetry = false)
    }
}