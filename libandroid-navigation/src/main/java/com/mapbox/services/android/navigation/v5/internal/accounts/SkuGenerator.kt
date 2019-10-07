package com.mapbox.services.android.navigation.v5.internal.accounts

internal interface SkuGenerator {
    fun generateSkuToken(): String

    fun onNavigationEnd()
}
