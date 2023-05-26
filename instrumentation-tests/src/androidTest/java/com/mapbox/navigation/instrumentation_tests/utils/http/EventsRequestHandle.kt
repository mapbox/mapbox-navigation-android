package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class EventsRequestHandle : MockRequestHandler {
    override fun handle(request: RecordedRequest): MockResponse? {
        if (request.path!!.startsWith("/events/v2")) {
            return MockResponse().setResponseCode(204)
        }
        return null
    }
}
