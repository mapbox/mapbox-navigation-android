package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxDropInRouteRequesterTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `setRoutes emits to flow`() = coroutineRule.runBlockingTest {
        val mockRoute = mockk<DirectionsRoute>()
        val def = async {
            MapboxDropInRouteRequester.setRouteRequests.first()
        }
        MapboxDropInRouteRequester.setRoutes(listOf(mockRoute))

        val result = def.await()

        assertEquals(mockRoute, result.first())
    }

    @Test
    fun `fetchAndSetRoute with Points emits to flow`() = coroutineRule.runBlockingTest {
        val points = listOf(
            Point.fromLngLat(33.0, 44.0),
            Point.fromLngLat(33.1, 44.1)
        )
        val def = async {
            MapboxDropInRouteRequester.routeRequests.first()
        }
        MapboxDropInRouteRequester.fetchAndSetRoute(points)

        val result = def.await()

        assertEquals(points.first(), result.first())
        assertEquals(points[1], result[1])
    }

    @Test
    fun `fetchAndSetRoute with RouteOptions emits to flow`() = coroutineRule.runBlockingTest {
        val expectedOptions = mockk<RouteOptions>()
        val def = async {
            MapboxDropInRouteRequester.routeOptionsRequests.first()
        }
        MapboxDropInRouteRequester.fetchAndSetRoute(expectedOptions)

        val result = def.await()

        assertEquals(expectedOptions, result)
    }
}
