package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.androidauto.logAndroidAutoFailure
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import java.util.concurrent.CopyOnWriteArrayList

/**
 * This class is to simplify the interaction with the [SearchEngine] for the specific
 * use cases that the app needs in the car.
 */
class CarSearchEngine(
    private val searchEngine: SearchEngine,
    private val navigationLocationProvider: NavigationLocationProvider
) {
    private val searchResults = CopyOnWriteArrayList<SearchSuggestion>()
    private var searchSuggestionsCallback: (List<SearchSuggestion>) -> Unit = { }

    private val searchCallback = object : SearchSuggestionsCallback {

        override fun onSuggestions(
            suggestions: List<SearchSuggestion>,
            responseInfo: ResponseInfo
        ) {
            logAndroidAuto("carLocationProvider result ${searchResults.size}")
            searchResults.clear()
            searchResults.addAll(suggestions)
            searchSuggestionsCallback.invoke(searchResults)
        }

        override fun onError(e: Exception) {
            logAndroidAuto("carLocationProvider error ${e.message}")
            searchSuggestionsCallback.invoke(searchResults)
        }
    }

    /**
     * Search for suggestions.
     */
    fun search(query: String, callback: (List<SearchSuggestion>) -> Unit) {
        val optionsBuilder = SearchOptions.Builder()
            .requestDebounce(SEARCH_DEBOUNCE_MILLIS)
            .limit(SEARCH_RESULT_LIMIT)
        navigationLocationProvider.lastLocation?.let {
            val currentPoint = Point.fromLngLat(it.longitude, it.latitude)
            optionsBuilder
                .origin(currentPoint)
                .proximity(currentPoint)
        }
        val options = optionsBuilder.build()

        logAndroidAuto("carLocationProvider request $query")
        searchSuggestionsCallback = callback
        searchEngine.search(query, options, searchCallback)
    }

    /**
     * Given a [SearchSuggestion], request for [SearchResult]s.
     *
     * @param selection
     */
    fun select(selection: SearchSuggestion, callback: (List<SearchResult>) -> Unit) {
        val selectionCallback = object : SearchSelectionCallback {
            override fun onCategoryResult(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                if (results.isEmpty()) {
                    callback(emptyList())
                } else {
                    callback(results)
                }
            }

            override fun onError(e: Exception) {
                callback(emptyList())
            }

            override fun onResult(
                suggestion: SearchSuggestion,
                result: SearchResult,
                responseInfo: ResponseInfo
            ) {
                callback(listOf(result))
            }

            override fun onSuggestions(
                suggestions: List<SearchSuggestion>,
                responseInfo: ResponseInfo
            ) {
                logAndroidAutoFailure(
                    """
                    Why are there search suggestions coming from a selection
                    suggestions: $suggestions
                    responseInfo: $responseInfo
                    """.trimIndent(),
                )
                error("Why are there search suggestions coming from a selection")
            }
        }
        searchEngine.select(selection, selectionCallback)
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 100
        private const val SEARCH_RESULT_LIMIT = 5
    }
}
