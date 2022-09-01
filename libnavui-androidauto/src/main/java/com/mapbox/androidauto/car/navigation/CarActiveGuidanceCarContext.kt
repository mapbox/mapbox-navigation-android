package com.mapbox.androidauto.car.navigation

import com.mapbox.androidauto.car.MainCarContext

class CarActiveGuidanceCarContext(
    val mainCarContext: MainCarContext
) {
    /** MapCarContext **/
    val carContext = mainCarContext.carContext
    val mapboxCarMap = mainCarContext.mapboxCarMap
    val mapboxNavigation = mainCarContext.mapboxNavigation
    val distanceFormatter = mainCarContext.distanceFormatter
}
