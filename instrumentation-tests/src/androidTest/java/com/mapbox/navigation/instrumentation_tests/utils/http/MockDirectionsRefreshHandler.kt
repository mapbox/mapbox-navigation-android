package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.navigation.instrumentation_tests.ExperimentalData
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
        val pathSegments = request.path!!.split("/")
        if (
            pathSegments.getOrNull(1) == "directions-refresh"
            && (pathSegments.getOrNull(2) == "v1")
            && (pathSegments.getOrNull(3) == "mapbox")
            && (pathSegments.getOrNull(4)?.endsWith("-EXPERIMENTAL") == true)
            && (pathSegments.getOrNull(5) == testUuid)
            && (routeIndex == null || pathSegments.getOrNull(6) == routeIndex.toString())
        ) {
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
