package com.mapbox.navigation.core.internal.accounts

import android.content.Context
import android.text.format.DateUtils
import com.mapbox.android.accounts.navigation.sku.v1.MauSku
import com.mapbox.android.accounts.navigation.sku.v1.SkuGenerator
import com.mapbox.android.accounts.navigation.sku.v1.TripsSku
import com.mapbox.android.accounts.v1.AccountsConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.navigation.base.accounts.SkuTokenProvider

// TODO: make the class internal
class MapboxNavigationAccounts private constructor() : SkuTokenProvider {

    companion object {
        private const val SKU_KEY = "sku"
        private const val MAU_TIMER_EXPIRE_THRESHOLD = 1
        private const val TRIPS_TIMER_EXPIRE_THRESHOLD = 2
        private const val TRIPS_REQUEST_COUNT_THRESHOLD = 5
        private const val TIMER_EXPIRE_AFTER = DateUtils.HOUR_IN_MILLIS / 1000
        private var skuGenerator: SkuGenerator? = null
        private var INSTANCE: MapboxNavigationAccounts? = null

        @JvmStatic
        fun getInstance(context: Context): MapboxNavigationAccounts =
            INSTANCE
                ?: synchronized(this) {
                MapboxNavigationAccounts().also { mapboxNavigationAccount ->
                    INSTANCE = mapboxNavigationAccount
                    init(context)
                }
            }

        private fun init(context: Context) {
            val preferences = context.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            skuGenerator = when (Billing.getInstance(context).getBillingType()) {
                Billing.BillingModel.MAU -> MauSku(preferences, TIMER_EXPIRE_AFTER * MAU_TIMER_EXPIRE_THRESHOLD)
                Billing.BillingModel.TRIPS -> TripsSku(preferences, TIMER_EXPIRE_AFTER * TRIPS_TIMER_EXPIRE_THRESHOLD,
                    TRIPS_REQUEST_COUNT_THRESHOLD
                )
            }
        }
    }

    override fun obtainSkuToken(resourceUrl: String?, querySize: Int): String {
        val skuToken = skuGenerator?.generateToken()
        check(!skuToken.isNullOrEmpty()) { "MapboxNavigationAccounts: skuToken cannot be null or empty" }

        return when (resourceUrl.isNullOrEmpty() && querySize < 0) {
            true -> { skuToken }
            false -> { buildResourceUrlWithSku(resourceUrl!!, querySize, skuToken) }
        }
    }

    fun navigationStopped() {
        skuGenerator?.onNavigationEnd()
    }

    fun navigationStarted() {
        skuGenerator?.onNavigationStart()
    }

    private fun buildResourceUrlWithSku(resourceUrl: String, querySize: Int, skuToken: String): String {
        val urlBuilder = StringBuilder(resourceUrl)
        if (querySize == 0) {
            urlBuilder.append("?")
        } else {
            urlBuilder.append("&")
        }
        urlBuilder.append("$SKU_KEY=$skuToken")
        urlBuilder.toString()
        return urlBuilder.toString()
    }
}
