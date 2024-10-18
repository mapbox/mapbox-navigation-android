package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
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
class UnrecognizedRouteOptionsUpdaterTest(
    private val description: String,
    private val routeOptions: RouteOptions,
    private val idxOfNextRequestedCoordinate: Int,
    private val expected: Map<String, JsonElement>?,
) {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var routeOptionsUpdater: RouteOptionsUpdater

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = listOf(
            arrayOf(
                "null unrecognized properties to null",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(null)
                    .build(),
                2,
                null,
            ),
            arrayOf(
                "empty unrecognized properties to empty",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(emptyMap())
                    .build(),
                2,
                emptyMap<String, JsonElement>(),
            ),
            arrayOf(
                "ev data present for non ev route should not be changed",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";;ac;dc"),
                        ),
                    )
                    .build(),
                2,
                mapOf(
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";;ac;dc"),
                ),
            ),
            arrayOf(
                "ev data present for ev route should be changed",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "engine" to JsonPrimitive("electric"),
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";2;;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";2000;;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";ac;;dc"),
                        ),
                    )
                    .build(),
                2,
                mapOf(
                    "engine" to JsonPrimitive("electric"),
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";;3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";;3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";;dc"),
                ),
            ),
            arrayOf(
                "ev data present for ev route should not be changed for the first coordinate",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "engine" to JsonPrimitive("electric"),
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";;ac;dc"),
                        ),
                    )
                    .build(),
                1,
                mapOf(
                    "engine" to JsonPrimitive("electric"),
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";;2;3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";;2000;3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";;ac;dc"),
                ),
            ),
            arrayOf(
                "ev data present for ev route should be changed for the last coordinate",
                provideRouteOptionsWithCoordinates().toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "engine" to JsonPrimitive("electric"),
                            "aaa" to JsonPrimitive("bbb"),
                            "waypoints.charging_station_id" to JsonPrimitive(";2;;3"),
                            "waypoints.charging_station_power" to JsonPrimitive(";2000;;3000"),
                            "waypoints.charging_station_current_type" to
                                JsonPrimitive(";ac;;dc"),
                        ),
                    )
                    .build(),
                3,
                mapOf(
                    "engine" to JsonPrimitive("electric"),
                    "aaa" to JsonPrimitive("bbb"),
                    "waypoints.charging_station_id" to JsonPrimitive(";3"),
                    "waypoints.charging_station_power" to JsonPrimitive(";3000"),
                    "waypoints.charging_station_current_type" to JsonPrimitive(";dc"),
                ),
            ),
        )
    }

    @Before
    fun setup() {
        routeOptionsUpdater = RouteOptionsUpdater()
    }

    @Test
    fun unrecognizedJsonProperties() {
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

            assertEquals(expected, updatedRouteOptions.unrecognizedJsonProperties)
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
