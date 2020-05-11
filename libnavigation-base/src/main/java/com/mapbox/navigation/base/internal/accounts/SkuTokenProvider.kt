package com.mapbox.navigation.base.internal.accounts

/**
 * Internal usage.
 */
interface SkuTokenProvider {

    /**
     * Returns current SDK SKU token needed for API Routing Tiles.
     */
    fun obtainSkuToken(): String
}
