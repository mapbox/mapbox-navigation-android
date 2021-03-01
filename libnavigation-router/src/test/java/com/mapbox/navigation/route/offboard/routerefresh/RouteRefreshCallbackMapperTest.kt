package com.mapbox.navigation.route.offboard.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteRefreshCallbackMapperTest {

    @Test
    fun testResponse() {
        val originalRoute = originalRoute()
        val refreshRoute = refreshRoute()
        val result = RouteRefreshCallbackMapper.mapToDirectionsRoute(
            originalRoute,
            refreshRoute
        )

        for (i in 0..1) {
            assertEquals(
                refreshRoute.legs()!![i].annotation(),
                result!!.legs()!![i].annotation()
            )
        }
    }

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
                                .duration(listOf(4.0, 5.0, 6.0))
                                .maxspeed(listOf(mockk(), mockk(), mockk()))
                                .speed(listOf(7.0, 8.0, 9.0))
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
