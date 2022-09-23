package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.preview.CarRouteRequest
import com.mapbox.androidauto.internal.car.search.CarPlaceSearch
import com.mapbox.androidauto.internal.car.search.CarPlaceSearchImpl
import com.mapbox.androidauto.internal.car.search.CarSearchLocationProvider

/**
 * Contains the dependencies for the search feature.
 */
class SearchCarContext(
    val mainCarContext: MainCarContext,
) {
    /** MainCarContext **/
    val carContext = mainCarContext.carContext
    val distanceFormatter = mainCarContext.distanceFormatter
    val feedbackPollProvider = mainCarContext.feedbackPollProvider

    /** SearchCarContext **/
    val mapboxCarMap = mainCarContext.mapboxCarMap
    val carRouteRequest = CarRouteRequest(mainCarContext.routeOptionsInterceptor)
    internal val carPlaceSearch: CarPlaceSearch = CarPlaceSearchImpl(
        mainCarContext.carPlaceSearchOptions,
        CarSearchLocationProvider()
    )
}
