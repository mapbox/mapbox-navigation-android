package com.mapbox.navigation.dropin.util

import android.location.Location
import android.location.LocationManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.MapboxNavigationObserverChain
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.internal.extensions.ReloadingComponent
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

    /**
     * Recursively traverse a receiver's ([MapboxNavigationObserverChain] or [ReloadingComponent])
     * component tree and return first component that matches given [predicate].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun MapboxNavigationObserver?.findComponent(
        predicate: (MapboxNavigationObserver?) -> Boolean
    ): MapboxNavigationObserver? {
        return when (this) {
            is MapboxNavigationObserverChain -> {
                toList().firstNotNullOfOrNull {
                    it.findComponent(predicate)
                }
            }
            is ReloadingComponent<*> -> {
                childComponent.findComponent(predicate)
            }
            else -> if (predicate(this)) this else null
        }
    }
}
