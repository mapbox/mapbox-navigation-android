package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog.logAndroidAutoWarning
import com.mapbox.navigation.ui.androidauto.internal.search.CarPlaceSearch
import com.mapbox.navigation.ui.androidauto.internal.search.CarPlaceSearchImpl
import com.mapbox.navigation.ui.androidauto.internal.search.CarSearchLocationProvider

/**
 * Contains the dependencies for the search feature.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class SearchCarContext(
    val mapboxCarContext: MapboxCarContext,
) {
    /** MapboxCarContext **/
    val carContext = mapboxCarContext.carContext
    val mapboxScreenManager = mapboxCarContext.mapboxScreenManager

    /** SearchCarContext **/
    val mapboxCarMap = mapboxCarContext.mapboxCarMap
    val routePreviewRequest = mapboxCarContext.routePreviewRequest
    val carPlaceSearch: CarPlaceSearch = CarPlaceSearchImpl(
        mapboxCarContext.options,
        CarSearchLocationProvider(),
        mapboxCarContext.options.searchMode,
    )

    init {
        @Suppress("DEPRECATION")
        if (
            mapboxCarContext.options.searchMode is CarSearchMode.Legacy &&
            LEGACY_WARN_LOGGED.compareAndSet(false, true)
        ) {
            logAndroidAutoWarning(
                "CarSearchMode.Legacy is deprecated and will be removed in a future release. " +
                    "Geocoding V5 no longer provides POI data. " +
                    "Migrate to CarSearchMode.SearchBox via " +
                    "MapboxCarContext.customize { searchMode = CarSearchMode.SearchBox }.",
            )
        }
    }

    private companion object {
        val LEGACY_WARN_LOGGED = java.util.concurrent.atomic.AtomicBoolean(false)
    }
}
