package com.history_walk.history_walk_i.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val delayBeforeInitialRetry = 3000L // milliseconds
    private var indexOfRetry = 0
    private val maximumNumberOfRetries = 5
    private val mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded = MutableLiveData(false)
    private val mutex = Mutex()
    private val productId = "premium_upgrade"
    val userHasUpgraded: LiveData<Boolean> = mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded


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
                /* TODO: Consider performing additional actions including
                    granting purchase items or benefits to user,
                    notifying user, and
                    syncing purchase with a server.
                    */
            } else {
                Log.e(
                    "BillingRepository",
                    "Failed to acknowledge purchase: ${billingResult.responseCode}"
                )
                /* TODO: Consider performing additional actions including
                    retrying,
                    notifying user,
                    logging detailed error information, and
                    handling specific error codes. */
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
                        mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(true)
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
                // TODO: Notify user.
                return@launch
            }

            val delayBeforeRetry = delayBeforeInitialRetry * (2.0.pow(indexOfRetry.toDouble())).toLong()
            Log.i(
                "BillingRepository",
                "Retrying BillingClient connection in $delayBeforeRetry ms (Attempt $indexOfRetry)"
            )
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
                // TODO: Notify user.
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR -> {
                Log.e(
                    "BillingRepository",
                    "Billing setup failed with recoverable response code: ${billingResult.responseCode}"
                )
                // TODO: Notify user.
                handleBillingServiceDisconnected()
            }

            // TODO: Handle other potential response codes.

            else -> {
                Log.e(
                    "BillingRepository",
                    "Billing setup failed with unknown response code: ${billingResult.responseCode}"
                )
                // TODO: Notify user.
                handleBillingServiceDisconnected()
            }
        }
    }


    // Function handlePurchase handles a purchase.
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(productId)) {
            mutableLiveDataOfIndicatorOfWhetherUserHasUpgraded.postValue(true)
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
                // TODO: Notify user.
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
            // TODO: Notify user.
        }

        // TODO: Handle other potential response codes.

        else {
            Log.e(
                "BillingRepository",
                "Purchase update failed with response code: ${billingResult.responseCode}"
            )
            // TODO: Notify user.
        }
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