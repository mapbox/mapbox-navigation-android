package com.mapbox.services.android.navigation.v5.accounts

internal class DisableSku : TokenGenerator {

    override fun obtainSkuToken(): String {
        return ""
    }

    override fun onNavigationEnd() {
    }
}
