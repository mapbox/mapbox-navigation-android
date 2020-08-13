package com.mapbox.navigation.ui.camera

import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.map.NavigationMapboxMap

/**
 * This class handles calculating camera's zoom and tilt properties while routing.
 * The [NavigationMapboxMap] uses
 * a [SimpleCamera] by default. If you would like to customize the camera properties, create a
 * concrete implementation of this class or subclass [SimpleCamera] and update
 * [NavigationMapboxMap.setCamera] or [NavigationViewOptions.Builder.camera].
 *
 * @see SimpleCamera
 * @see DynamicCamera
 */
abstract class Camera {

    /**
     * The angle, in degrees, of the camera angle from the nadir (directly facing the Earth).
     *
     * See [CameraPosition.Builder.tilt] for constraints.
     */
    abstract fun tilt(routeInformation: RouteInformation): Double

    /**
     * Zoom level near the center of the screen.
     *
     * See [CameraPosition.Builder.zoom] for constraints.
     */
    abstract fun zoom(routeInformation: RouteInformation): Double

    /**
     * Return a list of route coordinates that should be visible when creating the route's overview.
     */
    abstract fun overview(routeInformation: RouteInformation): List<Point>
}
