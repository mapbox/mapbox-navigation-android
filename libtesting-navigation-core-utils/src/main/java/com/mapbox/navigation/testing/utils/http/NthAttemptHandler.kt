package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class NthAttemptHandler(
    private val originalHandler: MockRequestHandler,
    private val successfulAttemptNumber: Int,
) : MockRequestHandler {

    private var attemptsCount = 0

    override fun handle(request: RecordedRequest): MockResponse? {
        val response = originalHandler.handle(request)
        val result = if (attemptsCount < successfulAttemptNumber) {
            null
        } else {
            response
        }
        if (response != null) attemptsCount++
        return result
    }
}
