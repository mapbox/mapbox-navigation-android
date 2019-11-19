package com.mapbox.navigation.route.offboard.callback

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.base.BaseTest
import com.mapbox.navigation.route.offboard.extension.mapToRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class RouteRetrieveCallbackTest : BaseTest() {

    @Test
    fun onSuccessfulResponseAndHasRoutes_onRouteReadyCalled() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = mockk<Response<DirectionsResponse>>()
        val route = buildMultipleLegRoute()
        val callback = buildRouteRetrieveCallback(
            routerCallback,
            response,
            listOf(route),
            true
        )
        val call = mockk<Call<DirectionsResponse>>()

        callback.onResponse(call, response)

        verify { routerCallback.onRouteReady(listOf(route.mapToRoute())) }
    }

    @Test
    fun onUnsuccessfulResponseAndHasRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = mockk<Response<DirectionsResponse>>()
        val route = buildMultipleLegRoute()
        val callback = buildRouteRetrieveCallback(
            routerCallback,
            response,
            listOf(route),
            false
        )
        val call = mockk<Call<DirectionsResponse>>()

        callback.onResponse(call, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onSuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = mockk<Response<DirectionsResponse>>()
        val callback = buildRouteRetrieveCallback(
            routerCallback,
            response,
            listOf(),
            true
        )
        val call = mockk<Call<DirectionsResponse>>()

        callback.onResponse(call, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onUnsuccessfulResponseAndNoRoutes_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val response = mockk<Response<DirectionsResponse>>()
        val callback = buildRouteRetrieveCallback(
            routerCallback,
            response,
            listOf(),
            false
        )
        val call = mockk<Call<DirectionsResponse>>()

        callback.onResponse(call, response)

        verify { routerCallback.onFailure(any()) }
    }

    @Test
    fun onFailure_errorIsProvided() {
        val routerCallback = mockk<Router.Callback>(relaxed = true)
        val callback = RouteRetrieveCallback(routerCallback)
        val call = mockk<Call<DirectionsResponse>>()
        val throwable = mockk<Throwable>()

        callback.onFailure(call, throwable)

        verify { routerCallback.onFailure(any()) }
    }

    private fun buildRouteRetrieveCallback(
        routerCallback: Router.Callback,
        response: Response<DirectionsResponse>,
        routeList: List<DirectionsRoute>,
        isSuccessful: Boolean
    ): RouteRetrieveCallback {
        val callback = RouteRetrieveCallback(routerCallback)
        val directionsResponse = mockk<DirectionsResponse>()
        every { directionsResponse.routes() } returns routeList
        every { response.body() } returns directionsResponse
        every { response.isSuccessful } returns isSuccessful

        return callback
    }
}
