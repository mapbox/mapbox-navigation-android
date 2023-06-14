/* ktlint-disable */
package com.mapbox.navigation.instrumentation_tests.utils.routes

import android.content.Context
import androidx.annotation.IntegerRes
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.bufferFromRawFile
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockVoiceRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText

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
                ),
                MockVoiceRequestHandler(
                    bufferFromRawFile(context, R.raw.route_response_dc_very_short_voice_1),
                    readRawFileText(context, R.raw.route_response_dc_very_announcement_1)
                ),
                MockVoiceRequestHandler(
                    bufferFromRawFile(context, R.raw.route_response_dc_very_short_voice_2),
                    readRawFileText(context, R.raw.route_response_dc_very_announcement_2)
                )
            ),
            coordinates,
            listOf(
                BannerInstructions.fromJson(readRawFileText(context, R.raw.route_response_dc_very_short_banner_instructions_1)),
                BannerInstructions.fromJson(readRawFileText(context, R.raw.route_response_dc_very_short_banner_instructions_2)),
                BannerInstructions.fromJson(readRawFileText(context, R.raw.route_response_dc_very_short_banner_instructions_3))
            )
        )
    }

    // valid primary route, first invalid alternative, second valid alternative
    fun dc_very_short_with_invalid_alternatives(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short_with_invalid_alternatives)
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
            emptyList()
        )
    }

    fun dc_very_short_two_legs(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short_two_legs)
        val coordinates = listOf(
            Point.fromLngLat(-77.031991, 38.894721),
            Point.fromLngLat(-77.031991,38.895433),
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
            emptyList()
        )
    }

    fun dc_very_short_two_legs_with_silent_waypoint(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short_silent_waypoints)
        val coordinates = listOf(
            Point.fromLngLat(-77.031991, 38.894721),
            Point.fromLngLat(-77.031991,38.895433),
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
            emptyList()
        )
    }

    fun MockRoute.toNavigationRoutes(
        routeOrigin: RouterOrigin = RouterOrigin.Custom(),
        routeOptionsBlock: RouteOptions.Builder.() -> RouteOptions.Builder = { this },
    ) : List<NavigationRoute> {
        return NavigationRoute.create(
            this.routeResponse,
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinatesList(this.routeWaypoints)
                .routeOptionsBlock()
                .build(),
            routeOrigin
        )
    }

    fun loadDirectionsResponse(context: Context, @IntegerRes routeFileResource: Int): DirectionsResponse {
        val jsonResponse = readRawFileText(context, routeFileResource)
        return DirectionsResponse.fromJson(jsonResponse)
    }
}
