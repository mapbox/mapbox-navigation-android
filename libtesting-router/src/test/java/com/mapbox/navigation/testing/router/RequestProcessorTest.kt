@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.testing.router

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.testing.assertIs
import com.mapbox.navigation.testing.factories.createClosure
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createMaxSpeed
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createRouteOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestProcessorTest {
    @Test
    fun `route request - successful response`() {
        val testUrl = createRouteOptions()
            .toUrl("***")
            .toString()
        val testResponse = createDirectionsResponse()

        val result = testProcessRequest(
            testUrl,
            router = alwaysResponse(testResponse),
        )
        assertTrue(result is RequestProcessingResult.GetRouteResponse)
        assertEquals(
            (result as RequestProcessingResult.GetRouteResponse).response,
            testResponse,
        )
    }

    @Test
    fun `route request - no routes found response`() {
        val testUrl = createRouteOptions()
            .toUrl("***")
            .toString()

        val result = testProcessRequest(
            testUrl,
            router = noRoutesFoundRouter(),
        )
        val failure = assertIs<RequestProcessingResult.Failure>(result)
        assertEquals(200, failure.code)
        val body = DirectionsResponse.fromJson(failure.body)
        assertNotNull(body)
    }

    @Test
    fun `unsupported url`() {
        val result = testProcessRequest(
            "https://www.mapbox.com/",
            router = noRoutesFoundRouter(),
        )
        assertIs<RequestProcessingResult.RequestNotSupported>(result)
    }

    @Test
    fun `invalid url`() {
        val result = testProcessRequest(
            "invalid",
            router = noRoutesFoundRouter(),
        )
        assertIs<RequestProcessingResult.RequestNotSupported>(result)
    }

    @Test
    fun `wrong leg index for route refresh`() {
        val result = testProcessRequest(
            directionsRefreshUrl(legIndex = 2),
            refresher = alwaysTestRoute(),
        )
        val failure = assertIs<RequestProcessingResult.Failure>(result)
        assertEquals(
            RequestProcessingResult.Failure.wrongInput().code,
            failure.code,
        )
    }

    @Test
    fun `refresh route without legs`() {
        val result = testProcessRequest(
            directionsRefreshUrl(),
            refresher = alwaysTestRoute(
                createDirectionsRoute(legs = null),
            ),
        )
        assertIs<RequestProcessingResult.Failure>(result)
    }

    @Test
    fun `refresh options, route index 0`() {
        val route = createDirectionsRoute()
        var requestedOptions: RefreshOptions? = null
        testProcessRequest(
            directionsRefreshUrl(
                responseUUID = "test-response-uuid-0",
                routeIndex = 0,
                legIndex = 0,
                currentRouteGeometryIndex = 0,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    requestedOptions = options
                    callback.onRefresh(route)
                }
            },
        )

        assertNotNull(requestedOptions)
        val routeRefreshOptions = requestedOptions!!
        assertEquals("test-response-uuid-0", routeRefreshOptions.responseUUID)
        assertEquals(0, routeRefreshOptions.routeIndex)
    }

    @Test
    fun `refresh options, route index 1`() {
        val route = createDirectionsRoute()
        var requestedOptions: RefreshOptions? = null
        testProcessRequest(
            directionsRefreshUrl(
                responseUUID = "test-response-uuid-1",
                routeIndex = 1,
                legIndex = 0,
                currentRouteGeometryIndex = 0,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    requestedOptions = options
                    callback.onRefresh(route)
                }
            },
        )

        assertNotNull(requestedOptions)
        val routeRefreshOptions = requestedOptions!!
        assertEquals("test-response-uuid-1", routeRefreshOptions.responseUUID)
        assertEquals(1, routeRefreshOptions.routeIndex)
    }

    @Test
    fun `refresh route, single leg, route geometry index 0`() {
        val route = createDirectionsRoute()
        var requestedOptions: RefreshOptions? = null
        val result = testProcessRequest(
            directionsRefreshUrl(
                responseUUID = "test-response-uuid",
                routeIndex = 0,
                legIndex = 0,
                currentRouteGeometryIndex = 0,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    requestedOptions = options
                    callback.onRefresh(route)
                }
            },
        )

        assertNotNull(requestedOptions)
        val routeRefreshOptions = requestedOptions!!
        assertEquals("test-response-uuid", routeRefreshOptions.responseUUID)
        assertEquals(0, routeRefreshOptions.routeIndex)

        val routeRefreshResponse = assertIs<RequestProcessingResult.RefreshRouteResponse>(result)
            .response
        assertEquals(
            route.legs()?.map { it.annotation() },
            routeRefreshResponse.route()?.legs()?.map { it.annotation() },
        )
    }

    @Test
    fun `refresh route, single leg, route geometry index 1`() {
        val route = createDirectionsRoute(
            legs = listOf(
                createRouteLeg(
                    annotation = createRouteLegAnnotation(
                        congestion = listOf("test", "test"),
                        congestionNumeric = listOf(1, 1),
                        distance = listOf(2.0, 2.0),
                        duration = listOf(3.0, 3.0),
                        maxSpeed = listOf(createMaxSpeed(4), createMaxSpeed(4)),
                        speed = listOf(5.0, 5.0),
                        stateOfCharge = listOf(6, 6),
                        freeFlowSpeed = listOf(9, 9),
                        currentSpeed = listOf(10, 10),
                    ),
                    incidents = listOf(
                        createIncident(
                            id = "0",
                            startGeometryIndex = 0,
                            endGeometryIndex = 0,
                        ),
                        createIncident(
                            id = "1",
                            startGeometryIndex = 1,
                            endGeometryIndex = 1,
                        ),
                    ),
                    closures = listOf(
                        createClosure(
                            geometryIndexStart = 0,
                            geometryIndexEnd = 0,
                        ),
                        createClosure(
                            geometryIndexStart = 1,
                            geometryIndexEnd = 1,
                        ),
                    ),
                ),
            ),
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 0,
                currentRouteGeometryIndex = 1,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        val routeRefreshResponse = assertIs<RequestProcessingResult.RefreshRouteResponse>(result)
            .response
        assertEquals(
            createRouteLegAnnotation(
                congestion = listOf("test"),
                congestionNumeric = listOf(1),
                distance = listOf(2.0),
                duration = listOf(3.0),
                maxSpeed = listOf(createMaxSpeed(4)),
                speed = listOf(5.0),
                stateOfCharge = listOf(6),
                freeFlowSpeed = listOf(9),
                currentSpeed = listOf(10),
            ),
            routeRefreshResponse.route()!!.legs()!!.first().annotation(),
        )
        assertEquals(
            listOf("1"),
            routeRefreshResponse.route()!!.legs()!!.first().incidents()!!.map { it.id() },
        )
        assertEquals(
            1,
            routeRefreshResponse.route()!!.legs()!!.first().closures()!!.size,
        )
    }

    @Test
    fun `refresh route, two leg, route index 2(beginning of the second leg)`() {
        val route = createDirectionsRoute(
            legs = createRouteLeg().let { listOf(it, it) },
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 1,
                currentRouteGeometryIndex = 2,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        val routeRefreshResponse = assertIs<RequestProcessingResult.RefreshRouteResponse>(result)
            .response
        assertEquals(
            route.legs()!![1].annotation(),
            routeRefreshResponse.route()!!.legs()!!.single().annotation(),
        )
    }

    @Test
    fun `refresh 2 legs route, missing annotations`() {
        val route = createDirectionsRoute(
            legs = createRouteLeg(
                annotation = createRouteLegAnnotation(
                    congestionNumeric = null,
                    maxSpeed = null,
                    speed = null,
                ),
            ).let { listOf(it, it) },
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 1,
                currentRouteGeometryIndex = 2,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        val routeRefreshResponse = assertIs<RequestProcessingResult.RefreshRouteResponse>(result)
            .response
        assertEquals(
            route.legs()!![1].annotation(),
            routeRefreshResponse.route()!!.legs()!!.single().annotation(),
        )
    }

    @Test
    fun `refresh 2 legs route, no annotations`() {
        val route = createDirectionsRoute(
            legs = createRouteLeg(
                annotation = null,
            ).let { listOf(it, it) },
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 1,
                currentRouteGeometryIndex = 2,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        assertIs<RequestProcessingResult.Failure>(result)
    }

    @Test
    fun `refresh 2 legs route, empty annotations`() {
        val route = createDirectionsRoute(
            legs = createRouteLeg(
                annotation = LegAnnotation.builder().build(),
            ).let { listOf(it, it) },
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 1,
                currentRouteGeometryIndex = 2,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        assertIs<RequestProcessingResult.Failure>(result)
    }

    @Test
    fun `refresh route, two leg, route index 1(end of the first leg)`() {
        val route = createDirectionsRoute(
            legs = listOf(
                createRouteLeg(
                    annotation = createRouteLegAnnotation(
                        congestion = listOf("test", "test"),
                        congestionNumeric = listOf(1, 1),
                        distance = listOf(2.0, 2.0),
                        duration = listOf(3.0, 3.0),
                        maxSpeed = listOf(createMaxSpeed(4), createMaxSpeed(4)),
                        speed = listOf(5.0, 5.0),
                        stateOfCharge = listOf(6, 6),
                        freeFlowSpeed = listOf(9, 9),
                        currentSpeed = listOf(10, 10),
                    ),
                    incidents = listOf(
                        createIncident(
                            id = "0",
                            startGeometryIndex = 0,
                            endGeometryIndex = 0,
                        ),
                        createIncident(
                            id = "1",
                            startGeometryIndex = 1,
                            endGeometryIndex = 1,
                        ),
                    ),
                    closures = listOf(
                        createClosure(
                            geometryIndexStart = 0,
                            geometryIndexEnd = 0,
                        ),
                        createClosure(
                            geometryIndexStart = 1,
                            geometryIndexEnd = 1,
                        ),
                    ),
                ),
                createRouteLeg(
                    annotation = createRouteLegAnnotation(
                        congestion = listOf("test2", "test2"),
                        congestionNumeric = listOf(11, 11),
                        distance = listOf(22.0, 22.0),
                        duration = listOf(33.0, 33.0),
                        maxSpeed = listOf(createMaxSpeed(44), createMaxSpeed(44)),
                        speed = listOf(55.0, 55.0),
                        stateOfCharge = listOf(66, 66),
                        freeFlowSpeed = listOf(9, 9),
                        currentSpeed = listOf(10, 10),
                    ),
                    incidents = listOf(
                        createIncident(
                            id = "2",
                            startGeometryIndex = 0,
                            endGeometryIndex = 0,
                        ),
                        createIncident(
                            id = "3",
                            startGeometryIndex = 1,
                            endGeometryIndex = 1,
                        ),
                    ),
                    closures = listOf(
                        createClosure(
                            geometryIndexStart = 0,
                            geometryIndexEnd = 0,
                        ),
                        createClosure(
                            geometryIndexStart = 1,
                            geometryIndexEnd = 1,
                        ),
                    ),
                ),
            ),
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 0,
                currentRouteGeometryIndex = 1,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        val routeRefreshResponse = assertIs<RequestProcessingResult.RefreshRouteResponse>(result)
            .response
        assertEquals(
            createRouteLegAnnotation(
                congestion = listOf("test"),
                congestionNumeric = listOf(1),
                distance = listOf(2.0),
                duration = listOf(3.0),
                maxSpeed = listOf(createMaxSpeed(4)),
                speed = listOf(5.0),
                stateOfCharge = listOf(6),
                freeFlowSpeed = listOf(9),
                currentSpeed = listOf(10),
            ),
            routeRefreshResponse.route()!!.legs()!!.first().annotation(),
        )
        assertEquals(
            route.legs()!![1].annotation(),
            routeRefreshResponse.route()!!.legs()!![1].annotation(),
        )
        assertEquals(
            listOf("1"),
            routeRefreshResponse.route()!!.legs()!!.first().incidents()!!.map { it.id() },
        )
        assertEquals(
            listOf("2", "3"),
            routeRefreshResponse.route()!!.legs()!![1].incidents()!!.map { it.id() },
        )
        assertEquals(
            1,
            routeRefreshResponse.route()!!.legs()!!.first().closures()!!.size,
        )
        assertEquals(
            2,
            routeRefreshResponse.route()!!.legs()!![1].closures()!!.size,
        )
    }

    @Test
    fun `refresh route, two leg, route index 3(end of the second leg)`() {
        val route = createDirectionsRoute(
            legs = listOf(
                createRouteLeg(
                    annotation = createRouteLegAnnotation(
                        congestion = listOf("test", "test"),
                        congestionNumeric = listOf(1, 1),
                        distance = listOf(2.0, 2.0),
                        duration = listOf(3.0, 3.0),
                        maxSpeed = listOf(createMaxSpeed(4), createMaxSpeed(4)),
                        speed = listOf(5.0, 5.0),
                        stateOfCharge = listOf(6, 6),
                        freeFlowSpeed = listOf(9, 9),
                        currentSpeed = listOf(10, 10),
                    ),
                    incidents = listOf(
                        createIncident(
                            id = "0",
                            startGeometryIndex = 0,
                            endGeometryIndex = 0,
                        ),
                        createIncident(
                            id = "1",
                            startGeometryIndex = 1,
                            endGeometryIndex = 1,
                        ),
                    ),
                    closures = listOf(
                        createClosure(
                            geometryIndexStart = 0,
                            geometryIndexEnd = 0,
                        ),
                        createClosure(
                            geometryIndexStart = 1,
                            geometryIndexEnd = 1,
                        ),
                    ),
                ),
                createRouteLeg(
                    annotation = createRouteLegAnnotation(
                        congestion = listOf("test2", "test2"),
                        congestionNumeric = listOf(11, 11),
                        distance = listOf(22.0, 22.0),
                        duration = listOf(33.0, 33.0),
                        maxSpeed = listOf(createMaxSpeed(44), createMaxSpeed(44)),
                        speed = listOf(55.0, 55.0),
                        stateOfCharge = listOf(66, 66),
                        freeFlowSpeed = listOf(9, 9),
                        currentSpeed = listOf(10, 10),
                    ),
                    incidents = listOf(
                        createIncident(
                            id = "2",
                            startGeometryIndex = 0,
                            endGeometryIndex = 0,
                        ),
                        createIncident(
                            id = "3",
                            startGeometryIndex = 1,
                            endGeometryIndex = 1,
                        ),
                    ),
                    closures = listOf(
                        createClosure(
                            geometryIndexStart = 0,
                            geometryIndexEnd = 0,
                        ),
                        createClosure(
                            geometryIndexStart = 1,
                            geometryIndexEnd = 1,
                        ),
                    ),
                ),
            ),
        )
        val result = testProcessRequest(
            directionsRefreshUrl(
                routeIndex = 0,
                legIndex = 1,
                currentRouteGeometryIndex = 3,
            ),
            refresher = object : MapboxNavigationTestRouteRefresher {
                override fun getRouteRefresh(
                    options: RefreshOptions,
                    callback: RouteRefreshCallback,
                ) {
                    callback.onRefresh(route)
                }
            },
        )

        val routeRefreshResponse = assertIs<RequestProcessingResult.RefreshRouteResponse>(result)
            .response
        assertEquals(
            createRouteLegAnnotation(
                congestion = listOf("test2"),
                congestionNumeric = listOf(11),
                distance = listOf(22.0),
                duration = listOf(33.0),
                maxSpeed = listOf(createMaxSpeed(44)),
                speed = listOf(55.0),
                stateOfCharge = listOf(66),
                freeFlowSpeed = listOf(9),
                currentSpeed = listOf(10),
            ),
            routeRefreshResponse.route()!!.legs()!!.single().annotation(),
        )

        assertEquals(
            listOf("3"),
            routeRefreshResponse.route()!!.legs()!!.single().incidents()!!.map { it.id() },
        )
        assertEquals(
            1,
            routeRefreshResponse.route()!!.legs()!!.single().closures()!!.size,
        )
    }
}

private fun testProcessRequest(
    url: String,
    router: MapboxNavigationTestRouter = noRoutesFoundRouter(),
    refresher: MapboxNavigationTestRouteRefresher = alwaysErrorRefresher(),
) = runBlocking { processRequest(router, refresher, url) }

private fun noRoutesFoundRouter() = object : MapboxNavigationTestRouter {
    override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
        callback.onFailure(TestRouterFailure.noRoutesFound())
    }
}

private fun alwaysErrorRefresher() = DefaultRefresher()

private fun alwaysResponse(response: DirectionsResponse) = object : MapboxNavigationTestRouter {
    override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
        callback.onRoutesReady(response)
    }
}

private fun alwaysTestRoute(
    route: DirectionsRoute = createDirectionsRoute(),
) = object : MapboxNavigationTestRouteRefresher {
    override fun getRouteRefresh(options: RefreshOptions, callback: RouteRefreshCallback) {
        callback.onRefresh(route)
    }
}

private fun directionsRefreshUrl(
    responseUUID: String = "test-response-uuid",
    routeIndex: Int = 0,
    legIndex: Int = 0,
    currentRouteGeometryIndex: Int = 0,
): String = (
    "https://api.mapbox.com/directions-refresh/v1/mapbox/" +
        "driving-traffic/$responseUUID/$routeIndex/$legIndex" +
        "?access_token=test-token&current_route_geometry_index=$currentRouteGeometryIndex"
    )
