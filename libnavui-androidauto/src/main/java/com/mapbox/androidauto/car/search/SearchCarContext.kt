package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.preview.CarRouteRequest
import com.mapbox.androidauto.internal.car.search.CarPlaceSearchImpl
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Contains the dependencies for the search feature.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SearchCarContext(
    val mainCarContext: MainCarContext,
) {
    /** MainCarContext **/
    val carContext = mainCarContext.carContext
    val distanceFormatter = mainCarContext.distanceFormatter
    val feedbackPollProvider = mainCarContext.feedbackPollProvider

    /** SearchCarContext **/
    val carPlaceSearch: CarPlaceSearch = CarPlaceSearchImpl(
        mainCarContext.searchEngineSettings
    )
    val carRouteRequest = CarRouteRequest(
        mainCarContext.mapboxNavigation,
        mainCarContext.routeOptionsInterceptor,
        MapboxCarApp.carAppLocationService().navigationLocationProvider
    )
}
