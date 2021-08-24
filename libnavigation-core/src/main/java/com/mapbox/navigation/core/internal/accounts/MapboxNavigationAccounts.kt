package com.mapbox.navigation.core.internal.accounts

import android.net.Uri
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SKUIdentifier
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.core.accounts.BillingServiceWrapper
import com.mapbox.navigation.core.accounts.TokenGeneratorWrapper
import java.net.URL

/**
 * This class generates and retains the Navigation SDK's SKU token according to internal Mapbox policies
 */
object MapboxNavigationAccounts : UrlSkuTokenProvider {

    private const val SKU_KEY = "sku"

    /**
     * Returns a token attached to the URI query.
     */
    override fun obtainUrlWithSkuToken(resourceUrl: URL): URL {
        val skuToken = obtainSkuToken()
        return URL(
            Uri.parse(resourceUrl.toString())
                .buildUpon().apply {
                    if (skuToken.isNotBlank()) {
                        appendQueryParameter(SKU_KEY, skuToken)
                    }
                }
                .build()
                .toString()
        )
    }

    private fun obtainSkuToken(): String {
        // first check for the active guidance token
        val activeGuidanceSessionStatus = BillingServiceWrapper.getSessionStatus(
            SKUIdentifier.NAV2_SES_TRIP
        )
        val activeGuidanceToken =
            if (activeGuidanceSessionStatus == BillingSessionStatus.SESSION_ACTIVE) {
                TokenGeneratorWrapper.getSKUTokenIfValid(SKUIdentifier.NAV2_SES_TRIP) ?: ""
            } else {
                ""
            }

        return if (activeGuidanceToken.isBlank()) {
            // if the token is not available, check for the free drive token
            val freeDriveSessionStatus = BillingServiceWrapper.getSessionStatus(
                SKUIdentifier.NAV2_SES_FDTRIP
            )
            if (freeDriveSessionStatus == BillingSessionStatus.SESSION_ACTIVE) {
                TokenGeneratorWrapper.getSKUTokenIfValid(SKUIdentifier.NAV2_SES_FDTRIP) ?: ""
            } else {
                ""
            }
        } else {
            activeGuidanceToken
        }
    }

    // fixme workaround for missing the public SKU ID constant
    internal fun obtainSkuId(): String = "09" // SKUIdentifier::Nav2SesMAU
}
