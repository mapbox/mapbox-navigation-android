package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Mocks NN available tiles versions request.
 *
 * @param jsonResponse the full JSON response
 */
class MockAvailableTilesVersionsRequestHandler(
    private val jsonResponse: String
) : MockRequestHandler {
    override fun handle(request: RecordedRequest): MockResponse? {
        val prefix = """/route-tiles/v2/mapbox/driving-traffic/versions"""
        return if (request.path!!.startsWith(prefix)) {
            MockResponse().setBody(jsonResponse)
        } else {
            null
        }
    }
}
