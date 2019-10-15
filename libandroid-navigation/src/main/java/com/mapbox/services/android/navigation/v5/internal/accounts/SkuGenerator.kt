package com.mapbox.services.android.navigation.v5.internal.accounts

internal interface SkuGenerator {
    fun generateToken(): String

    fun onNavigationEnd()
}
