@file:Suppress("DEPRECATION")

package com.mapbox.navigation.testing

import android.content.Context
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider

fun createMapboxNavigation(context: Context): MapboxNavigation {
    val navigationOptions = NavigationOptions.Builder(context)
        .accessToken(context.getMapboxAccessTokenFromResources())
        .build()
    return MapboxNavigationProvider.create(navigationOptions)
}

fun Context.getMapboxAccessTokenFromResources(): String {
    return getString(resources.getIdentifier("mapbox_access_token", "string", packageName))
}
