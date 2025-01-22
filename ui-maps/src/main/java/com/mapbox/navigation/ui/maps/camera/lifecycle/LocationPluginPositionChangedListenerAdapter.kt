package com.mapbox.navigation.ui.maps.camera.lifecycle

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.ui.maps.camera.internal.lifecycle.UserLocationIndicatorPositionObserver

internal class LocationPluginPositionChangedListenerAdapter(
    private val observer: UserLocationIndicatorPositionObserver,
) : OnIndicatorPositionChangedListener {

    override fun onIndicatorPositionChanged(point: Point) {
        observer.onPositionUpdated(point)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationPluginPositionChangedListenerAdapter

        if (observer != other.observer) return false

        return true
    }

    override fun hashCode(): Int {
        return observer.hashCode()
    }
}
