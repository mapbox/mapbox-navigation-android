package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.preview.CarRouteRequest
import com.mapbox.search.MapboxSearchSdk

/**
 * Contains the dependencies for the search feature.
 */
class SearchCarContext(
    val mainCarContext: MainCarContext
) {
    /** MainCarContext **/
    val carContext = mainCarContext.carContext
    val distanceFormatter = mainCarContext.distanceFormatter

    /** SearchCarContext **/
    val carSearchEngine = CarSearchEngine(
        MapboxSearchSdk.getSearchEngine(),
        MapboxCarApp.carAppLocationService().navigationLocationProvider
    )
    val carRouteRequest = CarRouteRequest(
        mainCarContext.mapboxNavigation,
        MapboxCarApp.carAppLocationService().navigationLocationProvider
    )
}
