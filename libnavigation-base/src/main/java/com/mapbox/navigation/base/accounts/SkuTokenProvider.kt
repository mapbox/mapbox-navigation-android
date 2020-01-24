package com.mapbox.navigation.base.accounts

/**
 * Internal usage.
 */
interface SkuTokenProvider {

    /**
     * Returns a raw SKU token or a token attached to the URL query.
     */
    fun obtainSkuToken(resourceUrl: String? = null, querySize: Int = -1): String?
}
