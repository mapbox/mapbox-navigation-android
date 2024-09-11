package com.mapbox.navigation.testing.utils.http

import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.navigation.testing.ui.http.BaseMockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

data class RequestRequestParams(
    val routeUUID: String,
    val routeIndex: Int,
    val legIndex: Int,
    val geometryIndex: Int
)

class MockDynamicDirectionsRefreshHandler(
    val directionsResponseGenerator: (RequestRequestParams) -> DirectionsRouteRefresh
) : BaseMockRequestHandler() {

    override fun handleInternal(request: RecordedRequest): MockResponse? {
        val expectedPrefix = "/directions-refresh/v1/mapbox/driving-traffic/"
        if (request.path!!.startsWith(expectedPrefix)) {
            val segments = request.requestUrl!!.encodedPathSegments
            if (segments.size < 7) return null
            val routeUUID = segments[4]
            val routeIndex = segments[5].toIntOrNull() ?: return null
            val legIndex = segments[6].toIntOrNull() ?: return null
            val currentGeometryIndex = request.requestUrl!!
                .queryParameter("current_route_geometry_index")?.toIntOrNull()
                ?: return null
            val routeRefresh = directionsResponseGenerator(
                RequestRequestParams(
                    legIndex = legIndex,
                    geometryIndex = currentGeometryIndex,
                    routeUUID = routeUUID,
                    routeIndex = routeIndex
                )
            )
            val response = DirectionsRefreshResponse.builder()
                .code("Ok")
                .route(routeRefresh)
                .build()
                .toJson()
            return MockResponse().setBody(response)
        }
        return null
    }
}
