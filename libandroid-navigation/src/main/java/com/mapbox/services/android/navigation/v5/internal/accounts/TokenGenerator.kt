package com.mapbox.services.android.navigation.v5.internal.accounts

internal interface TokenGenerator {
    fun obtainSkuToken(): String

    fun onNavigationEnd()
}
