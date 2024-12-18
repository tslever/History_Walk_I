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

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    private var billingListener: BillingListener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val delayBeforeInitialRetry = 3000L // milliseconds
    private var indexOfRetry = 0
    private val maximumNumberOfRetries = 5
    private val mutex = Mutex()
    private val productId = "premium_upgrade"


    // Function acknowledgePurchase acknowledge a purchase.
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(
                    "BillingRepository",
                    "Purchase acknowledged successfully."
                )
                billingListener?.onNotifyUser("Purchase acknowledged successfully.")
            } else {
                Log.e(
                    "BillingRepository",
                    "Failed to acknowledge purchase: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Failed to acknowledge purchase: ${billingResult.responseCode}")
            }
        }
    }


    // Function checkExistingPurchases checks existing purchases to restore premium status.
    private fun checkExistingPurchases() {
        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.products.contains(productId) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        billingListener?.onPurchaseSuccess()
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            } else {
                Log.e(
                    "BillingRepository",
                    "Error querying purchases: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Error querying purchases: ${billingResult.responseCode}")
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

        CoroutineScope(Dispatchers.IO).launch {
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
                Log.e(
                    "BillingRepository",
                    "Exceeded maximum retry attempts for BillingClient connection."
                )
                billingListener?.onNotifyUser("Exceeded maximum retry attempts for BillingClient connection.")
                return@launch
            }

            val delayBeforeRetry = delayBeforeInitialRetry * (2.0.pow(indexOfRetry.toDouble())).toLong()
            Log.i(
                "BillingRepository",
                "Retrying BillingClient connection in $delayBeforeRetry ms (Attempt $indexOfRetry)"
            )
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

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                indexOfRetry = 0
                checkExistingPurchases()
            }

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                Log.e(
                    "BillingRepository",
                    "Billing setup failed with irrecoverable response code: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Billing setup failed with irrecoverable response code: ${billingResult.responseCode}")
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR -> {
                Log.e(
                    "BillingRepository",
                    "Billing setup failed with recoverable response code: ${billingResult.responseCode}"
                )
                billingListener?.onNotifyUser("Billing setup failed with recoverable error code: ${billingResult.responseCode}")
                handleBillingServiceDisconnected()
            }

            // TODO: Handle other potential response codes.

            else -> {
                Log.e(
                    "BillingRepository",
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
            billingListener?.onPurchaseSuccess()
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
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
                    "BillingRepository",
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
                "BillingRepository",
                "User canceled the purchase flow."
            )
            billingListener?.onNotifyUser("User canceled the purchase flow.")
        }
        else {
            Log.e(
                "BillingRepository",
                "Purchase update failed with response code: ${billingResult.responseCode}"
            )
            billingListener?.onPurchaseFailure(billingResult.responseCode)
            billingListener?.onNotifyUser("Purchase update failed with response code: ${billingResult.responseCode}")
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