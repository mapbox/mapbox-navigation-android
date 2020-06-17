package com.mapbox.navigation.route.offboard.router

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxDirectionsUtilsTest {

    private val mapboxDirectionsBuilder: MapboxDirections.Builder = mockk(relaxed = true)

    @Test
    fun `should create mapbox directions from options`() {
        val routeOptions = RouteOptions.builder()
            .accessToken("test_access_token")
            .coordinates(
                listOf(
                    Point.fromLngLat(-121.470162, 38.563121),
                    Point.fromLngLat(-121.483304, 38.583313)
                )
            )
            .applyDefaultParams()
            .build()

        mapboxDirectionsBuilder.routeOptions(routeOptions)

        verify { mapboxDirectionsBuilder.baseUrl(routeOptions.baseUrl()) }
        verify { mapboxDirectionsBuilder.user(routeOptions.user()) }
        verify { mapboxDirectionsBuilder.profile(routeOptions.profile()) }
    }
}
