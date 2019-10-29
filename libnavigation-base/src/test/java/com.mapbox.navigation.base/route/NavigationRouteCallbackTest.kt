package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.utils.time.ElapsedTime
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.ArrayList
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NavigationRouteCallbackTest {

    @Test
    fun onResponse_callbackIsCalled() {
        val listener = mockk<NavigationRouteEventListener>()
        val elapsedTime = mockk<ElapsedTime>()
        every { listener.time } returns elapsedTime
        val callback = mockk<Callback<DirectionsResponse>>(relaxed = true)
        val call = mockk<Call<DirectionsResponse>>()
        val uuid = "some_uuid"
        val response = buildMockResponse(uuid)
        val routeCallback =
            NavigationRouteCallback(listener, callback)

        routeCallback.onResponse(call, response)

        verify { callback.onResponse(call, response) }
    }

    // TODO move NavigationTelemetry to base module ?
    // @Test
    // fun onResponse_validResponseSendsEvent() {
    //     mockkObject(NavigationTelemetry)
    //     val listener = mockk<NavigationRouteEventListener>()
    //     val elapsedTime = mockk<ElapsedTime>()
    //     every { listener.time } returns elapsedTime
    //     val callback = mockk<Callback<DirectionsResponse>>(relaxed = true)
    //     val call = mockk<Call<DirectionsResponse>>()
    //     val uuid = "any_uuid"
    //     val response = buildMockResponse(uuid)
    //     val routeCallback =
    //         NavigationRouteCallback(listener, callback)
    //
    //     routeCallback.onResponse(call, response)
    //
    //     verify { NavigationTelemetry.routeRetrievalEvent(eq(elapsedTime), eq(uuid)) }
    // }

    @Test
    fun onFailure_callbackIsCalled() {
        val listener = mockk<NavigationRouteEventListener>()
        val callback = mockk<Callback<DirectionsResponse>>(relaxed = true)
        val call = mockk<Call<DirectionsResponse>>()
        val throwable = mockk<Throwable>()
        val routeCallback =
            NavigationRouteCallback(listener, callback)

        routeCallback.onFailure(call, throwable)

        verify { callback.onFailure(call, throwable) }
    }

    private fun buildMockResponse(uuid: String): Response<DirectionsResponse> {
        val response = mockk<Response<DirectionsResponse>>()
        val directionsResponse = mockk<DirectionsResponse>()
        val routes = ArrayList<DirectionsRoute>()
        routes.add(mockk())
        every { directionsResponse.uuid() } returns uuid
        every { directionsResponse.routes() } returns routes
        every { response.body() } returns directionsResponse
        return response
    }
}
