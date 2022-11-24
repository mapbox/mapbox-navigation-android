package com.mapbox.navigation.core.navigator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavigatorMapperStepGeometryIndexTest(
    private val legIndex: Int,
    private val stepIndex: Int,
    private val legGeometryIndex: Int,
    private val expectedStepGeometryIndex: Int,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "legIndex = {0}, stepIndex = {1}, legGeometryIndex = {2}")
        fun data(): Collection<Array<Any?>> = listOf(
            arrayOf(0, 0, 0, 0),
            arrayOf(0, 0, 1, 1),
            arrayOf(0, 1, 1, 0),
            arrayOf(0, 1, 2, 1),
            arrayOf(0, 2, 2, 0),
            arrayOf(0, 2, 3, 1),
            arrayOf(0, 2, 4, 2),
            arrayOf(0, 2, 9, 7),
            arrayOf(0, 2, 10, 8),
            arrayOf(0, 3, 10, 0),
            arrayOf(0, 3, 27, 17),
            arrayOf(0, 4, 27, 0),
            arrayOf(0, 4, 31, 4),
            arrayOf(0, 5, 31, 0),
            arrayOf(0, 5, 33, 2),
            arrayOf(0, 6, 33, 0),
            arrayOf(0, 6, 41, 8),
            arrayOf(0, 7, 41, 0),
            arrayOf(0, 7, 42, 1),
            arrayOf(1, 0, 0, 0),
            arrayOf(1, 0, 1, 1),
            arrayOf(1, 0, 8, 8),
            arrayOf(1, 1, 8, 0),
            arrayOf(1, 1, 9, 1),
            arrayOf(1, 1, 10, 2),
            arrayOf(1, 2, 10, 0),
            arrayOf(1, 2, 13, 3),
            arrayOf(1, 3, 13, 0),
            arrayOf(1, 3, 36, 23),
            arrayOf(1, 4, 36, 0),
            arrayOf(1, 4, 69, 33),
            arrayOf(1, 5, 69, 0),
            arrayOf(1, 5, 72, 3),
            arrayOf(1, 6, 72, 0),
            arrayOf(1, 6, 73, 1),
            // invalid shapeIndex
            arrayOf(1, 6, 40, 0),
        )
    }

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun stepGeometryIndex() {
        // points sizes: [[2, 2, 9, 18, 5, 3, 9, 1], [9, 3, 4, 24, 34, 4, 1]]
        val testDirectionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("multileg_route.json")
        )
        val route = mockk<NavigationRoute> {
            every { directionsRoute } returns testDirectionsRoute
        }
        val status = mockk<NavigationStatus>(relaxed = true) {
            every { stepIndex } returns this@NavigatorMapperStepGeometryIndexTest.stepIndex
            every { legIndex } returns this@NavigatorMapperStepGeometryIndexTest.legIndex
            every { shapeIndex } returns legGeometryIndex
            every { routeState } returns RouteState.INITIALIZED
        }
        val actual = getRouteProgressFrom(route, status, 0, null, null, null)
        assertEquals(
            expectedStepGeometryIndex,
            actual!!.currentLegProgress!!.currentStepProgress!!.geometryIndex
        )
    }
}
