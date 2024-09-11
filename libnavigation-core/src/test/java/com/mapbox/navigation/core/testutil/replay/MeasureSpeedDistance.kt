package com.mapbox.navigation.core.testutil.replay

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * This is used to verify the simulated locations have accurate measurements for speed, distance,
 * and time. The difference between [locationSpeed] and [distanceSpeed] should be minimized. In
 * other words, we expect the estimated speed to match the actual speed.
 *
 * [locationSpeed] is the estimated speed on a location, normalized by the time.
 * [distanceSpeed] is the haversine distance between location coordinates, normalized by time.
 */
internal data class LocationSpeedDistance(
    val locationSpeed: Double,
    val distanceSpeed: Double,
)

internal fun List<ReplayEventBase>.measureSpeedDistances(): List<LocationSpeedDistance> {
    val locations = filterIsInstance(ReplayEventUpdateLocation::class.java)
        .toList().map { it.location }
    val speedDistances = mutableListOf<LocationSpeedDistance>()

    for (i in 1..lastIndex) {
        val previousLocation = locations[i - 1]
        val currentLocation = locations[i]
        val previous = Point.fromLngLat(previousLocation.lon, previousLocation.lat)
        val current = Point.fromLngLat(currentLocation.lon, currentLocation.lat)
        val distance = TurfMeasurement.distance(previous, current, TurfConstants.UNIT_METERS)
        speedDistances.add(
            measureSpeedDistance(
                fromSpeed = previousLocation.speed!!,
                toSpeed = currentLocation.speed!!,
                fromTime = previousLocation.time!!,
                toTime = currentLocation.time!!,
                distance = distance,
            ),
        )
    }
    return speedDistances
}

internal fun measureSpeedDistance(
    fromSpeed: Double,
    toSpeed: Double,
    fromTime: Double,
    toTime: Double,
    distance: Double,
) = LocationSpeedDistance(
    locationSpeed = (fromSpeed + toSpeed) / 2.0,
    distanceSpeed = distance / (toTime - fromTime),
)

class MeasureSpeedDistanceTest {

    @Test
    fun `should create speed distances for all but the first event`() {
        val mockEvents = mock1HzEvents()

        val speedDistances = mockEvents.measureSpeedDistances()

        assertEquals(mockEvents.size - 1, speedDistances.size)
    }

    @Test
    fun `location and speed distances are nearly identical`() {
        val mockEvents = mock1HzEvents()

        val speedDistances = mockEvents.measureSpeedDistances()
        assertEquals(mockEvents.size - 1, speedDistances.size)
        listOf(
            1.1969,
            3.5880,
            5.9847,
            8.3695,
        ).forEachIndexed { index, expected ->
            assertEquals(expected, speedDistances[index].locationSpeed, 0.01)
            assertEquals(expected, speedDistances[index].distanceSpeed, 0.01)
        }
    }

    private fun mock1HzEvents() = listOf<ReplayEventUpdateLocation>(
        mockk {
            every { location } returns mockk {
                every { lat } returns 48.27279
                every { lon } returns 11.57012
                every { time } returns 0.0
                every { speed } returns 0.0
            }
        },
        mockk {
            every { location } returns mockk {
                every { lat } returns 48.272782150915674
                every { lon } returns 11.57011476727463
                every { time } returns 0.7979728559957331
                every { speed } returns 2.393918567987199
            }
        },
        mockk {
            every { location } returns mockk {
                every { lat } returns 48.2727587283017
                every { lon } returns 11.570098728300525
                every { time } returns 1.5959457119914662
                every { speed } returns 4.787837135974398
            }
        },
        mockk {
            every { location } returns mockk {
                every { lat } returns 48.2727229860069
                every { lon } returns 11.570062986004263
                every { time } returns 2.3939185679871993
                every { speed } returns 7.1817557039615965
            }
        },
        mockk {
            every { location } returns mockk {
                every { lat } returns 48.27268
                every { lon } returns 11.57
                every { time } returns 3.1918914239829324
                every { speed } returns 9.575674271948794
            }
        },
    )
}
