package com.mapbox.navigation.compose

import android.app.Application
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup MapboxNavigation
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(applicationContext)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        ).attachAllActivities()
    }
}
