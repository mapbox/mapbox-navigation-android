package com.mapbox.androidauto.car.preview

import com.mapbox.androidauto.car.MapboxCarContext

data class RoutePreviewCarContext internal constructor(
    val mapboxCarContext: MapboxCarContext
) {
    /** MapboxCarContext **/
    val carContext = mapboxCarContext.carContext
    val mapboxCarMap = mapboxCarContext.mapboxCarMap
    val mapboxScreenManager = mapboxCarContext.mapboxScreenManager
}
