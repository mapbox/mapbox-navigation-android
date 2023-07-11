package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavigationRouteWaypointsTest(
    private val waypointsPerRoute: Boolean?,
    private val responseWaypoints: List<DirectionsWaypoint>?,
    private val routeWaypoints: List<DirectionsWaypoint>?,
    private val expectedWaypoints: List<DirectionsWaypoint>?
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> {
            val filledWaypoints1 = listOf(
                DirectionsWaypoint.builder()
                    .name("name1")
                    .rawLocation(doubleArrayOf(1.1, 2.2))
                    .build()
            )
            val filledWaypoints2 = listOf(
                DirectionsWaypoint.builder()
                    .name("name2")
                    .rawLocation(doubleArrayOf(1.1, 2.2))
                    .build()
            )
            return listOf(
                // #0
                arrayOf(null, null, null, null),
                arrayOf(false, null, null, null),
                arrayOf(true, null, null, null),
                // #3
                arrayOf(null, null, emptyList<DirectionsWaypoint>(), null),
                arrayOf(false, null, emptyList<DirectionsWaypoint>(), null),
                arrayOf(
                    true,
                    null,
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>()
                ),
                // #6
                arrayOf(null, null, filledWaypoints1, null),
                arrayOf(false, null, filledWaypoints1, null),
                arrayOf(true, null, filledWaypoints1, filledWaypoints1),
                // #9
                arrayOf(
                    null,
                    emptyList<DirectionsWaypoint>(),
                    null,
                    emptyList<DirectionsWaypoint>()
                ),
                arrayOf(
                    false,
                    emptyList<DirectionsWaypoint>(),
                    null,
                    emptyList<DirectionsWaypoint>()
                ),

                arrayOf(
                    true,
                    emptyList<DirectionsWaypoint>(),
                    null,
                    // TODO: make expected result null when NN-731 is fixed
                    emptyList<DirectionsWaypoint>()
                ),
                // #12
                arrayOf(
                    null,
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>()
                ),
                arrayOf(
                    false,
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>()
                ),
                arrayOf(
                    true,
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>()
                ),
                // #15
                arrayOf(
                    null,
                    emptyList<DirectionsWaypoint>(),
                    filledWaypoints1,
                    emptyList<DirectionsWaypoint>()
                ),
                arrayOf(
                    false,
                    emptyList<DirectionsWaypoint>(),
                    filledWaypoints1,
                    emptyList<DirectionsWaypoint>()
                ),
                arrayOf(true, emptyList<DirectionsWaypoint>(), filledWaypoints1, filledWaypoints1),
                // #18
                arrayOf(null, filledWaypoints2, null, filledWaypoints2),
                arrayOf(false, filledWaypoints2, null, filledWaypoints2),
                arrayOf(
                    true,
                    filledWaypoints2,
                    null,
                    // TODO: make expected result null when NN-731 is fixed
                    filledWaypoints2
                ),
                // #21
                arrayOf(null, filledWaypoints2, emptyList<DirectionsWaypoint>(), filledWaypoints2),
                arrayOf(false, filledWaypoints2, emptyList<DirectionsWaypoint>(), filledWaypoints2),
                arrayOf(
                    true,
                    filledWaypoints2,
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>()
                ),
                // #24
                arrayOf(null, filledWaypoints2, filledWaypoints1, filledWaypoints2),
                arrayOf(false, filledWaypoints2, filledWaypoints1, filledWaypoints2),
                arrayOf(true, filledWaypoints2, filledWaypoints1, filledWaypoints1),
            )
        }
    }

    @Test
    fun waypoints() {
        val route = NavigationRoute(
            DirectionsResponse.builder()
                .routes(
                    listOf(
                        DirectionsRoute.builder()
                            .distance(1.0)
                            .duration(2.9)
                            .waypoints(routeWaypoints)
                            .build()
                    )
                )
                .waypoints(responseWaypoints)
                .code("Ok")
                .build(),
            0,
            RouteOptions.builder()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 2.2),
                        Point.fromLngLat(3.3, 4.4)
                    )
                )
                .waypointsPerRoute(waypointsPerRoute)
                .build(),
            mockk(relaxed = true),
            null
        )
        assertEquals(expectedWaypoints, route.waypoints)
    }
}
