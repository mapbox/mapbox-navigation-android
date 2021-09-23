package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider

internal class DropInUIMapboxNavigationFactory(
    private val context: Context,
    private val accessToken: String
) {

    fun getMapboxNavigation(): MapboxNavigation {
        return MapboxNavigationProvider.create(
            NavigationOptions.Builder(context.applicationContext)
                .accessToken(accessToken)
                .build()
        )
    }
}
