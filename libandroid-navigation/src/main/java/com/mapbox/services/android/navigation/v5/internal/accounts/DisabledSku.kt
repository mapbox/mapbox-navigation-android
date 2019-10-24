package com.mapbox.services.android.navigation.v5.internal.accounts

internal class DisabledSku : SkuGenerator {

    override fun generateToken(): String {
        return ""
    }

    override fun onNavigationStart() {

    }

    override fun onNavigationEnd() {
    }
}
