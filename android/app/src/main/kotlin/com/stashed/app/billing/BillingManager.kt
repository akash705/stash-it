package com.stashed.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.stashed.app.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play Billing for Stashed Premium.
 *
 * Products (configured in Google Play Console):
 *   - stashed_premium_monthly  — ₹199/month subscription
 *   - stashed_premium_annual   — ₹1,499/year subscription
 *   - stashed_premium_lifetime — ₹2,999 one-time purchase
 *
 * The manager exposes reactive state that the PaywallScreen observes.
 * Purchase verification happens locally (client-side) for v1.
 * Server-side verification should be added before scaling.
 */
@Singleton
class BillingManager @Inject constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
) {
    companion object {
        // Subscription product IDs (register these in Google Play Console)
        const val PRODUCT_MONTHLY = "stashed_premium_monthly"
        const val PRODUCT_ANNUAL = "stashed_premium_annual"
        // In-app purchase (one-time)
        const val PRODUCT_LIFETIME = "stashed_premium_lifetime"
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        // Observe the DataStore premium flag
        scope.launch {
            preferencesManager.isPremiumActive.collect { active ->
                _isPremium.value = active
            }
        }
    }

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry on next app launch
            }
        })
    }

    private fun queryProducts() {
        val subProducts = listOf(PRODUCT_MONTHLY, PRODUCT_ANNUAL).map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val inAppProducts = listOf(PRODUCT_LIFETIME).map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        // Query subscriptions
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(subProducts)
                .build(),
        ) { _, details ->
            val current = _products.value.toMutableList()
            current.addAll(details)
            _products.value = current
        }

        // Query in-app (lifetime)
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(inAppProducts)
                .build(),
        ) { _, details ->
            val current = _products.value.toMutableList()
            current.addAll(details)
            _products.value = current
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()?.offerToken

        val params = if (offerToken != null) {
            // Subscription
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build(),
                    ),
                )
                .build()
        } else {
            // One-time purchase (lifetime)
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build(),
                    ),
                )
                .build()
        }

        billingClient.launchBillingFlow(activity, params)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            scope.launch {
                preferencesManager.setPremiumActive(true)
            }

            // Acknowledge the purchase if not already
            if (!purchase.isAcknowledged) {
                val params = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { /* result */ }
            }
        }
    }

    /** Check for existing purchases on app start (restore purchases). */
    private fun queryExistingPurchases() {
        // Check subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { _, purchases ->
            val hasActiveSub = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasActiveSub) {
                scope.launch { preferencesManager.setPremiumActive(true) }
            }
        }

        // Check in-app (lifetime)
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        ) { _, purchases ->
            val hasLifetime = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasLifetime) {
                scope.launch { preferencesManager.setPremiumActive(true) }
            }
        }
    }

    fun disconnect() {
        billingClient.endConnection()
    }
}
