package com.mapbox.androidauto.navigation.location

import android.location.Location
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

/**
 * Provides a way to access the car or app navigation location.
 * Access through [MapboxCarApp.carAppServices].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
interface CarAppLocation : MapboxNavigationObserver {

    /**
     * location provider that is attached to [MapboxNavigation].
     * This provider can be used as a relay for the latest map coordinates.
     */
    val navigationLocationProvider: NavigationLocationProvider

    /**
     * Helper function that will suspend until a location is found,
     * or until the coroutine scope is no longer active.
     */
    suspend fun validLocation(): Location?
}
