package com.mapbox.navigation.core.replay.route

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point

/**
 * The [ReplayLocationConverter] interface is for the [ReplayRouteLocationEngine] to interpolate
 * speed and time on the route.
 */
interface ReplayLocationConverter {

    /**
     * *true* if route has more than one leg, *false* otherwise
     */
    val isMultiLegRoute: Boolean

    /**
     * Set replaying route
     */
    fun setRoute(route: DirectionsRoute)

    /**
     * Set new speed(km/h)
     */
    fun updateSpeed(customSpeedInKmPerHour: Int)

    /**
     * Delay between each route [Point]
     */
    fun updateDelay(customDelayInSeconds: Int)

    /**
     * Provide list of route's [Point] in List<Location>
     */
    fun toLocations(): List<Location>

    /**
     * Start time tracking. Should be called to interpolate time on location
     */
    fun initializeTime()

    /**
     * Interpolates the route into even points along the route and adds these to the points list.
     *
     * @param lineString our route geometry.
     * @return list of sliced [Point]s.
     */
    fun sliceRoute(lineString: LineString): List<Point>

    /**
     * Calculate locations according to speed and delay
     */
    fun calculateMockLocations(points: List<Point>): List<Location>
}
