package com.mapbox.navigation.qa_test_app

import android.app.Application
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.search.MapboxSearchSdk

class QaTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MapboxCarApp.setup(this)

        System.setProperty("com.mapbox.mapboxsearch.enableSBS", true.toString())
        MapboxSearchSdk.initialize(
            application = this,
            accessToken = Utils.getMapboxAccessToken(this),
            locationEngine = SearchLocationProvider(),
        )
    }
}
