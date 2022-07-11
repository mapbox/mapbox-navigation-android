package com.mapbox.navigation.qa_test_app.car.search

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object MapboxCarSearchApp {

    fun setup() {
        System.setProperty("com.mapbox.mapboxsearch.enableSBS", true.toString())
        MapboxNavigationApp.registerObserver(CarSearchLocationProvider())
    }
}
