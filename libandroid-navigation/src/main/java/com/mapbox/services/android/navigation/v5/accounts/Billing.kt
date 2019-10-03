package com.mapbox.services.android.navigation.v5.accounts

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import timber.log.Timber

private const val ENABLE_MAU = "EnableMAU"
private const val META_DATA = "com.mapbox.services.android.navigation.v5"
private const val ENABLE_MAU_META_DATA = META_DATA + ENABLE_MAU

class Billing private constructor() {

    enum class BillingModel {
        TRIPS,
        MAU
    }

    companion object {
        private var INSTANCE: Billing? = null
        private var billingType = BillingModel.TRIPS

        @JvmStatic
        fun getInstance(context: Context): Billing =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Billing().also { billing ->
                        INSTANCE = billing
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

        private fun setBillingType(context: Context) {
            val applicationInfo = getApplicationInfo(context)
            applicationInfo?.let { appInfo ->
                appInfo.metaData?.let { metadata ->
                    billingType = when (metadata.getBoolean(ENABLE_MAU_META_DATA, false)) {
                        true -> BillingModel.MAU
                        else -> BillingModel.TRIPS
                    }
                }
            }
        }

        private fun init(context: Context) {
            setBillingType(context)
        }
    }

    fun getBillingType(): BillingModel {
        return billingType
    }
}
