package com.mapbox.navigation.core.internal.accounts

import android.net.Uri
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
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
        val skuToken = TokenGeneratorProvider.getNavigationTokenGenerator().getSKUToken()
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

    // fixme workaround for missing the public SKU ID constant
    internal fun obtainSkuId(): String = "09" // SKUIdentifier::Nav2SesMAU
}
