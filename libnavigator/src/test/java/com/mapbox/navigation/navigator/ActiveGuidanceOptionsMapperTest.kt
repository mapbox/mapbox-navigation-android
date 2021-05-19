package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.navigator.internal.ActiveGuidanceOptionsMapper
import com.mapbox.navigator.ActiveGuidanceGeometryEncoding
import com.mapbox.navigator.ActiveGuidanceMode
import com.mapbox.navigator.ActiveGuidanceOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveGuidanceOptionsMapperTest {

    @Test
    fun checksNullRouteOptions() {
        val routeOptions: RouteOptions? = null
        val directionsRoute: DirectionsRoute = mockk()
        every { directionsRoute.routeOptions() } returns routeOptions

        val drivingPolyline6 = ActiveGuidanceOptionsMapper.mapFrom(directionsRoute)

        assertEquals(
            ActiveGuidanceOptions(
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf()
            ),
            drivingPolyline6
        )
    }

    @Test
    fun checksNullDirectionsRoute() {
        val directionsRoute = null

        val drivingPolyline6 = ActiveGuidanceOptionsMapper.mapFrom(directionsRoute)

        assertEquals(
            ActiveGuidanceOptions(
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf()
            ),
            drivingPolyline6
        )
    }
}
