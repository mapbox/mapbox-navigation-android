package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.preview.CarRouteRequest

/**
 * Contains the dependencies for the search feature.
 */
class SearchCarContext(
    val mainCarContext: MainCarContext
) {
    /** MainCarContext **/
    val carContext = mainCarContext.carContext
    val distanceFormatter = mainCarContext.distanceFormatter
    val feedbackPollProvider = mainCarContext.feedbackPollProvider

    /** SearchCarContext **/
    val carSearchEngine = CarSearchEngine(
        mainCarContext.searchEngine,
        MapboxCarApp.carAppLocationService().navigationLocationProvider
    )
    val carRouteRequest = CarRouteRequest(
        mainCarContext.mapboxNavigation,
        mainCarContext.routeOptionsInterceptor,
        MapboxCarApp.carAppLocationService().navigationLocationProvider
    )
}
