package com.mapbox.navigation.base.route

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouterOrigin
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavigationRouteEqualsTest(
    private val description: String,
    private val directionsResponse1: DirectionsResponse,
    private val routeOptions1: RouteOptions,
    private val id1: String,
    private val expirationTime1: Long?,
    private val directionsResponse2: DirectionsResponse,
    private val routeOptions2: RouteOptions,
    private val id2: String,
    private val expirationTime2: Long?,
    private val expected: Boolean,
) {

    companion object {

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): Collection<Array<Any?>> {
            val waypoint1 = DirectionsWaypoint.builder()
                .name("name1")
                .rawLocation(doubleArrayOf(1.5, 2.5))
                .build()
            val unrecognizedWaypoints1 = mapOf(
                "waypoints" to JsonArray().apply {
                    add(
                        JsonObject().apply {
                            add("name", JsonPrimitive("name1"))
                            add(
                                "location",
                                JsonArray().apply {
                                    add(1.5)
                                    add(2.5)
                                }
                            )
                        }
                    )
                }
            )
            val waypoint2 = DirectionsWaypoint.builder()
                .name("name2")
                .rawLocation(doubleArrayOf(3.5, 4.5))
                .build()
            val unrecognizedWaypoints2 = mapOf(
                "waypoints" to JsonArray().apply {
                    add(
                        JsonObject().apply {
                            add("name", JsonPrimitive("name2"))
                            add(
                                "location",
                                JsonArray().apply {
                                    add(3.5)
                                    add(4.5)
                                }
                            )
                        }
                    )
                }
            )
            val route1WithWaypoints1FromRoute = createDirectionsRoute(
                distance = 1.0,
                waypoints = listOf(waypoint1)
            )
            val route1WithWaypoints2FromRoute = createDirectionsRoute(
                distance = 1.0,
                waypoints = listOf(waypoint2),
            )
            val route1WithWaypoints1FromResponse = createDirectionsRoute(
                distance = 1.0,
            )
            val route2WithWaypoints1FromRoute = createDirectionsRoute(
                distance = 2.0,
                waypoints = listOf(waypoint1),
            )
            return listOf(
                arrayOf(
                    "different ids",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#1",
                    null,
                    false
                ),
                arrayOf(
                    "different routes",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    createDirectionsResponse(routes = listOf(route2WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    false
                ),
                arrayOf(
                    "different waypoints from the same source",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints2FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    false
                ),
                arrayOf(
                    "different waypoints from different sources",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    createDirectionsResponse(
                        routes = listOf(route1WithWaypoints1FromResponse),
                        unrecognizedProperties = unrecognizedWaypoints2
                    ),
                    createRouteOptions(waypointsPerRoute = false),
                    "id#0",
                    null,
                    false
                ),
                arrayOf(
                    "same waypoints from the same source",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    true
                ),
                arrayOf(
                    "same waypoints from different source",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    null,
                    createDirectionsResponse(
                        routes = listOf(route1WithWaypoints1FromResponse),
                        unrecognizedProperties = unrecognizedWaypoints1
                    ),
                    createRouteOptions(waypointsPerRoute = false),
                    "id#0",
                    null,
                    false
                ),
                arrayOf(
                    "different expiration times",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    1L,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    "id#0",
                    2L,
                    true
                ),
            )
        }
    }

    private lateinit var route1: NavigationRoute
    private lateinit var route2: NavigationRoute

    @Before
    fun setUp() {
        route1 = NavigationRoute(
            directionsResponse1,
            0,
            routeOptions1,
            mockk(relaxUnitFun = true) {
                every { routeId } returns id1
                every { responseUuid } returns "uuid#0"
                every { routerOrigin } returns RouterOrigin.ONBOARD
                every { routeInfo } returns RouteInfo(listOf(mockk(relaxed = true)))
                every { waypoints } returns emptyList()
            },
            expirationTime = expirationTime1
        )
        route2 = NavigationRoute(
            directionsResponse2,
            0,
            routeOptions2,
            mockk(relaxUnitFun = true) {
                every { routeId } returns id2
                every { responseUuid } returns "uuid#1"
                every { routerOrigin } returns RouterOrigin.ONLINE
                every {
                    routeInfo
                } returns RouteInfo(listOf(mockk(relaxed = true), mockk(relaxed = true)))
                every { waypoints } returns emptyList()
            },
            expirationTime = expirationTime2
        )
    }

    @Test
    fun equalsTest() {
        assertEquals(expected, route1 == route2)
    }
}
