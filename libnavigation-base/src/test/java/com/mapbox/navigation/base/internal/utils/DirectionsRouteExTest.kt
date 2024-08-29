package com.mapbox.navigation.base.internal.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DirectionsRouteExTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun isRouteTheSame() {
        val mockkRoute = mockk<DirectionsRoute> {
            every { geometry() } throws AssertionError(
                "we should check reference first",
            )
            every { legs() } throws AssertionError(
                "we should check reference first",
            )
        }
        val comparing = listOf<Triple<DirectionsRoute, DirectionsRoute?, Boolean>>(
            Triple(
                getDirectionRouteBuilder().geometry("geomery").build(),
                getDirectionRouteBuilder().geometry("geomery").build(),
                true,
            ),
            Triple(
                getDirectionRouteBuilder().geometry("geomery_1").build(),
                getDirectionRouteBuilder().geometry("geomery").build(),
                false,
            ),
            Triple(
                getDirectionRouteBuilder().geometry("geomery").build(),
                null,
                false,
            ),
            Triple(
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four"),
                    ),
                ).build(),
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four"),
                    ),
                ).build(),
                true,
            ),
            Triple(
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four"),
                    ),
                ).build(),
                getDirectionRouteBuilder().legs(
                    listOf(
                        getListOfRouteLegs("one, two"),
                        getListOfRouteLegs("three, four_NOT"),
                    ),
                ).build(),
                false,
            ),
            Triple(
                mockkRoute,
                mockkRoute,
                true,
            ),
        )

        comparing.forEach { (route1, route2, compareResult) ->
            if (compareResult) {
                assertTrue(route1.isSameRoute(route2))
            } else {
                assertFalse(route1.isSameRoute(route2))
            }
        }
    }

    @Test
    fun waypointsMapToSdk_type() {
        val nativeWaypoints = listOf(
            mockk<Waypoint>(relaxed = true) {
                every { type } returns WaypointType.EV_CHARGING_USER
            },
            mockk(relaxed = true) {
                every { type } returns WaypointType.SILENT
            },
            mockk(relaxed = true) {
                every { type } returns WaypointType.EV_CHARGING_SERVER
            },
            mockk(relaxed = true) {
                every { type } returns WaypointType.REGULAR
            },
        )
        assertEquals(WaypointType.values().size, nativeWaypoints.size)

        assertEquals(
            listOf(
                com.mapbox.navigation.base.internal.route.Waypoint.EV_CHARGING_USER,
                com.mapbox.navigation.base.internal.route.Waypoint.SILENT,
                com.mapbox.navigation.base.internal.route.Waypoint.EV_CHARGING_SERVER,
                com.mapbox.navigation.base.internal.route.Waypoint.REGULAR,
            ),
            nativeWaypoints.mapToSdk().map { it.type },
        )
    }

    @Test
    fun waypointsMapToSdk_metadata() {
        val nativeWaypoints = listOf(
            mockk<Waypoint>(relaxed = true) {
                every { metadata } returns null
            },
            mockk(relaxed = true) {
                every { metadata } returns ""
            },
            mockk(relaxed = true) {
                every { metadata } returns "not a json"
            },
            mockk(relaxed = true) {
                every { metadata } returns "{}"
            },
            mockk(relaxed = true) {
                every { metadata } returns "{" +
                    "\"key\":\"value\"," +
                    "\"int key\":222," +
                    "\"array key\":[111,333,555]," +
                    "\"json key\":{\"inner key\":10}" +
                    "}"
            },
        )

        assertEquals(
            listOf(
                null,
                null,
                null,
                emptyMap<String, JsonElement>(),
                mapOf(
                    "key" to JsonPrimitive("value"),
                    "int key" to JsonPrimitive(222),
                    "array key" to JsonArray().apply {
                        add(JsonPrimitive(111))
                        add(JsonPrimitive(333))
                        add(JsonPrimitive(555))
                    },
                    "json key" to JsonObject().apply {
                        add("inner key", JsonPrimitive(10))
                    },
                ),
            ),
            nativeWaypoints.mapToSdk().map { it.metadata },
        )
    }

    @Test
    fun refreshTtl_noUnrecognizedProperties() {
        val route = getDirectionRouteBuilder().build()

        assertNull(route.refreshTtl())
    }

    @Test
    fun refreshTtl_noRefreshTtl() {
        val route = getDirectionRouteBuilder().unrecognizedJsonProperties(emptyMap()).build()

        assertNull(route.refreshTtl())
    }

    @Test
    fun refreshTtl_nonIntRefreshTtl() {
        val route = getDirectionRouteBuilder()
            .unrecognizedJsonProperties(mapOf("refresh_ttl" to JsonPrimitive("aaa")))
            .build()

        assertNull(route.refreshTtl())
    }

    @Test
    fun refreshTtl_hasRefreshTtl() {
        val route = getDirectionRouteBuilder()
            .unrecognizedJsonProperties(mapOf("refresh_ttl" to JsonPrimitive(10)))
            .build()

        assertEquals(10, route.refreshTtl())
    }

    private fun getDirectionRouteBuilder(): DirectionsRoute.Builder =
        DirectionsRoute.builder().duration(1.0).distance(2.0)

    private fun getListOfRouteLegs(vararg stepsNames: String): RouteLeg =
        RouteLeg.builder()
            .also { legBuilder ->
                legBuilder.steps(
                    stepsNames.map {
                        LegStep.builder()
                            .name(it)
                            .distance(3.0)
                            .duration(4.0)
                            .mode("")
                            .maneuver(mockk(relaxUnitFun = true))
                            .weight(5.0)
                            .build()
                    },
                )
            }
            .build()
}
