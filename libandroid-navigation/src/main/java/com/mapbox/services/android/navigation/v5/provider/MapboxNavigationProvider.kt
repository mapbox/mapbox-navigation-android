package com.mapbox.services.android.navigation.v5.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import com.mapbox.services.android.navigation.v5.internal.accounts.Billing
import com.mapbox.services.android.navigation.v5.internal.accounts.MapboxNavigationAccounts
import timber.log.Timber

class MapboxNavigationProvider : ContentProvider() {

    companion object {
        private const val TAG = "MapboxNavigationProvider"
        private const val EMPTY_APPLICATION_ID_PROVIDER_AUTHORITY = "com.mapbox.services.android.navigation.v5.provider.mapboxnavigationinitprovider"
    }

    override fun onCreate(): Boolean {
        try {
            context?.let { ctx ->
                if (Billing.getInstance(ctx).getBillingType() == Billing.BillingModel.MAU) {
                    MapboxNavigationAccounts.getInstance(ctx).obtainSkuToken()
                }
            }
        } catch (throwable: Throwable) {
            Timber.e("$TAG + $throwable")
        }
        return false
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        checkContentProviderAuthority(info)
        super.attachInfo(context, info)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    private fun checkContentProviderAuthority(info: ProviderInfo?) {
        checkNotNull(info) { throw IllegalStateException("$TAG: ProviderInfo cannot be null.") }
        check(EMPTY_APPLICATION_ID_PROVIDER_AUTHORITY == info.authority) {
            throw IllegalStateException(
                    "Incorrect provider authority in manifest. Most likely due to a missing " + "applicationId variable in application's build.gradle.")
        }
    }
}
