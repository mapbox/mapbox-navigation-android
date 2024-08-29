package com.mapbox.navigation.testing.utils.http

import com.mapbox.navigation.testing.ui.http.BaseMockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

/**
 * Mocks a directions refresh.
 *
 * @param testUuid override the `uuid` field in the request that is being refreshed
 * @param jsonResponse the full JSON response
 */
data class MockDirectionsRefreshHandler(
    val testUuid: String,
    val jsonResponse: String,
    val acceptedGeometryIndex: Int? = null,
    val routeIndex: Int? = null,
) : BaseMockRequestHandler() {

    var jsonResponseModifier: ((String) -> String) = { it }

    override fun handleInternal(request: RecordedRequest): MockResponse? {
        val prefix = """/directions-refresh/v1/mapbox/driving-traffic/$testUuid""" +
            if (routeIndex != null) { "/$routeIndex/" } else ""
        if (request.path!!.startsWith(prefix)) {
            val currentGeometryIndex = request.requestUrl
                ?.queryParameter("current_route_geometry_index")
                ?.toInt()
            if (acceptedGeometryIndex == null || acceptedGeometryIndex == currentGeometryIndex) {
                return MockResponse().setBody(jsonResponseModifier(jsonResponse))
            }
        }
        return null
    }
}
