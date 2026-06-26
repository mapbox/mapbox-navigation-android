package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.search.ApiType

/**
 * Defines the search engine mode used for Android Auto place search and geo deeplink handling.
 * Supports only 2 modes:
 * - [Legacy]
 * - [SearchBox]
 *
 * [SearchBox] is the recommended mode. [Legacy] (default) is retained only to avoid unexpected
 * billing changes for existing customers and should not be used for new integrations.
 */
abstract class CarSearchMode private constructor(
    @JvmSynthetic
    internal val apiType: ApiType,
) {

    /**
     * Uses the legacy SBS API for place search and the Geocoding V5 API for geo deeplink handling.
     *
     * **Not recommended.** Geocoding V5 no longer provides POI data, which means place searches
     * may return incomplete or missing results. This mode is retained solely to avoid unexpected
     * billing changes for existing customers. New integrations should use [SearchBox] instead.
     *
     * @see <a href="https://docs.mapbox.com/api/search/geocoding/">Geocoding API documentation</a>
     */
    @Deprecated(
        message = "Legacy search mode uses Geocoding V5 which no longer provides POI data. " +
            "Use CarSearchMode.SearchBox.",
        replaceWith = ReplaceWith("CarSearchMode.SearchBox"),
    )
    object Legacy : CarSearchMode(ApiType.SBS)

    /**
     * Uses the Search Box API for both place search and geo deeplink handling.
     *
     * This is the recommended mode. It provides full POI support and is backed by the latest
     * Mapbox Search Box backend.
     *
     * @see <a href="https://docs.mapbox.com/api/search/search-box/">Search Box API documentation</a>
     * @see <a href="https://docs.mapbox.com/api/search/search-box/#search-box-api-pricing">Search Box API pricing</a>
     */
    object SearchBox : CarSearchMode(ApiType.SEARCH_BOX)

    internal companion object {
        @JvmSynthetic
        @Suppress("DEPRECATION")
        val DEFAULT: CarSearchMode = Legacy
    }
}
