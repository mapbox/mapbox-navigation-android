package com.mapbox.androidauto.car.location

import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * Create a simple 3d location puck. This class is demonstrating how to
 * create a renderer. To Create a new location experience, try creating a new class.
 */
@OptIn(MapboxExperimental::class)
class CarLocationRenderer : MapboxCarMapObserver {

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarLocationRenderer carMapSurface loaded")
        mapboxCarMapSurface.mapSurface.location.apply {
            locationPuck = CarLocationPuck.navigationPuck2D(mapboxCarMapSurface.carContext)
            enabled = true
            pulsingEnabled = true
            setLocationProvider(CarLocationProvider.getRegisteredInstance())
        }
    }
}
