package com.mapbox.navigation.testing.utils.http

import android.content.Context
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import okhttp3.mockwebserver.MockResponse

fun mockIsochroneApiRequestHandler(
    context: Context,
    pointToResponseResource: Map<Point, Int>
) = MockRequestHandler {
    val tag = "mock-isochrone"
    if (it.path!!.contains("isochrone")) {
        Log.d(tag, "handling ${it.requestUrl}")
        val requestedLocations = it.requestUrl!!.pathSegments.last().split(",")
        val longitude = requestedLocations[0].toDoubleOrNull()
        val latitude = requestedLocations[1].toDoubleOrNull()
        if (longitude != null && latitude != null) {
            val requestedPoint = Point.fromLngLat(longitude, latitude)
            val response = pointToResponseResource.entries.firstOrNull {
                TurfMeasurement.distance(requestedPoint, it.key, TurfConstants.UNIT_METERS) < 1000
            }
            if (response != null) {
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        readRawFileText(
                            context,
                            response.value
                        )
                    )
            } else {
                Log.e(
                    tag,
                    "requested point $requestedPoint is too far from any provided: ${pointToResponseResource.keys}"
                )
                MockResponse().setResponseCode(500).setBody("no configured response")
            }
        } else {
            Log.e(
                tag,
                "can't parse location for isochrone request from $requestedLocations, ${it.requestUrl}"
            )
            MockResponse().setResponseCode(500).setBody("can't parse location")
        }
    } else null
}
