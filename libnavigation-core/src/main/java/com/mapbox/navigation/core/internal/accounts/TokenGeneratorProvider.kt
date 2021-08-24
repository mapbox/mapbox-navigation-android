package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator

internal object TokenGeneratorProvider {

    private val tokenGenerator = NavigationTokenGenerator {
        // first checks for the active guidance token
        TokenGenerator.getSKUTokenIfValid(SKUIdentifier.NAV2_SES_TRIP).let {
            if (it == null || it.isBlank()) {
                // if it's empty, check for free drive token
                TokenGenerator.getSKUTokenIfValid(SKUIdentifier.NAV2_SES_FDTRIP) ?: ""
            } else {
                // if not empty, return it
                it
            }
        }
    }

    fun getNavigationTokenGenerator() = tokenGenerator
}
