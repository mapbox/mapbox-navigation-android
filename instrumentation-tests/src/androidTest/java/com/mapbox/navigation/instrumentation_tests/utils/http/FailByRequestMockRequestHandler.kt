package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class FailByRequestMockRequestHandler(
    private val wrapped: MockRequestHandler
) : MockRequestHandler {

    var failResponse: Boolean = false

    override fun handle(request: RecordedRequest): MockResponse? {
        val result = wrapped.handle(request)
        return if (result != null) {
            if (failResponse) {
                MockResponse().setResponseCode(500).setBody("")
            } else {
                result
            }
        } else null
    }
}
