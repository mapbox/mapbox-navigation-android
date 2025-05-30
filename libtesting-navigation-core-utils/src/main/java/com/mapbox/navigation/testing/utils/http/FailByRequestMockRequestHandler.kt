package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class FailByRequestMockRequestHandler(
    private val wrapped: MockRequestHandler
) : MockRequestHandler {

    var failResponse: Boolean = false
    var failResponseCode: Int = 500
    var errorBody: String = ""

    override fun handle(request: RecordedRequest): MockResponse? {
        val result = wrapped.handle(request)
        return if (result != null) {
            if (failResponse) {
                MockResponse().setResponseCode(failResponseCode).setBody(errorBody)
            } else {
                result
            }
        } else {
            null
        }
    }
}
