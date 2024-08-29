package com.mapbox.navigation.ui.androidauto.location

import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto

/**
 * Create a simple 3d location puck. This class is demonstrating how to
 * create a renderer. To Create a new location experience, try creating a new class.
 */
class CarLocationRenderer : MapboxCarMapObserver {

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarLocationRenderer carMapSurface loaded")
        mapboxCarMapSurface.mapSurface.location.apply {
            locationPuck = CarLocationPuck.navigationPuck2D()
            enabled = true
            pulsingEnabled = true
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
            setLocationProvider(CarLocationProvider.getRegisteredInstance())
        }
    }
}
