package com.mapbox.navigation.ui.androidauto.telemetry

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.AndroidAutoEvent
import com.mapbox.navigation.core.internal.telemetry.postAndroidAutoEvent
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

internal class MapboxCarTelemetry : MapboxNavigationObserver {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.postAndroidAutoEvent(AndroidAutoEvent.CONNECTED)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.postAndroidAutoEvent(AndroidAutoEvent.DISCONNECTED)
    }
}
