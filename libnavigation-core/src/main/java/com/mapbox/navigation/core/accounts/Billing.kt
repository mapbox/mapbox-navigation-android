package com.mapbox.navigation.core.accounts

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.mapbox.android.accounts.v1.AccountsConstants.DEFAULT_TOKEN_MANAGE_SKU
import com.mapbox.android.accounts.v1.AccountsConstants.KEY_META_DATA_MANAGE_SKU

internal class Billing private constructor() {

    enum class BillingModel {
        NO_SKU,
        MAU
    }

    companion object {
        internal var INSTANCE: Billing? = null
        private var billingType = BillingModel.MAU

        fun getInstance(context: Context): Billing =
                INSTANCE ?: synchronized(this) {
                    Billing().also { billing ->
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
            }
            return applicationInfo
        }

        private fun setBillingType(context: Context) {
            billingType = when (getApplicationInfo(context)?.metaData
                    ?.getBoolean(KEY_META_DATA_MANAGE_SKU, DEFAULT_TOKEN_MANAGE_SKU)) {
                true -> BillingModel.NO_SKU
                else -> BillingModel.MAU
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
