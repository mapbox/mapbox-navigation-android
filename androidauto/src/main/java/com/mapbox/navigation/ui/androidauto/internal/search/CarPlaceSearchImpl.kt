package com.mapbox.navigation.ui.androidauto.internal.search

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.search.ApiType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Default implementation of the [CarPlaceSearch].
 */
class CarPlaceSearchImpl(
    private val options: MapboxCarOptions,
    private val locationProvider: CarSearchLocationProvider,
) : CarPlaceSearch {

    private var searchEngine: SearchEngine? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        locationProvider.onAttached(mapboxNavigation)
        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            apiType = ApiType.SBS,
            settings = SearchEngineSettings(
                locationProvider,
            ),
        )
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        locationProvider.onDetached(mapboxNavigation)
        searchEngine = null
    }

    override suspend fun search(query: String): Result<List<SearchSuggestion>> {
        val searchEngine = searchEngine
            ?: return searchEngineNotAvailable("search")

        val optionsBuilder = SearchOptions.Builder()
            .requestDebounce(SEARCH_DEBOUNCE_MILLIS)
            .limit(SEARCH_RESULT_LIMIT)
        locationProvider.point?.let { origin ->
            optionsBuilder
                .origin(origin)
                .proximity(origin)
        }
        val options = optionsBuilder.build()

        logAndroidAuto("carLocationProvider request $query")
        return suspendCancellableCoroutine { continuation ->
            val searchCallback = object : SearchSuggestionsCallback {
                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo,
                ) {
                    logAndroidAuto("carLocationProvider result ${suggestions.size}")
                    continuation.resume(Result.success(suggestions))
                }

                override fun onError(e: Exception) {
                    logAndroidAuto("carLocationProvider error ${e.message}")
                    continuation.resume(Result.failure(e))
                }
            }
            val task = searchEngine.search(query, options, searchCallback)
            continuation.invokeOnCancellation {
                task.cancel()
            }
        }
    }

    override suspend fun select(selection: SearchSuggestion): Result<List<SearchResult>> {
        val searchEngine = searchEngine
            ?: return searchEngineNotAvailable("select")

        return suspendCancellableCoroutine { continuation ->
            val selectionCallback = object : SearchSelectionCallback {

                override fun onResults(
                    suggestion: SearchSuggestion,
                    results: List<SearchResult>,
                    responseInfo: ResponseInfo,
                ) {
                    continuation.resume(Result.success(results))
                }

                override fun onError(e: Exception) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResult(
                    suggestion: SearchSuggestion,
                    result: SearchResult,
                    responseInfo: ResponseInfo,
                ) {
                    continuation.resume(Result.success(listOf(result)))
                }

                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo,
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
            val task = searchEngine.select(selection, selectionCallback)
            continuation.invokeOnCancellation { task.cancel() }
        }
    }

    private fun <T> searchEngineNotAvailable(source: String): Result<T> {
        val message = "SearchEngine is not available for place $source"
        logAndroidAutoFailure(message)
        return Result.failure(
            IllegalStateException("SearchEngine is not available for place $source"),
        )
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 100
        private const val SEARCH_RESULT_LIMIT = 5
    }
}
