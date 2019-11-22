package com.mapbox.navigation.route.offboard

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.base.BaseTest
import com.mapbox.navigation.route.offboard.extension.mapToRoute
import com.mapbox.navigation.route.offboard.router.NavigationRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Callback
import retrofit2.Response

class MapboxOffboardRouterTest : BaseTest() {

    companion object {
        private const val ACCESS_TOKEN = "pk.token"
    }

    private val navigationRoute = mockk<NavigationRoute>(relaxed = true)
    private val navigationRouteBuilder = mockk<NavigationRoute.Builder>(relaxed = true)
    private lateinit var offboardRouter: MapboxOffboardRouter
    private lateinit var callback: Callback<DirectionsResponse>
    private val origin = Point.fromLngLat(1.0, 2.0)
    private val waypoints = listOf<Point>()
    private val destination = Point.fromLngLat(1.0, 5.0)

    @Before
    fun setUp() {
        val listener = slot<Callback<DirectionsResponse>>()
        every { navigationRouteBuilder.build() } returns navigationRoute
        every { navigationRoute.getRoute(capture(listener)) } answers {
            callback = listener.captured
        }
        offboardRouter = MapboxOffboardRouter(mockk(), ACCESS_TOKEN) { navigationRouteBuilder }
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(offboardRouter)
    }

    @Test
    fun getRoute_NavigationRouteGetRouteCalled() {
        getRoute(mockk())

        verify { navigationRoute.getRoute(callback) }
    }

    @Test
    fun cancel_NavigationRouteCancelCallCalled() {
        getRoute(mockk())

        offboardRouter.cancel()

        verify { navigationRoute.cancelCall() }
    }

    @Test
    fun cancel_NavigationRouteCancelCallNotCalled() {
        offboardRouter.cancel()

        verify(exactly = 0) { navigationRoute.cancelCall() }
    }

    @Test
    fun onSuccessfulResponseAndHasRoutes_onRouteReadyCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildResponse(listOf(route), true)
        getRoute(routerCallback)

        callback.onResponse(mockk(), response)

        verify { routerCallback.onRouteReady(listOf(route.mapToRoute())) }
    }

    @Test
    fun onUnsuccessfulResponseAndHasRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val route = buildMultipleLegRoute()
        val response = buildResponse(listOf(route), false)
        getRoute(routerCallback)

        callback.onResponse(mockk(), response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onSuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = buildResponse(listOf(), true)
        getRoute(routerCallback)

        callback.onResponse(mockk(), response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onUnsuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = buildResponse(listOf(), false)
        getRoute(routerCallback)

        callback.onResponse(mockk(), response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onFailure_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val throwable = mockk<Throwable>()
        getRoute(routerCallback)

        callback.onFailure(mockk(), throwable)

        verify { routerCallback.onFailure(throwable) }
    }

    private fun getRoute(routerCallback: Router.Callback) {
        offboardRouter.getRoute(
            origin,
            waypoints,
            destination,
            routerCallback
        )
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
