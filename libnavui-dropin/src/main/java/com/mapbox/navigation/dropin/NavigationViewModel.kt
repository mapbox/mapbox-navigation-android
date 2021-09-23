package com.mapbox.navigation.dropin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider

class NavigationViewModel(
    accessToken: String,
    application: Application
) : AndroidViewModel(application) {

    internal val mapboxNavigation: MapboxNavigation = MapboxNavigationProvider.create(
        NavigationOptions.Builder(application.applicationContext)
            .accessToken(accessToken)
            .build()
    )
}
