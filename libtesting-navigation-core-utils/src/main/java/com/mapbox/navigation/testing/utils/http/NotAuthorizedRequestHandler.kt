package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class NotAuthorizedRequestHandler(
    private val wrongToken: String
) : MockRequestHandler {
    override fun handle(request: RecordedRequest): MockResponse? {
        val isWrongToken = request.requestUrl!!
            .queryParameter("access_token") == wrongToken
        return if (isWrongToken) {
            MockResponse()
                .setBody("{\"message\":\"Not Authorized - Invalid Token\"}")
                .setResponseCode(401)
        } else {
            null
        }
    }
}
