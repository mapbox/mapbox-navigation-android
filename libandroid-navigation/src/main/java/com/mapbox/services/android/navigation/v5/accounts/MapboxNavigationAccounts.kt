package com.mapbox.services.android.navigation.v5.accounts

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import timber.log.Timber

private const val ENABLE_MAU = "EnableMAU"
private const val META_DATA = "com.mapbox.services.android.navigation.v5"
private const val ENABLE_MAU_META_DATA = META_DATA + ENABLE_MAU
private const val MAPBOX_NAV_PREFERENCES = "mapbox.navigation.preferences"

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

        private fun getApplicationInfo(context: Context): ApplicationInfo? {
            var applicationInfo: ApplicationInfo? = null
            try {
                applicationInfo = context
                        .packageManager
                        .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            } catch (exception: PackageManager.NameNotFoundException) {
                Timber.e(exception)
            }
            return applicationInfo
        }

        private fun isMauBillingEnabled(context: Context): Boolean {
            val applicationInfo = getApplicationInfo(context)
            applicationInfo?.let { appInfo ->
                appInfo.metaData?.let { metadata ->
                    return metadata.getBoolean(ENABLE_MAU_META_DATA, false)
                } ?: return false
            } ?: return false
        }

        private fun init(context: Context) {
            val preferences = context.getSharedPreferences(MAPBOX_NAV_PREFERENCES, Context.MODE_PRIVATE)
            tokenGenerator = when (isMauBillingEnabled(context)) {
                true -> Mau(preferences)
                else -> Trips(preferences)
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
