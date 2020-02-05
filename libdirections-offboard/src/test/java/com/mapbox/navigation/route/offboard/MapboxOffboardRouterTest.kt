package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.accounts.SkuTokenProvider
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.base.BaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapboxOffboardRouterTest : BaseTest() {

    private val mapboxDirections = mockk<MapboxDirections>(relaxed = true)
    private val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)
    private val context = mockk<Context>()
    private val accessToken = "pk.1234"
    private lateinit var offboardRouter: MapboxOffboardRouter
    private lateinit var callback: Callback<DirectionsResponse>
    private val routeOptions: RouteOptions = mockk(relaxed = true)
    private val mockSkuTokenProvider = mockk<SkuTokenProvider>(relaxed = true)
    private val call: Call<DirectionsResponse> = mockk()

    @Before
    fun setUp() {
        val listener = slot<Callback<DirectionsResponse>>()

        mockkObject(RouteBuilderProvider)
        every { mockSkuTokenProvider.obtainUrlWithSkuToken("/mock", 1) } returns ("/mock&sku=102jaksdhfj")
        every { RouteBuilderProvider.getBuilder(accessToken, context, mockSkuTokenProvider) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.interceptor(any()) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.build() } returns mapboxDirections
        every { mapboxDirections.enqueueCall(capture(listener)) } answers {
            callback = listener.captured
        }
        every { routeOptions.coordinates().size } returns 2
        every { call.isCanceled } returns false
        offboardRouter = MapboxOffboardRouter(accessToken, context, mockSkuTokenProvider)
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(offboardRouter)
    }

    @Test
    fun getRoute_NavigationRouteGetRouteCalled() {
        getRoute(mockk())

        verify { mapboxDirections.enqueueCall(callback) }
    }

    @Test
    fun cancel_NavigationRouteCancelCallCalled() {
        getRoute(mockk())

        offboardRouter.cancel()

        verify { mapboxDirections.cancelCall() }
    }

    @Test
    fun cancel_NavigationRouteCancelCallNotCalled() {
        offboardRouter.cancel()

        verify(exactly = 0) { mapboxDirections.cancelCall() }
    }

    @Test
    fun onSuccessfulResponseAndHasRoutes_onRouteReadyCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildResponse(listOf(route), true)
        getRoute(routerCallback)

        callback.onResponse(call, response)

        verify { routerCallback.onResponse(any()) }
    }

    @Test
    fun onUnsuccessfulResponseAndHasRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildResponse(listOf(route), false)
        getRoute(routerCallback)

        callback.onResponse(call, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onSuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = buildResponse(listOf(), true)
        getRoute(routerCallback)

        callback.onResponse(call, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onUnsuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = buildResponse(listOf(), false)
        getRoute(routerCallback)

        callback.onResponse(call, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onFailure_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val throwable = mockk<Throwable>()
        getRoute(routerCallback)

        callback.onFailure(call, throwable)

        verify { routerCallback.onFailure(throwable) }
    }

    @Test
    fun onFailure_canceled_onCanceledIsCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val throwable = mockk<Throwable>()
        getRoute(routerCallback)

        val call: Call<DirectionsResponse> = mockk()
        every { call.isCanceled } returns true

        callback.onFailure(call, throwable)

        verify { routerCallback.onCanceled() }
    }

    @Test
    fun onSuccess_canceled_onCanceledIsCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildResponse(listOf(route), true)
        getRoute(routerCallback)

        every { call.isCanceled } returns true

        callback.onResponse(call, response)

        verify { routerCallback.onCanceled() }
    }

    private fun getRoute(routerCallback: Router.Callback) {
        offboardRouter.getRoute(routeOptions, routerCallback)
    }

    private fun buildResponse(
        routeList: List<DirectionsRoute>,
        isSuccessful: Boolean
    ): Response<DirectionsResponse> {
        val response = mockk<Response<DirectionsResponse>>()
        val directionsResponse = mockk<DirectionsResponse>()
        every { directionsResponse.routes() } returns routeList
        every { response.body() } returns directionsResponse
        every { response.isSuccessful } returns isSuccessful

        return response
    }
}
