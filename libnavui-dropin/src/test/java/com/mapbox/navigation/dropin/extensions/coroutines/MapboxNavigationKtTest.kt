package com.mapbox.navigation.dropin.extensions.coroutines

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxNavigationKtTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    lateinit var mockNavigation: MapboxNavigation

    lateinit var stubRouteOptions: RouteOptions

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.dropin.extensions.coroutines.MapboxNavigationKt")
        MockKAnnotations.init(this)
        stubRouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(10.0, 11.0),
                    Point.fromLngLat(20.0, 20.0)
                )
            )
            .alternatives(true)
            .build()
    }

    @Test
    fun `requestRoutes should call main requestRoutes method`() =
        coroutineRule.runBlockingTest {
            val optionsCapture = slot<RouteOptions>()
            givenRequestRoutesResponse(
                requestOptions = optionsCapture,
                responseRoutes = emptyList(),
                responseRouterOrigin = RouterOrigin.Onboard
            )

            mockNavigation.requestRoutes(stubRouteOptions)

            assertEquals(stubRouteOptions, optionsCapture.captured)
        }

    @Test
    fun `requestRoutes should return RoutesWithOrigin on success`() =
        coroutineRule.runBlockingTest {
            val routes = emptyList<DirectionsRoute>()
            val routerOrigin = RouterOrigin.Offboard
            givenRequestRoutesResponse(
                requestOptions = slot(),
                responseRoutes = routes,
                responseRouterOrigin = routerOrigin
            )

            val result = mockNavigation.requestRoutes(stubRouteOptions)

            assertNotNull(result)
            assertEquals(routes, result.routes)
            assertEquals(routerOrigin, result.routerOrigin)
        }

    @Test
    fun `requestRoutes should return RequestRoutesError on failure`() =
        coroutineRule.runBlockingTest {
            val reasons = listOf(
                RouterFailure(URL("http://example.com"), RouterOrigin.Offboard, "message")
            )
            val callback = slot<RouterCallback>()
            every {
                mockNavigation.requestRoutes(any(), routesRequestCallback = capture(callback))
            } answers {
                callback.captured.onFailure(reasons, stubRouteOptions)
                0
            }

            try {
                mockNavigation.requestRoutes(stubRouteOptions)
                fail("Expected RequestRoutesError")
            } catch (e: RequestRoutesError) {
                assertEquals(reasons, e.reasons)
            }
        }

    private fun givenRequestRoutesResponse(
        requestOptions: CapturingSlot<RouteOptions>,
        responseRoutes: List<DirectionsRoute>,
        responseRouterOrigin: RouterOrigin,
        requestId: Long = 0
    ) {
        val callback = slot<RouterCallback>()
        every {
            mockNavigation.requestRoutes(
                routeOptions = capture(requestOptions),
                routesRequestCallback = capture(callback)
            )
        } answers {
            callback.captured.onRoutesReady(responseRoutes, responseRouterOrigin)
            requestId
        }
    }
}
