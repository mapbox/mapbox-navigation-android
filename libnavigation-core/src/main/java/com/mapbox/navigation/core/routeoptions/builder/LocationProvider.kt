package com.mapbox.navigation.core.routeoptions.builder

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.TripSessionLocationProvider
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

internal interface LocationProvider {
    suspend fun getCurrentLocation(): CurrentLocation
}

internal data class CurrentLocation(
    val point: Point,
    val bearing: Double?,
    val zLevel: Int?
)

internal class LocationFromTripSessionProvider(
    private val tripSessionLocationProvider: TripSessionLocationProvider
) : LocationProvider {
    override suspend fun getCurrentLocation(): CurrentLocation {
        val currentLocation = tripSessionLocationProvider.locationMatcherResult
        return currentLocation?.toCurrentLocation() ?: waitForTheFirstLocationEventWithTimeout()
    }

    private suspend fun waitForTheFirstLocationEventWithTimeout() =
        withTimeout(GETTING_LOCATION_TIMEOUT_MILLISECONDS) {
            waitForTheFirstLocationEvent()
        }

    private suspend fun waitForTheFirstLocationEvent(): CurrentLocation {
        val (result, cleanup) = suspendCancellableCoroutine<Pair<CurrentLocation, () -> Unit>>
        { continuation ->
            val observer = object : LocationObserver {
                override fun onNewRawLocation(rawLocation: Location) {
                }

                override fun onNewLocationMatcherResult(
                    locationMatcherResult: LocationMatcherResult
                ) {
                    continuation.resume(
                        Pair(
                            locationMatcherResult.toCurrentLocation(),
                            {
                                tripSessionLocationProvider.unregisterLocationObserver(this)
                            }
                        )
                    )
                }
            }
            tripSessionLocationProvider.registerLocationObserver(observer)
            continuation.invokeOnCancellation {
                tripSessionLocationProvider.unregisterLocationObserver(observer)
            }
        }
        cleanup()
        return result
    }

    private fun LocationMatcherResult.toCurrentLocation() = CurrentLocation(
        point = enhancedLocation.toPoint(),
        bearing = enhancedLocation.bearing.toDouble(),
        zLevel = zLevel
    )

    private companion object {
        private const val GETTING_LOCATION_TIMEOUT_MILLISECONDS = 30_000L
    }
}
