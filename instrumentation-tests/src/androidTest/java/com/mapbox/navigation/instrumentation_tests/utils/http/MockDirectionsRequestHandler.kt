package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.ui.http.BaseMockRequestHandler
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Mocks a directions response.
 *
 * @param profile the request profile, ex. "driving"
 * @param jsonResponse the full JSON response
 * @param expectedCoordinates optionally the expected coordinates
 * that the handler should match when providing the response
 */
data class MockDirectionsRequestHandler constructor(
    val profile: String,
    val lazyJsonResponse: () -> String,
    val expectedCoordinates: List<Point>?,
    val relaxedExpectedCoordinates: Boolean = false,
    val coordinatesAccuracyInMeters: Double = 30.0,
) : BaseMockRequestHandler() {

    constructor(
        profile: String,
        jsonResponse: String,
        expectedCoordinates: List<Point>?,
        relaxedExpectedCoordinates: Boolean = false,
        coordinatesAccuracyInMeters: Double = 30.0,
    ) : this(
        profile,
        { jsonResponse },
        expectedCoordinates,
        relaxedExpectedCoordinates,
        coordinatesAccuracyInMeters,
    )

    var jsonResponseModifier: ((String) -> String) = { it }

    override fun handleInternal(request: RecordedRequest): MockResponse? {
        val routeOptions = try {
            val result = RouteOptions.fromUrl(request.requestUrl!!.toUrl())
            result.coordinatesList() // checks that coordinates are parsable
            result
        } catch (t: Throwable) {
            null
        }
        if (routeOptions != null) {
            if (relaxedExpectedCoordinates) {
                return createMockResponse()
            }
            require(expectedCoordinates != null) {
                "specify expected coordinates if they are not relaxed"
            }
            val coordinatesMatchExpectedAccordingToAccuracy = coordinatesWithinRadius(
                expectedCoordinates,
                routeOptions.coordinatesList(),
                coordinatesAccuracyInMeters
            )
            if (coordinatesMatchExpectedAccordingToAccuracy) {
                return createMockResponse()
            }
        }
        return null
    }

    private fun createMockResponse() =
        MockResponse().setBody(jsonResponseModifier(lazyJsonResponse()))

    private fun coordinatesWithinRadius(
        expected: List<Point>,
        actual: List<Point>,
        coordinatesAccuracyInMeters: Double
    ): Boolean {
        if (expected.size != actual.size) return false
        return expected.zip(actual).all { (a, b) ->
            TurfMeasurement.distance(
                a,
                b,
                TurfConstants.UNIT_METERS
            ) < coordinatesAccuracyInMeters
        }
    }

    override fun toString(): String {
        return "MockDirectionsRequestHandler(" +
            "profile='$profile', " +
            "expectedCoordinates=$expectedCoordinates, " +
            "relaxedExpectedCoordinates=$relaxedExpectedCoordinates, " +
            "lazyJsonResponse='$lazyJsonResponse', " +
            "coordinatesAccuracyInMeters='$coordinatesAccuracyInMeters'" +
            ")"
    }
}
