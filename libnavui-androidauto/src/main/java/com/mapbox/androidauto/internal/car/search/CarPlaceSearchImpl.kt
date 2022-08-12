package com.mapbox.androidauto.internal.car.search

import com.mapbox.androidauto.car.search.CarPlaceSearch
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.search.MapboxSearchSdk
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
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CarPlaceSearchImpl(
    private val searchEngineSettings: SearchEngineSettings? = null,
) : CarPlaceSearch {

    private var searchEngine: SearchEngine? = null
    private val locationProvider = CarSearchLocationProvider()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        locationProvider.onAttached(mapboxNavigation)
        val settings = searchEngineSettings
            ?: SearchEngineSettings(
                accessToken = mapboxNavigation.navigationOptions.accessToken!!,
                locationProvider,
            )
        searchEngine = MapboxSearchSdk.createSearchEngineWithBuiltInDataProviders(settings)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        locationProvider.onDetached(mapboxNavigation)
    }

    override suspend fun search(query: String): Result<List<SearchSuggestion>> {
        val searchEngine = searchEngine
            ?: return Result.failure(
                IllegalStateException("SearchEngine is not available for place search")
            )

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
                    responseInfo: ResponseInfo
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
            continuation.invokeOnCancellation { task.cancel() }
        }
    }

    override suspend fun select(selection: SearchSuggestion): Result<List<SearchResult>> {
        val searchEngine = searchEngine
            ?: return Result.failure(
                IllegalStateException("SearchEngine is not available for place selection")
            )

        return suspendCancellableCoroutine { continuation ->
            val selectionCallback = object : SearchSelectionCallback {
                override fun onCategoryResult(
                    suggestion: SearchSuggestion,
                    results: List<SearchResult>,
                    responseInfo: ResponseInfo
                ) {
                    continuation.resume(Result.success(results))
                }

                override fun onError(e: Exception) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResult(
                    suggestion: SearchSuggestion,
                    result: SearchResult,
                    responseInfo: ResponseInfo
                ) {
                    continuation.resume(Result.success(listOf(result)))
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
            val task = searchEngine.select(selection, selectionCallback)
            continuation.invokeOnCancellation { task.cancel() }
        }
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 100
        private const val SEARCH_RESULT_LIMIT = 5
    }
}
