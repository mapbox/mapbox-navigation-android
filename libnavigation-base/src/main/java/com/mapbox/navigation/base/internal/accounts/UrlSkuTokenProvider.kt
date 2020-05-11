package com.mapbox.navigation.base.internal.accounts

/**
 * Internal usage.
 */
interface UrlSkuTokenProvider {

    /**
     * Returns a token attached to the URL query or the given [resourceUrl].
     */
    fun obtainUrlWithSkuToken(resourceUrl: String, querySize: Int): String
}
