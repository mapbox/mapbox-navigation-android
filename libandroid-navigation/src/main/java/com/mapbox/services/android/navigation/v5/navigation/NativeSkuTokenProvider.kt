package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.navigator.SkuTokenSource

internal class NativeSkuTokenProvider : SkuTokenSource() {
    override fun getToken(): String = ""
}
