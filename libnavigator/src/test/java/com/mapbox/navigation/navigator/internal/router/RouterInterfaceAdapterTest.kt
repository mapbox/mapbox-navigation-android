package com.mapbox.navigation.navigator.internal.router

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouter
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterRefreshCallback
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

class RouterInterfaceAdapterTest {

    private lateinit var mockRouter: NavigationRouter

    private companion object {
        private const val VALID_URL = "https://mapbox.com"
    }

    @Before
    fun setUp() {
        mockRouter = mockk()

        mockkStatic(RouteOptions::class)
        mockkStatic(DirectionsRoute::class)
    }

    @After
    fun cleanUp() {
        unmockkStatic(RouteOptions::class)
        unmockkStatic(DirectionsRoute::class)
    }

    @Test
    fun routeRequestSuccess() {
        val routerInterface = provideRouteInterfaceDelegate(mockRouter)
        val routerCallback = slot<NavigationRouterCallback>()
        val requestId = 5L
        every { RouteOptions.fromUrl(any()) } returns mockk()
        every { mockRouter.getRoute(any(), capture(routerCallback)) } returns requestId
        val (nativeRouterCallback, slotNativeRouterCallback, slotNativeRouterOrigin) =
            provideNativeRouteCallbackWithSlots()
        val expectedNavigationRoute = provideNavigationRoute()

        val receivedRequestId = routerInterface.getRoute(VALID_URL, mockk(), nativeRouterCallback)
        routerCallback.captured.onRoutesReady(
            listOf(expectedNavigationRoute),
            RouterOrigin.Onboard,
        )

        assertNotNull(slotNativeRouterCallback.captured)
        assertTrue(slotNativeRouterCallback.captured.isValue)
        val directionsResponse =
            DirectionsResponse.fromJson(slotNativeRouterCallback.captured.value!!)
        assertEquals(expectedNavigationRoute.directionsRoute, directionsResponse.routes().first())
        assertEquals(com.mapbox.navigator.RouterOrigin.ONBOARD, slotNativeRouterOrigin.captured)
        assertEquals(requestId, receivedRequestId)
    }

    @Test
    fun routeRequestSuccessEmptyList() {
        val routerInterface = provideRouteInterfaceDelegate(mockRouter)
        val routerCallback = slot<NavigationRouterCallback>()
        val requestId = 5L
        every { RouteOptions.fromUrl(any()) } returns mockk()
        every { mockRouter.getRoute(any(), capture(routerCallback)) } returns requestId
        val (nativeRouterCallback, slotNativeRouterCallback, slotNativeRouterOrigin) =
            provideNativeRouteCallbackWithSlots()

        val receivedRequestId = routerInterface.getRoute(VALID_URL, mockk(), nativeRouterCallback)
        routerCallback.captured.onRoutesReady(
            emptyList(),
            RouterOrigin.Offboard,
        )

        assertNotNull(slotNativeRouterCallback.captured)
        assertTrue(slotNativeRouterCallback.captured.isError)
        with(slotNativeRouterCallback.captured.error!!) {
            assertEquals(RouterErrorType.UNKNOWN, type)
        }
        assertEquals(com.mapbox.navigator.RouterOrigin.ONLINE, slotNativeRouterOrigin.captured)
        assertEquals(requestId, receivedRequestId)
    }

    @Test
    fun routeRequestFailure() {
        val routerInterface = provideRouteInterfaceDelegate(mockRouter)
        val routerCallback = slot<NavigationRouterCallback>()
        val requestId = 5L
        every { RouteOptions.fromUrl(any()) } returns mockk()
        every { mockRouter.getRoute(any(), capture(routerCallback)) } returns requestId
        val (nativeRouterCallback, slotNativeRouterCallback, slotNativeRouterOrigin) =
            provideNativeRouteCallbackWithSlots()
        val routeFailureList = listOf(
            RouterFailure(
                URL("https:://any.url"),
                RouterOrigin.Onboard,
                "Message failure",
                400,
            )
        )

        val receivedRequestId = routerInterface.getRoute(VALID_URL, mockk(), nativeRouterCallback)
        routerCallback.captured.onFailure(
            routeFailureList,
            mockk()
        )

        assertNotNull(slotNativeRouterCallback.captured)
        assertTrue(slotNativeRouterCallback.captured.isError)
        with(slotNativeRouterCallback.captured.error!!) {
            assertEquals(requestId, receivedRequestId)
            assertEquals(requestId, this.requestId)
            assertEquals("Message failure", message)
            assertEquals(400, code)
            assertEquals(RouterErrorType.UNKNOWN, type)
            assertEquals(
                com.mapbox.navigator.RouterOrigin.ONBOARD,
                slotNativeRouterOrigin.captured
            )
            assertNull(refreshTtl)
        }
    }

