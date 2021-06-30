package com.mapbox.navigation.route.internal.offboard

import android.content.Context
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.route.offboard.base.BaseTest
import com.mapbox.navigation.route.offboard.routerefresh.RouteRefreshCallbackMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapboxOffboardRouterTest : BaseTest() {

    private val mapboxDirections = mockk<MapboxDirections>(relaxed = true)
    private val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
    private val mapboxDirectionsRefresh = mockk<MapboxDirectionsRefresh>(relaxed = true)
    private val mapboxDirectionsRefreshBuilder = mockk<MapboxDirectionsRefresh.Builder>()
    private val context = mockk<Context>()
    private val mockBaseUrl = "https://api.mapbox.test.com"
    private val accessToken = "pk.1234"
    private lateinit var offboardRouter: MapboxOffboardRouter
    private lateinit var routeCallback: Callback<DirectionsResponse>
    private lateinit var refreshCallback: Callback<DirectionsRefreshResponse>
    private val routeOptions: RouteOptions = mockk(relaxed = true)
    private val mockSkuTokenProvider = mockk<UrlSkuTokenProvider>(relaxed = true)
    private val routeCall: Call<DirectionsResponse> = mockk()
    private val refreshCall: Call<DirectionsRefreshResponse> = mockk()

    @Before
    fun setUp() {
        mockkObject(RouteBuilderProvider)
        every {
            mockSkuTokenProvider.obtainUrlWithSkuToken(any())
        } returns (mockk())
        every {
            RouteBuilderProvider.getBuilder(mockSkuTokenProvider)
        } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.interceptor(any()) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.routeOptions(any()) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.eventListener(any()) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.build() } returns mapboxDirections
        val routeListener = slot<Callback<DirectionsResponse>>()
        every { mapboxDirections.enqueueCall(capture(routeListener)) } answers {
            routeCallback = routeListener.captured
        }
        every { routeOptions.coordinatesList().size } returns 2
        every { routeCall.isCanceled } returns false

        // refresh
        mockkObject(RouteRefreshCallbackMapper)
        every { RouteBuilderProvider.getRefreshBuilder() } returns mapboxDirectionsRefreshBuilder
        every {
            mapboxDirectionsRefreshBuilder.baseUrl(any())
        } returns mapboxDirectionsRefreshBuilder
        every {
            mapboxDirectionsRefreshBuilder.accessToken(accessToken)
        } returns mapboxDirectionsRefreshBuilder
        every {
            mapboxDirectionsRefreshBuilder.interceptor(any())
        } returns mapboxDirectionsRefreshBuilder
        every {
            mapboxDirectionsRefreshBuilder.requestId(any())
        } returns mapboxDirectionsRefreshBuilder
        every {
            mapboxDirectionsRefreshBuilder.legIndex(any())
        } returns mapboxDirectionsRefreshBuilder
        every {
            mapboxDirectionsRefreshBuilder.routeIndex(any())
        } returns mapboxDirectionsRefreshBuilder
        every { mapboxDirectionsRefreshBuilder.build() } returns mapboxDirectionsRefresh
        val refreshListener = slot<Callback<DirectionsRefreshResponse>>()
        every { mapboxDirectionsRefresh.enqueueCall(capture(refreshListener)) } answers {
            refreshCallback = refreshListener.captured
        }

        offboardRouter =
            MapboxOffboardRouter(accessToken, context, mockSkuTokenProvider, mockk())

        every { (refreshCall.request() as Request).url } returns "https://test.com".toHttpUrl()
    }

    @After
    fun freeRecourse() {
        unmockkObject(RouteBuilderProvider)
        unmockkObject(RouteRefreshCallbackMapper)
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(offboardRouter)
    }

    @Test
    fun getRoute_NavigationRouteGetRouteCalled() {
        offboardRouter.getRoute(routeOptions, mockk())

        verify { mapboxDirections.enqueueCall(routeCallback) }
    }

    @Test
    fun cancelAll_NavigationRouteCancelCallCalled() {
        offboardRouter.getRoute(routeOptions, mockk())

        offboardRouter.cancelAll()

        verify { mapboxDirections.cancelCall() }
    }

    @Test
    fun `cancel a specific route request when multiple are running`() {
        val newMapboxDirections = mockk<MapboxDirections>(relaxed = true)
        var firstCall = true
        every { mapboxDirectionsBuilder.build() } answers {
            if (firstCall) {
                firstCall = false
                mapboxDirections
            } else {
                newMapboxDirections
            }
        }
        val firstId = offboardRouter.getRoute(routeOptions, mockk())
        val secondId = offboardRouter.getRoute(routeOptions, mockk())
        verify(exactly = 0) { mapboxDirections.cancelCall() }
        verify(exactly = 0) { newMapboxDirections.cancelCall() }

        offboardRouter.cancelRouteRequest(firstId)
        verify(exactly = 1) { mapboxDirections.cancelCall() }
        verify(exactly = 0) { newMapboxDirections.cancelCall() }

        offboardRouter.cancelRouteRequest(secondId)
        verify(exactly = 1) { mapboxDirections.cancelCall() }
        verify(exactly = 1) { newMapboxDirections.cancelCall() }
    }

    @Test
    fun cancel_NavigationRouteCancelCallNotCalled() {
        offboardRouter.cancelAll()

        verify(exactly = 0) { mapboxDirections.cancelCall() }
    }

    @Test
    fun onSuccessfulResponseAndHasRoutes_onRouteReadyCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildRouteResponse(listOf(route), true)
        offboardRouter.getRoute(routeOptions, routerCallback)

        routeCallback.onResponse(routeCall, response)

        verify { routerCallback.onResponse(listOf(route)) }
    }

    @Test
    fun onUnsuccessfulResponseAndHasRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildRouteResponse(listOf(route), false)
        offboardRouter.getRoute(routeOptions, routerCallback)

        routeCallback.onResponse(routeCall, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onSuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = buildRouteResponse(listOf(), true)
        offboardRouter.getRoute(routeOptions, routerCallback)

        routeCallback.onResponse(routeCall, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onUnsuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = buildRouteResponse(listOf(), false)
        offboardRouter.getRoute(routeOptions, routerCallback)

        routeCallback.onResponse(routeCall, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onFailure_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val throwable = mockk<Throwable>()
        offboardRouter.getRoute(routeOptions, routerCallback)

        routeCallback.onFailure(routeCall, throwable)

        verify { routerCallback.onFailure(throwable) }
    }

    @Test
    fun onFailure_canceled_onCanceledIsCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val throwable = mockk<Throwable>()
        offboardRouter.getRoute(routeOptions, routerCallback)

        val call: Call<DirectionsResponse> = mockk()
        every { call.isCanceled } returns true

        routeCallback.onFailure(call, throwable)

        verify { routerCallback.onCanceled() }
    }

    @Test
    fun onSuccess_canceled_onCanceledIsCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildRouteResponse(listOf(route), true)
        offboardRouter.getRoute(routeOptions, routerCallback)

        every { routeCall.isCanceled } returns true

        routeCallback.onResponse(routeCall, response)

        verify { routerCallback.onCanceled() }
    }

    @Test
    fun `route request list cleared on response`() {
        offboardRouter.getRoute(routeOptions, mockk(relaxUnitFun = true))
        val route = buildMultipleLegRoute()
        val response = buildRouteResponse(listOf(route), true)

        routeCallback.onResponse(routeCall, response)
        offboardRouter.cancelAll()

        verify(exactly = 0) { mapboxDirections.cancelCall() }
    }

    @Test
    fun `route request list cleared on failure`() {
        offboardRouter.getRoute(routeOptions, mockk(relaxUnitFun = true))
        val throwable = mockk<Throwable>()

        routeCallback.onFailure(routeCall, throwable)
        offboardRouter.cancelAll()

        verify(exactly = 0) { mapboxDirections.cancelCall() }
    }

    @Test
    fun `route refresh set right params`() {
        val route = mockRouteForRefresh("uuid_123", "1")
        offboardRouter.getRouteRefresh(route, 1, mockk(relaxUnitFun = true))

        verify(exactly = 1) {
            mapboxDirectionsRefreshBuilder.baseUrl(mockBaseUrl)
            mapboxDirectionsRefreshBuilder.accessToken(accessToken)
            mapboxDirectionsRefreshBuilder.requestId("uuid_123")
            mapboxDirectionsRefreshBuilder.routeIndex(1)
            mapboxDirectionsRefreshBuilder.legIndex(1)
        }
    }

    @Test
    fun `route refresh set non-valid route index`() {
        val route = mockRouteForRefresh("uuid_321", "")
        offboardRouter.getRouteRefresh(route, 1, mockk(relaxUnitFun = true))

        verify(exactly = 1) {
            mapboxDirectionsRefreshBuilder.baseUrl(mockBaseUrl)
            mapboxDirectionsRefreshBuilder.accessToken(accessToken)
            mapboxDirectionsRefreshBuilder.requestId("uuid_321")
            mapboxDirectionsRefreshBuilder.routeIndex(0)
            mapboxDirectionsRefreshBuilder.legIndex(1)
        }
    }

    @Test
    fun `route refresh successful`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val annotationRoute = mockAnnotationsForRefresh()
        val resultingRoute = mockk<DirectionsRoute>()
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        val response = buildRefreshResponse(mockk(), true)
        every { response.body() } returns mockk {
            every { route() } returns annotationRoute
        }
        every {
            RouteRefreshCallbackMapper.mapToDirectionsRoute(
                originalRoute,
                annotationRoute
            )
        } returns resultingRoute

        offboardRouter.getRouteRefresh(originalRoute, 0, callback)

        refreshCallback.onResponse(refreshCall, response)

        verify(exactly = 1) { callback.onRefresh(resultingRoute) }
    }

    @Test
    fun `route refresh failure - failed to parse`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val annotationRoute = mockAnnotationsForRefresh()
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        val response = buildRefreshResponse(mockk(), true)
        val ex = mockk<Exception>()
        every { response.body() } returns mockk {
            every { route() } returns annotationRoute
        }
        every {
            RouteRefreshCallbackMapper.mapToDirectionsRoute(
                originalRoute,
                annotationRoute
            )
        }.throws(ex)

        offboardRouter.getRouteRefresh(originalRoute, 0, callback)

        refreshCallback.onResponse(refreshCall, response)

        verify(exactly = 1) {
            callback.onError(
                RouteRefreshError("Failed to read refresh response", ex)
            )
        }
    }

    @Test
    fun `route refresh failure - failed response`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        val slot = slot<RouteRefreshError>()
        val ex = mockk<Exception>()
        every { callback.onError(capture(slot)) } returns Unit

        offboardRouter.getRouteRefresh(originalRoute, 0, callback)
        refreshCallback.onFailure(refreshCall, ex)

        assertEquals(slot.captured.throwable, ex)
    }

    @Test
    fun `route refresh failure - unsuccessful response`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val annotationRoute = mockAnnotationsForRefresh()
        val resultingRoute = mockk<DirectionsRoute>()
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        val response = buildRefreshResponse(mockk(), false)
        every { response.body() } returns mockk {
            every { route() } returns annotationRoute
        }
        every {
            RouteRefreshCallbackMapper.mapToDirectionsRoute(
                originalRoute,
                annotationRoute
            )
        } returns resultingRoute

        offboardRouter.getRouteRefresh(originalRoute, 0, callback)

        refreshCallback.onResponse(refreshCall, response)

        verify(exactly = 1) { callback.onError(any()) }
    }

    @Test
    fun `cancel all refresh calls`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        offboardRouter.getRouteRefresh(originalRoute, 0, mockk())

        offboardRouter.cancelAll()

        verify { mapboxDirectionsRefresh.cancelCall() }
    }

    @Test
    fun `cancel a specific refresh request when multiple are running`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val newMapboxDirectionsRefresh = mockk<MapboxDirectionsRefresh>(relaxed = true)
        var firstCall = true
        every { mapboxDirectionsRefreshBuilder.build() } answers {
            if (firstCall) {
                firstCall = false
                mapboxDirectionsRefresh
            } else {
                newMapboxDirectionsRefresh
            }
        }
        val firstId = offboardRouter.getRouteRefresh(originalRoute, 0, mockk())
        val secondId = offboardRouter.getRouteRefresh(originalRoute, 0, mockk())
        verify(exactly = 0) { mapboxDirectionsRefresh.cancelCall() }
        verify(exactly = 0) { newMapboxDirectionsRefresh.cancelCall() }

        offboardRouter.cancelRouteRefreshRequest(firstId)
        verify(exactly = 1) { mapboxDirectionsRefresh.cancelCall() }
        verify(exactly = 0) { newMapboxDirectionsRefresh.cancelCall() }

        offboardRouter.cancelRouteRefreshRequest(secondId)
        verify(exactly = 1) { mapboxDirectionsRefresh.cancelCall() }
        verify(exactly = 1) { newMapboxDirectionsRefresh.cancelCall() }
    }

    @Test
    fun `refresh request list cleared on response`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val annotationRoute = mockAnnotationsForRefresh()
        val resultingRoute = mockk<DirectionsRoute>()
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        val response = buildRefreshResponse(mockk(), true)
        every { response.body() } returns mockk {
            every { route() } returns annotationRoute
        }
        every {
            RouteRefreshCallbackMapper.mapToDirectionsRoute(
                originalRoute,
                annotationRoute
            )
        } returns resultingRoute

        offboardRouter.getRouteRefresh(originalRoute, 0, callback)

        refreshCallback.onResponse(refreshCall, response)

        offboardRouter.cancelAll()

        verify(exactly = 0) { mapboxDirectionsRefresh.cancelCall() }
    }

    @Test
    fun `refresh request list cleared on failure`() {
        val originalRoute = mockRouteForRefresh("uuid_123", "1")
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        val ex = mockk<Exception>()

        offboardRouter.getRouteRefresh(originalRoute, 0, callback)

        refreshCallback.onFailure(refreshCall, ex)

        offboardRouter.cancelAll()

        verify(exactly = 0) { mapboxDirectionsRefresh.cancelCall() }
    }

    private fun buildRouteResponse(
        routeList: List<DirectionsRoute>,
        isSuccessful: Boolean
    ): Response<DirectionsResponse> {
        val response = mockk<Response<DirectionsResponse>>()
        val directionsResponse = mockk<DirectionsResponse>()
        every { directionsResponse.routes() } returns routeList
        every { response.body() } returns directionsResponse
        every { response.isSuccessful } returns isSuccessful
        every { response.message() } returns "mock"

        return response
    }

    private fun buildRefreshResponse(
        route: DirectionsRouteRefresh,
        isSuccessful: Boolean
    ): Response<DirectionsRefreshResponse> {
        val response = mockk<Response<DirectionsRefreshResponse>>()
        val refreshResponse = mockk<DirectionsRefreshResponse>()
        every { refreshResponse.route() } returns route
        every { response.body() } returns refreshResponse
        every { response.isSuccessful } returns isSuccessful
        every { response.message() } returns "mock"
        every { response.code() } returns 123
        every { response.errorBody() } returns mockk()

        return response
    }

    private fun mockRouteForRefresh(
        mockRequestUuid: String,
        mockRouteIndex: String
    ): DirectionsRoute = mockk {
        every { routeOptions() } returns mockk {
            every { baseUrl() } returns mockBaseUrl
            every { requestUuid() } returns mockRequestUuid
        }
        every { routeIndex() } returns mockRouteIndex
    }

    private fun mockAnnotationsForRefresh(): DirectionsRouteRefresh = mockk()
}
