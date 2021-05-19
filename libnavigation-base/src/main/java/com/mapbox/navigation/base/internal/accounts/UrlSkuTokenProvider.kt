package com.mapbox.navigation.base.internal.accounts

import java.net.URL

/**
 * Internal usage.
 */
fun interface UrlSkuTokenProvider {

    /**
     * Returns a token attached to the URI query.
     */
    fun obtainUrlWithSkuToken(resourceUrl: URL): URL
}