    @Test
    fun routeRequestCanceled() {
        val routerInterface = provideRouteInterfaceDelegate(mockRouter)
        val routerCallback = slot<NavigationRouterCallback>()
        val requestId = 5L
        every { RouteOptions.fromUrl(any()) } returns mockk()
        every { mockRouter.getRoute(any(), capture(routerCallback)) } returns requestId
        val (nativeRouterCallback, slotNativeRouterCallback, slotNativeRouterOrigin) =
            provideNativeRouteCallbackWithSlots()

        val receivedRequestId = routerInterface.getRoute(VALID_URL, mockk(), nativeRouterCallback)
        routerCallback.captured.onCanceled(
            mockk(),
            RouterOrigin.Offboard,
        )

        assertNotNull(slotNativeRouterCallback.captured)
        assertTrue(slotNativeRouterCallback.captured.isError)
        with(slotNativeRouterCallback.captured.error!!) {
            assertEquals(requestId, receivedRequestId)
            assertEquals(requestId, this.requestId)
            assertEquals(RouterErrorType.REQUEST_CANCELLED, type)
            assertEquals(RouterInterfaceAdapter.ROUTE_REFRESH_FAILED_DEFAULT_CODE, code)
        }
    }

    @Test
    fun routeRefreshSuccess() {
        val originalNavigationRoute = provideNavigationRoute(
            _directionsResponse = provideDirectionsResponse(
                uuid = "refresh_uuid"
            ),
            _routeIndex = 0
        )
        val routerInterface = provideRouteInterfaceDelegate(mockRouter) {
            listOf(originalNavigationRoute)
        }
        val routerRefreshCallback = slot<NavigationRouterRefreshCallback>()
        val requestId = 5L
        every { RouteOptions.fromUrl(any()) } returns mockk()
        every {
            mockRouter.getRouteRefresh(any(), any(), capture(routerRefreshCallback))
        } returns requestId
        val (
            nativeRouterRefreshCallback, slotNativeRouterRefreshCallback, slotNativeRouterOrigin
        ) = provideNativeRouteRefreshCallbackWithSlots()
        val mockRouteOptions = mockk<com.mapbox.navigator.RouteRefreshOptions> {
            every { legIndex } returns 0
            every { getRequestId() } returns "refresh_uuid"
            every { routeIndex } returns 0
        }
        every {
            DirectionsRoute.fromJson(any())
        } returns provideDirectionsResponse().routes().first()
        val navigationRoute = provideNavigationRoute()

        routerInterface
            .getRouteRefresh(mockRouteOptions, nativeRouterRefreshCallback)
        routerRefreshCallback.captured.onRefreshReady(navigationRoute)

        assertNotNull(slotNativeRouterRefreshCallback.captured)
        assertTrue(slotNativeRouterRefreshCallback.captured.isValue)
        with(slotNativeRouterRefreshCallback.captured.value!!) {
            assertEquals(navigationRoute.directionsResponse.toJson(), this)
        }
        assertEquals(com.mapbox.navigator.RouterOrigin.CUSTOM, slotNativeRouterOrigin.captured)
    }

