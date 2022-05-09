package com.mapbox.navigation.qa_test_app.car.search

import android.app.Application
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.search.MapboxSearchSdk

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
object MapboxCarSearchApp {

    fun setup(application: Application) {
        System.setProperty("com.mapbox.mapboxsearch.enableSBS", true.toString())
        val carSearchLocationProvider = CarSearchLocationProvider()
        MapboxSearchSdk.initialize(
            application = application,
            accessToken = Utils.getMapboxAccessToken(application),
            locationEngine = carSearchLocationProvider,
        )
        MapboxNavigationApp.registerObserver(carSearchLocationProvider)
    }
}
