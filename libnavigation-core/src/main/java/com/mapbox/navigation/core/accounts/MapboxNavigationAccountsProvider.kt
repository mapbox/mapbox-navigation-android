package com.mapbox.navigation.core.accounts

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.utils.internal.ifNonNull

internal class MapboxNavigationAccountsProvider : ContentProvider() {

    companion object {
        private const val TAG = "MapboxNavigationAccountsProvider"
        private const val EMPTY_APPLICATION_ID_PROVIDER_AUTHORITY = "com.mapbox.navigation.core.accounts.MapboxNavigationAccountsProvider"
    }

    override fun onCreate(): Boolean {
        try {
            ifNonNull(context, context?.applicationContext) { _, applicationContext ->
                if (Billing.getInstance(applicationContext).getBillingType() == Billing.BillingModel.MAU) {
                    MapboxNavigationAccounts.getInstance(applicationContext).initializeSku()
                }
            }
        } catch (throwable: Throwable) {
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
        check(EMPTY_APPLICATION_ID_PROVIDER_AUTHORITY != info.authority) {
            throw IllegalStateException(
                "Incorrect provider authority in manifest. Most likely due to a missing " + "applicationId variable in application's build.gradle."
            )
        }
    }
}
