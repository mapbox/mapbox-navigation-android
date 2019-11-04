package com.mapbox.services.android.navigation.v5.internal.accounts

import com.mapbox.android.accounts.navigation.sku.v1.SkuGenerator

// TODO: Remove this class when SKU is ready to be released publicly
internal class DisabledSku : SkuGenerator {

    override fun generateToken(): String {
        return ""
    }

    override fun onNavigationStart() {
    }

    override fun onNavigationEnd() {
    }
}
