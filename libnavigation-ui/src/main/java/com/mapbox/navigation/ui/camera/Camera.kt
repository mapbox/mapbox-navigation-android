package com.mapbox.navigation.ui.camera

import com.mapbox.geojson.Point

/**
 * This class handles calculating camera's zoom and tilt properties while routing.
 * The [MapboxNavigation] uses
 * a [SimpleCamera] by default. If you would like to customize the camera properties, create a
 * concrete implementation of this class or subclass [SimpleCamera] and update
 * [MapboxNavigation.setCameraEngine].
 *
 */
abstract class Camera {

    /**
     * The angle, in degrees, of the camera angle from the nadir (directly facing the Earth).
     * See tilt(float) for details of restrictions on the range of values.
     */
    abstract fun tilt(routeInformation: RouteInformation): Double

    /**
     * Zoom level near the center of the screen. See zoom(float) for the definition of the camera's
     * zoom level.
     */
    abstract fun zoom(routeInformation: RouteInformation): Double

    /**
     * Return a list of route coordinates that should be visible when creating the route's overview.
     */
    abstract fun overview(routeInformation: RouteInformation): List<Point>
}
