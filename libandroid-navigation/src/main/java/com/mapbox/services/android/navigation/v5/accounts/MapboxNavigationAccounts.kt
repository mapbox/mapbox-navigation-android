package com.mapbox.services.android.navigation.v5.accounts

import android.content.Context
import android.text.format.DateUtils
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_SHARED_PREFERENCES

const val MAU_TIMER_EXPIRE_THRESHOLD = 1
const val TRIPS_TIMER_EXPIRE_THRESHOLD = 2
const val TRIPS_REQUEST_COUNT_THRESHOLD = 5
const val TIMER_EXPIRE_AFTER = DateUtils.HOUR_IN_MILLIS / 1000
class MapboxNavigationAccounts private constructor() {

    companion object {
        private var tokenGenerator: TokenGenerator = DisableSku()
        private var INSTANCE: MapboxNavigationAccounts? = null

        @JvmStatic
        fun getInstance(context: Context): MapboxNavigationAccounts =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: MapboxNavigationAccounts().also { mapboxNavigationAccount ->
                        INSTANCE = mapboxNavigationAccount
                        init(context)
                    }
                }

        private fun init(context: Context) {
            val preferences = context.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            tokenGenerator = when (Billing.getInstance(context).getBillingType() == Billing.BillingModel.MAU) {
                true -> Mau(preferences, TIMER_EXPIRE_AFTER * MAU_TIMER_EXPIRE_THRESHOLD)
                else -> Trips(preferences, TIMER_EXPIRE_AFTER * TRIPS_TIMER_EXPIRE_THRESHOLD, TRIPS_REQUEST_COUNT_THRESHOLD)
            }
        }
    }

    fun obtainSkuToken(): String {
        return tokenGenerator.obtainSkuToken()
    }

    fun navigationStopped() {
        tokenGenerator.onNavigationEnd()
    }
}