    @Test
    fun routeRefreshFailure() {
        val originalNavigationRoute = provideNavigationRoute(
            _directionsResponse = provideDirectionsResponse(
                uuid = "refresh_uuid"
            ),
            _routeIndex = 0
        )
        val routerInterface = provideRouteInterfaceDelegate(mockRouter) {
            listOf(originalNavigationRoute)
        }
        val routerRefreshCallback = slot<NavigationRouterRefreshCallback>()
        val requestId = 5L
        every { RouteOptions.fromUrl(any()) } returns mockk()
        every {
            mockRouter.getRouteRefresh(any(), any(), capture(routerRefreshCallback))
        } returns requestId
        val (
            nativeRouterRefreshCallback, slotNativeRouterRefreshCallback, slotNativeRouterOrigin
        ) = provideNativeRouteRefreshCallbackWithSlots()
        val mockRouteOptions = mockk<com.mapbox.navigator.RouteRefreshOptions> {
            every { legIndex } returns 0
            every { getRequestId() } returns "refresh_uuid"
            every { routeIndex } returns 0
        }
        every {
            DirectionsRoute.fromJson(any())
        } returns provideDirectionsResponse().routes().first()

        val receivedRequestId = routerInterface
            .getRouteRefresh(mockRouteOptions, nativeRouterRefreshCallback)
        routerRefreshCallback.captured.onFailure(
            mockk(relaxed = true) { every { refreshTtl } returns 10 }
        )

        assertNotNull(slotNativeRouterRefreshCallback.captured)
        assertTrue(slotNativeRouterRefreshCallback.captured.isError)
        with(slotNativeRouterRefreshCallback.captured.error!!) {
            assertEquals(requestId, receivedRequestId)
            assertEquals(requestId, this.requestId)
            assertEquals(RouterInterfaceAdapter.ROUTE_REFRESH_FAILED_DEFAULT_CODE, this.code)
            assertEquals(RouterErrorType.UNKNOWN, this.type)
            assertEquals(10, this.refreshTtl)
        }
        assertEquals(com.mapbox.navigator.RouterOrigin.CUSTOM, slotNativeRouterOrigin.captured)
    }

    private fun provideNativeRouteCallbackWithSlots(
        nativeRouterCallback: com.mapbox.navigator.RouterCallback = mockk(),
        slotNativeRouterCallback: CapturingSlot<Expected<RouterError, String>> = slot(),
        slotNativeRouterOrigin: CapturingSlot<com.mapbox.navigator.RouterOrigin> = slot(),
    ): Triple<com.mapbox.navigator.RouterCallback,
        CapturingSlot<Expected<RouterError, String>>,
        CapturingSlot<com.mapbox.navigator.RouterOrigin>> {
        every {
            nativeRouterCallback.run(
                capture(slotNativeRouterCallback),
                capture(slotNativeRouterOrigin),
            )
        } just Runs
        return Triple(nativeRouterCallback, slotNativeRouterCallback, slotNativeRouterOrigin)
    }

    private fun provideNativeRouteRefreshCallbackWithSlots(
        nativeRouteRefreshCallback: RouterRefreshCallback = mockk(),
        slotNativeRouterRefreshCallback: CapturingSlot<Expected<RouterError, String>> = slot(),
        slotNativeRouterOrigin: CapturingSlot<com.mapbox.navigator.RouterOrigin> = slot(),
    ): Triple<RouterRefreshCallback,
        CapturingSlot<Expected<RouterError, String>>,
        CapturingSlot<com.mapbox.navigator.RouterOrigin>> {
        every {
            nativeRouteRefreshCallback.run(
                capture(slotNativeRouterRefreshCallback),
                capture(slotNativeRouterOrigin),
                hashMapOf(),
            )
        } just Runs
        return Triple(
            nativeRouteRefreshCallback,
            slotNativeRouterRefreshCallback,
            slotNativeRouterOrigin
        )
    }

    private fun provideNavigationRoute(
        _directionsResponse: DirectionsResponse = provideDirectionsResponse(),
        _routeOptions: RouteOptions = provideRouteOptions(),
        _routeIndex: Int = 0,
    ): NavigationRoute {
        return mockk {
            every { directionsResponse } returns _directionsResponse
            every { routeOptions } returns _routeOptions
            every { routeIndex } returns _routeIndex
            every { directionsRoute } returns _directionsResponse.routes()[_routeIndex]
        }
    }

    private fun provideDirectionsResponse(
        routeOptions: RouteOptions = provideRouteOptions(),
        uuid: String = "uuid",
    ): DirectionsResponse =
        DirectionsResponse.builder()
            .code("Ok")
            .uuid(uuid)
            .routes(
                listOf(
                    DirectionsRoute.builder()
                        .routeIndex("0")
                        .distance(10.0)
                        .duration(20.0)
                        .routeOptions(routeOptions)
                        .build()
                )
            )
            .build()

    private fun provideRouteOptions(): RouteOptions =
        RouteOptions.builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .coordinatesList(
                listOf(
                    Point.fromLngLat(0.0, 0.0),
                    Point.fromLngLat(1.0, 1.0),
                )
            )
            .build()

    private fun provideRouteInterfaceDelegate(
        router: NavigationRouter,
        getCurrentRoutesFun: () -> List<NavigationRoute> = { listOf(provideNavigationRoute()) },
    ): RouterInterfaceAdapter =
        RouterInterfaceAdapter(router, getCurrentRoutesFun)
}
