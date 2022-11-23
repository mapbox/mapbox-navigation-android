package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StepDistanceCacheTest {

    private val route = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns "id#0"
    }
    private val p1 = mockk<Point>(relaxed = true)
    private val p2 = mockk<Point>(relaxed = true)
    private val p3 = mockk<Point>(relaxed = true)
    private val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
        every { stepIndex } returns 2
        every { stepPoints } returns listOf(p1, p2, p3)
    }
    private val legProgress = mockk<RouteLegProgress>(relaxed = true) {
        every { currentStepProgress } returns this@StepDistanceCacheTest.stepProgress
        every { legIndex } returns 1
    }
    private val routeProgress = mockk<RouteProgress>(relaxed = true) {
        every { navigationRoute } returns this@StepDistanceCacheTest.route
        every { currentLegProgress } returns this@StepDistanceCacheTest.legProgress
    }
    private val sut = StepDistanceCache()

    @Before
    fun setUp() {
        mockkStatic(TurfMeasurement::class)
        every { TurfMeasurement.distance(p1, p2, TurfConstants.UNIT_METERS) } returns 1.2
        every { TurfMeasurement.distance(p2, p3, TurfConstants.UNIT_METERS) } returns 3.4
    }

    @After
    fun tearDown() {
        unmockkStatic(TurfMeasurement::class)
    }

    @Test
    fun initialCurrentDistances() {
        assertNull(sut.currentDistances())
    }

    @Test
    fun firstValidOnRouteProgressUpdate() {
        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(listOf(1.2, 3.4), sut.currentDistances())
    }

    @Test
    fun firstValidOnRouteProgressUpdateNullStepPoints() {
        every { stepProgress.stepPoints } returns null

        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(emptyList<Double>(), sut.currentDistances())
    }

    @Test
    fun firstValidOnRouteProgressUpdateEmptyStepPoints() {
        every { stepProgress.stepPoints } returns emptyList()

        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(emptyList<Double>(), sut.currentDistances())
    }

    @Test
    fun firstValidOnRouteProgressUpdateOneStepPoint() {
        every { stepProgress.stepPoints } returns listOf(p1)

        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(emptyList<Double>(), sut.currentDistances())
    }

    @Test
    fun secondValidOnRouteProgressUpdateSameRouteLegIndexChanged() {
        sut.onRouteProgressUpdate(routeProgress)

        every { legProgress.legIndex } returns 2
        every { TurfMeasurement.distance(p1, p2, TurfConstants.UNIT_METERS) } returns 5.6
        every { TurfMeasurement.distance(p2, p3, TurfConstants.UNIT_METERS) } returns 7.8
        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(listOf(5.6, 7.8), sut.currentDistances())
    }

    @Test
    fun secondValidOnRouteProgressUpdateSameRouteStepIndexChanged() {
        sut.onRouteProgressUpdate(routeProgress)

        every { stepProgress.stepIndex } returns 3
        every { TurfMeasurement.distance(p1, p2, TurfConstants.UNIT_METERS) } returns 5.6
        every { TurfMeasurement.distance(p2, p3, TurfConstants.UNIT_METERS) } returns 7.8
        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(listOf(5.6, 7.8), sut.currentDistances())
    }

    @Test
    fun secondValidOnRouteProgressUpdateSameRouteNothingChanged() {
        sut.onRouteProgressUpdate(routeProgress)
        clearStaticMockk(TurfMeasurement::class, answers = false)
        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(listOf(1.2, 3.4), sut.currentDistances())
        verify(exactly = 0) { TurfMeasurement.distance(any(), any(), any()) }
    }

    @Test
    fun secondValidOnRouteProgressUpdateNewRoute() {
        sut.onRouteProgressUpdate(routeProgress)

        every { route.id } returns "id#1"
        every { TurfMeasurement.distance(p1, p2, TurfConstants.UNIT_METERS) } returns 5.6
        every { TurfMeasurement.distance(p2, p3, TurfConstants.UNIT_METERS) } returns 7.8
        sut.onRouteProgressUpdate(routeProgress)

        assertEquals(listOf(5.6, 7.8), sut.currentDistances())
    }

    @Test
    fun secondInvalidOnRouteProgressUpdateNewRoute_nullLegProgress() {
        sut.onRouteProgressUpdate(routeProgress)

        every { route.id } returns "id#1"
        every { routeProgress.currentLegProgress } returns null
        sut.onRouteProgressUpdate(routeProgress)

        assertNull(sut.currentDistances())
    }

    @Test
    fun secondInvalidOnRouteProgressUpdateNewRoute_nullStepProgress() {
        sut.onRouteProgressUpdate(routeProgress)

        every { route.id } returns "id#1"
        every { legProgress.currentStepProgress } returns null
        sut.onRouteProgressUpdate(routeProgress)

        assertNull(sut.currentDistances())
    }
}
