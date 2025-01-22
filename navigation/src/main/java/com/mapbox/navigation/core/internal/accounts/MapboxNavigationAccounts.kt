/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core.internal.accounts

import android.net.Uri
import com.mapbox.common.BillingSessionStatus
import com.mapbox.navigation.base.internal.accounts.SkuIdProvider
import com.mapbox.navigation.base.internal.accounts.SkuIdProviderImpl
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.core.accounts.BillingServiceProvider
import com.mapbox.navigation.core.accounts.BillingServiceProxy
import java.net.URL

class MapboxNavigationAccounts internal constructor(
    private val skuIdProvider: SkuIdProvider,
    private val billingService: BillingServiceProxy,
) : UrlSkuTokenProvider {

    constructor() : this(
        SkuIdProviderImpl(),
        BillingServiceProvider.getInstance(),
    )

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
                .toString(),
        )
    }

    private fun obtainSkuToken(): String {
        // first check for the active guidance token
        val activeGuidanceSessionStatus = billingService.getSessionStatus(
            skuIdProvider.getActiveGuidanceSku(),
        )
        val activeGuidanceToken =
            if (activeGuidanceSessionStatus == BillingSessionStatus.SESSION_ACTIVE) {
                billingService.getSessionSKUTokenIfValid(skuIdProvider.getActiveGuidanceSku()) ?: ""
            } else {
                ""
            }

        return activeGuidanceToken.ifBlank {
            // if the token is not available, check for the free drive token
            val freeDriveSessionStatus = billingService.getSessionStatus(
                skuIdProvider.getFreeDriveSku(),
            )
            if (freeDriveSessionStatus == BillingSessionStatus.SESSION_ACTIVE) {
                billingService.getSessionSKUTokenIfValid(skuIdProvider.getFreeDriveSku()) ?: ""
            } else {
                ""
            }
        }
    }

    private companion object {
        const val SKU_KEY = "sku"
    }
}
