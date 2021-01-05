package com.mapbox.navigation.testing.ui.http

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Handles a [MockWebServerRule]'s [RecordedRequest].
 */
interface MockRequestHandler {

    /**
     * Invoked whenever a test makes a request with [MockWebServerRule.baseUrl].
     */
    fun handle(request: RecordedRequest): MockResponse?
}
