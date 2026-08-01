package com.linedraw.game.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing (v7) wrapper for the one-time, non-consumable
 * "Remove Ads" purchase.
 *
 * The product must be created in Play Console with id [REMOVE_ADS_PRODUCT_ID]
 * and priced at USD 1.99 (the "\$2 to remove ads" offer) — see README.
 */
class BillingManager(
    context: Context,
    private val onAdsRemovedChanged: suspend (Boolean) -> Unit,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _removeAdsPrice = MutableStateFlow<String?>(null)
    /** Localized price string (e.g. "$1.99") once product details are loaded. */
    val removeAdsPrice: StateFlow<String?> = _removeAdsPrice

    private val _billingAvailable = MutableStateFlow(false)
    val billingAvailable: StateFlow<Boolean> = _billingAvailable

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingAvailable.value = true
                    queryProductDetails()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingAvailable.value = false
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsList.firstOrNull()
                _removeAdsPrice.value =
                    productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /** Launches the Google Play purchase sheet for Remove Ads. */
    fun launchRemoveAdsPurchase(activity: Activity) {
        val details = productDetails ?: run {
            queryProductDetails()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    /** Re-checks owned purchases (also wired to the "Restore purchases" button). */
    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.any { purchase ->
                    purchase.products.contains(REMOVE_ADS_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                purchases.forEach { acknowledgeIfNeeded(it) }
                scope.launch { onAdsRemovedChanged(owned) }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
            return
        }
        for (purchase in purchases) {
            if (
                purchase.products.contains(REMOVE_ADS_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            ) {
                acknowledgeIfNeeded(purchase)
                scope.launch { onAdsRemovedChanged(true) }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
        }
    }

    companion object {
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
        private const val TAG = "BillingManager"
    }
}
