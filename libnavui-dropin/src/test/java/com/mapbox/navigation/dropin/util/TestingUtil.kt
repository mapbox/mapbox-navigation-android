package com.mapbox.navigation.dropin.util

import android.location.Location
import android.location.LocationManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.FileUtils
import io.mockk.every
import io.mockk.mockk

object TestingUtil {
    fun loadRoute(routeFileName: String): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        return DirectionsRoute.fromJson(routeAsJson)
    }

    fun makeLocation(latitude: Double, longitude: Double, bearing: Float = 0f) =
        Location(LocationManager.PASSIVE_PROVIDER).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.bearing = bearing
        }

    fun makeLocationMatcherResult(lon: Double, lat: Double, bearing: Float) =
        mockk<LocationMatcherResult> {
            val location = makeLocation(lat, lon, bearing)
            every { keyPoints } returns listOf(location)
            every { enhancedLocation } returns location
        }
}
