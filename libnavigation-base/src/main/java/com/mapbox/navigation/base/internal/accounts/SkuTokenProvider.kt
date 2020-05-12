package com.mapbox.navigation.base.internal.accounts

/**
 * Internal usage.
 */
interface SkuTokenProvider {

    /**
     * Returns current SDK SKU token needed for ART.
     */
    fun obtainSkuToken(): String
}
