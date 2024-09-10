package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.net.URLEncoder

class MockMapMatchingRequestHandler(
    coordinates: String,
    private val jsonResponse: () -> String
) : MockRequestHandler {

    private val encodedCoordinates = URLEncoder.encode(coordinates, "utf-8")

    override fun handle(request: RecordedRequest): MockResponse? {
        val pathSegments = request.requestUrl?.pathSegments ?: return null
        return if (
            pathSegments.getOrNull(0) == "matching" &&
            pathSegments.getOrNull(1) == "v5" &&
            request.path?.contains(encodedCoordinates) == true
        ) {
            MockResponse()
                .setBody(jsonResponse())
                .setResponseCode(200)
        } else {
            null
        }
    }
}
