package com.mapbox.navigation.ui.maps.camera.lifecycle

import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.navigation.ui.maps.internal.camera.lifecycle.UserLocationIndicatorPositionObserver
import com.mapbox.navigation.ui.maps.internal.camera.lifecycle.UserLocationIndicatorPositionProvider

internal class LocationPluginPositionProvider(
    private val locationPlugin: LocationComponentPlugin,
) : UserLocationIndicatorPositionProvider {

    override fun addObserver(observer: UserLocationIndicatorPositionObserver) {
        locationPlugin.addOnIndicatorPositionChangedListener(
            LocationPluginPositionChangedListenerAdapter(observer),
        )
    }

    override fun removeObserver(observer: UserLocationIndicatorPositionObserver) {
        locationPlugin.removeOnIndicatorPositionChangedListener(
            LocationPluginPositionChangedListenerAdapter(observer),
        )
    }
}
