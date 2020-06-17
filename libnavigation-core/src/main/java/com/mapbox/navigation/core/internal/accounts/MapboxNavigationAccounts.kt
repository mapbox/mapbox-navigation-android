package com.mapbox.navigation.core.internal.accounts

import android.content.Context
import android.text.format.DateUtils
import com.mapbox.android.accounts.navigation.sku.v1.MauSku
import com.mapbox.android.accounts.navigation.sku.v1.SkuGenerator
import com.mapbox.android.accounts.v1.AccountsConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.android.accounts.v1.MapboxAccounts
import com.mapbox.navigation.base.internal.accounts.SkuTokenProvider
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.core.accounts.Billing

/**
 * This class generates and retains the Navigation SDK's SKU token according to internal Mapbox policies
 */
class MapboxNavigationAccounts private constructor() :
    UrlSkuTokenProvider,
    SkuTokenProvider {

    companion object {
        private const val SKU_KEY = "sku"
        private const val EMPTY_SKU = ""
        private const val MAU_TIMER_EXPIRE_THRESHOLD = 1
        private const val TIMER_EXPIRE_AFTER = DateUtils.HOUR_IN_MILLIS / 1000
        private var skuGenerator: SkuGenerator? = null
        private var INSTANCE: MapboxNavigationAccounts? = null

        /**
         * Provide singleton instance of [MapboxNavigationAccounts]
         */
        @JvmStatic
        fun getInstance(context: Context): MapboxNavigationAccounts =
            INSTANCE ?: synchronized(this) {
                MapboxNavigationAccounts().also { mapboxNavigationAccount ->
                    INSTANCE = mapboxNavigationAccount
                    init(context)
                }
            }

        private fun init(context: Context) {
            val preferences =
                context.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            skuGenerator = when (Billing.getInstance(context).getBillingType()) {
                Billing.BillingModel.MAU -> MauSku(
                    preferences,
                    TIMER_EXPIRE_AFTER * MAU_TIMER_EXPIRE_THRESHOLD,
                    context.applicationContext.packageName
                )
                Billing.BillingModel.NO_SKU -> null
            }
        }
    }

    /**
     * Returns a token attached to the URL query or the given [resourceUrl].
     */
    @Synchronized
    override fun obtainUrlWithSkuToken(resourceUrl: String, querySize: Int): String {
        return skuGenerator?.let { generator ->
            val skuToken = generator.generateToken()
            check(skuToken.isNotEmpty()) { throw IllegalStateException("skuToken cannot be empty") }

            when {
                querySize < 0 -> throw IllegalStateException("querySize cannot be less than 0")
                resourceUrl.isEmpty() -> throw IllegalStateException("resourceUrl cannot be empty")
                else -> {
                    buildResourceUrlWithSku(resourceUrl, querySize, skuToken)
                }
            }
        } ?: resourceUrl
    }

    /**
     * Returns current SDK SKU token needed for API Routing Tiles.
     */
    @Synchronized
    override fun obtainSkuToken(): String = skuGenerator?.generateToken() ?: EMPTY_SKU

    internal fun initializeSku() {
        skuGenerator?.apply {
            initializeSKU()
            generateToken()
        }
    }

    internal fun navigationStopped() {
        skuGenerator?.onNavigationEnd()
    }

    internal fun navigationStarted() {
        skuGenerator?.onNavigationStart()
    }

    internal fun obtainSkuId(): String = MapboxAccounts.SKU_ID_NAVIGATION_MAUS

    private fun buildResourceUrlWithSku(
        resourceUrl: String,
        querySize: Int,
        skuToken: String
    ): String {
        val urlBuilder = StringBuilder(resourceUrl)
        when (querySize == 0) {
            true -> urlBuilder.append("?")
            false -> urlBuilder.append("&")
        }
        urlBuilder.append("$SKU_KEY=$skuToken")
        return urlBuilder.toString()
    }
}
