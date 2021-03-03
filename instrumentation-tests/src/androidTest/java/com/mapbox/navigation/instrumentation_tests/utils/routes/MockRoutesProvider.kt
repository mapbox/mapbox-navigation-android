/* ktlint-disable */
package com.mapbox.navigation.instrumentation_tests.utils.routes

import android.content.Context
import androidx.annotation.IntegerRes
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.bufferFromRawFile
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockVoiceRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText

object MockRoutesProvider {

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
                    profile = "driving",
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

    fun loadDirectionsResponse(context: Context, @IntegerRes routeFileResource: Int): DirectionsResponse {
        val jsonResponse = readRawFileText(context, routeFileResource)
        return DirectionsResponse.fromJson(jsonResponse)
    }
}
