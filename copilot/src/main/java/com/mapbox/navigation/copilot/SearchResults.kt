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
class SearchResults(
    val provider: String,
    val request: String,
    val response: String?,
    val error: String?,
    val searchQuery: String,
    val results: List<HistorySearchResult>?,
) : EventDTO {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResults

        if (provider != other.provider) return false
        if (request != other.request) return false
        if (response != other.response) return false
        if (error != other.error) return false
        if (searchQuery != other.searchQuery) return false
        return results == other.results
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = provider.hashCode()
        result = 31 * result + request.hashCode()
        result = 31 * result + (response?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + searchQuery.hashCode()
        result = 31 * result + (results?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SearchResults(" +
            "provider='$provider', " +
            "request='$request', " +
            "response=$response, " +
            "error=$error, " +
            "searchQuery='$searchQuery', " +
            "results=$results" +
            ")"
    }
}
