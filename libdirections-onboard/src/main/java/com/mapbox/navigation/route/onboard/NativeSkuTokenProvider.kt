package com.mapbox.navigation.route.onboard

import com.mapbox.navigation.base.internal.accounts.SkuTokenProvider
import com.mapbox.navigator.SkuTokenSource

internal class NativeSkuTokenProvider(private val skuTokenProvider: SkuTokenProvider) : SkuTokenSource() {
    override fun getToken(): String {
        return skuTokenProvider.obtainSkuToken()
    }
}
