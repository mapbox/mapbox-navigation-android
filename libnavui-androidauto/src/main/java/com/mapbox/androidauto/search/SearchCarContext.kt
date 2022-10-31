package com.mapbox.androidauto.search

import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.internal.search.CarPlaceSearch
import com.mapbox.androidauto.internal.search.CarPlaceSearchImpl
import com.mapbox.androidauto.internal.search.CarSearchLocationProvider

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
        CarSearchLocationProvider()
    )
}
