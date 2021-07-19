package com.mapbox.navigation.core.internal.accounts

import com.mapbox.common.BillingService
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SKUIdentifier
import com.mapbox.common.TokenGenerator

internal object TokenGeneratorProvider {

    private val tokenGenerator = NavigationTokenGenerator {
        // session status check is a workaround since
        // token is not cleared when a session is stopped
        val status = BillingService.getSessionStatus(SKUIdentifier.NAV2_SES_TRIP)
        if (status == BillingSessionStatus.SESSION_ACTIVE) {
            // todo implement a switch between AG session sku and FD session sku
            TokenGenerator.getSKUTokenIfValid(SKUIdentifier.NAV2_SES_TRIP)
        } else {
            ""
        }
    }

    fun getNavigationTokenGenerator() = tokenGenerator
}
