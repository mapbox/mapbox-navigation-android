package com.mapbox.androidauto.car.preview

import com.mapbox.androidauto.car.MainCarContext

data class RoutePreviewCarContext internal constructor(
    val mainCarContext: MainCarContext
) {
    /** MainCarContext **/
    val carContext = mainCarContext.carContext
    val mapboxCarMap = mainCarContext.mapboxCarMap
    val distanceFormatter = mainCarContext.distanceFormatter
    val mapboxNavigation = mainCarContext.mapboxNavigation
}
