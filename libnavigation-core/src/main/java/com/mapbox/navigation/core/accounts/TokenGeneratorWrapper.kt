package com.mapbox.navigation.core.accounts

import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator

object TokenGeneratorWrapper {
    fun getSKUTokenIfValid(skuIdentifier: SKUIdentifier): String? {
        return TokenGenerator.getSKUTokenIfValid(skuIdentifier)
    }
}
