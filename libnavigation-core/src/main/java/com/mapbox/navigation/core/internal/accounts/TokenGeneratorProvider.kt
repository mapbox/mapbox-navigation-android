package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator

internal object TokenGeneratorProvider {

    private val tokenGenerator = NavigationTokenGenerator {
        TokenGenerator.getSKUToken(SKUIdentifier.NAVIGATION_MAUS)
    }

    fun getNavigationTokenGenerator() = tokenGenerator
}
