package com.mapbox.navigation.ui.androidauto.internal.search

import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion

/**
 * Service that allows you to search for points of interest.
 *
 * This is an internal interface because [SearchSuggestion] and [SearchResult] are subject to
 * change.
 */
interface CarPlaceSearch : MapboxNavigationObserver {

    /**
     * Search for suggestions with a query string.
     *
     * @param query place search query or address
     */
    suspend fun search(query: String): Result<List<SearchSuggestion>>

    /**
     * Given a [SearchSuggestion], request for [SearchResult]s. This includes location coordinates
     * needed for routing.
     *
     * @param selection selected result from a [search]
     */
    suspend fun select(selection: SearchSuggestion): Result<List<SearchResult>>
}
