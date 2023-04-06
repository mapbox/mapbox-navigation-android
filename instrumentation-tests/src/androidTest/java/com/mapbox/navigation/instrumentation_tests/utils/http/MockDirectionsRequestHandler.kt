package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.ui.http.BaseMockRequestHandler
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
) : BaseMockRequestHandler() {

    constructor(
        profile: String,
        jsonResponse: String,
        expectedCoordinates: List<Point>?,
        relaxedExpectedCoordinates: Boolean = false,
    ) : this(profile, { jsonResponse }, expectedCoordinates, relaxedExpectedCoordinates)

    var jsonResponseModifier: ((String) -> String) = { it }

    override fun handleInternal(request: RecordedRequest): MockResponse? {
        val prefix = if (relaxedExpectedCoordinates) {
            """/directions/v5/mapbox/$profile"""
        } else {
            """/directions/v5/mapbox/$profile/${expectedCoordinates.parseCoordinates()}"""
        }

        return if (request.path!!.startsWith(prefix)) {
            MockResponse().setBody(jsonResponseModifier(lazyJsonResponse()))
        } else {
            null
        }
    }

    private fun List<Point>?.parseCoordinates(): String {
        if (this == null) {
            return ""
        }

        return this.joinToString(";") { "${it.longitude()},${it.latitude()}" }
    }

    override fun toString(): String {
        return "MockDirectionsRequestHandler(" +
            "profile='$profile', " +
            "expectedCoordinates=$expectedCoordinates, " +
            "relaxedExpectedCoordinates=$relaxedExpectedCoordinates, " +
            "lazyJsonResponse='$lazyJsonResponse'" +
            ")"
    }
}
