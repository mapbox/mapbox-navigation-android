package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Handles all ART request with code 500.
 */
class MockRoutingTileEndpointErrorRequestHandler : MockRequestHandler {
    override fun handle(request: RecordedRequest): MockResponse? {
        val prefixVersion = """/route-tiles/v2/"""
        return if (request.path!!.startsWith(prefixVersion)) {
            MockResponse().setResponseCode(500).setBody("""{}""")
        } else {
            null
        }
    }
}
