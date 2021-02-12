package com.mapbox.navigation.route.offboard.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Response

class RouteRefreshCallbackMapperTest {

    @Test
    fun sanity() {
        RouteRefreshCallbackMapper(mockk(), mockk())
    }

    @Test
    fun testError() {
        val callback = mockCallback()
        val error = slot<RouteRefreshError>()
        val throwable: Throwable = mockk()
        every { callback.onError(capture(error)) } returns Unit
        val routeRefreshCallbackMapper = RouteRefreshCallbackMapper(originalRoute(), callback)

        routeRefreshCallbackMapper.onFailure(mockk(), throwable)

        verify {
            callback.onError(error.captured)
        }
        assertEquals(error.captured.throwable, throwable)
    }

    @Test
    fun testResponse() {
        val callback = mockCallback()
        val directionsRoute = slot<DirectionsRoute>()
        every { callback.onRefresh(capture(directionsRoute)) } returns Unit
        val routeRefreshCallbackMapper = RouteRefreshCallbackMapper(originalRoute(), callback)
        val responseMock = mockResponse(refreshRoute())

        routeRefreshCallbackMapper.onResponse(mockk(), responseMock)
        val captured = directionsRoute.captured

        verify(exactly = 1) { callback.onRefresh(captured) }
        assertNotNull(captured)
        for (i in 0..1) {
            assertEquals(
                refreshRoute().legs()!![i].annotation(),
                captured.legs()!![i].annotation()
            )
        }
    }

    private fun mockResponse(
        directionsRouteRefresh: DirectionsRouteRefresh
    ): Response<DirectionsRefreshResponse> {
        val response: Response<DirectionsRefreshResponse> = mockk()
        every { response.body()?.route() } returns directionsRouteRefresh
        return response
    }

    private fun mockCallback(): RouteRefreshCallback = mockk(relaxUnitFun = true)

    private fun refreshRoute(): DirectionsRouteRefresh =
        DirectionsRouteRefresh.builder()
            .legs(
                listOf(
                    RouteLegRefresh.builder()
                        .annotation(
                            LegAnnotation.builder()
                                .congestion(listOf("congestion5", "congestion10"))
                                .distance(listOf(44.0, 55.0))
                                .build()
                        )
                        .build(),
                    RouteLegRefresh.builder()
                        .annotation(
                            LegAnnotation.builder()
                                .congestion(
                                    listOf(
                                        "congestion333",
                                        "congestion999",
                                        "congestion1000"
                                    )
                                )
                                .distance(listOf(1.0, 2.0, 3.0))
                                .build()
                        )
                        .build()
                )
            )
            .build()

    private fun originalRoute(): DirectionsRoute =
        DirectionsRoute.builder()
            .distance(10.0)
            .duration(20.0)
            .legs(
                listOf(
                    RouteLeg.builder()
                        .annotation(
                            LegAnnotation.builder()
                                .congestion(listOf("congestion1", "congestion2"))
                                .distance(listOf(11.0, 21.0))
                                .build()
                        )
                        .build(),
                    RouteLeg.builder()
                        .annotation(
                            LegAnnotation.builder()
                                .congestion(listOf("congestion11", "congestion22", "congestion33"))
                                .distance(listOf(111.0, 211.0, 311.0))
                                .build()
                        )
                        .build()
                )
            )
            .build()
}
