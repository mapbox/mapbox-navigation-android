package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.internal.search.forward
import com.mapbox.navigation.ui.androidauto.internal.search.search
import com.mapbox.search.ApiType
import com.mapbox.search.ForwardSearchOptions
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.result.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

internal class GeoDeeplinkSearchBox(
    private val searchEngine: SearchEngine = defaultSearchEngine(),
) {

    private var currentJob: Job? = null

    suspend fun requestPlaces(geoDeeplink: GeoDeeplink, origin: Point?): List<SearchResult>? {
        logAndroidAuto("GeoDeeplinkSearchBox.requestPlaces($geoDeeplink)")

        currentJob?.cancel()
        val point = geoDeeplink.point
        val placeQuery = geoDeeplink.placeQuery
        return coroutineScope {
            currentJob = coroutineContext[Job]
            when {
                point != null -> searchEngine.search(
                    ReverseGeoOptions(center = point, limit = SEARCH_RESULT_LIMIT),
                )
                placeQuery != null -> searchEngine.forward(
                    placeQuery,
                    ForwardSearchOptions.Builder()
                        .proximity(origin)
                        .limit(SEARCH_RESULT_LIMIT)
                        .build(),
                )
                else -> error("GeoDeeplink must have a point or a placeQuery")
            }.getOrElse { e ->
                logAndroidAutoFailure("GeoDeeplinkSearchBox error: ${e.message}")
                null
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }

    private companion object {
        private const val SEARCH_RESULT_LIMIT = 5

        fun defaultSearchEngine() = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            apiType = ApiType.SEARCH_BOX,
            settings = SearchEngineSettings(),
        )
    }
}
