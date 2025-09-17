/* ktlint-disable */
package com.mapbox.navigation.testing.utils.routes

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.R
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import java.net.URL

object RoutesProvider {

    fun dc_very_short(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short)
        val coordinates = listOf(
            Point.fromLngLat(-77.031991, 38.894721),
            Point.fromLngLat(-77.030923, 38.895433)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    // primary route is valid
    // first alternative is valid
    // second alternative is invalid because of absence of intersections
    // third alternative is invalid because it doesn't have a fork point from primary route
    // fourth alternative is invalid because it doesn't have common points with primary route
    fun dc_short_with_invalid_alternatives(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_dc_short_with_invalid_alternatives)
        val coordinates = listOf(
            Point.fromLngLat(-77.03195769941682, 38.894396260868234),
            Point.fromLngLat(-77.02899192253159, 38.89624973628139)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_with_alternative(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_short_with_alternative)
        val coordinates = listOf(
            Point.fromLngLat(-77.033625, 38.891164),
            Point.fromLngLat(-77.03002, 38.895453)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_with_alternative_no_uuid(context: Context): MockRoute {
        val jsonResponse = readRawFileText(
            context,
            R.raw.route_response_dc_short_with_alternative_no_uuid
        )
        val coordinates = listOf(
            Point.fromLngLat(-77.033625, 38.891164),
            Point.fromLngLat(-77.03002, 38.895453)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_with_alternative_same_beginning(context: Context): MockRoute {
        val jsonResponse = readRawFileText(
            context,
            R.raw.route_response_dc_short_with_alternative_same_begining
        )
        val coordinates = listOf(
            Point.fromLngLat(-77.02821219854371, 38.887758247504166),
            Point.fromLngLat(-77.03002, 38.895453)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_two_legs_with_alternative(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_dc_very_short_two_legs_alternative)
        val coordinates = listOf(
            Point.fromLngLat(-77.036082, 38.887578),
            Point.fromLngLat(-77.033625, 38.891164),
            Point.fromLngLat(-77.03002, 38.895453)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_two_legs_with_alternative_for_second_leg(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_dc_short_with_alternative_for_second_leg)
        val coordinates = listOf(
            Point.fromLngLat(-77.031957, 38.894721),
            Point.fromLngLat(-77.029671, 38.895531),
            Point.fromLngLat(-77.021913, 38.899821)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_alternative_has_more_legs(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_dc_short_alternative_has_more_legs)
        val coordinates = listOf(
            Point.fromLngLat(-77.031957, 38.894721),
            Point.fromLngLat(-77.029671, 38.895531),
            Point.fromLngLat(-77.021913, 38.899821)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_short_alternative_with_fork_point(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_alternative_and_fork_point)

        val coordinates = listOf(
            Point.fromLngLat(-77.02949848947188, 38.90802940158288),
            Point.fromLngLat(-77.028611, 38.910507),
        )

        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates,
                ),
            ),
            coordinates,
        )
    }

    fun dc_short_with_alternative_reroute(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_dc_short_with_alternative_reroute)
        val coordinates = listOf(
            Point.fromLngLat(-77.036178, 38.892106),
            Point.fromLngLat(-77.03002, 38.895453)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates,
                )
            ),
            coordinates,
        )
    }

    fun dc_very_short_two_legs(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short_two_legs)
        val coordinates = listOf(
            Point.fromLngLat(-77.031991, 38.894721),
            Point.fromLngLat(-77.031991, 38.895433),
            Point.fromLngLat(-77.030923, 38.895433)
        )
        // TODO: add more data if you need it for your scenarios
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun dc_very_short_two_legs_with_silent_waypoint(context: Context): MockRoute {
        val jsonResponse =
            readRawFileText(context, R.raw.route_response_dc_very_short_silent_waypoints)
        val coordinates = listOf(
            Point.fromLngLat(-77.031991, 38.894721),
            Point.fromLngLat(-77.031991, 38.895433),
            Point.fromLngLat(-77.030923, 38.895433)
        )
        // TODO: add more data if you need it for your scenarios
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun near_munich_with_waypoints(context: Context): MockRoute {
        val jsonResponse = readRawFileText(
            context,
            R.raw.route_response_near_munich_with_waypoints
        )
        val coordinates = listOf(
            Point.fromLngLat(12.733982017085935, 48.30224175840664),
            Point.fromLngLat(12.690353, 48.254544),
            Point.fromLngLat(12.686236328301874, 48.251801613727025)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
        )
    }

    fun near_munich_with_waypoints_for_reroute(context: Context): DirectionsRoute {
        val jsonResponse = readRawFileText(
            context,
            R.raw.route_response_near_munich_with_waypoints_for_reroute
        )
        return DirectionsResponse.fromJson(jsonResponse).routes()[0]
    }

    fun two_routes_different_legs_count_the_same_incident(
        context: Context
    ): Triple<MockRoute, MockRoute, String> {
        val origin = "11.428011943347627,48.143406486859135"
        val destination = "11.443258702449555,48.14554279886465"
        val oneLegRouteOptions = RouteOptions.fromUrl(
            URL(
                "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                    "$origin;$destination" +
                    "?access_token=**&alternatives=true" +
                    "&annotations=closure,congestion_numeric,congestion,speed,duration,distance" +
                    "&geometries=polyline6&language=en&overview=full&steps=true"
            )
        )
        val oneLegRoute = readRawFileText(
            context,
            R.raw.route_through_incident_6058002857835914_one_leg
        )
        val oneLegMockRoute = MockRoute(
            oneLegRoute,
            DirectionsResponse.fromJson(oneLegRoute),
            listOf(
                MockDirectionsRequestHandler(
                    DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = oneLegRoute,
                    expectedCoordinates = oneLegRouteOptions.coordinatesList()
                )
            ),
            routeWaypoints = oneLegRouteOptions.coordinatesList()
        )

        val twoLegsRouteOptions = RouteOptions.fromUrl(
            URL(
                "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                    "$origin;11.42945687746061,48.1436160028498" +
                    ";$destination" +
                    "?access_token=**&alternatives=true" +
                    "&annotations=closure,congestion_numeric,congestion,speed,duration,distance" +
                    "&geometries=polyline6&language=en&overview=full&steps=true"
            )
        )
        val twoLegsRouteJson = readRawFileText(
            context,
            R.raw.route_through_incident_6058002857835914_two_legs
        )
        val twoLegsMockRoute = MockRoute(
            twoLegsRouteJson,
            DirectionsResponse.fromJson(twoLegsRouteJson),
            listOf(
                MockDirectionsRequestHandler(
                    DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = twoLegsRouteJson,
                    expectedCoordinates = twoLegsRouteOptions.coordinatesList()
                )
            ),
            twoLegsRouteOptions.coordinatesList()
        )
        val incident = oneLegMockRoute.routeResponse.routes().first()
            .legs()!!.first()
            .incidents()!!.first()
        return Triple(oneLegMockRoute, twoLegsMockRoute, incident.id())
    }

    fun multiple_routes(context: Context): MockRoute {
        val routeResponseJson = readRawFileText(context, R.raw.multiple_routes)
        val routeResponseModel = DirectionsResponse.fromJson(routeResponseJson)
        val waypoints = routeResponseModel.routes().first().routeOptions()!!.coordinatesList()
        return MockRoute(
            routeResponseJson,
            routeResponseModel,
            mockRequestHandlers = listOf(
                MockDirectionsRequestHandler(
                    DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = routeResponseJson,
                    expectedCoordinates = waypoints
                )
            ),
            waypoints
        )
    }

    fun route_alternative_with_closure(context: Context): MockRoute {
        val responseJson = readRawFileText(context, R.raw.route_response_route_refresh)
        val model = DirectionsResponse.fromJson(responseJson)
        val waypoints = model.waypoints()!!.map { it.location() }
        val handler = MockDirectionsRequestHandler(
                profile = "driving-traffic",
                jsonResponse = responseJson,
                expectedCoordinates = waypoints,
                relaxedExpectedCoordinates = true
            )
        return MockRoute(
            responseJson,
            model,
            listOf(handler),
            waypoints
        )
    }
}

suspend fun MapboxNavigation.requestMockRoutes(
    mockWebServerRule: MockWebServerRule,
    mockRoute: MockRoute
): List<NavigationRoute> {
    mockRoute.mockRequestHandlers.forEach {
        if (!mockWebServerRule.requestHandlers.contains(it)) {
            mockWebServerRule.requestHandlers.add(it)
        }
    }
    return requestRoutes(
        mockRoute.routeOptions(mockWebServerRule.baseUrl)
    ).getSuccessfulResultOrThrowException().routes
}
