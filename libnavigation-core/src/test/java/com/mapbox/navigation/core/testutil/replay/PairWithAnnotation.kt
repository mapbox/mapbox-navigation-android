package com.mapbox.navigation.core.testutil.replay

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.replay.route.ReplayRouteLocation
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

data class AnnotationProgress(
    val routeDistance: Double, // The distance along the route
    val annotationDistance: Double, // comes from LegAnnotation
    val speed: Double, // comes from LegAnnotation
)

internal fun List<ReplayRouteLocation>.pairWithAnnotation(
    annotation: LegAnnotation
): List<Pair<ReplayRouteLocation, AnnotationProgress>> {
    val result = mutableListOf<Pair<ReplayRouteLocation, AnnotationProgress>>()

    var driverLocationIndex = 0
    var currentAnnotationDistance = 0.0
    var currentDriverDistance = 0.0
    annotation.distance()!!.forEachIndexed { index, value ->

        // Increment the upper bound and accumulate the distances. All driver locations less than
        // the annotation distance will have the annotation speed.
        currentAnnotationDistance += value
        while (currentDriverDistance < currentAnnotationDistance) {
            val annotationProgress = AnnotationProgress(
                routeDistance = currentDriverDistance,
                annotationDistance = currentAnnotationDistance,
                speed = annotation.speed()!![index]
            )
            result.add(Pair(get(driverLocationIndex), annotationProgress))

            if (driverLocationIndex + 1 > lastIndex) {
                return result
            }
            currentDriverDistance += TurfMeasurement.distance(
                get(driverLocationIndex++).point,
                get(driverLocationIndex).point,
                TurfConstants.UNIT_METERS
            )
        }
    }

    // Handle edge case where there are more driver locations than annotations available.
    // This is supposed to be small because of floating point errors in annotation distances.
    while (driverLocationIndex <= lastIndex) {
        val annotationProgress = AnnotationProgress(
            routeDistance = currentDriverDistance,
            annotationDistance = currentAnnotationDistance,
            speed = annotation.speed()!!.last()
        )
        result.add(Pair(get(driverLocationIndex), annotationProgress))
        driverLocationIndex++
        if (driverLocationIndex <= lastIndex) {
            currentDriverDistance += TurfMeasurement.distance(
                get(driverLocationIndex - 1).point,
                get(driverLocationIndex).point,
                TurfConstants.UNIT_METERS
            )
        }
    }

    return result
}

class PairWithTrafficTest {
    @Test
    fun `verify driver locations pair with their distance and speed`() {
        val speeds = listOf(35.0, 13.2, 13.2, 13.2, 25.0)
        val annotation: LegAnnotation = mockk {
            every { distance() } returns listOf(1.7, 107.1, 80.9, 60.8, 67.8)
            every { speed() } returns speeds
        }
        val locations = listOf(
            mockReplayRouteLocation(9.484914, 51.228828),
            mockReplayRouteLocation(9.484899, 51.228817),
            mockReplayRouteLocation(9.484897, 51.228816),
            mockReplayRouteLocation(9.484824, 51.228766),
            mockReplayRouteLocation(9.484723, 51.228696),
            mockReplayRouteLocation(9.484593, 51.228606),
            mockReplayRouteLocation(9.484434, 51.228496),
            mockReplayRouteLocation(9.484246, 51.228366),
            mockReplayRouteLocation(9.484029, 51.228216),
            mockReplayRouteLocation(9.483866, 51.228103),
            mockReplayRouteLocation(9.483610, 51.227907),
            mockReplayRouteLocation(9.483345, 51.227703),
            mockReplayRouteLocation(9.483132, 51.227540),
            mockReplayRouteLocation(9.482873, 51.227333),
            mockReplayRouteLocation(9.482615, 51.227126),
            mockReplayRouteLocation(9.482595, 51.227110),
            mockReplayRouteLocation(9.482351, 51.226896),
            mockReplayRouteLocation(9.482108, 51.226682),
            mockReplayRouteLocation(9.482031, 51.226614)
        )

        val results = locations.pairWithAnnotation(annotation)

        val expected = listOf(
            AnnotationProgress(routeDistance = 0.0, annotationDistance = 1.7, speed = 35.0),
            AnnotationProgress(routeDistance = 1.6089223, annotationDistance = 1.7, speed = 35.0),
            AnnotationProgress(routeDistance = 1.7871874, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 9.3227297, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 19.816276, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 33.314663, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 49.817901, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 69.325999, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 91.838972, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 108.77664, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 136.94136, annotationDistance = 189.7, speed = 13.2),
            AnnotationProgress(routeDistance = 166.19192, annotationDistance = 189.7, speed = 13.2),
            AnnotationProgress(routeDistance = 189.61920, annotationDistance = 189.7, speed = 13.2),
            AnnotationProgress(routeDistance = 218.86985, annotationDistance = 250.5, speed = 13.2),
            AnnotationProgress(routeDistance = 248.07764, annotationDistance = 250.5, speed = 13.2),
            AnnotationProgress(routeDistance = 250.33774, annotationDistance = 250.5, speed = 13.2),
            AnnotationProgress(routeDistance = 279.58598, annotationDistance = 318.3, speed = 25.0),
            AnnotationProgress(routeDistance = 308.79383, annotationDistance = 318.3, speed = 25.0),
            AnnotationProgress(routeDistance = 318.06618, annotationDistance = 318.3, speed = 25.0),
        )

        assertEquals(locations.size, results.size)
        assertEquals(expected.size, results.size)
        expected.forEachIndexed { index, value ->
            val resultProgress = results[index].second
            assertEquals(value.routeDistance, resultProgress.routeDistance, 0.001)
            assertEquals(value.annotationDistance, resultProgress.annotationDistance, 0.001)
            assertEquals(value.speed, resultProgress.speed, 0.001)
        }
    }

