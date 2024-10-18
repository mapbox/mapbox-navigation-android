package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.indexOfNextRequestedCoordinate
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdaterTestUtils.provideRouteOptionsWithCoordinates
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalMapboxNavigationAPI
@RunWith(Parameterized::class)
class ApproachesRouteOptionsUpdaterTest(
    private val description: String,
    private val routeOptions: RouteOptions,
    private val idxOfNextRequestedCoordinate: Int,
    private val expectedApproachesList: List<String?>?,
) {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeOptionsUpdater: RouteOptionsUpdater

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = listOf(
            arrayOf(
                "empty approaches list correspond to null result list",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .approachesList(emptyList())
                    .build(),
                2,
                null,
            ),
            arrayOf(
                "approaches list exist, index of next coordinate is 1, it has to become " +
                    "null in the result list",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .approachesList(
                        provideApproachesList(
                            null,
                            DirectionsCriteria.APPROACH_CURB,
                            DirectionsCriteria.APPROACH_UNRESTRICTED,
                            DirectionsCriteria.APPROACH_CURB,
                        ),
                    )
                    .build(),
                1,
                provideApproachesList(
                    null,
                    DirectionsCriteria.APPROACH_UNRESTRICTED,
                    DirectionsCriteria.APPROACH_CURB,
                ),
            ),
        )

        private fun provideApproachesList(vararg approaches: String?): List<String?> =
            approaches.toList()
    }

    @Before
    fun setup() {
        routeOptionsUpdater = RouteOptionsUpdater()
    }

    @Test
    fun testCases() {
        mockkStatic(::indexOfNextRequestedCoordinate) {
            val mockRemainingWaypoints = -1
            val mockWaypoints = listOf(mockk<Waypoint>())
            val routeProgress: RouteProgress = mockk(relaxed = true) {
                every { remainingWaypoints } returns mockRemainingWaypoints
                every { navigationRoute.internalWaypoints() } returns mockWaypoints
            }
            every {
                indexOfNextRequestedCoordinate(mockWaypoints, mockRemainingWaypoints)
            } returns idxOfNextRequestedCoordinate

            val updatedRouteOptions = routeOptionsUpdater.update(
                routeOptions,
                routeProgress,
                mockLocationMatcher(),
            ).let {
                assertTrue(it is RouteOptionsUpdater.RouteOptionsResult.Success)
                it as RouteOptionsUpdater.RouteOptionsResult.Success
            }.routeOptions

            assertEquals(expectedApproachesList, updatedRouteOptions.approachesList())
        }
    }

    private fun mockLocationMatcher(): LocationMatcherResult = mockk {
        every { enhancedLocation } returns mockk {
            every { latitude } returns 1.1
            every { longitude } returns 2.2
            every { bearing } returns 3.3
            every { zLevel } returns 4
        }
    }
}
