package com.mapbox.navigation.core

import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator
import com.mapbox.navigator.SkuTokenSource

internal class NativeSkuTokenProvider : SkuTokenSource() {
    override fun getToken(): String {
        return TokenGenerator.getSKUToken(SKUIdentifier.NAVIGATION_MAUS)
    }
}
