/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core.internal.accounts

import android.net.Uri
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.core.accounts.BillingServiceProvider
import java.net.URL

object MapboxNavigationAccounts : UrlSkuTokenProvider {

    private const val SKU_KEY = "sku"
    private val billingService = BillingServiceProvider.getInstance()

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
        val activeGuidanceSessionStatus = billingService.getSessionStatus(
            SessionSKUIdentifier.NAV2_SES_TRIP
        )
        val activeGuidanceToken =
            if (activeGuidanceSessionStatus == BillingSessionStatus.SESSION_ACTIVE) {
                billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV2_SES_TRIP) ?: ""
            } else {
                ""
            }

        return if (activeGuidanceToken.isBlank()) {
            // if the token is not available, check for the free drive token
            val freeDriveSessionStatus = billingService.getSessionStatus(
                SessionSKUIdentifier.NAV2_SES_FDTRIP
            )
            if (freeDriveSessionStatus == BillingSessionStatus.SESSION_ACTIVE) {
                billingService.getSessionSKUTokenIfValid(SessionSKUIdentifier.NAV2_SES_FDTRIP) ?: ""
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
