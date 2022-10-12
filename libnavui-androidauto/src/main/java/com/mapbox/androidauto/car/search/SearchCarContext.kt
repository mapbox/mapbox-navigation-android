package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.internal.car.search.CarPlaceSearch
import com.mapbox.androidauto.internal.car.search.CarPlaceSearchImpl
import com.mapbox.androidauto.internal.car.search.CarSearchLocationProvider

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
    val carRoutePreviewRequest = mapboxCarContext.carRoutePreviewRequest
    val carPlaceSearch: CarPlaceSearch = CarPlaceSearchImpl(
        mapboxCarContext.carPlaceSearchOptions,
        CarSearchLocationProvider()
    )
}
