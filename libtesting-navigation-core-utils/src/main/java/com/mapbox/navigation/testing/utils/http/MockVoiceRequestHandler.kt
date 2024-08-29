package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer

/**
 * Mocks a voice response.
 *
 * @param buffer byte buffer of the read mp3 file
 * @param requestDetails an initial part of the requested resource that goes after "/speak/",
 * ex. "%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody%20rate=%221.08%22%3EDrive%20north"
 */
data class MockVoiceRequestHandler(
    private val buffer: Buffer,
    private val requestDetails: String
) : MockRequestHandler {

    override fun handle(request: RecordedRequest): MockResponse? {
        val prefix =
            """/voice/v1/speak/$requestDetails"""
        return if (request.path!!.startsWith(prefix)) {
            MockResponse().apply { setBody(buffer) }
        } else {
            null
        }
    }
}
