package com.mapbox.services.android.navigation.v5.accounts

internal interface TokenGenerator {
    fun obtainSkuToken(): String

    fun onNavigationEnd()
}