    @Test
    fun `accepts locations that do not reach full distance`() {
        val speeds = listOf(35.0, 13.2, 13.2, 13.2, 25.0)
        val annotation: LegAnnotation = mockk {
            every { distance() } returns listOf(1.7, 107.1, 80.9, 60.8, 67.8)
            every { speed() } returns speeds
        }
        val locations = listOf(
            mockReplayRouteLocation(9.484914, 51.228828),
            mockReplayRouteLocation(9.484899, 51.228817),
            mockReplayRouteLocation(9.484897, 51.228816),
            mockReplayRouteLocation(9.484824, 51.228766),
            mockReplayRouteLocation(9.484723, 51.228696),
            mockReplayRouteLocation(9.484593, 51.228606),
            mockReplayRouteLocation(9.484434, 51.228496),
        )

        val results = locations.pairWithAnnotation(annotation)

        val expected = listOf(
            AnnotationProgress(routeDistance = 0.0, annotationDistance = 1.7, speed = 35.0),
            AnnotationProgress(routeDistance = 1.6089223, annotationDistance = 1.7, speed = 35.0),
            AnnotationProgress(routeDistance = 1.7871874, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 9.3227297, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 19.816276, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 33.314663, annotationDistance = 108.8, speed = 13.2),
            AnnotationProgress(routeDistance = 49.817901, annotationDistance = 108.8, speed = 13.2),
        )

        assertEquals(locations.size, results.size)
        assertEquals(expected.size, results.size)
        expected.forEachIndexed { index, value ->
            val resultProgress = results[index].second
            assertEquals(value.routeDistance, resultProgress.routeDistance, 0.001)
            assertEquals(value.annotationDistance, resultProgress.annotationDistance, 0.001)
            assertEquals(value.speed, resultProgress.speed, 0.001)
        }
    }

    @Test
    fun `uses last speed for extra locations`() {
        val speeds = listOf(35.0, 13.2)
        val annotation: LegAnnotation = mockk {
            every { distance() } returns listOf(1.7, 20.1)
            every { speed() } returns speeds
        }
        val locations = listOf(
            mockReplayRouteLocation(9.484914, 51.228828),
            mockReplayRouteLocation(9.484899, 51.228817),
            mockReplayRouteLocation(9.484897, 51.228816),
            mockReplayRouteLocation(9.484824, 51.228766),
            mockReplayRouteLocation(9.484723, 51.228696),
            mockReplayRouteLocation(9.484593, 51.228606),
            mockReplayRouteLocation(9.484434, 51.228496),
        )

        val results = locations.pairWithAnnotation(annotation)

        val expected = listOf(
            AnnotationProgress(routeDistance = 0.0, annotationDistance = 1.7, speed = 35.0),
            AnnotationProgress(routeDistance = 1.6089223, annotationDistance = 1.7, speed = 35.0),
            AnnotationProgress(routeDistance = 1.7871874, annotationDistance = 21.8, speed = 13.2),
            AnnotationProgress(routeDistance = 9.3227297, annotationDistance = 21.8, speed = 13.2),
            AnnotationProgress(routeDistance = 19.816276, annotationDistance = 21.8, speed = 13.2),
            AnnotationProgress(routeDistance = 33.314663, annotationDistance = 21.8, speed = 13.2),
            AnnotationProgress(routeDistance = 49.817901, annotationDistance = 21.8, speed = 13.2),
        )

        assertEquals(locations.size, results.size)
        assertEquals(expected.size, results.size)
        expected.forEachIndexed { index, value ->
            val resultProgress = results[index].second
            assertEquals(value.routeDistance, resultProgress.routeDistance, 0.001)
            assertEquals(value.annotationDistance, resultProgress.annotationDistance, 0.001)
            assertEquals(value.speed, resultProgress.speed, 0.001)
        }
    }

    private fun mockReplayRouteLocation(lng: Double, lat: Double): ReplayRouteLocation = mockk {
        every { point } returns Point.fromLngLat(lng, lat)
    }
}
