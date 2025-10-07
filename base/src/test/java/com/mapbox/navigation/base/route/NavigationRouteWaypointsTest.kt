package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.NativeRouteParserRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavigationRouteWaypointsTest(
    private val responseWaypoints: List<DirectionsWaypoint>?,
    private val routeWaypoints: List<DirectionsWaypoint>?,
    private val expectedWaypoints: List<DirectionsWaypoint>?,
) {

    @get:Rule
    val routeParserRule = NativeRouteParserRule()

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any?>> {
            val filledWaypoints1 = listOf(
                DirectionsWaypoint.builder()
                    .name("name1")
                    .rawLocation(doubleArrayOf(1.1, 2.2))
                    .build(),
            )
            val filledWaypoints2 = listOf(
                DirectionsWaypoint.builder()
                    .name("name2")
                    .rawLocation(doubleArrayOf(1.1, 2.2))
                    .build(),
            )
            return listOf(
                // response null, route null -> null
                arrayOf(null, null, null),
                // response null, route empty -> route has empty waypoints -> result empty (no fallback)
                arrayOf(null, emptyList<DirectionsWaypoint>(), emptyList<DirectionsWaypoint>()),
                // response null, route filled -> route
                arrayOf(null, filledWaypoints1, filledWaypoints1),
                // response empty, route null -> response (empty)
                arrayOf(emptyList<DirectionsWaypoint>(), null, emptyList<DirectionsWaypoint>()),
                // response empty, route empty -> route empty
                arrayOf(
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>(),
                ),
                // response empty, route filled -> route
                arrayOf(emptyList<DirectionsWaypoint>(), filledWaypoints1, filledWaypoints1),
                // response filled, route null -> response
                arrayOf(filledWaypoints2, null, filledWaypoints2),
                // response filled, route empty -> route has empty waypoints -> result empty (no fallback)
                arrayOf(
                    filledWaypoints2,
                    emptyList<DirectionsWaypoint>(),
                    emptyList<DirectionsWaypoint>(),
                ),
                // response filled, route filled -> route
                arrayOf(filledWaypoints2, filledWaypoints1, filledWaypoints1),
            )
        }
    }

    @Test
    fun waypoints() {
        val route = NavigationRoute.create(
            DirectionsResponse.builder()
                .waypoints(responseWaypoints)
                .routes(
                    listOf(
                        DirectionsRoute.builder()
                            .distance(1.0)
                            .duration(2.9)
                            .waypoints(routeWaypoints)
                            .build(),
                    ),
                )
                .code("Ok")
                .build(),
            RouteOptions.builder()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 2.2),
                        Point.fromLngLat(3.3, 4.4),
                    ),
                )
                .build(),
            RouterOrigin.OFFLINE,
        ).first()
        assertEquals(expectedWaypoints, route.waypoints)
    }
}
