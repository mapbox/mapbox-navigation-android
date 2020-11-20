package com.mapbox.navigation.ui.maps.route.routearrow.internal

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maps.route.routearrow.internal.RouteArrowUtils.obtainArrowPointsFrom
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteArrowUtilsTest {

    @Test
    fun obtainArrowPointsFromWhenCurrentArrowLessThan2Points() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val routeStepPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun obtainArrowPointsFromWhenUpComingArrowLineLessThan2Points() {
        val upcomingPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun obtainArrowPointsFrom() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = obtainArrowPointsFrom(routeProgress)

        assertEquals(4, result.size)
    }
}
