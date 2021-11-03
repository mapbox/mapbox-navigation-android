package com.mapbox.navigation.ui.car.map

import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import com.mapbox.maps.MapSurface
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * This contains the Android Auto head unit map surface.
 *
 * @see MapboxCarMap.registerObserver
 *
 * @param carContext ContextWrapper accessible through your CarAppService and Screen instances
 * @param mapSurface Mapbox map controllable
 * @param surfaceContainer A container for the Surface created by the host
 * @param style the loaded Mapbox style
 */
@ExperimentalMapboxNavigationAPI
class MapboxCarMapSurface internal constructor(
    val carContext: CarContext,
    val mapSurface: MapSurface,
    val surfaceContainer: SurfaceContainer,
    val style: Style
) {
    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxCarMapSurface(carContext=$carContext," +
            " mapSurface=$mapSurface," +
            " surfaceContainer=$surfaceContainer," +
            " style=$style" +
            ")"
    }
}
