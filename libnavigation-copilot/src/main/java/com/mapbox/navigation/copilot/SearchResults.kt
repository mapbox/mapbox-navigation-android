package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * SearchResults.
 *
 * @property provider used search provider, e.g. mapbox, google
 * @property request HTTP request or SDK method with parameters being used to get search point, all secure tokens should be sanitized
 * @property response or result of the method with search result, null in case of error
 * @property error message and details of getting search point, null in case of successful response
 * @property searchQuery used search text to find point or coordinates to reverse geocoding
 * @property results [HistorySearchResult]s
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
data class SearchResults(
    val provider: String,
    val request: String,
    val response: String?,
    val error: String?,
    val searchQuery: String,
    val results: List<HistorySearchResult>?,
) : EventDTO
