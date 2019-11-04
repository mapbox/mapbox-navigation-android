package com.mapbox.services.android.navigation.v5.internal.accounts

import android.content.Context
import android.text.format.DateUtils
import com.mapbox.android.accounts.navigation.sku.v1.MauSku
import com.mapbox.android.accounts.navigation.sku.v1.SkuGenerator
import com.mapbox.android.accounts.navigation.sku.v1.TripsSku
import com.mapbox.android.accounts.v1.AccountsConstants.MAPBOX_SHARED_PREFERENCES

internal class MapboxNavigationAccounts private constructor() {

    companion object {
        private const val MAU_TIMER_EXPIRE_THRESHOLD = 1
        private const val TRIPS_TIMER_EXPIRE_THRESHOLD = 2
        private const val TRIPS_REQUEST_COUNT_THRESHOLD = 5
        private const val TIMER_EXPIRE_AFTER = DateUtils.HOUR_IN_MILLIS / 1000
        private var skuGenerator: SkuGenerator = DisabledSku()
        private var INSTANCE: MapboxNavigationAccounts? = null

        @JvmStatic
        fun getInstance(context: Context): MapboxNavigationAccounts =
                INSTANCE ?: synchronized(this) {
                    MapboxNavigationAccounts().also { mapboxNavigationAccount ->
                        INSTANCE = mapboxNavigationAccount
                        // TODO uncomment when ready to release as a part of 1.0
                        // init(context)
                    }
                }

        private fun init(context: Context) {
            val preferences = context.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            skuGenerator = when (Billing.getInstance(context).getBillingType()) {
                Billing.BillingModel.MAU -> MauSku(preferences, TIMER_EXPIRE_AFTER * MAU_TIMER_EXPIRE_THRESHOLD)
                Billing.BillingModel.TRIPS -> TripsSku(preferences, TIMER_EXPIRE_AFTER * TRIPS_TIMER_EXPIRE_THRESHOLD, TRIPS_REQUEST_COUNT_THRESHOLD)
            }
        }
    }

    fun obtainSkuToken(): String {
        return skuGenerator.generateToken()
    }

    fun navigationStopped() {
        skuGenerator.onNavigationEnd()
    }

    fun navigationStarted() {
        skuGenerator.onNavigationStart()
    }
}
