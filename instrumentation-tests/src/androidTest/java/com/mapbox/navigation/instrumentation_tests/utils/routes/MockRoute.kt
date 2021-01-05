package com.mapbox.navigation.instrumentation_tests.utils.routes

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.http.MockWebServerRule

/**
 * Contains data needed to fully mock the replay of a route.
 *
 * @param routeResponseJson fully JSON Directions API response
 * @param routeResponse deserialized response
 * @param mockRequestHandlers all handlers that needed to be fed to [MockWebServerRule]
 * @param routeWaypoints all waypoints, including the starting point
 * @param bannerInstructions all banner instructions on the route
 */
data class MockRoute(
    val routeResponseJson: String,
    val routeResponse: DirectionsResponse,
    val mockRequestHandlers: List<MockRequestHandler>,
    val routeWaypoints: List<Point>,
    val bannerInstructions: List<BannerInstructions>
)
