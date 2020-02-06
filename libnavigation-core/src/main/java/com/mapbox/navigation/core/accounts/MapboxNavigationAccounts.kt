package com.mapbox.navigation.core.accounts

import android.content.Context
import android.text.format.DateUtils
import com.mapbox.android.accounts.navigation.sku.v1.MauSku
import com.mapbox.android.accounts.navigation.sku.v1.SkuGenerator
import com.mapbox.android.accounts.v1.AccountsConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.navigation.base.accounts.SkuTokenProvider
import java.lang.IllegalStateException

// TODO: make the class internal
class MapboxNavigationAccounts private constructor() : SkuTokenProvider {

    companion object {
        private const val SKU_KEY = "sku"
        private const val MAU_TIMER_EXPIRE_THRESHOLD = 1
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

    internal fun initializeSku() {
        skuGenerator?.initializeSKU()
    }

    internal fun navigationStopped() {
        skuGenerator?.onNavigationEnd()
    }

    internal fun navigationStarted() {
        skuGenerator?.onNavigationStart()
    }

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
