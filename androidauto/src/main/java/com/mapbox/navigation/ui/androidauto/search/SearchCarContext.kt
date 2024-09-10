package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.internal.search.CarPlaceSearch
import com.mapbox.navigation.ui.androidauto.internal.search.CarPlaceSearchImpl
import com.mapbox.navigation.ui.androidauto.internal.search.CarSearchLocationProvider

/**
 * Contains the dependencies for the search feature.
 */
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
    )
}
