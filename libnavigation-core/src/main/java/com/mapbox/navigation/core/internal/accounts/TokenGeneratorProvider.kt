package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator

internal object TokenGeneratorProvider {

    private val tokenGenerator = object : NavigationTokenGenerator {
        override fun getSKUToken() = TokenGenerator.getSKUToken(SKUIdentifier.NAVIGATION_MAUS)
    }

    fun getNavigationTokenGenerator() = tokenGenerator
}
