package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Mocks a directions refresh.
 *
 * @param testUuid override the `uuid` field in the request that is being refreshed
 * @param jsonResponse the full JSON response
 */
data class MockDirectionsRefreshHandler(
    val testUuid: String,
    val jsonResponse: String
) : MockRequestHandler {
    override fun handle(request: RecordedRequest): MockResponse? {
        val prefix = """/directions-refresh/v1/mapbox/driving-traffic/$testUuid"""
        return if (request.path!!.startsWith(prefix)) {
            MockResponse().setBody(jsonResponse)
        } else {
            null
        }
    }
}
