package com.mapbox.navigation.testing.ui.http

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

abstract class BaseMockRequestHandler : MockRequestHandler {

    private val _handledRequests = mutableListOf<RecordedRequest>()
    val handledRequests: List<RecordedRequest> = _handledRequests

    override fun handle(request: RecordedRequest): MockResponse? {
        return handleInternal(request).also {
            if (it != null) {
                _handledRequests.add(request)
            }
        }
    }

    abstract fun handleInternal(request: RecordedRequest): MockResponse?
}
