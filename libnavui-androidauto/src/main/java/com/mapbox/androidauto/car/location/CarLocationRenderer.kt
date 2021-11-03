package com.mapbox.androidauto.car.location

import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * Create a simple 3d location puck. This class is demonstrating how to
 * create a renderer. To Create a new location experience, try creating a new class.
 */
@OptIn(MapboxExperimental::class)
class CarLocationRenderer(
    private val mainCarContext: MainCarContext
) : MapboxCarMapObserver {

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarLocationRenderer carMapSurface loaded")
        mapboxCarMapSurface.mapSurface.location.apply {
            locationPuck = CarLocationPuck.navigationPuck2D(mainCarContext.carContext)
            enabled = true
            pulsingEnabled = true
            setLocationProvider(MapboxCarApp.carAppLocationService().navigationLocationProvider)
        }
    }
}
