package com.mapbox.navigation.base.location

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point

interface ReplayLocationConverter {

    val isMultiLegRoute: Boolean

    fun setRoute(route: DirectionsRoute)

    fun updateSpeed(customSpeedInKmPerHour: Int)

    fun updateDelay(customDelayInSeconds: Int)

    fun toLocations(): List<Location>

    fun initializeTime()

    fun sliceRoute(lineString: LineString): List<Point>

    fun calculateMockLocations(points: List<Point>): List<Location>
}
