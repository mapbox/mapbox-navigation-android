package com.mapbox.navigation.base.internal

import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationRouteProviderTest {

    @Suppress("MaxLineLength")
    private val validUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
        "11.5850871,48.1767574;11.5203954,48.2175713" +
        "?geometries=polyline6&overview=full&access_token=pk.test"

    private val nativeRoute = mockk<RouteInterface>(relaxed = true) {
        every { responseJson } returns createDirectionsResponse().toJson()
        every { routeIndex } returns 0
        every { requestUri } returns validUrl
    }

    @Test
    fun `createSingleRoute should be null if json is invalid`() {
        every { nativeRoute.responseJson } returns "not a json"

        assertNull(NavigationRouteProvider.createSingleRoute(nativeRoute))
    }

    @Test
    fun `createSingleRoute should be null if url is invalid`() {
        every { nativeRoute.requestUri } returns "not a uri"

        assertNull(NavigationRouteProvider.createSingleRoute(nativeRoute))
    }

    @Test
    fun `createSingleRoute should be non-null if everything is valid`() {
        assertNotNull(NavigationRouteProvider.createSingleRoute(nativeRoute))
    }
}
