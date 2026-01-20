package com.mapbox.navigation.base.route

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createTestNavigationRoutesParsing
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavigationRouteEqualsTest(
    private val description: String,
    private val directionsResponse1: DirectionsResponse,
    private val routeOptions1: RouteOptions,
    private val expirationTime1: Long?,
    private val directionsResponse2: DirectionsResponse,
    private val routeOptions2: RouteOptions,
    private val expirationTime2: Long?,
    private val expected: Boolean,
) {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

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
                                },
                            )
                        },
                    )
                },
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
                                },
                            )
                        },
                    )
                },
            )
            val route1WithWaypoints1FromRoute = createDirectionsRoute(
                distance = 1.0,
                waypoints = listOf(waypoint1),
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
                    createDirectionsResponse(
                        routes = listOf(route1WithWaypoints1FromRoute),
                        uuid = "test1",
                    ),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    createDirectionsResponse(
                        routes = listOf(route1WithWaypoints1FromRoute),
                        uuid = "test2",
                    ),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    false,
                ),
                arrayOf(
                    "different routes",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    createDirectionsResponse(routes = listOf(route2WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    false,
                ),
                arrayOf(
                    "different waypoints from the same source",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints2FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    false,
                ),
                arrayOf(
                    "different waypoints from different sources",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    createDirectionsResponse(
                        routes = listOf(route1WithWaypoints1FromResponse),
                        unrecognizedProperties = unrecognizedWaypoints2,
                    ),
                    createRouteOptions(waypointsPerRoute = false),
                    null,
                    false,
                ),
                arrayOf(
                    "same waypoints from the same source",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    true,
                ),
                arrayOf(
                    "same waypoints from different source",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    null,
                    createDirectionsResponse(
                        routes = listOf(route1WithWaypoints1FromResponse),
                        unrecognizedProperties = unrecognizedWaypoints1,
                    ),
                    createRouteOptions(waypointsPerRoute = false),
                    null,
                    false,
                ),
                arrayOf(
                    "different expiration times",
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    1L,
                    createDirectionsResponse(routes = listOf(route1WithWaypoints1FromRoute)),
                    createRouteOptions(waypointsPerRoute = true),
                    2L,
                    true,
                ),
            )
        }
    }

    private lateinit var route1: NavigationRoute
    private lateinit var route2: NavigationRoute

    @Before
    fun setUp() {
        val parser = createTestNavigationRoutesParsing()
        route1 = runBlocking {
            parser.parseDirectionsResponse(
                DirectionsResponseToParse.from(
                    responseBody = directionsResponse1.toJson().toDataRefJava(),
                    routeRequest = routeOptions1.toUrl("***").toString(),
                    routerOrigin = RouterOrigin.ONLINE,
                ),
            ).getOrThrow().routes.first()
        }
        route2 = runBlocking {
            parser.parseDirectionsResponse(
                DirectionsResponseToParse.from(
                    responseBody = directionsResponse2.toJson().toDataRefJava(),
                    routeRequest = routeOptions2.toUrl("***").toString(),
                    routerOrigin = RouterOrigin.ONLINE,
                ),
            ).getOrThrow().routes.first()
        }
    }

    @Test
    fun equalsTest() {
        assertEquals(expected, route1 == route2)
    }
}
